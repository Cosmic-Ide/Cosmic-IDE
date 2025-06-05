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
 * Config for LiveConstrainedParameters for Auth Token creation.
 */
@AutoValue
@JsonDeserialize(builder = LiveConstrainedParameters.Builder.class)
public abstract class LiveConstrainedParameters extends JsonSerializable {
    /**
     * Instantiates a builder for LiveConstrainedParameters.
     */
    public static Builder builder() {
        return new AutoValue_LiveConstrainedParameters.Builder();
    }

    /**
     * Deserializes a JSON string to a LiveConstrainedParameters object.
     */
    public static LiveConstrainedParameters fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, LiveConstrainedParameters.class);
    }

    /**
     * ID of the model to configure in the ephemeral token for Live API. For a list of models, see
     * `Gemini models <https://ai.google.dev/gemini-api/docs/models>`.
     */
    @JsonProperty("model")
    public abstract Optional<String> model();

    /**
     * Configuration specific to Live API connections created using this token.
     */
    @JsonProperty("config")
    public abstract Optional<LiveConnectConfig> config();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for LiveConstrainedParameters.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `LiveConstrainedParameters.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_LiveConstrainedParameters.Builder();
        }

        @JsonProperty("model")
        public abstract Builder model(String model);

        @JsonProperty("config")
        public abstract Builder config(LiveConnectConfig config);

        public abstract LiveConstrainedParameters build();
    }
}
