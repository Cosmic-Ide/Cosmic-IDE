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
import com.google.genai.errors.GenAiIOException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * An image.
 */
@AutoValue
@JsonDeserialize(builder = Image.Builder.class)
public abstract class Image extends JsonSerializable {
    /**
     * Instantiates a builder for Image.
     */
    public static Builder builder() {
        return new AutoValue_Image.Builder();
    }

    /**
     * Deserializes a JSON string to a Image object.
     */
    public static Image fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, Image.class);
    }

    /**
     * Creates an Image object from a local file.
     *
     * <p>If mimeType is not specified, it will be inferred from the file extension.
     */
    public static Image fromFile(String location) {
        try {
            String mimeType = Files.probeContentType(Paths.get(location));
            return Image.fromFile(location, mimeType);
        } catch (IOException e) {
            return Image.fromFile(location, null);
        }
    }

    /**
     * Creates an Image object from a local file and mime type.
     */
    public static Image fromFile(String location, String mimeType) {
        try {
            File imagefile = new File(location);
            byte[] imageBytes = Files.readAllBytes(imagefile.toPath());
            Image.Builder builder = builder().imageBytes(imageBytes);
            if (mimeType != null) {
                builder = builder.mimeType(mimeType);
            }
            return builder.build();
        } catch (IOException e) {
            throw new GenAiIOException(String.format("Failed to read image file: %s", location), e);
        }
    }

    /**
     * The Cloud Storage URI of the image. ``Image`` can contain a value for this field or the
     * ``image_bytes`` field but not both.
     */
    @JsonProperty("gcsUri")
    public abstract Optional<String> gcsUri();

    /**
     * The image bytes data. ``Image`` can contain a value for this field or the ``gcs_uri`` field but
     * not both.
     */
    @JsonProperty("imageBytes")
    public abstract Optional<byte[]> imageBytes();

    /**
     * The MIME type of the image.
     */
    @JsonProperty("mimeType")
    public abstract Optional<String> mimeType();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for Image.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `Image.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_Image.Builder();
        }

        @JsonProperty("gcsUri")
        public abstract Builder gcsUri(String gcsUri);

        @JsonProperty("imageBytes")
        public abstract Builder imageBytes(byte[] imageBytes);

        @JsonProperty("mimeType")
        public abstract Builder mimeType(String mimeType);

        public abstract Image build();
    }
}
