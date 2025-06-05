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
 * Optional parameters for the embed_content method.
 */
@AutoValue
@JsonDeserialize(builder = EmbedContentConfig.Builder.class)
public abstract class EmbedContentConfig extends JsonSerializable {
    /**
     * Instantiates a builder for EmbedContentConfig.
     */
    public static Builder builder() {
        return new AutoValue_EmbedContentConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a EmbedContentConfig object.
     */
    public static EmbedContentConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, EmbedContentConfig.class);
    }

    /**
     * Used to override HTTP request options.
     */
    @JsonProperty("httpOptions")
    public abstract Optional<HttpOptions> httpOptions();

    /**
     * Type of task for which the embedding will be used.
     */
    @JsonProperty("taskType")
    public abstract Optional<String> taskType();

    /**
     * Title for the text. Only applicable when TaskType is `RETRIEVAL_DOCUMENT`.
     */
    @JsonProperty("title")
    public abstract Optional<String> title();

    /**
     * Reduced dimension for the output embedding. If set, excessive values in the output embedding
     * are truncated from the end. Supported by newer models since 2024 only. You cannot set this
     * value if using the earlier model (`models/embedding-001`).
     */
    @JsonProperty("outputDimensionality")
    public abstract Optional<Integer> outputDimensionality();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for EmbedContentConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `EmbedContentConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_EmbedContentConfig.Builder();
        }

        @JsonProperty("httpOptions")
        public abstract Builder httpOptions(HttpOptions httpOptions);

        @JsonProperty("taskType")
        public abstract Builder taskType(String taskType);

        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("outputDimensionality")
        public abstract Builder outputDimensionality(Integer outputDimensionality);

        public abstract EmbedContentConfig build();
    }
}
