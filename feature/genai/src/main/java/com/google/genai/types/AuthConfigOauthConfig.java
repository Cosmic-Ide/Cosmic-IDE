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
 * Config for user oauth.
 */
@AutoValue
@JsonDeserialize(builder = AuthConfigOauthConfig.Builder.class)
public abstract class AuthConfigOauthConfig extends JsonSerializable {
    /**
     * Instantiates a builder for AuthConfigOauthConfig.
     */
    public static Builder builder() {
        return new AutoValue_AuthConfigOauthConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a AuthConfigOauthConfig object.
     */
    public static AuthConfigOauthConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, AuthConfigOauthConfig.class);
    }

    /**
     * Access token for extension endpoint. Only used to propagate token from
     * [[ExecuteExtensionRequest.runtime_auth_config]] at request time.
     */
    @JsonProperty("accessToken")
    public abstract Optional<String> accessToken();

    /**
     * The service account used to generate access tokens for executing the Extension. - If the
     * service account is specified, the `iam.serviceAccounts.getAccessToken` permission should be
     * granted to Vertex AI Extension Service Agent
     * (https://cloud.google.com/vertex-ai/docs/general/access-control#service-agents) on the provided
     * service account.
     */
    @JsonProperty("serviceAccount")
    public abstract Optional<String> serviceAccount();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for AuthConfigOauthConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `AuthConfigOauthConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_AuthConfigOauthConfig.Builder();
        }

        @JsonProperty("accessToken")
        public abstract Builder accessToken(String accessToken);

        @JsonProperty("serviceAccount")
        public abstract Builder serviceAccount(String serviceAccount);

        public abstract AuthConfigOauthConfig build();
    }
}
