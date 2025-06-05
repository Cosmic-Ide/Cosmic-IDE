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
 * Config for proactivity features.
 */
@AutoValue
@JsonDeserialize(builder = ProactivityConfig.Builder.class)
public abstract class ProactivityConfig extends JsonSerializable {
    /**
     * Instantiates a builder for ProactivityConfig.
     */
    public static Builder builder() {
        return new AutoValue_ProactivityConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a ProactivityConfig object.
     */
    public static ProactivityConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, ProactivityConfig.class);
    }

    /**
     * If enabled, the model can reject responding to the last prompt. For example, this allows the
     * model to ignore out of context speech or to stay silent if the user did not make a request,
     * yet.
     */
    @JsonProperty("proactiveAudio")
    public abstract Optional<Boolean> proactiveAudio();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for ProactivityConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `ProactivityConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_ProactivityConfig.Builder();
        }

        @JsonProperty("proactiveAudio")
        public abstract Builder proactiveAudio(boolean proactiveAudio);

        public abstract ProactivityConfig build();
    }
}
