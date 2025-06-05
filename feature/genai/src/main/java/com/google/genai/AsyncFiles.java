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

// Auto-generated code. Do not edit.

package com.google.genai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.DeleteFileConfig;
import com.google.genai.types.DeleteFileResponse;
import com.google.genai.types.DownloadFileConfig;
import com.google.genai.types.File;
import com.google.genai.types.GeneratedVideo;
import com.google.genai.types.GetFileConfig;
import com.google.genai.types.ListFilesConfig;
import com.google.genai.types.UploadFileConfig;
import com.google.genai.types.Video;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Async module of {@link Files}
 */
public final class AsyncFiles {
    Files files;

    public AsyncFiles(ApiClient apiClient) {
        this.files = new Files(apiClient);
    }

    /**
     * Asynchronously retrieves the file information from the service.
     *
     * @param name   - The name identifier for the file to retrieve.
     * @param config - Optional, configuration for the get method.
     * @return A File object representing the file.
     */
    public CompletableFuture<File> get(String name, GetFileConfig config) {
        return CompletableFuture.supplyAsync(() -> files.get(name, config));
    }

    /**
     * Asynchronously deletes a remotely stored file.
     *
     * @param name   - The name identifier for the file to delete.
     * @param config - Optional, configuration for the delete method.
     * @return The DeleteFileResponse, the response for the delete method.
     */
    public CompletableFuture<DeleteFileResponse> delete(String name, DeleteFileConfig config) {
        return CompletableFuture.supplyAsync(() -> files.delete(name, config));
    }

    /**
     * Asynchronously uploads a file to the GenAI API.
     *
     * @param file   The file to upload.
     * @param config The configuration for the upload.
     * @return A future that resolves to the uploaded file.
     */
    public CompletableFuture<File> upload(java.io.File file, UploadFileConfig config) {
        return CompletableFuture.supplyAsync(() -> files.upload(file, config));
    }

    /**
     * Asynchronously uploads a bytes array as a file to the GenAI API.
     *
     * @param bytes  The bytes of the file to upload.
     * @param config The configuration for the upload.
     * @return A future that resolves to the uploaded file.
     */
    public CompletableFuture<File> upload(byte[] bytes, UploadFileConfig config) {
        return CompletableFuture.supplyAsync(() -> files.upload(bytes, config));
    }

    /**
     * Asynchronously uploads a stream as a file to the GenAI API.
     *
     * @param stream The stream of the file to upload.
     * @param size   The size of the file in bytes.
     * @param config The configuration for the upload.
     * @return A future that resolves to the uploaded file.
     */
    public CompletableFuture<File> upload(InputStream stream, long size, UploadFileConfig config) {
        return CompletableFuture.supplyAsync(() -> files.upload(stream, size, config));
    }

    /**
     * Asynchronously uploads a file by its path to the GenAI API.
     *
     * @param filePath The path to the file to upload.
     * @param config   The configuration for the upload.
     * @return A future that resolves to the uploaded file.
     */
    public CompletableFuture<File> upload(String filePath, UploadFileConfig config) {
        return CompletableFuture.supplyAsync(() -> files.upload(filePath, config));
    }

    /**
     * Asynchronously downloads a file from the GenAI API to the provided path.
     *
     * @param fileName     The name of the file to download.
     * @param downloadPath The path to download the file to.
     * @param config       The configuration for the download.
     * @return A future that resolves to the downloaded file.
     */
    public CompletableFuture<Void> download(
            String fileName, String downloadPath, DownloadFileConfig config) {
        return CompletableFuture.runAsync(() -> files.download(fileName, downloadPath, config));
    }

    /**
     * Asynchronously downloads a video from the GenAI API to the provided path.
     *
     * @param video        The video to download.
     * @param downloadPath The path to download the video to.
     * @param config       The configuration for the download.
     * @return A future that resolves to the downloaded video.
     */
    public CompletableFuture<Void> download(
            Video video, String downloadPath, DownloadFileConfig config) {
        return CompletableFuture.runAsync(() -> files.download(video, downloadPath, config));
    }

    /**
     * Asynchronously downloads a generated video from the GenAI API to the provided path.
     *
     * @param video        The generated video to download.
     * @param downloadPath The path to download the video to.
     * @param config       The configuration for the download.
     * @return A future that resolves to the downloaded video.
     */
    public CompletableFuture<Void> download(
            GeneratedVideo video, String downloadPath, DownloadFileConfig config) {
        return CompletableFuture.runAsync(() -> files.download(video, downloadPath, config));
    }

    /**
     * Asynchronously downloads a file from the GenAI API to the provided path.
     *
     * @param file         The file to download.
     * @param downloadPath The path to download the file to.
     * @param config       The configuration for the download.
     * @return A future that resolves to the downloaded file.
     */
    public CompletableFuture<Void> download(
            File file, String downloadPath, DownloadFileConfig config) {
        return CompletableFuture.runAsync(() -> files.download(file, downloadPath, config));
    }

    /**
     * Asynchronously makes an API request to list the available files.
     *
     * @param config A {@link ListFilesConfig} for configuring the list request.
     * @return A CompletableFuture that resolves to a {@link AsyncPager}. The AsyncPager has a
     * `forEach` method that can be used to asynchronously process items in the page and
     * automatically query the next page once the current page is exhausted.
     */
    @SuppressWarnings("PatternMatchingInstanceof")
    public CompletableFuture<AsyncPager<File>> list(ListFilesConfig config) {
        Function<JsonSerializable, CompletableFuture<JsonNode>> request =
                requestConfig -> {
                    if (!(requestConfig instanceof ListFilesConfig)) {
                        throw new GenAiIOException(
                                "Internal error: Pager expected ListFilesConfig but received "
                                        + requestConfig.getClass().getName());
                    }
                    return CompletableFuture.supplyAsync(
                            () ->
                                    JsonSerializable.toJsonNode(files.privateList((ListFilesConfig) requestConfig)));
                };
        return CompletableFuture.supplyAsync(
                () ->
                        new AsyncPager<>(
                                Pager.PagedItem.FILES,
                                request,
                                (ObjectNode) JsonSerializable.toJsonNode(config),
                                request.apply(config)));
    }
}
