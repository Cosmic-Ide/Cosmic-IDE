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
 * A wrapper class for the http response.
 */
@AutoValue
@JsonDeserialize(builder = HttpResponse.Builder.class)
public abstract class HttpResponse extends JsonSerializable {
    /**
     * Instantiates a builder for HttpResponse.
     */
    public static Builder builder() {
        return new AutoValue_HttpResponse.Builder();
    }

    /**
     * Deserializes a JSON string to a HttpResponse object.
     */
    public static HttpResponse fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, HttpResponse.class);
    }

    /**
     * Used to retain the processed HTTP headers in the response.
     */
    @JsonProperty("headers")
    public abstract Optional<Map<String, String>> headers();

    /**
     * The raw HTTP response body, in JSON format.
     */
    @JsonProperty("body")
    public abstract Optional<String> body();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for HttpResponse.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `HttpResponse.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_HttpResponse.Builder();
        }

        @JsonProperty("headers")
        public abstract Builder headers(Map<String, String> headers);

        @JsonProperty("body")
        public abstract Builder body(String body);

        public abstract HttpResponse build();
    }
}
