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
 * Config for models.generate_content parameters.
 */
@AutoValue
@JsonDeserialize(builder = GenerateContentParameters.Builder.class)
public abstract class GenerateContentParameters extends JsonSerializable {
    /**
     * Instantiates a builder for GenerateContentParameters.
     */
    public static Builder builder() {
        return new AutoValue_GenerateContentParameters.Builder();
    }

    /**
     * Deserializes a JSON string to a GenerateContentParameters object.
     */
    public static GenerateContentParameters fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GenerateContentParameters.class);
    }

    /**
     * ID of the model to use. For a list of models, see `Google models
     * <https://cloud.google.com/vertex-ai/generative-ai/docs/learn/models>`_.
     */
    @JsonProperty("model")
    public abstract Optional<String> model();

    /**
     * Content of the request.
     */
    @JsonProperty("contents")
    public abstract Optional<List<Content>> contents();

    /**
     * Configuration that contains optional model parameters.
     */
    @JsonProperty("config")
    public abstract Optional<GenerateContentConfig> config();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GenerateContentParameters.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GenerateContentParameters.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GenerateContentParameters.Builder();
        }

        @JsonProperty("model")
        public abstract Builder model(String model);

        @JsonProperty("contents")
        public abstract Builder contents(List<Content> contents);

        @JsonProperty("config")
        public abstract Builder config(GenerateContentConfig config);

        public abstract GenerateContentParameters build();
    }
}
