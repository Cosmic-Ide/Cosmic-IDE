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
 * A mask reference image.
 *
 * <p>This encapsulates either a mask image provided by the user and configs for the user provided
 * mask, or only config parameters for the model to generate a mask.
 *
 * <p>A mask image is an image whose non-zero values indicate where to edit the base image. If the
 * user provides a mask image, the mask must be in the same dimensions as the raw image.
 */
@AutoValue
@JsonDeserialize(builder = MaskReferenceImage.Builder.class)
public abstract class MaskReferenceImage extends JsonSerializable implements ReferenceImage {
    /**
     * Instantiates a builder for MaskReferenceImage.
     */
    public static Builder builder() {
        return new AutoValue_MaskReferenceImage.Builder();
    }

    /**
     * Deserializes a JSON string to a MaskReferenceImage object.
     */
    public static MaskReferenceImage fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, MaskReferenceImage.class);
    }

    /**
     * The reference image for the editing operation.
     */
    @JsonProperty("referenceImage")
    public abstract Optional<Image> referenceImage();

    /**
     * The id of the reference image.
     */
    @JsonProperty("referenceId")
    public abstract Optional<Integer> referenceId();

    /**
     * The type of the reference image. Only set by the SDK.
     */
    @JsonProperty("referenceType")
    public abstract Optional<String> referenceType();

    /**
     * Configuration for the mask reference image.
     */
    @JsonProperty("config")
    public abstract Optional<MaskReferenceConfig> config();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    @Override
    public ReferenceImageAPI toReferenceImageAPI() {
        ReferenceImageAPI.Builder referenceImageAPIBuilder = ReferenceImageAPI.builder();
        referenceImage().ifPresent(referenceImageAPIBuilder::referenceImage);
        referenceId().ifPresent(referenceImageAPIBuilder::referenceId);
        config().ifPresent(referenceImageAPIBuilder::maskImageConfig);
        referenceImageAPIBuilder.referenceType("REFERENCE_TYPE_MASK");
        return referenceImageAPIBuilder.build();
    }

    /**
     * Builder for MaskReferenceImage.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `MaskReferenceImage.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_MaskReferenceImage.Builder();
        }

        @JsonProperty("referenceImage")
        public abstract Builder referenceImage(Image referenceImage);

        @JsonProperty("referenceId")
        public abstract Builder referenceId(Integer referenceId);

        @JsonProperty("referenceType")
        public abstract Builder referenceType(String referenceType);

        @JsonProperty("config")
        public abstract Builder config(MaskReferenceConfig config);

        public abstract MaskReferenceImage build();
    }
}
