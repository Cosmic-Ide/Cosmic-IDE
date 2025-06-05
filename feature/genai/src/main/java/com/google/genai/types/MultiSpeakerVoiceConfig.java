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
 * The configuration for the multi-speaker setup.
 */
@AutoValue
@JsonDeserialize(builder = MultiSpeakerVoiceConfig.Builder.class)
public abstract class MultiSpeakerVoiceConfig extends JsonSerializable {
    /**
     * Instantiates a builder for MultiSpeakerVoiceConfig.
     */
    public static Builder builder() {
        return new AutoValue_MultiSpeakerVoiceConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a MultiSpeakerVoiceConfig object.
     */
    public static MultiSpeakerVoiceConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, MultiSpeakerVoiceConfig.class);
    }

    /**
     * The configuration for the speaker to use.
     */
    @JsonProperty("speakerVoiceConfigs")
    public abstract Optional<List<SpeakerVoiceConfig>> speakerVoiceConfigs();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for MultiSpeakerVoiceConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `MultiSpeakerVoiceConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_MultiSpeakerVoiceConfig.Builder();
        }

        @JsonProperty("speakerVoiceConfigs")
        public abstract Builder speakerVoiceConfigs(List<SpeakerVoiceConfig> speakerVoiceConfigs);

        public abstract MultiSpeakerVoiceConfig build();
    }
}
