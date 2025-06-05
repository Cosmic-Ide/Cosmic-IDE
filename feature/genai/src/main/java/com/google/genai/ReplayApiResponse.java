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

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.message.StatusLine;

// TODO(b/369384123): Change the replay API response to use the ReplayFile.

/**
 * Provides a simulated HTTP response from a replay file.
 */
@ExcludeFromGeneratedCoverageReport
final class ReplayApiResponse extends ApiResponse {

    private final HttpEntity entity;
    private final StatusLine statusLine;
    private final Header[] headers;

    public ReplayApiResponse(HttpEntity entity, StatusLine statusLine, Header[] headers) {
        this.entity = entity;
        this.statusLine = statusLine;
        this.headers = headers;
    }

    @Override
    public HttpEntity getEntity() {
        return this.entity;
    }

    @Override
    public Header[] getHeaders() {
        return this.headers;
    }

    @Override
    public void close() {
    }
}
