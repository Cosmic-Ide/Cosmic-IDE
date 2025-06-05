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
 * Request for the client to execute the `function_calls` and return the responses with the matching
 * `id`s.
 */
@AutoValue
@JsonDeserialize(builder = LiveServerToolCall.Builder.class)
public abstract class LiveServerToolCall extends JsonSerializable {
    /**
     * Instantiates a builder for LiveServerToolCall.
     */
    public static Builder builder() {
        return new AutoValue_LiveServerToolCall.Builder();
    }

    /**
     * Deserializes a JSON string to a LiveServerToolCall object.
     */
    public static LiveServerToolCall fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, LiveServerToolCall.class);
    }

    /**
     * The function call to be executed.
     */
    @JsonProperty("functionCalls")
    public abstract Optional<List<FunctionCall>> functionCalls();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for LiveServerToolCall.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `LiveServerToolCall.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_LiveServerToolCall.Builder();
        }

        @JsonProperty("functionCalls")
        public abstract Builder functionCalls(List<FunctionCall> functionCalls);

        public abstract LiveServerToolCall build();
    }
}
