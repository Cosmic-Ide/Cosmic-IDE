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
 * Parameters for the request to edit an image.
 */
@AutoValue
@JsonDeserialize(builder = EditImageParameters.Builder.class)
public abstract class EditImageParameters extends JsonSerializable {
    /**
     * Instantiates a builder for EditImageParameters.
     */
    public static Builder builder() {
        return new AutoValue_EditImageParameters.Builder();
    }

    /**
     * Deserializes a JSON string to a EditImageParameters object.
     */
    public static EditImageParameters fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, EditImageParameters.class);
    }

    /**
     * The model to use.
     */
    @JsonProperty("model")
    public abstract Optional<String> model();

    /**
     * A text description of the edit to apply to the image.
     */
    @JsonProperty("prompt")
    public abstract Optional<String> prompt();

    /**
     * The reference images for Imagen 3 editing.
     */
    @JsonProperty("referenceImages")
    public abstract Optional<List<ReferenceImageAPI>> referenceImages();

    /**
     * Configuration for editing.
     */
    @JsonProperty("config")
    public abstract Optional<EditImageConfig> config();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for EditImageParameters.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `EditImageParameters.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_EditImageParameters.Builder();
        }

        @JsonProperty("model")
        public abstract Builder model(String model);

        @JsonProperty("prompt")
        public abstract Builder prompt(String prompt);

        @JsonProperty("referenceImages")
        public abstract Builder referenceImages(List<ReferenceImageAPI> referenceImages);

        @JsonProperty("config")
        public abstract Builder config(EditImageConfig config);

        public abstract EditImageParameters build();
    }
}
