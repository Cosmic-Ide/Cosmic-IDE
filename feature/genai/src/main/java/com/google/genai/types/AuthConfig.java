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
 * Auth configuration to run the extension.
 */
@AutoValue
@JsonDeserialize(builder = AuthConfig.Builder.class)
public abstract class AuthConfig extends JsonSerializable {
    /**
     * Instantiates a builder for AuthConfig.
     */
    public static Builder builder() {
        return new AutoValue_AuthConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a AuthConfig object.
     */
    public static AuthConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, AuthConfig.class);
    }

    /**
     * Config for API key auth.
     */
    @JsonProperty("apiKeyConfig")
    public abstract Optional<ApiKeyConfig> apiKeyConfig();

    /**
     * Type of auth scheme.
     */
    @JsonProperty("authType")
    public abstract Optional<AuthType> authType();

    /**
     * Config for Google Service Account auth.
     */
    @JsonProperty("googleServiceAccountConfig")
    public abstract Optional<AuthConfigGoogleServiceAccountConfig> googleServiceAccountConfig();

    /**
     * Config for HTTP Basic auth.
     */
    @JsonProperty("httpBasicAuthConfig")
    public abstract Optional<AuthConfigHttpBasicAuthConfig> httpBasicAuthConfig();

    /**
     * Config for user oauth.
     */
    @JsonProperty("oauthConfig")
    public abstract Optional<AuthConfigOauthConfig> oauthConfig();

    /**
     * Config for user OIDC auth.
     */
    @JsonProperty("oidcConfig")
    public abstract Optional<AuthConfigOidcConfig> oidcConfig();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for AuthConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `AuthConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_AuthConfig.Builder();
        }

        @JsonProperty("apiKeyConfig")
        public abstract Builder apiKeyConfig(ApiKeyConfig apiKeyConfig);

        @JsonProperty("authType")
        public abstract Builder authType(AuthType authType);

        @CanIgnoreReturnValue
        public Builder authType(AuthType.Known knownType) {
            return authType(new AuthType(knownType));
        }

        @CanIgnoreReturnValue
        public Builder authType(String authType) {
            return authType(new AuthType(authType));
        }

        @JsonProperty("googleServiceAccountConfig")
        public abstract Builder googleServiceAccountConfig(
                AuthConfigGoogleServiceAccountConfig googleServiceAccountConfig);

        @JsonProperty("httpBasicAuthConfig")
        public abstract Builder httpBasicAuthConfig(AuthConfigHttpBasicAuthConfig httpBasicAuthConfig);

        @JsonProperty("oauthConfig")
        public abstract Builder oauthConfig(AuthConfigOauthConfig oauthConfig);

        @JsonProperty("oidcConfig")
        public abstract Builder oidcConfig(AuthConfigOidcConfig oidcConfig);

        public abstract AuthConfig build();
    }
}
