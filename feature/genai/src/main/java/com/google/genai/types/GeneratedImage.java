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
 * An output image.
 */
@AutoValue
@JsonDeserialize(builder = GeneratedImage.Builder.class)
public abstract class GeneratedImage extends JsonSerializable {
    /**
     * Instantiates a builder for GeneratedImage.
     */
    public static Builder builder() {
        return new AutoValue_GeneratedImage.Builder();
    }

    /**
     * Deserializes a JSON string to a GeneratedImage object.
     */
    public static GeneratedImage fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GeneratedImage.class);
    }

    /**
     * The output image data.
     */
    @JsonProperty("image")
    public abstract Optional<Image> image();

    /**
     * Responsible AI filter reason if the image is filtered out of the response.
     */
    @JsonProperty("raiFilteredReason")
    public abstract Optional<String> raiFilteredReason();

    /**
     * Safety attributes of the image. Lists of RAI categories and their scores of each content.
     */
    @JsonProperty("safetyAttributes")
    public abstract Optional<SafetyAttributes> safetyAttributes();

    /**
     * The rewritten prompt used for the image generation if the prompt enhancer is enabled.
     */
    @JsonProperty("enhancedPrompt")
    public abstract Optional<String> enhancedPrompt();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GeneratedImage.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GeneratedImage.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GeneratedImage.Builder();
        }

        @JsonProperty("image")
        public abstract Builder image(Image image);

        @JsonProperty("raiFilteredReason")
        public abstract Builder raiFilteredReason(String raiFilteredReason);

        @JsonProperty("safetyAttributes")
        public abstract Builder safetyAttributes(SafetyAttributes safetyAttributes);

        @JsonProperty("enhancedPrompt")
        public abstract Builder enhancedPrompt(String enhancedPrompt);

        public abstract GeneratedImage build();
    }
}
