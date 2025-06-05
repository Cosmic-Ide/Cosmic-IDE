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
import java.util.Optional;

/**
 * Represents a time interval, encoded as a start time (inclusive) and an end time (exclusive).
 *
 * <p>The start time must be less than or equal to the end time. When the start equals the end time,
 * the interval is an empty interval. (matches no time) When both start and end are unspecified, the
 * interval matches any time.
 */
@AutoValue
@JsonDeserialize(builder = Interval.Builder.class)
public abstract class Interval extends JsonSerializable {
    /**
     * Instantiates a builder for Interval.
     */
    public static Builder builder() {
        return new AutoValue_Interval.Builder();
    }

    /**
     * Deserializes a JSON string to a Interval object.
     */
    public static Interval fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, Interval.class);
    }

    /**
     * The start time of the interval.
     */
    @JsonProperty("startTime")
    public abstract Optional<Instant> startTime();

    /**
     * The end time of the interval.
     */
    @JsonProperty("endTime")
    public abstract Optional<Instant> endTime();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for Interval.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `Interval.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_Interval.Builder();
        }

        @JsonProperty("startTime")
        public abstract Builder startTime(Instant startTime);

        @JsonProperty("endTime")
        public abstract Builder endTime(Instant endTime);

        public abstract Interval build();
    }
}
