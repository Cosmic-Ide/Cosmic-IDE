/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.genai;

import com.google.genai.errors.GenAiIOException;
import com.google.common.collect.ImmutableMap;
import com.google.genai.types.HttpOptions;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Client which handles the upload process for files on the SDK.
 */
final class UploadClient {

    public static final int DEFAULT_CHUNK_SIZE = 8 * 1024 * 1024;
    public static final int MAX_RETRY_COUNT = 3;
    public static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(1);
    public static final int DELAY_MULTIPLIER = 2;
    private final ApiClient apiClient;
    private final int chunkSize;

    public UploadClient(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.chunkSize = DEFAULT_CHUNK_SIZE;
    }

    public UploadClient(ApiClient apiClient, int chunkSize) {
        this.apiClient = apiClient;
        this.chunkSize = chunkSize;
    }

    public HttpEntity upload(String uploadUrl, String filePath) {
        File file = new File(filePath);
        HttpEntity entity;
        try (InputStream inputStream = new FileInputStream(file)) {
            entity = upload(uploadUrl, inputStream, file.length());
        } catch (IOException e) {
            throw new GenAiIOException("Failed to process input stream", e);
        }
        return entity;
    }

    public HttpEntity upload(String uploadUrl, byte[] bytes) {
        HttpEntity entity;
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            entity = upload(uploadUrl, inputStream, bytes.length);
        } catch (IOException e) {
            throw new GenAiIOException("Failed to process input stream", e);
        }
        return entity;
    }

    public HttpEntity upload(String uploadUrl, InputStream inputStream, long size) {
        String uploadCommand = "upload";
        byte[] buffer = new byte[chunkSize];
        int bytesRead;
        int offset = 0;
        try {
            while ((bytesRead = inputStream.read(buffer, 0, chunkSize)) == chunkSize) {
                UploadChunkResponse uploadChunkResponse =
                        uploadChunk(uploadUrl, buffer, offset, uploadCommand);
                String uploadStatus = uploadChunkResponse.getUploadStatus();
                offset += bytesRead;
                if (uploadStatus == null || !uploadStatus.equals("active")) {
                    throw new IllegalStateException(
                            "Unexpected upload status: " + uploadStatus + " please try again.");
                }
            }
        } catch (IOException e) {
            throw new GenAiIOException("Failed to process input stream", e);
        }
        buffer = Arrays.copyOfRange(buffer, 0, bytesRead);
        uploadCommand = uploadCommand + ", finalize";
        UploadChunkResponse uploadChunkResponse = uploadChunk(uploadUrl, buffer, offset, uploadCommand);
        String uploadStatus = uploadChunkResponse.getUploadStatus();
        if (uploadStatus == null || !uploadStatus.equals("final")) {
            throw new IllegalStateException(
                    "Unexpected final upload status: " + uploadStatus + " please try again.");
        }
        return uploadChunkResponse.getEntity();
    }

    private UploadChunkResponse uploadChunk(
            String uploadUrl, byte[] chunk, long offset, String uploadCommand) {
        HttpOptions httpOptions =
                HttpOptions.builder()
                        .headers(
                                ImmutableMap.of(
                                        "X-Goog-Upload-Command",
                                        uploadCommand,
                                        "X-Goog-Upload-Offset",
                                        Long.toString(offset)))
                        .build();

        int retryCount = 0;
        boolean uploadStatusHeaderFound = false;
        String uploadStatus = "";
        ApiResponse response = null;
        while (retryCount < MAX_RETRY_COUNT) {
            response = apiClient.request("POST", uploadUrl, chunk, Optional.of(httpOptions));
            Header[] headers = response.getHeaders();
            for (Header header : headers) {
                if (header.getName().equals("X-Goog-Upload-Status")) {
                    uploadStatusHeaderFound = true;
                    uploadStatus = header.getValue();
                    break;
                }
            }
            if (uploadStatusHeaderFound) {
                break;
            }
            Duration delay =
                    INITIAL_RETRY_DELAY.multipliedBy((long) Math.pow(DELAY_MULTIPLIER, retryCount));
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while sleeping", e);
            }
            retryCount++;
        }

        if (!uploadStatusHeaderFound) {
            throw new IllegalStateException("Upload failed. Retries exhausted, please try again.");
        }
        return new UploadChunkResponse(uploadStatus, response.getEntity());
    }

    private static class UploadChunkResponse {
        private final String uploadStatus;
        private final HttpEntity entity;

        UploadChunkResponse(String uploadStatus, HttpEntity entity) {
            this.uploadStatus = uploadStatus;
            this.entity = entity;
        }

        public String getUploadStatus() {
            return uploadStatus;
        }

        public HttpEntity getEntity() {
            return entity;
        }
    }
}
