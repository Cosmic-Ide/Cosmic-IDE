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
 * Describes the machine learning model version checkpoint.
 */
@AutoValue
@JsonDeserialize(builder = Checkpoint.Builder.class)
public abstract class Checkpoint extends JsonSerializable {
    /**
     * Instantiates a builder for Checkpoint.
     */
    public static Builder builder() {
        return new AutoValue_Checkpoint.Builder();
    }

    /**
     * Deserializes a JSON string to a Checkpoint object.
     */
    public static Checkpoint fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, Checkpoint.class);
    }

    /**
     * The ID of the checkpoint.
     */
    @JsonProperty("checkpointId")
    public abstract Optional<String> checkpointId();

    /**
     * The epoch of the checkpoint.
     */
    @JsonProperty("epoch")
    public abstract Optional<Long> epoch();

    /**
     * The step of the checkpoint.
     */
    @JsonProperty("step")
    public abstract Optional<Long> step();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for Checkpoint.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `Checkpoint.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_Checkpoint.Builder();
        }

        @JsonProperty("checkpointId")
        public abstract Builder checkpointId(String checkpointId);

        @JsonProperty("epoch")
        public abstract Builder epoch(Long epoch);

        @JsonProperty("step")
        public abstract Builder step(Long step);

        public abstract Checkpoint build();
    }
}
