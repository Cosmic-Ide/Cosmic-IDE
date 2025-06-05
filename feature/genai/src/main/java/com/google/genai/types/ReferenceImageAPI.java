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
 * Private class that represents a Reference image that is sent to API.
 */
@AutoValue
@JsonDeserialize(builder = ReferenceImageAPI.Builder.class)
public abstract class ReferenceImageAPI extends JsonSerializable {
    /**
     * Instantiates a builder for ReferenceImageAPI.
     */
    public static Builder builder() {
        return new AutoValue_ReferenceImageAPI.Builder();
    }

    /**
     * Deserializes a JSON string to a ReferenceImageAPI object.
     */
    public static ReferenceImageAPI fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, ReferenceImageAPI.class);
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
    @JsonProperty("maskImageConfig")
    public abstract Optional<MaskReferenceConfig> maskImageConfig();

    /**
     * Configuration for the control reference image.
     */
    @JsonProperty("controlImageConfig")
    public abstract Optional<ControlReferenceConfig> controlImageConfig();

    /**
     * Configuration for the style reference image.
     */
    @JsonProperty("styleImageConfig")
    public abstract Optional<StyleReferenceConfig> styleImageConfig();

    /**
     * Configuration for the subject reference image.
     */
    @JsonProperty("subjectImageConfig")
    public abstract Optional<SubjectReferenceConfig> subjectImageConfig();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for ReferenceImageAPI.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `ReferenceImageAPI.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_ReferenceImageAPI.Builder();
        }

        @JsonProperty("referenceImage")
        public abstract Builder referenceImage(Image referenceImage);

        @JsonProperty("referenceId")
        public abstract Builder referenceId(Integer referenceId);

        @JsonProperty("referenceType")
        public abstract Builder referenceType(String referenceType);

        @JsonProperty("maskImageConfig")
        public abstract Builder maskImageConfig(MaskReferenceConfig maskImageConfig);

        @JsonProperty("controlImageConfig")
        public abstract Builder controlImageConfig(ControlReferenceConfig controlImageConfig);

        @JsonProperty("styleImageConfig")
        public abstract Builder styleImageConfig(StyleReferenceConfig styleImageConfig);

        @JsonProperty("subjectImageConfig")
        public abstract Builder subjectImageConfig(SubjectReferenceConfig subjectImageConfig);

        public abstract ReferenceImageAPI build();
    }
}
