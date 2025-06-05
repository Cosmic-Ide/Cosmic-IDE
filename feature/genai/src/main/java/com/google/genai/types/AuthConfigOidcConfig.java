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
 * Config for user OIDC auth.
 */
@AutoValue
@JsonDeserialize(builder = AuthConfigOidcConfig.Builder.class)
public abstract class AuthConfigOidcConfig extends JsonSerializable {
    /**
     * Instantiates a builder for AuthConfigOidcConfig.
     */
    public static Builder builder() {
        return new AutoValue_AuthConfigOidcConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a AuthConfigOidcConfig object.
     */
    public static AuthConfigOidcConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, AuthConfigOidcConfig.class);
    }

    /**
     * OpenID Connect formatted ID token for extension endpoint. Only used to propagate token from
     * [[ExecuteExtensionRequest.runtime_auth_config]] at request time.
     */
    @JsonProperty("idToken")
    public abstract Optional<String> idToken();

    /**
     * The service account used to generate an OpenID Connect (OIDC)-compatible JWT token signed by
     * the Google OIDC Provider (accounts.google.com) for extension endpoint
     * (https://cloud.google.com/iam/docs/create-short-lived-credentials-direct#sa-credentials-oidc).
     * - The audience for the token will be set to the URL in the server url defined in the OpenApi
     * spec. - If the service account is provided, the service account should grant
     * `iam.serviceAccounts.getOpenIdToken` permission to Vertex AI Extension Service Agent
     * (https://cloud.google.com/vertex-ai/docs/general/access-control#service-agents).
     */
    @JsonProperty("serviceAccount")
    public abstract Optional<String> serviceAccount();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for AuthConfigOidcConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `AuthConfigOidcConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_AuthConfigOidcConfig.Builder();
        }

        @JsonProperty("idToken")
        public abstract Builder idToken(String idToken);

        @JsonProperty("serviceAccount")
        public abstract Builder serviceAccount(String serviceAccount);

        public abstract AuthConfigOidcConfig build();
    }
}
