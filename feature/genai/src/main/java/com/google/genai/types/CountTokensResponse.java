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
 * Response for counting tokens.
 */
@AutoValue
@JsonDeserialize(builder = CountTokensResponse.Builder.class)
public abstract class CountTokensResponse extends JsonSerializable {
    /**
     * Instantiates a builder for CountTokensResponse.
     */
    public static Builder builder() {
        return new AutoValue_CountTokensResponse.Builder();
    }

    /**
     * Deserializes a JSON string to a CountTokensResponse object.
     */
    public static CountTokensResponse fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, CountTokensResponse.class);
    }

    /**
     * Total number of tokens.
     */
    @JsonProperty("totalTokens")
    public abstract Optional<Integer> totalTokens();

    /**
     * Number of tokens in the cached part of the prompt (the cached content).
     */
    @JsonProperty("cachedContentTokenCount")
    public abstract Optional<Integer> cachedContentTokenCount();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for CountTokensResponse.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `CountTokensResponse.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_CountTokensResponse.Builder();
        }

        @JsonProperty("totalTokens")
        public abstract Builder totalTokens(Integer totalTokens);

        @JsonProperty("cachedContentTokenCount")
        public abstract Builder cachedContentTokenCount(Integer cachedContentTokenCount);

        public abstract CountTokensResponse build();
    }
}
