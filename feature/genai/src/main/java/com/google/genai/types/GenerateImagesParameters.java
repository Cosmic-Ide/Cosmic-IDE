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
 * The parameters for generating images.
 */
@AutoValue
@JsonDeserialize(builder = GenerateImagesParameters.Builder.class)
public abstract class GenerateImagesParameters extends JsonSerializable {
    /**
     * Instantiates a builder for GenerateImagesParameters.
     */
    public static Builder builder() {
        return new AutoValue_GenerateImagesParameters.Builder();
    }

    /**
     * Deserializes a JSON string to a GenerateImagesParameters object.
     */
    public static GenerateImagesParameters fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GenerateImagesParameters.class);
    }

    /**
     * ID of the model to use. For a list of models, see `Google models
     * <https://cloud.google.com/vertex-ai/generative-ai/docs/learn/models>`_.
     */
    @JsonProperty("model")
    public abstract Optional<String> model();

    /**
     * Text prompt that typically describes the images to output.
     */
    @JsonProperty("prompt")
    public abstract Optional<String> prompt();

    /**
     * Configuration for generating images.
     */
    @JsonProperty("config")
    public abstract Optional<GenerateImagesConfig> config();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GenerateImagesParameters.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GenerateImagesParameters.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GenerateImagesParameters.Builder();
        }

        @JsonProperty("model")
        public abstract Builder model(String model);

        @JsonProperty("prompt")
        public abstract Builder prompt(String prompt);

        @JsonProperty("config")
        public abstract Builder config(GenerateImagesConfig config);

        public abstract GenerateImagesParameters build();
    }
}
