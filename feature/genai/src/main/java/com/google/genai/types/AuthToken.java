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
 * Config for auth_tokens.create parameters.
 */
@AutoValue
@JsonDeserialize(builder = AuthToken.Builder.class)
public abstract class AuthToken extends JsonSerializable {
    /**
     * Instantiates a builder for AuthToken.
     */
    public static Builder builder() {
        return new AutoValue_AuthToken.Builder();
    }

    /**
     * Deserializes a JSON string to a AuthToken object.
     */
    public static AuthToken fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, AuthToken.class);
    }

    /**
     * The name of the auth token.
     */
    @JsonProperty("name")
    public abstract Optional<String> name();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for AuthToken.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `AuthToken.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_AuthToken.Builder();
        }

        @JsonProperty("name")
        public abstract Builder name(String name);

        public abstract AuthToken build();
    }
}
