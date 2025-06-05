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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.genai.JsonSerializable;

import java.util.List;
import java.util.Optional;

/**
 * Generation config.
 */
@AutoValue
@JsonDeserialize(builder = GenerationConfig.Builder.class)
public abstract class GenerationConfig extends JsonSerializable {
    /**
     * Instantiates a builder for GenerationConfig.
     */
    public static Builder builder() {
        return new AutoValue_GenerationConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a GenerationConfig object.
     */
    public static GenerationConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GenerationConfig.class);
    }

    /**
     * Optional. Config for model selection.
     */
    @JsonProperty("modelSelectionConfig")
    public abstract Optional<ModelSelectionConfig> modelSelectionConfig();

    /**
     * Optional. If enabled, audio timestamp will be included in the request to the model.
     */
    @JsonProperty("audioTimestamp")
    public abstract Optional<Boolean> audioTimestamp();

    /**
     * Optional. Number of candidates to generate.
     */
    @JsonProperty("candidateCount")
    public abstract Optional<Integer> candidateCount();

    /**
     * Optional. Frequency penalties.
     */
    @JsonProperty("frequencyPenalty")
    public abstract Optional<Float> frequencyPenalty();

    /**
     * Optional. Logit probabilities.
     */
    @JsonProperty("logprobs")
    public abstract Optional<Integer> logprobs();

    /**
     * Optional. The maximum number of output tokens to generate per message.
     */
    @JsonProperty("maxOutputTokens")
    public abstract Optional<Integer> maxOutputTokens();

    /**
     * Optional. If specified, the media resolution specified will be used.
     */
    @JsonProperty("mediaResolution")
    public abstract Optional<MediaResolution> mediaResolution();

    /**
     * Optional. Positive penalties.
     */
    @JsonProperty("presencePenalty")
    public abstract Optional<Float> presencePenalty();

    /**
     * Optional. If true, export the logprobs results in response.
     */
    @JsonProperty("responseLogprobs")
    public abstract Optional<Boolean> responseLogprobs();

    /**
     * Optional. Output response mimetype of the generated candidate text. Supported mimetype: -
     * `text/plain`: (default) Text output. - `application/json`: JSON response in the candidates. The
     * model needs to be prompted to output the appropriate response type, otherwise the behavior is
     * undefined. This is a preview feature.
     */
    @JsonProperty("responseMimeType")
    public abstract Optional<String> responseMimeType();

    /**
     * Optional. The modalities of the response.
     */
    @JsonProperty("responseModalities")
    public abstract Optional<List<Modality>> responseModalities();

    /**
     * Optional. The `Schema` object allows the definition of input and output data types. These types
     * can be objects, but also primitives and arrays. Represents a select subset of an [OpenAPI 3.0
     * schema object](https://spec.openapis.org/oas/v3.0.3#schema). If set, a compatible
     * response_mime_type must also be set. Compatible mimetypes: `application/json`: Schema for JSON
     * response.
     */
    @JsonProperty("responseSchema")
    public abstract Optional<Schema> responseSchema();

    /**
     * Optional. Routing configuration.
     */
    @JsonProperty("routingConfig")
    public abstract Optional<GenerationConfigRoutingConfig> routingConfig();

    /**
     * Optional. Seed.
     */
    @JsonProperty("seed")
    public abstract Optional<Integer> seed();

    /**
     * Optional. The speech generation config.
     */
    @JsonProperty("speechConfig")
    public abstract Optional<SpeechConfig> speechConfig();

    /**
     * Optional. Stop sequences.
     */
    @JsonProperty("stopSequences")
    public abstract Optional<List<String>> stopSequences();

    /**
     * Optional. Controls the randomness of predictions.
     */
    @JsonProperty("temperature")
    public abstract Optional<Float> temperature();

    /**
     * Optional. Config for thinking features. An error will be returned if this field is set for
     * models that don't support thinking.
     */
    @JsonProperty("thinkingConfig")
    public abstract Optional<GenerationConfigThinkingConfig> thinkingConfig();

    /**
     * Optional. If specified, top-k sampling will be used.
     */
    @JsonProperty("topK")
    public abstract Optional<Float> topK();

    /**
     * Optional. If specified, nucleus sampling will be used.
     */
    @JsonProperty("topP")
    public abstract Optional<Float> topP();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GenerationConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GenerationConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GenerationConfig.Builder();
        }

        @JsonProperty("modelSelectionConfig")
        public abstract Builder modelSelectionConfig(ModelSelectionConfig modelSelectionConfig);

        @JsonProperty("audioTimestamp")
        public abstract Builder audioTimestamp(boolean audioTimestamp);

        @JsonProperty("candidateCount")
        public abstract Builder candidateCount(Integer candidateCount);

        @JsonProperty("frequencyPenalty")
        public abstract Builder frequencyPenalty(Float frequencyPenalty);

        @JsonProperty("logprobs")
        public abstract Builder logprobs(Integer logprobs);

        @JsonProperty("maxOutputTokens")
        public abstract Builder maxOutputTokens(Integer maxOutputTokens);

        @JsonProperty("mediaResolution")
        public abstract Builder mediaResolution(MediaResolution mediaResolution);

        @CanIgnoreReturnValue
        public Builder mediaResolution(MediaResolution.Known knownType) {
            return mediaResolution(new MediaResolution(knownType));
        }

        @CanIgnoreReturnValue
        public Builder mediaResolution(String mediaResolution) {
            return mediaResolution(new MediaResolution(mediaResolution));
        }

        @JsonProperty("presencePenalty")
        public abstract Builder presencePenalty(Float presencePenalty);

        @JsonProperty("responseLogprobs")
        public abstract Builder responseLogprobs(boolean responseLogprobs);

        @JsonProperty("responseMimeType")
        public abstract Builder responseMimeType(String responseMimeType);

        @JsonProperty("responseModalities")
        public abstract Builder responseModalities(List<Modality> responseModalities);

        @CanIgnoreReturnValue
        public Builder responseModalitiesFromKnown(List<Modality.Known> knownTypes) {
            ImmutableList<Modality> listItems =
                    knownTypes.stream().map(Modality::new).collect(toImmutableList());
            return responseModalities(listItems);
        }

        @CanIgnoreReturnValue
        public Builder responseModalitiesFromString(List<String> responseModalities) {
            ImmutableList<Modality> listItems =
                    responseModalities.stream().map(Modality::new).collect(toImmutableList());
            return responseModalities(listItems);
        }

        @JsonProperty("responseSchema")
        public abstract Builder responseSchema(Schema responseSchema);

        @JsonProperty("routingConfig")
        public abstract Builder routingConfig(GenerationConfigRoutingConfig routingConfig);

        @JsonProperty("seed")
        public abstract Builder seed(Integer seed);

        @JsonProperty("speechConfig")
        public abstract Builder speechConfig(SpeechConfig speechConfig);

        @JsonProperty("stopSequences")
        public abstract Builder stopSequences(List<String> stopSequences);

        @JsonProperty("temperature")
        public abstract Builder temperature(Float temperature);

        @JsonProperty("thinkingConfig")
        public abstract Builder thinkingConfig(GenerationConfigThinkingConfig thinkingConfig);

        @JsonProperty("topK")
        public abstract Builder topK(Float topK);

        @JsonProperty("topP")
        public abstract Builder topP(Float topP);

        public abstract GenerationConfig build();
    }
}
