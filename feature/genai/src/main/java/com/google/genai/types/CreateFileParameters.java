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
 * Generates the parameters for the private _create method.
 */
@AutoValue
@JsonDeserialize(builder = CreateFileParameters.Builder.class)
public abstract class CreateFileParameters extends JsonSerializable {
    /**
     * Instantiates a builder for CreateFileParameters.
     */
    public static Builder builder() {
        return new AutoValue_CreateFileParameters.Builder();
    }

    /**
     * Deserializes a JSON string to a CreateFileParameters object.
     */
    public static CreateFileParameters fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, CreateFileParameters.class);
    }

    /**
     * The file to be uploaded. mime_type: (Required) The MIME type of the file. Must be provided.
     * name: (Optional) The name of the file in the destination (e.g. 'files/sample-image').
     * display_name: (Optional) The display name of the file.
     */
    @JsonProperty("file")
    public abstract Optional<File> file();

    /**
     * Used to override the default configuration.
     */
    @JsonProperty("config")
    public abstract Optional<CreateFileConfig> config();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for CreateFileParameters.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `CreateFileParameters.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_CreateFileParameters.Builder();
        }

        @JsonProperty("file")
        public abstract Builder file(File file);

        @JsonProperty("config")
        public abstract Builder config(CreateFileConfig config);

        public abstract CreateFileParameters build();
    }
}
