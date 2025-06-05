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
 * A video generation operation.
 */
@AutoValue
@JsonDeserialize(builder = GenerateVideosOperation.Builder.class)
public abstract class GenerateVideosOperation extends JsonSerializable {
    /**
     * Instantiates a builder for GenerateVideosOperation.
     */
    public static Builder builder() {
        return new AutoValue_GenerateVideosOperation.Builder();
    }

    /**
     * Deserializes a JSON string to a GenerateVideosOperation object.
     */
    public static GenerateVideosOperation fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GenerateVideosOperation.class);
    }

    /**
     * The server-assigned name, which is only unique within the same service that originally returns
     * it. If you use the default HTTP mapping, the `name` should be a resource name ending with
     * `operations/{unique_id}`.
     */
    @JsonProperty("name")
    public abstract Optional<String> name();

    /**
     * Service-specific metadata associated with the operation. It typically contains progress
     * information and common metadata such as create time. Some services might not provide such
     * metadata. Any method that returns a long-running operation should document the metadata type,
     * if any.
     */
    @JsonProperty("metadata")
    public abstract Optional<Map<String, Object>> metadata();

    /**
     * If the value is `false`, it means the operation is still in progress. If `true`, the operation
     * is completed, and either `error` or `response` is available.
     */
    @JsonProperty("done")
    public abstract Optional<Boolean> done();

    /**
     * The error result of the operation in case of failure or cancellation.
     */
    @JsonProperty("error")
    public abstract Optional<Map<String, Object>> error();

    /**
     * The generated videos.
     */
    @JsonProperty("response")
    public abstract Optional<GenerateVideosResponse> response();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GenerateVideosOperation.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GenerateVideosOperation.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GenerateVideosOperation.Builder();
        }

        @JsonProperty("name")
        public abstract Builder name(String name);

        @JsonProperty("metadata")
        public abstract Builder metadata(Map<String, Object> metadata);

        @JsonProperty("done")
        public abstract Builder done(boolean done);

        @JsonProperty("error")
        public abstract Builder error(Map<String, Object> error);

        @JsonProperty("response")
        public abstract Builder response(GenerateVideosResponse response);

        public abstract GenerateVideosOperation build();
    }
}
