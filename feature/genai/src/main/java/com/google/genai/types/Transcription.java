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
 * Audio transcription in Server Conent.
 */
@AutoValue
@JsonDeserialize(builder = Transcription.Builder.class)
public abstract class Transcription extends JsonSerializable {
    /**
     * Instantiates a builder for Transcription.
     */
    public static Builder builder() {
        return new AutoValue_Transcription.Builder();
    }

    /**
     * Deserializes a JSON string to a Transcription object.
     */
    public static Transcription fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, Transcription.class);
    }

    /**
     * Transcription text.
     */
    @JsonProperty("text")
    public abstract Optional<String> text();

    /**
     * The bool indicates the end of the transcription.
     */
    @JsonProperty("finished")
    public abstract Optional<Boolean> finished();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for Transcription.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `Transcription.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_Transcription.Builder();
        }

        @JsonProperty("text")
        public abstract Builder text(String text);

        @JsonProperty("finished")
        public abstract Builder finished(boolean finished);

        public abstract Transcription build();
    }
}
