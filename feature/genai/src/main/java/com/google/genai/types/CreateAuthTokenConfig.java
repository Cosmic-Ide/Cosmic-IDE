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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Optional parameters.
 */
@AutoValue
@JsonDeserialize(builder = CreateAuthTokenConfig.Builder.class)
public abstract class CreateAuthTokenConfig extends JsonSerializable {
    /**
     * Instantiates a builder for CreateAuthTokenConfig.
     */
    public static Builder builder() {
        return new AutoValue_CreateAuthTokenConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a CreateAuthTokenConfig object.
     */
    public static CreateAuthTokenConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, CreateAuthTokenConfig.class);
    }

    /**
     * Used to override HTTP request options.
     */
    @JsonProperty("httpOptions")
    public abstract Optional<HttpOptions> httpOptions();

    /**
     * An optional time after which, when using the resulting token, messages in Live API sessions
     * will be rejected. (Gemini may preemptively close the session after this time.)
     *
     * <p>If not set then this defaults to 30 minutes in the future. If set, this value must be less
     * than 20 hours in the future.
     */
    @JsonProperty("expireTime")
    public abstract Optional<Instant> expireTime();

    /**
     * The time after which new Live API sessions using the token resulting from this request will be
     * rejected.
     *
     * <p>If not set this defaults to 60 seconds in the future. If set, this value must be less than
     * 20 hours in the future.
     */
    @JsonProperty("newSessionExpireTime")
    public abstract Optional<Instant> newSessionExpireTime();

    /**
     * The number of times the token can be used. If this value is zero then no limit is applied.
     * Default is 1. Resuming a Live API session does not count as a use.
     */
    @JsonProperty("uses")
    public abstract Optional<Integer> uses();

    /**
     * Configuration specific to Live API connections created using this token.
     */
    @JsonProperty("liveConnectConstraints")
    public abstract Optional<LiveConnectConstraints> liveConnectConstraints();

    /**
     * Additional fields to lock in the effective LiveConnectParameters.
     */
    @JsonProperty("lockAdditionalFields")
    public abstract Optional<List<String>> lockAdditionalFields();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for CreateAuthTokenConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `CreateAuthTokenConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_CreateAuthTokenConfig.Builder();
        }

        @JsonProperty("httpOptions")
        public abstract Builder httpOptions(HttpOptions httpOptions);

        @JsonProperty("expireTime")
        public abstract Builder expireTime(Instant expireTime);

        @JsonProperty("newSessionExpireTime")
        public abstract Builder newSessionExpireTime(Instant newSessionExpireTime);

        @JsonProperty("uses")
        public abstract Builder uses(Integer uses);

        @JsonProperty("liveConnectConstraints")
        public abstract Builder liveConnectConstraints(LiveConnectConstraints liveConnectConstraints);

        @JsonProperty("lockAdditionalFields")
        public abstract Builder lockAdditionalFields(List<String> lockAdditionalFields);

        public abstract CreateAuthTokenConfig build();
    }
}
