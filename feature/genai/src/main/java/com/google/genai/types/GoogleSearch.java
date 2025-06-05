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
 * Tool to support Google Search in Model. Powered by Google.
 */
@AutoValue
@JsonDeserialize(builder = GoogleSearch.Builder.class)
public abstract class GoogleSearch extends JsonSerializable {
    /**
     * Instantiates a builder for GoogleSearch.
     */
    public static Builder builder() {
        return new AutoValue_GoogleSearch.Builder();
    }

    /**
     * Deserializes a JSON string to a GoogleSearch object.
     */
    public static GoogleSearch fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GoogleSearch.class);
    }

    /**
     * Optional. Filter search results to a specific time range. If customers set a start time, they
     * must set an end time (and vice versa).
     */
    @JsonProperty("timeRangeFilter")
    public abstract Optional<Interval> timeRangeFilter();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GoogleSearch.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GoogleSearch.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GoogleSearch.Builder();
        }

        @JsonProperty("timeRangeFilter")
        public abstract Builder timeRangeFilter(Interval timeRangeFilter);

        public abstract GoogleSearch build();
    }
}
