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
 * An endpoint where you deploy models.
 */
@AutoValue
@JsonDeserialize(builder = Endpoint.Builder.class)
public abstract class Endpoint extends JsonSerializable {
    /**
     * Instantiates a builder for Endpoint.
     */
    public static Builder builder() {
        return new AutoValue_Endpoint.Builder();
    }

    /**
     * Deserializes a JSON string to a Endpoint object.
     */
    public static Endpoint fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, Endpoint.class);
    }

    /**
     * Resource name of the endpoint.
     */
    @JsonProperty("name")
    public abstract Optional<String> name();

    /**
     * ID of the model that's deployed to the endpoint.
     */
    @JsonProperty("deployedModelId")
    public abstract Optional<String> deployedModelId();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for Endpoint.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `Endpoint.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_Endpoint.Builder();
        }

        @JsonProperty("name")
        public abstract Builder name(String name);

        @JsonProperty("deployedModelId")
        public abstract Builder deployedModelId(String deployedModelId);

        public abstract Endpoint build();
    }
}
