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
 * Config for filters.
 */
@AutoValue
@JsonDeserialize(builder = RagRetrievalConfigFilter.Builder.class)
public abstract class RagRetrievalConfigFilter extends JsonSerializable {
    /**
     * Instantiates a builder for RagRetrievalConfigFilter.
     */
    public static Builder builder() {
        return new AutoValue_RagRetrievalConfigFilter.Builder();
    }

    /**
     * Deserializes a JSON string to a RagRetrievalConfigFilter object.
     */
    public static RagRetrievalConfigFilter fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, RagRetrievalConfigFilter.class);
    }

    /**
     * Optional. String for metadata filtering.
     */
    @JsonProperty("metadataFilter")
    public abstract Optional<String> metadataFilter();

    /**
     * Optional. Only returns contexts with vector distance smaller than the threshold.
     */
    @JsonProperty("vectorDistanceThreshold")
    public abstract Optional<Double> vectorDistanceThreshold();

    /**
     * Optional. Only returns contexts with vector similarity larger than the threshold.
     */
    @JsonProperty("vectorSimilarityThreshold")
    public abstract Optional<Double> vectorSimilarityThreshold();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for RagRetrievalConfigFilter.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `RagRetrievalConfigFilter.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_RagRetrievalConfigFilter.Builder();
        }

        @JsonProperty("metadataFilter")
        public abstract Builder metadataFilter(String metadataFilter);

        @JsonProperty("vectorDistanceThreshold")
        public abstract Builder vectorDistanceThreshold(Double vectorDistanceThreshold);

        @JsonProperty("vectorSimilarityThreshold")
        public abstract Builder vectorSimilarityThreshold(Double vectorSimilarityThreshold);

        public abstract RagRetrievalConfigFilter build();
    }
}
