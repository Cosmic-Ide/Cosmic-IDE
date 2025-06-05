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
 * Configuration for upscaling an image.
 *
 * <p>For more information on this configuration, refer to the `Imagen API reference documentation
 * <https://cloud.google.com/vertex-ai/generative-ai/docs/model-reference/imagen-api>`_.
 */
@AutoValue
@JsonDeserialize(builder = UpscaleImageConfig.Builder.class)
public abstract class UpscaleImageConfig extends JsonSerializable {
    /**
     * Instantiates a builder for UpscaleImageConfig.
     */
    public static Builder builder() {
        return new AutoValue_UpscaleImageConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a UpscaleImageConfig object.
     */
    public static UpscaleImageConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, UpscaleImageConfig.class);
    }

    /**
     * Used to override HTTP request options.
     */
    @JsonProperty("httpOptions")
    public abstract Optional<HttpOptions> httpOptions();

    /**
     * Whether to include a reason for filtered-out images in the response.
     */
    @JsonProperty("includeRaiReason")
    public abstract Optional<Boolean> includeRaiReason();

    /**
     * The image format that the output should be saved as.
     */
    @JsonProperty("outputMimeType")
    public abstract Optional<String> outputMimeType();

    /**
     * The level of compression if the ``output_mime_type`` is ``image/jpeg``.
     */
    @JsonProperty("outputCompressionQuality")
    public abstract Optional<Integer> outputCompressionQuality();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for UpscaleImageConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `UpscaleImageConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_UpscaleImageConfig.Builder();
        }

        @JsonProperty("httpOptions")
        public abstract Builder httpOptions(HttpOptions httpOptions);

        @JsonProperty("includeRaiReason")
        public abstract Builder includeRaiReason(boolean includeRaiReason);

        @JsonProperty("outputMimeType")
        public abstract Builder outputMimeType(String outputMimeType);

        @JsonProperty("outputCompressionQuality")
        public abstract Builder outputCompressionQuality(Integer outputCompressionQuality);

        public abstract UpscaleImageConfig build();
    }
}
