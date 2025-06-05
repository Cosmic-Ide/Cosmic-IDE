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
 * Marks the end of user activity.
 *
 * <p>This can only be sent if automatic (i.e. server-side) activity detection is disabled.
 */
@AutoValue
@JsonDeserialize(builder = RealtimeInputConfig.Builder.class)
public abstract class RealtimeInputConfig extends JsonSerializable {
    /**
     * Instantiates a builder for RealtimeInputConfig.
     */
    public static Builder builder() {
        return new AutoValue_RealtimeInputConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a RealtimeInputConfig object.
     */
    public static RealtimeInputConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, RealtimeInputConfig.class);
    }

    /**
     * If not set, automatic activity detection is enabled by default. If automatic voice detection is
     * disabled, the client must send activity signals.
     */
    @JsonProperty("automaticActivityDetection")
    public abstract Optional<AutomaticActivityDetection> automaticActivityDetection();

    /**
     * Defines what effect activity has.
     */
    @JsonProperty("activityHandling")
    public abstract Optional<ActivityHandling> activityHandling();

    /**
     * Defines which input is included in the user's turn.
     */
    @JsonProperty("turnCoverage")
    public abstract Optional<TurnCoverage> turnCoverage();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for RealtimeInputConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `RealtimeInputConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_RealtimeInputConfig.Builder();
        }

        @JsonProperty("automaticActivityDetection")
        public abstract Builder automaticActivityDetection(
                AutomaticActivityDetection automaticActivityDetection);

        @JsonProperty("activityHandling")
        public abstract Builder activityHandling(ActivityHandling activityHandling);

        @CanIgnoreReturnValue
        public Builder activityHandling(ActivityHandling.Known knownType) {
            return activityHandling(new ActivityHandling(knownType));
        }

        @CanIgnoreReturnValue
        public Builder activityHandling(String activityHandling) {
            return activityHandling(new ActivityHandling(activityHandling));
        }

        @JsonProperty("turnCoverage")
        public abstract Builder turnCoverage(TurnCoverage turnCoverage);

        @CanIgnoreReturnValue
        public Builder turnCoverage(TurnCoverage.Known knownType) {
            return turnCoverage(new TurnCoverage(knownType));
        }

        @CanIgnoreReturnValue
        public Builder turnCoverage(String turnCoverage) {
            return turnCoverage(new TurnCoverage(turnCoverage));
        }

        public abstract RealtimeInputConfig build();
    }
}
