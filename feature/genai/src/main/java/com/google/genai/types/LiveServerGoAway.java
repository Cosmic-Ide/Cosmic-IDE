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

import java.time.Duration;
import java.util.Optional;

/**
 * Server will not be able to service client soon.
 */
@AutoValue
@JsonDeserialize(builder = LiveServerGoAway.Builder.class)
public abstract class LiveServerGoAway extends JsonSerializable {
    /**
     * Instantiates a builder for LiveServerGoAway.
     */
    public static Builder builder() {
        return new AutoValue_LiveServerGoAway.Builder();
    }

    /**
     * Deserializes a JSON string to a LiveServerGoAway object.
     */
    public static LiveServerGoAway fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, LiveServerGoAway.class);
    }

    /**
     * The remaining time before the connection will be terminated as ABORTED. The minimal time
     * returned here is specified differently together with the rate limits for a given model.
     */
    @JsonProperty("timeLeft")
    public abstract Optional<Duration> timeLeft();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for LiveServerGoAway.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `LiveServerGoAway.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_LiveServerGoAway.Builder();
        }

        @JsonProperty("timeLeft")
        public abstract Builder timeLeft(Duration timeLeft);

        public abstract LiveServerGoAway build();
    }
}
