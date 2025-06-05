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
 * Retrieval config.
 */
@AutoValue
@JsonDeserialize(builder = RetrievalConfig.Builder.class)
public abstract class RetrievalConfig extends JsonSerializable {
    /**
     * Instantiates a builder for RetrievalConfig.
     */
    public static Builder builder() {
        return new AutoValue_RetrievalConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a RetrievalConfig object.
     */
    public static RetrievalConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, RetrievalConfig.class);
    }

    /**
     * Optional. The location of the user.
     */
    @JsonProperty("latLng")
    public abstract Optional<LatLng> latLng();

    /**
     * The language code of the user.
     */
    @JsonProperty("languageCode")
    public abstract Optional<String> languageCode();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for RetrievalConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `RetrievalConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_RetrievalConfig.Builder();
        }

        @JsonProperty("latLng")
        public abstract Builder latLng(LatLng latLng);

        @JsonProperty("languageCode")
        public abstract Builder languageCode(String languageCode);

        public abstract RetrievalConfig build();
    }
}
