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
 * Result of executing the [ExecutableCode]. Always follows a `part` containing the
 * [ExecutableCode].
 */
@AutoValue
@JsonDeserialize(builder = CodeExecutionResult.Builder.class)
public abstract class CodeExecutionResult extends JsonSerializable {
    /**
     * Instantiates a builder for CodeExecutionResult.
     */
    public static Builder builder() {
        return new AutoValue_CodeExecutionResult.Builder();
    }

    /**
     * Deserializes a JSON string to a CodeExecutionResult object.
     */
    public static CodeExecutionResult fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, CodeExecutionResult.class);
    }

    /**
     * Required. Outcome of the code execution.
     */
    @JsonProperty("outcome")
    public abstract Optional<Outcome> outcome();

    /**
     * Optional. Contains stdout when code execution is successful, stderr or other description
     * otherwise.
     */
    @JsonProperty("output")
    public abstract Optional<String> output();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for CodeExecutionResult.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `CodeExecutionResult.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_CodeExecutionResult.Builder();
        }

        @JsonProperty("outcome")
        public abstract Builder outcome(Outcome outcome);

        @CanIgnoreReturnValue
        public Builder outcome(Outcome.Known knownType) {
            return outcome(new Outcome(knownType));
        }

        @CanIgnoreReturnValue
        public Builder outcome(String outcome) {
            return outcome(new Outcome(outcome));
        }

        @JsonProperty("output")
        public abstract Builder output(String output);

        public abstract CodeExecutionResult build();
    }
}
