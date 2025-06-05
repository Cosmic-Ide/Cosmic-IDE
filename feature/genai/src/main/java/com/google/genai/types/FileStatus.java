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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Status of a File that uses a common error model.
 */
@AutoValue
@JsonDeserialize(builder = FileStatus.Builder.class)
public abstract class FileStatus extends JsonSerializable {
    /**
     * Instantiates a builder for FileStatus.
     */
    public static Builder builder() {
        return new AutoValue_FileStatus.Builder();
    }

    /**
     * Deserializes a JSON string to a FileStatus object.
     */
    public static FileStatus fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, FileStatus.class);
    }

    /**
     * A list of messages that carry the error details. There is a common set of message types for
     * APIs to use.
     */
    @JsonProperty("details")
    public abstract Optional<List<Map<String, Object>>> details();

    /**
     * A list of messages that carry the error details. There is a common set of message types for
     * APIs to use.
     */
    @JsonProperty("message")
    public abstract Optional<String> message();

    /**
     * The status code. 0 for OK, 1 for CANCELLED
     */
    @JsonProperty("code")
    public abstract Optional<Integer> code();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for FileStatus.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `FileStatus.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_FileStatus.Builder();
        }

        @JsonProperty("details")
        public abstract Builder details(List<Map<String, Object>> details);

        @JsonProperty("message")
        public abstract Builder message(String message);

        @JsonProperty("code")
        public abstract Builder code(Integer code);

        public abstract FileStatus build();
    }
}
