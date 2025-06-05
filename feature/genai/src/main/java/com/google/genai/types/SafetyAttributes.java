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
 * Safety attributes of a GeneratedImage or the user-provided prompt.
 */
@AutoValue
@JsonDeserialize(builder = SafetyAttributes.Builder.class)
public abstract class SafetyAttributes extends JsonSerializable {
    /**
     * Instantiates a builder for SafetyAttributes.
     */
    public static Builder builder() {
        return new AutoValue_SafetyAttributes.Builder();
    }

    /**
     * Deserializes a JSON string to a SafetyAttributes object.
     */
    public static SafetyAttributes fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, SafetyAttributes.class);
    }

    /**
     * List of RAI categories.
     */
    @JsonProperty("categories")
    public abstract Optional<List<String>> categories();

    /**
     * List of scores of each categories.
     */
    @JsonProperty("scores")
    public abstract Optional<List<Float>> scores();

    /**
     * Internal use only.
     */
    @JsonProperty("contentType")
    public abstract Optional<String> contentType();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for SafetyAttributes.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `SafetyAttributes.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_SafetyAttributes.Builder();
        }

        @JsonProperty("categories")
        public abstract Builder categories(List<String> categories);

        @JsonProperty("scores")
        public abstract Builder scores(List<Float> scores);

        @JsonProperty("contentType")
        public abstract Builder contentType(String contentType);

        public abstract SafetyAttributes build();
    }
}
