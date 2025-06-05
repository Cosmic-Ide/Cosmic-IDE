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
 * Config for Hybrid Search.
 */
@AutoValue
@JsonDeserialize(builder = RagRetrievalConfigHybridSearch.Builder.class)
public abstract class RagRetrievalConfigHybridSearch extends JsonSerializable {
    /**
     * Instantiates a builder for RagRetrievalConfigHybridSearch.
     */
    public static Builder builder() {
        return new AutoValue_RagRetrievalConfigHybridSearch.Builder();
    }

    /**
     * Deserializes a JSON string to a RagRetrievalConfigHybridSearch object.
     */
    public static RagRetrievalConfigHybridSearch fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, RagRetrievalConfigHybridSearch.class);
    }

    /**
     * Optional. Alpha value controls the weight between dense and sparse vector search results. The
     * range is [0, 1], while 0 means sparse vector search only and 1 means dense vector search only.
     * The default value is 0.5 which balances sparse and dense vector search equally.
     */
    @JsonProperty("alpha")
    public abstract Optional<Float> alpha();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for RagRetrievalConfigHybridSearch.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `RagRetrievalConfigHybridSearch.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_RagRetrievalConfigHybridSearch.Builder();
        }

        @JsonProperty("alpha")
        public abstract Builder alpha(Float alpha);

        public abstract RagRetrievalConfigHybridSearch build();
    }
}
