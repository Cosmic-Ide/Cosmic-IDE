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

package com.google.genai.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.genai.JsonSerializable;

import java.util.Map;
import java.util.Optional;

/**
 * HTTP options to be used in each of the requests.
 */
@AutoValue
@JsonDeserialize(builder = HttpOptions.Builder.class)
public abstract class HttpOptions extends JsonSerializable {
    /**
     * Instantiates a builder for HttpOptions.
     */
    public static Builder builder() {
        return new AutoValue_HttpOptions.Builder();
    }

    /**
     * Deserializes a JSON string to a HttpOptions object.
     */
    public static HttpOptions fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, HttpOptions.class);
    }

    /**
     * The base URL for the AI platform service endpoint.
     */
    @JsonProperty("baseUrl")
    public abstract Optional<String> baseUrl();

    /**
     * Specifies the version of the API to use.
     */
    @JsonProperty("apiVersion")
    public abstract Optional<String> apiVersion();

    /**
     * Additional HTTP headers to be sent with the request.
     */
    @JsonProperty("headers")
    public abstract Optional<Map<String, String>> headers();

    /**
     * Timeout for the request in milliseconds.
     */
    @JsonProperty("timeout")
    public abstract Optional<Integer> timeout();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for HttpOptions.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `HttpOptions.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_HttpOptions.Builder();
        }

        @JsonProperty("baseUrl")
        public abstract Builder baseUrl(String baseUrl);

        @JsonProperty("apiVersion")
        public abstract Builder apiVersion(String apiVersion);

        @JsonProperty("headers")
        public abstract Builder headers(Map<String, String> headers);

        @JsonProperty("timeout")
        public abstract Builder timeout(Integer timeout);

        public abstract HttpOptions build();
    }
}
