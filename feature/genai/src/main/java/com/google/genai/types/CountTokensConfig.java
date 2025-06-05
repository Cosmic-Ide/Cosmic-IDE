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
 * Config for the count_tokens method.
 */
@AutoValue
@JsonDeserialize(builder = CountTokensConfig.Builder.class)
public abstract class CountTokensConfig extends JsonSerializable {
    /**
     * Instantiates a builder for CountTokensConfig.
     */
    public static Builder builder() {
        return new AutoValue_CountTokensConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a CountTokensConfig object.
     */
    public static CountTokensConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, CountTokensConfig.class);
    }

    /**
     * Used to override HTTP request options.
     */
    @JsonProperty("httpOptions")
    public abstract Optional<HttpOptions> httpOptions();

    /**
     * Instructions for the model to steer it toward better performance.
     */
    @JsonProperty("systemInstruction")
    public abstract Optional<Content> systemInstruction();

    /**
     * Code that enables the system to interact with external systems to perform an action outside of
     * the knowledge and scope of the model.
     */
    @JsonProperty("tools")
    public abstract Optional<List<Tool>> tools();

    /**
     * Configuration that the model uses to generate the response. Not supported by the Gemini
     * Developer API.
     */
    @JsonProperty("generationConfig")
    public abstract Optional<GenerationConfig> generationConfig();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for CountTokensConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `CountTokensConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_CountTokensConfig.Builder();
        }

        @JsonProperty("httpOptions")
        public abstract Builder httpOptions(HttpOptions httpOptions);

        @JsonProperty("systemInstruction")
        public abstract Builder systemInstruction(Content systemInstruction);

        @JsonProperty("tools")
        public abstract Builder tools(List<Tool> tools);

        @JsonProperty("generationConfig")
        public abstract Builder generationConfig(GenerationConfig generationConfig);

        public abstract CountTokensConfig build();
    }
}
