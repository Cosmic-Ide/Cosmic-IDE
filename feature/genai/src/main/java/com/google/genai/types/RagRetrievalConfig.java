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
 * Specifies the context retrieval config.
 */
@AutoValue
@JsonDeserialize(builder = RagRetrievalConfig.Builder.class)
public abstract class RagRetrievalConfig extends JsonSerializable {
    /**
     * Instantiates a builder for RagRetrievalConfig.
     */
    public static Builder builder() {
        return new AutoValue_RagRetrievalConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a RagRetrievalConfig object.
     */
    public static RagRetrievalConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, RagRetrievalConfig.class);
    }

    /**
     * Optional. Config for filters.
     */
    @JsonProperty("filter")
    public abstract Optional<RagRetrievalConfigFilter> filter();

    /**
     * Optional. Config for Hybrid Search.
     */
    @JsonProperty("hybridSearch")
    public abstract Optional<RagRetrievalConfigHybridSearch> hybridSearch();

    /**
     * Optional. Config for ranking and reranking.
     */
    @JsonProperty("ranking")
    public abstract Optional<RagRetrievalConfigRanking> ranking();

    /**
     * Optional. The number of contexts to retrieve.
     */
    @JsonProperty("topK")
    public abstract Optional<Integer> topK();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for RagRetrievalConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `RagRetrievalConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_RagRetrievalConfig.Builder();
        }

        @JsonProperty("filter")
        public abstract Builder filter(RagRetrievalConfigFilter filter);

        @JsonProperty("hybridSearch")
        public abstract Builder hybridSearch(RagRetrievalConfigHybridSearch hybridSearch);

        @JsonProperty("ranking")
        public abstract Builder ranking(RagRetrievalConfigRanking ranking);

        @JsonProperty("topK")
        public abstract Builder topK(Integer topK);

        public abstract RagRetrievalConfig build();
    }
}
