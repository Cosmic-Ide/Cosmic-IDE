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
 * Metadata on the usage of the cached content.
 */
@AutoValue
@JsonDeserialize(builder = CachedContentUsageMetadata.Builder.class)
public abstract class CachedContentUsageMetadata extends JsonSerializable {
    /**
     * Instantiates a builder for CachedContentUsageMetadata.
     */
    public static Builder builder() {
        return new AutoValue_CachedContentUsageMetadata.Builder();
    }

    /**
     * Deserializes a JSON string to a CachedContentUsageMetadata object.
     */
    public static CachedContentUsageMetadata fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, CachedContentUsageMetadata.class);
    }

    /**
     * Duration of audio in seconds.
     */
    @JsonProperty("audioDurationSeconds")
    public abstract Optional<Integer> audioDurationSeconds();

    /**
     * Number of images.
     */
    @JsonProperty("imageCount")
    public abstract Optional<Integer> imageCount();

    /**
     * Number of text characters.
     */
    @JsonProperty("textCount")
    public abstract Optional<Integer> textCount();

    /**
     * Total number of tokens that the cached content consumes.
     */
    @JsonProperty("totalTokenCount")
    public abstract Optional<Integer> totalTokenCount();

    /**
     * Duration of video in seconds.
     */
    @JsonProperty("videoDurationSeconds")
    public abstract Optional<Integer> videoDurationSeconds();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for CachedContentUsageMetadata.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `CachedContentUsageMetadata.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_CachedContentUsageMetadata.Builder();
        }

        @JsonProperty("audioDurationSeconds")
        public abstract Builder audioDurationSeconds(Integer audioDurationSeconds);

        @JsonProperty("imageCount")
        public abstract Builder imageCount(Integer imageCount);

        @JsonProperty("textCount")
        public abstract Builder textCount(Integer textCount);

        @JsonProperty("totalTokenCount")
        public abstract Builder totalTokenCount(Integer totalTokenCount);

        @JsonProperty("videoDurationSeconds")
        public abstract Builder videoDurationSeconds(Integer videoDurationSeconds);

        public abstract CachedContentUsageMetadata build();
    }
}
