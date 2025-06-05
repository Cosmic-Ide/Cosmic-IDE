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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.genai.JsonSerializable;

/**
 * The audio transcription configuration in Setup.
 */
@AutoValue
@JsonDeserialize(builder = AudioTranscriptionConfig.Builder.class)
public abstract class AudioTranscriptionConfig extends JsonSerializable {
    /**
     * Instantiates a builder for AudioTranscriptionConfig.
     */
    public static Builder builder() {
        return new AutoValue_AudioTranscriptionConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a AudioTranscriptionConfig object.
     */
    public static AudioTranscriptionConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, AudioTranscriptionConfig.class);
    }

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for AudioTranscriptionConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `AudioTranscriptionConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_AudioTranscriptionConfig.Builder();
        }

        public abstract AudioTranscriptionConfig build();
    }
}
