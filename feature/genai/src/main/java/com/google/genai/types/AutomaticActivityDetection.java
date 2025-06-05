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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.genai.JsonSerializable;

import java.util.Optional;

/**
 * Configures automatic detection of activity.
 */
@AutoValue
@JsonDeserialize(builder = AutomaticActivityDetection.Builder.class)
public abstract class AutomaticActivityDetection extends JsonSerializable {
    /**
     * Instantiates a builder for AutomaticActivityDetection.
     */
    public static Builder builder() {
        return new AutoValue_AutomaticActivityDetection.Builder();
    }

    /**
     * Deserializes a JSON string to a AutomaticActivityDetection object.
     */
    public static AutomaticActivityDetection fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, AutomaticActivityDetection.class);
    }

    /**
     * If enabled, detected voice and text input count as activity. If disabled, the client must send
     * activity signals.
     */
    @JsonProperty("disabled")
    public abstract Optional<Boolean> disabled();

    /**
     * Determines how likely speech is to be detected.
     */
    @JsonProperty("startOfSpeechSensitivity")
    public abstract Optional<StartSensitivity> startOfSpeechSensitivity();

    /**
     * Determines how likely detected speech is ended.
     */
    @JsonProperty("endOfSpeechSensitivity")
    public abstract Optional<EndSensitivity> endOfSpeechSensitivity();

    /**
     * The required duration of detected speech before start-of-speech is committed. The lower this
     * value the more sensitive the start-of-speech detection is and the shorter speech can be
     * recognized. However, this also increases the probability of false positives.
     */
    @JsonProperty("prefixPaddingMs")
    public abstract Optional<Integer> prefixPaddingMs();

    /**
     * The required duration of detected non-speech (e.g. silence) before end-of-speech is committed.
     * The larger this value, the longer speech gaps can be without interrupting the user's activity
     * but this will increase the model's latency.
     */
    @JsonProperty("silenceDurationMs")
    public abstract Optional<Integer> silenceDurationMs();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for AutomaticActivityDetection.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `AutomaticActivityDetection.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_AutomaticActivityDetection.Builder();
        }

        @JsonProperty("disabled")
        public abstract Builder disabled(boolean disabled);

        @JsonProperty("startOfSpeechSensitivity")
        public abstract Builder startOfSpeechSensitivity(StartSensitivity startOfSpeechSensitivity);

        @CanIgnoreReturnValue
        public Builder startOfSpeechSensitivity(StartSensitivity.Known knownType) {
            return startOfSpeechSensitivity(new StartSensitivity(knownType));
        }

        @CanIgnoreReturnValue
        public Builder startOfSpeechSensitivity(String startOfSpeechSensitivity) {
            return startOfSpeechSensitivity(new StartSensitivity(startOfSpeechSensitivity));
        }

        @JsonProperty("endOfSpeechSensitivity")
        public abstract Builder endOfSpeechSensitivity(EndSensitivity endOfSpeechSensitivity);

        @CanIgnoreReturnValue
        public Builder endOfSpeechSensitivity(EndSensitivity.Known knownType) {
            return endOfSpeechSensitivity(new EndSensitivity(knownType));
        }

        @CanIgnoreReturnValue
        public Builder endOfSpeechSensitivity(String endOfSpeechSensitivity) {
            return endOfSpeechSensitivity(new EndSensitivity(endOfSpeechSensitivity));
        }

        @JsonProperty("prefixPaddingMs")
        public abstract Builder prefixPaddingMs(Integer prefixPaddingMs);

        @JsonProperty("silenceDurationMs")
        public abstract Builder silenceDurationMs(Integer silenceDurationMs);

        public abstract AutomaticActivityDetection build();
    }
}
