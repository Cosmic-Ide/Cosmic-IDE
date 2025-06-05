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
 * Parameters for sending client content to the live API.
 */
@AutoValue
@JsonDeserialize(builder = LiveSendClientContentParameters.Builder.class)
public abstract class LiveSendClientContentParameters extends JsonSerializable {
    /**
     * Instantiates a builder for LiveSendClientContentParameters.
     */
    public static Builder builder() {
        return new AutoValue_LiveSendClientContentParameters.Builder();
    }

    /**
     * Deserializes a JSON string to a LiveSendClientContentParameters object.
     */
    public static LiveSendClientContentParameters fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, LiveSendClientContentParameters.class);
    }

    /**
     * Client content to send to the session.
     */
    @JsonProperty("turns")
    public abstract Optional<List<Content>> turns();

    /**
     * If true, indicates that the server content generation should start with the currently
     * accumulated prompt. Otherwise, the server will await additional messages before starting
     * generation.
     */
    @JsonProperty("turnComplete")
    public abstract Optional<Boolean> turnComplete();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for LiveSendClientContentParameters.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `LiveSendClientContentParameters.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_LiveSendClientContentParameters.Builder();
        }

        @JsonProperty("turns")
        public abstract Builder turns(List<Content> turns);

        @JsonProperty("turnComplete")
        public abstract Builder turnComplete(boolean turnComplete);

        public abstract LiveSendClientContentParameters build();
    }
}
