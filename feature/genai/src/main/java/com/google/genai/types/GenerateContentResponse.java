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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.genai.JsonSerializable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

/**
 * Response message for PredictionService.GenerateContent.
 */
@AutoValue
@JsonDeserialize(builder = GenerateContentResponse.Builder.class)
public abstract class GenerateContentResponse extends JsonSerializable {
    private static final Logger logger = Logger.getLogger(GenerateContentResponse.class.getName());
    private static final ImmutableSet<FinishReason.Known> EXPECTED_FINISH_REASONS =
            ImmutableSet.of(
                    FinishReason.Known.FINISH_REASON_UNSPECIFIED,
                    FinishReason.Known.STOP,
                    FinishReason.Known.MAX_TOKENS);

    /**
     * Instantiates a builder for GenerateContentResponse.
     */
    public static Builder builder() {
        return new AutoValue_GenerateContentResponse.Builder();
    }

    /**
     * Deserializes a JSON string to a GenerateContentResponse object.
     */
    public static GenerateContentResponse fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GenerateContentResponse.class);
    }

    /**
     * Response variations returned by the model.
     */
    @JsonProperty("candidates")
    public abstract Optional<List<Candidate>> candidates();

    /**
     * Timestamp when the request is made to the server.
     */
    @JsonProperty("createTime")
    public abstract Optional<Instant> createTime();

    /**
     * Identifier for each response.
     */
    @JsonProperty("responseId")
    public abstract Optional<String> responseId();

    /**
     * The history of automatic function calling.
     */
    @JsonProperty("automaticFunctionCallingHistory")
    public abstract Optional<List<Content>> automaticFunctionCallingHistory();

    /**
     * Output only. The model version used to generate the response.
     */
    @JsonProperty("modelVersion")
    public abstract Optional<String> modelVersion();

    /**
     * Output only. Content filter results for a prompt sent in the request. Note: Sent only in the
     * first stream chunk. Only happens when no candidates were generated due to content violations.
     */
    @JsonProperty("promptFeedback")
    public abstract Optional<GenerateContentResponsePromptFeedback> promptFeedback();

    /**
     * Usage metadata about the response(s).
     */
    @JsonProperty("usageMetadata")
    public abstract Optional<GenerateContentResponseUsageMetadata> usageMetadata();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Returns the list of parts in the first candidate of the response.
     *
     * <p>Returns null if there is no candidate or no content in the first candidate
     */
    public @Nullable ImmutableList<Part> parts() {
        checkFinishReason();

        Optional<List<Candidate>> candidates = candidates();
        if (!candidates.isPresent() || candidates.get().isEmpty()) {
            return null;
        }

        Optional<Content> content = candidates.get().get(0).content();
        if (!content.isPresent()) {
            return null;
        }

        return ImmutableList.copyOf(content.get().parts().orElse(new ArrayList<>()));
    }

    /**
     * Returns the concatenation of all text parts in the first candidate of the response.
     *
     * <p>Returns null if there is no candidate, no content in the first candidate, or no parts in the
     * content.
     */
    public @Nullable String text() {
        return Content.aggregateTextFromParts(parts());
    }

    /**
     * Returns the list of function calls in the response.
     *
     * <p>Returns null if there is no candidate, no content in the first candidate, or no parts in the
     * content.
     */
    public @Nullable ImmutableList<FunctionCall> functionCalls() {
        ImmutableList<Part> parts = parts();
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        return ImmutableList.copyOf(
                parts.stream()
                        .filter(part -> part.functionCall().isPresent())
                        .map(part -> part.functionCall().get())
                        .collect(Collectors.toList()));
    }

    /**
     * Returns the executable code in the response.
     *
     * <p>Returns null if there is no candidate, no content in the first candidate, or no parts in the
     * content, or no executable code in the parts.
     */
    public @Nullable String executableCode() {
        ImmutableList<Part> parts = parts();
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        for (Part part : parts) {
            if (part.executableCode().isPresent()) {
                return part.executableCode().get().code().orElse("");
            }
        }

        return null;
    }

    /**
     * Returns the code execution result in the response.
     *
     * <p>Returns null if there is no candidate, no content in the first candidate, or no parts in the
     * content, or no code execution result in the parts.
     */
    public @Nullable String codeExecutionResult() {
        ImmutableList<Part> parts = parts();
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        for (Part part : parts) {
            if (part.codeExecutionResult().isPresent()) {
                return part.codeExecutionResult().get().output().orElse("");
            }
        }

        return null;
    }

    /**
     * Gets the finish reason in a GenerateContentResponse.
     */
    @SuppressWarnings("NotPresentToEmptyOptional")
    public FinishReason finishReason() {
        List<Candidate> candidates = candidates().orElse(Arrays.asList(Candidate.builder().build()));
        if (candidates.size() > 1) {
            logger.warning(
                    String.format(
                            "This response has %d candidates, will only use the first candidate",
                            candidates.size()));
        }
        Optional<FinishReason> finishReason = candidates.get(0).finishReason();
        if (!finishReason.isPresent()) {
            return new FinishReason(FinishReason.Known.FINISH_REASON_UNSPECIFIED);
        }
        return finishReason.get();
    }

    /**
     * Throws an exception if the response finishes unexpectedly.
     */
    public void checkFinishReason() {
        FinishReason finishReason = this.finishReason();
        if (!EXPECTED_FINISH_REASONS.contains(finishReason.knownEnum())) {
            throw new IllegalArgumentException(
                    String.format("The response finished unexpectedly with reason %s.", finishReason));
        }
    }

    /**
     * Builder for GenerateContentResponse.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GenerateContentResponse.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GenerateContentResponse.Builder();
        }

        @JsonProperty("candidates")
        public abstract Builder candidates(List<Candidate> candidates);

        @JsonProperty("createTime")
        public abstract Builder createTime(Instant createTime);

        @JsonProperty("responseId")
        public abstract Builder responseId(String responseId);

        @JsonProperty("automaticFunctionCallingHistory")
        public abstract Builder automaticFunctionCallingHistory(
                List<Content> automaticFunctionCallingHistory);

        @JsonProperty("modelVersion")
        public abstract Builder modelVersion(String modelVersion);

        @JsonProperty("promptFeedback")
        public abstract Builder promptFeedback(GenerateContentResponsePromptFeedback promptFeedback);

        @JsonProperty("usageMetadata")
        public abstract Builder usageMetadata(GenerateContentResponseUsageMetadata usageMetadata);

        public abstract GenerateContentResponse build();
    }
}
