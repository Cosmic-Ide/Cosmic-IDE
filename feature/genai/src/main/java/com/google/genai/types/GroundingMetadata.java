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
import java.util.Optional;

/**
 * Metadata returned to client when grounding is enabled.
 */
@AutoValue
@JsonDeserialize(builder = GroundingMetadata.Builder.class)
public abstract class GroundingMetadata extends JsonSerializable {
    /**
     * Instantiates a builder for GroundingMetadata.
     */
    public static Builder builder() {
        return new AutoValue_GroundingMetadata.Builder();
    }

    /**
     * Deserializes a JSON string to a GroundingMetadata object.
     */
    public static GroundingMetadata fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GroundingMetadata.class);
    }

    /**
     * List of supporting references retrieved from specified grounding source.
     */
    @JsonProperty("groundingChunks")
    public abstract Optional<List<GroundingChunk>> groundingChunks();

    /**
     * Optional. List of grounding support.
     */
    @JsonProperty("groundingSupports")
    public abstract Optional<List<GroundingSupport>> groundingSupports();

    /**
     * Optional. Output only. Retrieval metadata.
     */
    @JsonProperty("retrievalMetadata")
    public abstract Optional<RetrievalMetadata> retrievalMetadata();

    /**
     * Optional. Queries executed by the retrieval tools.
     */
    @JsonProperty("retrievalQueries")
    public abstract Optional<List<String>> retrievalQueries();

    /**
     * Optional. Google search entry for the following-up web searches.
     */
    @JsonProperty("searchEntryPoint")
    public abstract Optional<SearchEntryPoint> searchEntryPoint();

    /**
     * Optional. Web search queries for the following-up web search.
     */
    @JsonProperty("webSearchQueries")
    public abstract Optional<List<String>> webSearchQueries();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GroundingMetadata.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GroundingMetadata.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GroundingMetadata.Builder();
        }

        @JsonProperty("groundingChunks")
        public abstract Builder groundingChunks(List<GroundingChunk> groundingChunks);

        @JsonProperty("groundingSupports")
        public abstract Builder groundingSupports(List<GroundingSupport> groundingSupports);

        @JsonProperty("retrievalMetadata")
        public abstract Builder retrievalMetadata(RetrievalMetadata retrievalMetadata);

        @JsonProperty("retrievalQueries")
        public abstract Builder retrievalQueries(List<String> retrievalQueries);

        @JsonProperty("searchEntryPoint")
        public abstract Builder searchEntryPoint(SearchEntryPoint searchEntryPoint);

        @JsonProperty("webSearchQueries")
        public abstract Builder webSearchQueries(List<String> webSearchQueries);

        public abstract GroundingMetadata build();
    }
}
