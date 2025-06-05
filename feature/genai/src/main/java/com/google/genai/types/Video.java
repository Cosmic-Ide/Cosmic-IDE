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

import java.util.Optional;

/**
 * A generated video.
 */
@AutoValue
@JsonDeserialize(builder = Video.Builder.class)
public abstract class Video extends JsonSerializable {
    /**
     * Instantiates a builder for Video.
     */
    public static Builder builder() {
        return new AutoValue_Video.Builder();
    }

    /**
     * Deserializes a JSON string to a Video object.
     */
    public static Video fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, Video.class);
    }

    /**
     * Path to another storage.
     */
    @JsonProperty("uri")
    public abstract Optional<String> uri();

    /**
     * Video bytes.
     */
    @JsonProperty("videoBytes")
    public abstract Optional<byte[]> videoBytes();

    /**
     * Video encoding, for example "video/mp4".
     */
    @JsonProperty("mimeType")
    public abstract Optional<String> mimeType();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for Video.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `Video.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_Video.Builder();
        }

        @JsonProperty("uri")
        public abstract Builder uri(String uri);

        @JsonProperty("videoBytes")
        public abstract Builder videoBytes(byte[] videoBytes);

        @JsonProperty("mimeType")
        public abstract Builder mimeType(String mimeType);

        public abstract Video build();
    }
}
