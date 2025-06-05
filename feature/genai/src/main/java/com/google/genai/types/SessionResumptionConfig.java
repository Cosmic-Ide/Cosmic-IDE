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
 * Configuration of session resumption mechanism.
 *
 * <p>Included in `LiveConnectConfig.session_resumption`. If included server will send
 * `LiveServerSessionResumptionUpdate` messages.
 */
@AutoValue
@JsonDeserialize(builder = SessionResumptionConfig.Builder.class)
public abstract class SessionResumptionConfig extends JsonSerializable {
    /**
     * Instantiates a builder for SessionResumptionConfig.
     */
    public static Builder builder() {
        return new AutoValue_SessionResumptionConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a SessionResumptionConfig object.
     */
    public static SessionResumptionConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, SessionResumptionConfig.class);
    }

    /**
     * Session resumption handle of previous session (session to restore).
     *
     * <p>If not present new session will be started.
     */
    @JsonProperty("handle")
    public abstract Optional<String> handle();

    /**
     * If set the server will send `last_consumed_client_message_index` in the
     * `session_resumption_update` messages to allow for transparent reconnections.
     */
    @JsonProperty("transparent")
    public abstract Optional<Boolean> transparent();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for SessionResumptionConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `SessionResumptionConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_SessionResumptionConfig.Builder();
        }

        @JsonProperty("handle")
        public abstract Builder handle(String handle);

        @JsonProperty("transparent")
        public abstract Builder transparent(boolean transparent);

        public abstract SessionResumptionConfig build();
    }
}
