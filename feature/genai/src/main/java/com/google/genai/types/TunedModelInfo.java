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
 * A tuned machine learning model.
 */
@AutoValue
@JsonDeserialize(builder = TunedModelInfo.Builder.class)
public abstract class TunedModelInfo extends JsonSerializable {
    /**
     * Instantiates a builder for TunedModelInfo.
     */
    public static Builder builder() {
        return new AutoValue_TunedModelInfo.Builder();
    }

    /**
     * Deserializes a JSON string to a TunedModelInfo object.
     */
    public static TunedModelInfo fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, TunedModelInfo.class);
    }

    /**
     * ID of the base model that you want to tune.
     */
    @JsonProperty("baseModel")
    public abstract Optional<String> baseModel();

    /**
     * Date and time when the base model was created.
     */
    @JsonProperty("createTime")
    public abstract Optional<Instant> createTime();

    /**
     * Date and time when the base model was last updated.
     */
    @JsonProperty("updateTime")
    public abstract Optional<Instant> updateTime();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for TunedModelInfo.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `TunedModelInfo.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_TunedModelInfo.Builder();
        }

        @JsonProperty("baseModel")
        public abstract Builder baseModel(String baseModel);

        @JsonProperty("createTime")
        public abstract Builder createTime(Instant createTime);

        @JsonProperty("updateTime")
        public abstract Builder updateTime(Instant updateTime);

        public abstract TunedModelInfo build();
    }
}
