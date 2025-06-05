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

import java.util.List;
import java.util.Optional;

/**
 * Content filter results for a prompt sent in the request.
 */
@AutoValue
@JsonDeserialize(builder = GenerateContentResponsePromptFeedback.Builder.class)
public abstract class GenerateContentResponsePromptFeedback extends JsonSerializable {
    /**
     * Instantiates a builder for GenerateContentResponsePromptFeedback.
     */
    public static Builder builder() {
        return new AutoValue_GenerateContentResponsePromptFeedback.Builder();
    }

    /**
     * Deserializes a JSON string to a GenerateContentResponsePromptFeedback object.
     */
    public static GenerateContentResponsePromptFeedback fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GenerateContentResponsePromptFeedback.class);
    }

    /**
     * Output only. Blocked reason.
     */
    @JsonProperty("blockReason")
    public abstract Optional<BlockedReason> blockReason();

    /**
     * Output only. A readable block reason message.
     */
    @JsonProperty("blockReasonMessage")
    public abstract Optional<String> blockReasonMessage();

    /**
     * Output only. Safety ratings.
     */
    @JsonProperty("safetyRatings")
    public abstract Optional<List<SafetyRating>> safetyRatings();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GenerateContentResponsePromptFeedback.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GenerateContentResponsePromptFeedback.builder()` for
         * instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GenerateContentResponsePromptFeedback.Builder();
        }

        @JsonProperty("blockReason")
        public abstract Builder blockReason(BlockedReason blockReason);

        @CanIgnoreReturnValue
        public Builder blockReason(BlockedReason.Known knownType) {
            return blockReason(new BlockedReason(knownType));
        }

        @CanIgnoreReturnValue
        public Builder blockReason(String blockReason) {
            return blockReason(new BlockedReason(blockReason));
        }

        @JsonProperty("blockReasonMessage")
        public abstract Builder blockReasonMessage(String blockReasonMessage);

        @JsonProperty("safetyRatings")
        public abstract Builder safetyRatings(List<SafetyRating> safetyRatings);

        public abstract GenerateContentResponsePromptFeedback build();
    }
}
