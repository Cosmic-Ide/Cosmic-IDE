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
 * Context window will be truncated by keeping only suffix of it.
 *
 * <p>Context window will always be cut at start of USER role turn. System instructions and
 * `BidiGenerateContentSetup.prefix_turns` will not be subject to the sliding window mechanism, they
 * will always stay at the beginning of context window.
 */
@AutoValue
@JsonDeserialize(builder = SlidingWindow.Builder.class)
public abstract class SlidingWindow extends JsonSerializable {
    /**
     * Instantiates a builder for SlidingWindow.
     */
    public static Builder builder() {
        return new AutoValue_SlidingWindow.Builder();
    }

    /**
     * Deserializes a JSON string to a SlidingWindow object.
     */
    public static SlidingWindow fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, SlidingWindow.class);
    }

    /**
     * Session reduction target -- how many tokens we should keep. Window shortening operation has
     * some latency costs, so we should avoid running it on every turn. Should be < trigger_tokens. If
     * not set, trigger_tokens/2 is assumed.
     */
    @JsonProperty("targetTokens")
    public abstract Optional<Long> targetTokens();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for SlidingWindow.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `SlidingWindow.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_SlidingWindow.Builder();
        }

        @JsonProperty("targetTokens")
        public abstract Builder targetTokens(Long targetTokens);

        public abstract SlidingWindow build();
    }
}
