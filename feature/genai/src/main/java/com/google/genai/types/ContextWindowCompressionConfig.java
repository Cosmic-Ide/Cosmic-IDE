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
 * Enables context window compression -- mechanism managing model context window so it does not
 * exceed given length.
 */
@AutoValue
@JsonDeserialize(builder = ContextWindowCompressionConfig.Builder.class)
public abstract class ContextWindowCompressionConfig extends JsonSerializable {
    /**
     * Instantiates a builder for ContextWindowCompressionConfig.
     */
    public static Builder builder() {
        return new AutoValue_ContextWindowCompressionConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a ContextWindowCompressionConfig object.
     */
    public static ContextWindowCompressionConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, ContextWindowCompressionConfig.class);
    }

    /**
     * Number of tokens (before running turn) that triggers context window compression mechanism.
     */
    @JsonProperty("triggerTokens")
    public abstract Optional<Long> triggerTokens();

    /**
     * Sliding window compression mechanism.
     */
    @JsonProperty("slidingWindow")
    public abstract Optional<SlidingWindow> slidingWindow();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for ContextWindowCompressionConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `ContextWindowCompressionConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_ContextWindowCompressionConfig.Builder();
        }

        @JsonProperty("triggerTokens")
        public abstract Builder triggerTokens(Long triggerTokens);

        @JsonProperty("slidingWindow")
        public abstract Builder slidingWindow(SlidingWindow slidingWindow);

        public abstract ContextWindowCompressionConfig build();
    }
}
