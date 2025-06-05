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

import java.util.Map;
import java.util.Optional;

/**
 * A function call.
 */
@AutoValue
@JsonDeserialize(builder = FunctionCall.Builder.class)
public abstract class FunctionCall extends JsonSerializable {
    /**
     * Instantiates a builder for FunctionCall.
     */
    public static Builder builder() {
        return new AutoValue_FunctionCall.Builder();
    }

    /**
     * Deserializes a JSON string to a FunctionCall object.
     */
    public static FunctionCall fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, FunctionCall.class);
    }

    /**
     * The unique id of the function call. If populated, the client to execute the `function_call` and
     * return the response with the matching `id`.
     */
    @JsonProperty("id")
    public abstract Optional<String> id();

    /**
     * Optional. The function parameters and values in JSON object format. See
     * [FunctionDeclaration.parameters] for parameter details.
     */
    @JsonProperty("args")
    public abstract Optional<Map<String, Object>> args();

    /**
     * Required. The name of the function to call. Matches [FunctionDeclaration.name].
     */
    @JsonProperty("name")
    public abstract Optional<String> name();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for FunctionCall.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `FunctionCall.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_FunctionCall.Builder();
        }

        @JsonProperty("id")
        public abstract Builder id(String id);

        @JsonProperty("args")
        public abstract Builder args(Map<String, Object> args);

        @JsonProperty("name")
        public abstract Builder name(String name);

        public abstract FunctionCall build();
    }
}
