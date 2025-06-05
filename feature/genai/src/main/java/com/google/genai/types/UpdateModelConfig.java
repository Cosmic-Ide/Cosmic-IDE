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
 * Configuration for updating a tuned model.
 */
@AutoValue
@JsonDeserialize(builder = UpdateModelConfig.Builder.class)
public abstract class UpdateModelConfig extends JsonSerializable {
    /**
     * Instantiates a builder for UpdateModelConfig.
     */
    public static Builder builder() {
        return new AutoValue_UpdateModelConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a UpdateModelConfig object.
     */
    public static UpdateModelConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, UpdateModelConfig.class);
    }

    /**
     * Used to override HTTP request options.
     */
    @JsonProperty("httpOptions")
    public abstract Optional<HttpOptions> httpOptions();

    /**
     *
     */
    @JsonProperty("displayName")
    public abstract Optional<String> displayName();

    /**
     *
     */
    @JsonProperty("description")
    public abstract Optional<String> description();

    /**
     *
     */
    @JsonProperty("defaultCheckpointId")
    public abstract Optional<String> defaultCheckpointId();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for UpdateModelConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `UpdateModelConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_UpdateModelConfig.Builder();
        }

        @JsonProperty("httpOptions")
        public abstract Builder httpOptions(HttpOptions httpOptions);

        @JsonProperty("displayName")
        public abstract Builder displayName(String displayName);

        @JsonProperty("description")
        public abstract Builder description(String description);

        @JsonProperty("defaultCheckpointId")
        public abstract Builder defaultCheckpointId(String defaultCheckpointId);

        public abstract UpdateModelConfig build();
    }
}
