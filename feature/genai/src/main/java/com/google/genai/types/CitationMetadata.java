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

import java.util.List;
import java.util.Optional;

/**
 * Citation information when the model quotes another source.
 */
@AutoValue
@JsonDeserialize(builder = CitationMetadata.Builder.class)
public abstract class CitationMetadata extends JsonSerializable {
    /**
     * Instantiates a builder for CitationMetadata.
     */
    public static Builder builder() {
        return new AutoValue_CitationMetadata.Builder();
    }

    /**
     * Deserializes a JSON string to a CitationMetadata object.
     */
    public static CitationMetadata fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, CitationMetadata.class);
    }

    /**
     * Contains citation information when the model directly quotes, at length, from another source.
     * Can include traditional websites and code repositories.
     */
    @JsonProperty("citations")
    public abstract Optional<List<Citation>> citations();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for CitationMetadata.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `CitationMetadata.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_CitationMetadata.Builder();
        }

        @JsonProperty("citations")
        public abstract Builder citations(List<Citation> citations);

        public abstract CitationMetadata build();
    }
}
