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
 * Safety rating corresponding to the generated content.
 */
@AutoValue
@JsonDeserialize(builder = SafetyRating.Builder.class)
public abstract class SafetyRating extends JsonSerializable {
    /**
     * Instantiates a builder for SafetyRating.
     */
    public static Builder builder() {
        return new AutoValue_SafetyRating.Builder();
    }

    /**
     * Deserializes a JSON string to a SafetyRating object.
     */
    public static SafetyRating fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, SafetyRating.class);
    }

    /**
     * Output only. Indicates whether the content was filtered out because of this rating.
     */
    @JsonProperty("blocked")
    public abstract Optional<Boolean> blocked();

    /**
     * Output only. Harm category.
     */
    @JsonProperty("category")
    public abstract Optional<HarmCategory> category();

    /**
     * Output only. Harm probability levels in the content.
     */
    @JsonProperty("probability")
    public abstract Optional<HarmProbability> probability();

    /**
     * Output only. Harm probability score.
     */
    @JsonProperty("probabilityScore")
    public abstract Optional<Float> probabilityScore();

    /**
     * Output only. Harm severity levels in the content.
     */
    @JsonProperty("severity")
    public abstract Optional<HarmSeverity> severity();

    /**
     * Output only. Harm severity score.
     */
    @JsonProperty("severityScore")
    public abstract Optional<Float> severityScore();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for SafetyRating.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `SafetyRating.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_SafetyRating.Builder();
        }

        @JsonProperty("blocked")
        public abstract Builder blocked(boolean blocked);

        @JsonProperty("category")
        public abstract Builder category(HarmCategory category);

        @CanIgnoreReturnValue
        public Builder category(HarmCategory.Known knownType) {
            return category(new HarmCategory(knownType));
        }

        @CanIgnoreReturnValue
        public Builder category(String category) {
            return category(new HarmCategory(category));
        }

        @JsonProperty("probability")
        public abstract Builder probability(HarmProbability probability);

        @CanIgnoreReturnValue
        public Builder probability(HarmProbability.Known knownType) {
            return probability(new HarmProbability(knownType));
        }

        @CanIgnoreReturnValue
        public Builder probability(String probability) {
            return probability(new HarmProbability(probability));
        }

        @JsonProperty("probabilityScore")
        public abstract Builder probabilityScore(Float probabilityScore);

        @JsonProperty("severity")
        public abstract Builder severity(HarmSeverity severity);

        @CanIgnoreReturnValue
        public Builder severity(HarmSeverity.Known knownType) {
            return severity(new HarmSeverity(knownType));
        }

        @CanIgnoreReturnValue
        public Builder severity(String severity) {
            return severity(new HarmSeverity(severity));
        }

        @JsonProperty("severityScore")
        public abstract Builder severityScore(Float severityScore);

        public abstract SafetyRating build();
    }
}
