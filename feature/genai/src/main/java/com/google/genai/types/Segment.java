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
 * Segment of the content.
 */
@AutoValue
@JsonDeserialize(builder = Segment.Builder.class)
public abstract class Segment extends JsonSerializable {
    /**
     * Instantiates a builder for Segment.
     */
    public static Builder builder() {
        return new AutoValue_Segment.Builder();
    }

    /**
     * Deserializes a JSON string to a Segment object.
     */
    public static Segment fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, Segment.class);
    }

    /**
     * Output only. End index in the given Part, measured in bytes. Offset from the start of the Part,
     * exclusive, starting at zero.
     */
    @JsonProperty("endIndex")
    public abstract Optional<Integer> endIndex();

    /**
     * Output only. The index of a Part object within its parent Content object.
     */
    @JsonProperty("partIndex")
    public abstract Optional<Integer> partIndex();

    /**
     * Output only. Start index in the given Part, measured in bytes. Offset from the start of the
     * Part, inclusive, starting at zero.
     */
    @JsonProperty("startIndex")
    public abstract Optional<Integer> startIndex();

    /**
     * Output only. The text corresponding to the segment from the response.
     */
    @JsonProperty("text")
    public abstract Optional<String> text();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for Segment.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `Segment.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_Segment.Builder();
        }

        @JsonProperty("endIndex")
        public abstract Builder endIndex(Integer endIndex);

        @JsonProperty("partIndex")
        public abstract Builder partIndex(Integer partIndex);

        @JsonProperty("startIndex")
        public abstract Builder startIndex(Integer startIndex);

        @JsonProperty("text")
        public abstract Builder text(String text);

        public abstract Segment build();
    }
}
