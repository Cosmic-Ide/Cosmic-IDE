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

import com.google.common.collect.ImmutableMap;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.HttpOptions;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHttpRequest;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Base client for the HTTP APIs. This is for internal use only.
 */
public class HttpApiClient extends ApiClient {

    /**
     * Constructs an ApiClient for Google AI APIs.
     */
    HttpApiClient(Optional<String> apiKey, Optional<HttpOptions> httpOptions) {
        super(apiKey, httpOptions);
    }

    /**
     * Sends a Http request given the http method, path, request json string, and http options.
     */
    @Override
    public HttpApiResponse request(
            String httpMethod,
            String path,
            String requestJson,
            Optional<HttpOptions> requestHttpOptions) {

        HttpOptions mergedHttpOptions = mergeHttpOptions(requestHttpOptions.orElse(null));

        String requestUrl;

        if (mergedHttpOptions.apiVersion().get().isEmpty()) {
            requestUrl = String.format("%s/%s", mergedHttpOptions.baseUrl().get(), path);
        } else {
            requestUrl =
                    String.format(
                            "%s/%s/%s",
                            mergedHttpOptions.baseUrl().get(), mergedHttpOptions.apiVersion().get(), path);
        }

        if (httpMethod.equalsIgnoreCase("POST")) {
            HttpPost httpPost = new HttpPost(requestUrl);
            setHeaders(httpPost, mergedHttpOptions);
            httpPost.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));
            return executeRequest(httpPost);
        } else if (httpMethod.equalsIgnoreCase("GET")) {
            HttpGet httpGet = new HttpGet(requestUrl);
            setHeaders(httpGet, mergedHttpOptions);
            return executeRequest(httpGet);
        } else if (httpMethod.equalsIgnoreCase("DELETE")) {
            HttpDelete httpDelete = new HttpDelete(requestUrl);
            setHeaders(httpDelete, mergedHttpOptions);
            return executeRequest(httpDelete);
        } else if (httpMethod.equalsIgnoreCase("PATCH")) {
            HttpPatch httpPatch = new HttpPatch(requestUrl);
            setHeaders(httpPatch, mergedHttpOptions);
            httpPatch.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));
            return executeRequest(httpPatch);
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + httpMethod);
        }
    }

    @Override
    public ApiResponse request(
            String httpMethod,
            String url,
            byte[] requestBytes,
            Optional<HttpOptions> requestHttpOptions) {
        HttpOptions mergedHttpOptions = mergeHttpOptions(requestHttpOptions.orElse(null));
        if (httpMethod.equalsIgnoreCase("POST")) {
            HttpPost httpPost = new HttpPost(url);
            setHeaders(httpPost, mergedHttpOptions);
            httpPost.setEntity(new ByteArrayEntity(requestBytes, ContentType.APPLICATION_OCTET_STREAM));
            return executeRequest(httpPost);
        } else {
            throw new IllegalArgumentException(
                    "The request method with bytes is only supported for POST. Unsupported HTTP method: "
                            + httpMethod);
        }
    }

    /**
     * Sets the required headers (including auth) on the request object.
     */
    private void setHeaders(BasicHttpRequest request, HttpOptions requestHttpOptions) {
        for (Map.Entry<String, String> header :
                requestHttpOptions.headers().orElse(ImmutableMap.of()).entrySet()) {
            request.setHeader(header.getKey(), header.getValue());
        }

        if (apiKey.isPresent()) {
            request.setHeader("x-goog-api-key", apiKey.get());
        } else {
            throw new GenAiIOException(
                    "API key is required for HTTP requests but not provided.");
        }
    }

    /**
     * Executes the given HTTP request.
     */
    private HttpApiResponse executeRequest(ClassicHttpRequest request) {
        try {
            return new HttpApiResponse(httpClient.execute(request));
        } catch (IOException e) {
            throw new GenAiIOException("Failed to execute HTTP request.", e);
        }
    }
}
