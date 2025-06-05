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
 * A response candidate generated from the model.
 */
@AutoValue
@JsonDeserialize(builder = Candidate.Builder.class)
public abstract class Candidate extends JsonSerializable {
    /**
     * Instantiates a builder for Candidate.
     */
    public static Builder builder() {
        return new AutoValue_Candidate.Builder();
    }

    /**
     * Deserializes a JSON string to a Candidate object.
     */
    public static Candidate fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, Candidate.class);
    }

    /**
     * Contains the multi-part content of the response.
     */
    @JsonProperty("content")
    public abstract Optional<Content> content();

    /**
     * Source attribution of the generated content.
     */
    @JsonProperty("citationMetadata")
    public abstract Optional<CitationMetadata> citationMetadata();

    /**
     * Describes the reason the model stopped generating tokens.
     */
    @JsonProperty("finishMessage")
    public abstract Optional<String> finishMessage();

    /**
     * Number of tokens for this candidate.
     */
    @JsonProperty("tokenCount")
    public abstract Optional<Integer> tokenCount();

    /**
     * The reason why the model stopped generating tokens. If empty, the model has not stopped
     * generating the tokens.
     */
    @JsonProperty("finishReason")
    public abstract Optional<FinishReason> finishReason();

    /**
     * Metadata related to url context retrieval tool.
     */
    @JsonProperty("urlContextMetadata")
    public abstract Optional<UrlContextMetadata> urlContextMetadata();

    /**
     * Output only. Average log probability score of the candidate.
     */
    @JsonProperty("avgLogprobs")
    public abstract Optional<Double> avgLogprobs();

    /**
     * Output only. Metadata specifies sources used to ground generated content.
     */
    @JsonProperty("groundingMetadata")
    public abstract Optional<GroundingMetadata> groundingMetadata();

    /**
     * Output only. Index of the candidate.
     */
    @JsonProperty("index")
    public abstract Optional<Integer> index();

    /**
     * Output only. Log-likelihood scores for the response tokens and top tokens
     */
    @JsonProperty("logprobsResult")
    public abstract Optional<LogprobsResult> logprobsResult();

    /**
     * Output only. List of ratings for the safety of a response candidate. There is at most one
     * rating per category.
     */
    @JsonProperty("safetyRatings")
    public abstract Optional<List<SafetyRating>> safetyRatings();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for Candidate.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `Candidate.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_Candidate.Builder();
        }

        @JsonProperty("content")
        public abstract Builder content(Content content);

        @JsonProperty("citationMetadata")
        public abstract Builder citationMetadata(CitationMetadata citationMetadata);

        @JsonProperty("finishMessage")
        public abstract Builder finishMessage(String finishMessage);

        @JsonProperty("tokenCount")
        public abstract Builder tokenCount(Integer tokenCount);

        @JsonProperty("finishReason")
        public abstract Builder finishReason(FinishReason finishReason);

        @CanIgnoreReturnValue
        public Builder finishReason(FinishReason.Known knownType) {
            return finishReason(new FinishReason(knownType));
        }

        @CanIgnoreReturnValue
        public Builder finishReason(String finishReason) {
            return finishReason(new FinishReason(finishReason));
        }

        @JsonProperty("urlContextMetadata")
        public abstract Builder urlContextMetadata(UrlContextMetadata urlContextMetadata);

        @JsonProperty("avgLogprobs")
        public abstract Builder avgLogprobs(Double avgLogprobs);

        @JsonProperty("groundingMetadata")
        public abstract Builder groundingMetadata(GroundingMetadata groundingMetadata);

        @JsonProperty("index")
        public abstract Builder index(Integer index);

        @JsonProperty("logprobsResult")
        public abstract Builder logprobsResult(LogprobsResult logprobsResult);

        @JsonProperty("safetyRatings")
        public abstract Builder safetyRatings(List<SafetyRating> safetyRatings);

        public abstract Candidate build();
    }
}
