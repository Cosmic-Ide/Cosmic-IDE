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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.genai.JsonSerializable;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for a Mask reference image.
 */
@AutoValue
@JsonDeserialize(builder = MaskReferenceConfig.Builder.class)
public abstract class MaskReferenceConfig extends JsonSerializable {
    /**
     * Instantiates a builder for MaskReferenceConfig.
     */
    public static Builder builder() {
        return new AutoValue_MaskReferenceConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a MaskReferenceConfig object.
     */
    public static MaskReferenceConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, MaskReferenceConfig.class);
    }

    /**
     * Prompts the model to generate a mask instead of you needing to provide one (unless
     * MASK_MODE_USER_PROVIDED is used).
     */
    @JsonProperty("maskMode")
    public abstract Optional<MaskReferenceMode> maskMode();

    /**
     * A list of up to 5 class ids to use for semantic segmentation. Automatically creates an image
     * mask based on specific objects.
     */
    @JsonProperty("segmentationClasses")
    public abstract Optional<List<Integer>> segmentationClasses();

    /**
     * Dilation percentage of the mask provided. Float between 0 and 1.
     */
    @JsonProperty("maskDilation")
    public abstract Optional<Float> maskDilation();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for MaskReferenceConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `MaskReferenceConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_MaskReferenceConfig.Builder();
        }

        @JsonProperty("maskMode")
        public abstract Builder maskMode(MaskReferenceMode maskMode);

        @CanIgnoreReturnValue
        public Builder maskMode(MaskReferenceMode.Known knownType) {
            return maskMode(new MaskReferenceMode(knownType));
        }

        @CanIgnoreReturnValue
        public Builder maskMode(String maskMode) {
            return maskMode(new MaskReferenceMode(maskMode));
        }

        @JsonProperty("segmentationClasses")
        public abstract Builder segmentationClasses(List<Integer> segmentationClasses);

        @JsonProperty("maskDilation")
        public abstract Builder maskDilation(Float maskDilation);

        public abstract MaskReferenceConfig build();
    }
}
