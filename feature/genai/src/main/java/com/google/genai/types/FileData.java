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
 * URI based data.
 */
@AutoValue
@JsonDeserialize(builder = FileData.Builder.class)
public abstract class FileData extends JsonSerializable {
    /**
     * Instantiates a builder for FileData.
     */
    public static Builder builder() {
        return new AutoValue_FileData.Builder();
    }

    /**
     * Deserializes a JSON string to a FileData object.
     */
    public static FileData fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, FileData.class);
    }

    /**
     * Optional. Display name of the file data. Used to provide a label or filename to distinguish
     * file datas. It is not currently used in the Gemini GenerateContent calls.
     */
    @JsonProperty("displayName")
    public abstract Optional<String> displayName();

    /**
     * Required. URI.
     */
    @JsonProperty("fileUri")
    public abstract Optional<String> fileUri();

    /**
     * Required. The IANA standard MIME type of the source data.
     */
    @JsonProperty("mimeType")
    public abstract Optional<String> mimeType();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for FileData.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `FileData.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_FileData.Builder();
        }

        @JsonProperty("displayName")
        public abstract Builder displayName(String displayName);

        @JsonProperty("fileUri")
        public abstract Builder fileUri(String fileUri);

        @JsonProperty("mimeType")
        public abstract Builder mimeType(String mimeType);

        public abstract FileData build();
    }
}
