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
 * Config for thinking features.
 */
@AutoValue
@JsonDeserialize(builder = GenerationConfigThinkingConfig.Builder.class)
public abstract class GenerationConfigThinkingConfig extends JsonSerializable {
    /**
     * Instantiates a builder for GenerationConfigThinkingConfig.
     */
    public static Builder builder() {
        return new AutoValue_GenerationConfigThinkingConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a GenerationConfigThinkingConfig object.
     */
    public static GenerationConfigThinkingConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GenerationConfigThinkingConfig.class);
    }

    /**
     * Optional. Indicates whether to include thoughts in the response. If true, thoughts are returned
     * only when available.
     */
    @JsonProperty("includeThoughts")
    public abstract Optional<Boolean> includeThoughts();

    /**
     * Optional. Indicates the thinking budget in tokens. This is only applied when enable_thinking is
     * true.
     */
    @JsonProperty("thinkingBudget")
    public abstract Optional<Integer> thinkingBudget();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GenerationConfigThinkingConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GenerationConfigThinkingConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GenerationConfigThinkingConfig.Builder();
        }

        @JsonProperty("includeThoughts")
        public abstract Builder includeThoughts(boolean includeThoughts);

        @JsonProperty("thinkingBudget")
        public abstract Builder thinkingBudget(Integer thinkingBudget);

        public abstract GenerationConfigThinkingConfig build();
    }
}
