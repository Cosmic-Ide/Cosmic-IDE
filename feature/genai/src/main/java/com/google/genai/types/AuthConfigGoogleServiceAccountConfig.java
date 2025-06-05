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
 * Config for Google Service Account Authentication.
 */
@AutoValue
@JsonDeserialize(builder = AuthConfigGoogleServiceAccountConfig.Builder.class)
public abstract class AuthConfigGoogleServiceAccountConfig extends JsonSerializable {
    /**
     * Instantiates a builder for AuthConfigGoogleServiceAccountConfig.
     */
    public static Builder builder() {
        return new AutoValue_AuthConfigGoogleServiceAccountConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a AuthConfigGoogleServiceAccountConfig object.
     */
    public static AuthConfigGoogleServiceAccountConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, AuthConfigGoogleServiceAccountConfig.class);
    }

    /**
     * Optional. The service account that the extension execution service runs as. - If the service
     * account is specified, the `iam.serviceAccounts.getAccessToken` permission should be granted to
     * Vertex AI Extension Service Agent
     * (https://cloud.google.com/vertex-ai/docs/general/access-control#service-agents) on the
     * specified service account. - If not specified, the Vertex AI Extension Service Agent will be
     * used to execute the Extension.
     */
    @JsonProperty("serviceAccount")
    public abstract Optional<String> serviceAccount();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for AuthConfigGoogleServiceAccountConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `AuthConfigGoogleServiceAccountConfig.builder()` for
         * instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_AuthConfigGoogleServiceAccountConfig.Builder();
        }

        @JsonProperty("serviceAccount")
        public abstract Builder serviceAccount(String serviceAccount);

        public abstract AuthConfigGoogleServiceAccountConfig build();
    }
}
