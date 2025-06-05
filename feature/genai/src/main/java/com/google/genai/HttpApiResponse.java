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

import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;

import java.io.IOException;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;

/**
 * Wraps a real HTTP response to expose the methods needed by the GenAI SDK.
 */
final class HttpApiResponse extends ApiResponse {

    private final CloseableHttpResponse response;

    /**
     * Constructs a HttpApiResponse instance with the response.
     */
    public HttpApiResponse(CloseableHttpResponse response) {
        this.response = response;
    }

    /**
     * Returns the HttpEntity from the response.
     */
    @Override
    public HttpEntity getEntity() {
        ApiException.throwFromResponse(response);
        return response.getEntity();
    }

    /**
     * Returns all of the headers from the response.
     */
    @Override
    public Header[] getHeaders() {
        return response.getHeaders();
    }

    /**
     * Closes the Http response.
     */
    @Override
    public void close() {
        try {
            response.close();
        } catch (IOException e) {
            throw new GenAiIOException("Failed to close the HTTP response.", e);
        }
    }
}
