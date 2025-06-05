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
 * The configuration for the prebuilt speaker to use.
 */
@AutoValue
@JsonDeserialize(builder = PrebuiltVoiceConfig.Builder.class)
public abstract class PrebuiltVoiceConfig extends JsonSerializable {
    /**
     * Instantiates a builder for PrebuiltVoiceConfig.
     */
    public static Builder builder() {
        return new AutoValue_PrebuiltVoiceConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a PrebuiltVoiceConfig object.
     */
    public static PrebuiltVoiceConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, PrebuiltVoiceConfig.class);
    }

    /**
     * The name of the prebuilt voice to use.
     */
    @JsonProperty("voiceName")
    public abstract Optional<String> voiceName();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for PrebuiltVoiceConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `PrebuiltVoiceConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_PrebuiltVoiceConfig.Builder();
        }

        @JsonProperty("voiceName")
        public abstract Builder voiceName(String voiceName);

        public abstract PrebuiltVoiceConfig build();
    }
}
