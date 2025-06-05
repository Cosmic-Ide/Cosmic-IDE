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
 * Usage metadata about response(s).
 */
@AutoValue
@JsonDeserialize(builder = UsageMetadata.Builder.class)
public abstract class UsageMetadata extends JsonSerializable {
    /**
     * Instantiates a builder for UsageMetadata.
     */
    public static Builder builder() {
        return new AutoValue_UsageMetadata.Builder();
    }

    /**
     * Deserializes a JSON string to a UsageMetadata object.
     */
    public static UsageMetadata fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, UsageMetadata.class);
    }

    /**
     * Number of tokens in the prompt. When `cached_content` is set, this is still the total effective
     * prompt size meaning this includes the number of tokens in the cached content.
     */
    @JsonProperty("promptTokenCount")
    public abstract Optional<Integer> promptTokenCount();

    /**
     * Number of tokens in the cached part of the prompt (the cached content).
     */
    @JsonProperty("cachedContentTokenCount")
    public abstract Optional<Integer> cachedContentTokenCount();

    /**
     * Total number of tokens across all the generated response candidates.
     */
    @JsonProperty("responseTokenCount")
    public abstract Optional<Integer> responseTokenCount();

    /**
     * Number of tokens present in tool-use prompt(s).
     */
    @JsonProperty("toolUsePromptTokenCount")
    public abstract Optional<Integer> toolUsePromptTokenCount();

    /**
     * Number of tokens of thoughts for thinking models.
     */
    @JsonProperty("thoughtsTokenCount")
    public abstract Optional<Integer> thoughtsTokenCount();

    /**
     * Total token count for prompt, response candidates, and tool-use prompts(if present).
     */
    @JsonProperty("totalTokenCount")
    public abstract Optional<Integer> totalTokenCount();

    /**
     * List of modalities that were processed in the request input.
     */
    @JsonProperty("promptTokensDetails")
    public abstract Optional<List<ModalityTokenCount>> promptTokensDetails();

    /**
     * List of modalities that were processed in the cache input.
     */
    @JsonProperty("cacheTokensDetails")
    public abstract Optional<List<ModalityTokenCount>> cacheTokensDetails();

    /**
     * List of modalities that were returned in the response.
     */
    @JsonProperty("responseTokensDetails")
    public abstract Optional<List<ModalityTokenCount>> responseTokensDetails();

    /**
     * List of modalities that were processed in the tool-use prompt.
     */
    @JsonProperty("toolUsePromptTokensDetails")
    public abstract Optional<List<ModalityTokenCount>> toolUsePromptTokensDetails();

    /**
     * Traffic type. This shows whether a request consumes Pay-As-You-Go or Provisioned Throughput
     * quota.
     */
    @JsonProperty("trafficType")
    public abstract Optional<TrafficType> trafficType();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for UsageMetadata.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `UsageMetadata.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_UsageMetadata.Builder();
        }

        @JsonProperty("promptTokenCount")
        public abstract Builder promptTokenCount(Integer promptTokenCount);

        @JsonProperty("cachedContentTokenCount")
        public abstract Builder cachedContentTokenCount(Integer cachedContentTokenCount);

        @JsonProperty("responseTokenCount")
        public abstract Builder responseTokenCount(Integer responseTokenCount);

        @JsonProperty("toolUsePromptTokenCount")
        public abstract Builder toolUsePromptTokenCount(Integer toolUsePromptTokenCount);

        @JsonProperty("thoughtsTokenCount")
        public abstract Builder thoughtsTokenCount(Integer thoughtsTokenCount);

        @JsonProperty("totalTokenCount")
        public abstract Builder totalTokenCount(Integer totalTokenCount);

        @JsonProperty("promptTokensDetails")
        public abstract Builder promptTokensDetails(List<ModalityTokenCount> promptTokensDetails);

        @JsonProperty("cacheTokensDetails")
        public abstract Builder cacheTokensDetails(List<ModalityTokenCount> cacheTokensDetails);

        @JsonProperty("responseTokensDetails")
        public abstract Builder responseTokensDetails(List<ModalityTokenCount> responseTokensDetails);

        @JsonProperty("toolUsePromptTokensDetails")
        public abstract Builder toolUsePromptTokensDetails(
                List<ModalityTokenCount> toolUsePromptTokensDetails);

        @JsonProperty("trafficType")
        public abstract Builder trafficType(TrafficType trafficType);

        @CanIgnoreReturnValue
        public Builder trafficType(TrafficType.Known knownType) {
            return trafficType(new TrafficType(knownType));
        }

        @CanIgnoreReturnValue
        public Builder trafficType(String trafficType) {
            return trafficType(new TrafficType(trafficType));
        }

        public abstract UsageMetadata build();
    }
}
