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
 * Config for HTTP Basic Authentication.
 */
@AutoValue
@JsonDeserialize(builder = AuthConfigHttpBasicAuthConfig.Builder.class)
public abstract class AuthConfigHttpBasicAuthConfig extends JsonSerializable {
    /**
     * Instantiates a builder for AuthConfigHttpBasicAuthConfig.
     */
    public static Builder builder() {
        return new AutoValue_AuthConfigHttpBasicAuthConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a AuthConfigHttpBasicAuthConfig object.
     */
    public static AuthConfigHttpBasicAuthConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, AuthConfigHttpBasicAuthConfig.class);
    }

    /**
     * Required. The name of the SecretManager secret version resource storing the base64 encoded
     * credentials. Format: `projects/{project}/secrets/{secrete}/versions/{version}` - If specified,
     * the `secretmanager.versions.access` permission should be granted to Vertex AI Extension Service
     * Agent (https://cloud.google.com/vertex-ai/docs/general/access-control#service-agents) on the
     * specified resource.
     */
    @JsonProperty("credentialSecret")
    public abstract Optional<String> credentialSecret();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for AuthConfigHttpBasicAuthConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `AuthConfigHttpBasicAuthConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_AuthConfigHttpBasicAuthConfig.Builder();
        }

        @JsonProperty("credentialSecret")
        public abstract Builder credentialSecret(String credentialSecret);

        public abstract AuthConfigHttpBasicAuthConfig build();
    }
}
