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
 * Parameters for sending tool responses to the live API.
 */
@AutoValue
@JsonDeserialize(builder = LiveSendToolResponseParameters.Builder.class)
public abstract class LiveSendToolResponseParameters extends JsonSerializable {
    /**
     * Instantiates a builder for LiveSendToolResponseParameters.
     */
    public static Builder builder() {
        return new AutoValue_LiveSendToolResponseParameters.Builder();
    }

    /**
     * Deserializes a JSON string to a LiveSendToolResponseParameters object.
     */
    public static LiveSendToolResponseParameters fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, LiveSendToolResponseParameters.class);
    }

    /**
     * Tool responses to send to the session.
     */
    @JsonProperty("functionResponses")
    public abstract Optional<List<FunctionResponse>> functionResponses();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for LiveSendToolResponseParameters.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `LiveSendToolResponseParameters.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_LiveSendToolResponseParameters.Builder();
        }

        @JsonProperty("functionResponses")
        public abstract Builder functionResponses(List<FunctionResponse> functionResponses);

        public abstract LiveSendToolResponseParameters build();
    }
}
