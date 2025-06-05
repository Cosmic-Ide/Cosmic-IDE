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
import java.util.Map;
import java.util.Optional;

/**
 * Optional model configuration parameters.
 *
 * <p>For more information, see `Content generation parameters
 * <https://cloud.google.com/vertex-ai/generative-ai/docs/multimodal/content-generation-parameters>`_.
 */
@AutoValue
@JsonDeserialize(builder = GenerateContentConfig.Builder.class)
public abstract class GenerateContentConfig extends JsonSerializable {
    /**
     * Instantiates a builder for GenerateContentConfig.
     */
    public static Builder builder() {
        return new AutoValue_GenerateContentConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a GenerateContentConfig object.
     */
    public static GenerateContentConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GenerateContentConfig.class);
    }

    /**
     * Used to override HTTP request options.
     */
    @JsonProperty("httpOptions")
    public abstract Optional<HttpOptions> httpOptions();

    /**
     * Instructions for the model to steer it toward better performance. For example, "Answer as
     * concisely as possible" or "Don't use technical terms in your response".
     */
    @JsonProperty("systemInstruction")
    public abstract Optional<Content> systemInstruction();

    /**
     * Value that controls the degree of randomness in token selection. Lower temperatures are good
     * for prompts that require a less open-ended or creative response, while higher temperatures can
     * lead to more diverse or creative results.
     */
    @JsonProperty("temperature")
    public abstract Optional<Float> temperature();

    /**
     * Tokens are selected from the most to least probable until the sum of their probabilities equals
     * this value. Use a lower value for less random responses and a higher value for more random
     * responses.
     */
    @JsonProperty("topP")
    public abstract Optional<Float> topP();

    /**
     * For each token selection step, the ``top_k`` tokens with the highest probabilities are sampled.
     * Then tokens are further filtered based on ``top_p`` with the final token selected using
     * temperature sampling. Use a lower number for less random responses and a higher number for more
     * random responses.
     */
    @JsonProperty("topK")
    public abstract Optional<Float> topK();

    /**
     * Number of response variations to return.
     */
    @JsonProperty("candidateCount")
    public abstract Optional<Integer> candidateCount();

    /**
     * Maximum number of tokens that can be generated in the response.
     */
    @JsonProperty("maxOutputTokens")
    public abstract Optional<Integer> maxOutputTokens();

    /**
     * List of strings that tells the model to stop generating text if one of the strings is
     * encountered in the response.
     */
    @JsonProperty("stopSequences")
    public abstract Optional<List<String>> stopSequences();

    /**
     * Whether to return the log probabilities of the tokens that were chosen by the model at each
     * step.
     */
    @JsonProperty("responseLogprobs")
    public abstract Optional<Boolean> responseLogprobs();

    /**
     * Number of top candidate tokens to return the log probabilities for at each generation step.
     */
    @JsonProperty("logprobs")
    public abstract Optional<Integer> logprobs();

    /**
     * Positive values penalize tokens that already appear in the generated text, increasing the
     * probability of generating more diverse content.
     */
    @JsonProperty("presencePenalty")
    public abstract Optional<Float> presencePenalty();

    /**
     * Positive values penalize tokens that repeatedly appear in the generated text, increasing the
     * probability of generating more diverse content.
     */
    @JsonProperty("frequencyPenalty")
    public abstract Optional<Float> frequencyPenalty();

    /**
     * When ``seed`` is fixed to a specific number, the model makes a best effort to provide the same
     * response for repeated requests. By default, a random number is used.
     */
    @JsonProperty("seed")
    public abstract Optional<Integer> seed();

    /**
     * Output response mimetype of the generated candidate text. Supported mimetype: - `text/plain`:
     * (default) Text output. - `application/json`: JSON response in the candidates. The model needs
     * to be prompted to output the appropriate response type, otherwise the behavior is undefined.
     * This is a preview feature.
     */
    @JsonProperty("responseMimeType")
    public abstract Optional<String> responseMimeType();

    /**
     * The `Schema` object allows the definition of input and output data types. These types can be
     * objects, but also primitives and arrays. Represents a select subset of an [OpenAPI 3.0 schema
     * object](https://spec.openapis.org/oas/v3.0.3#schema). If set, a compatible response_mime_type
     * must also be set. Compatible mimetypes: `application/json`: Schema for JSON response.
     */
    @JsonProperty("responseSchema")
    public abstract Optional<Schema> responseSchema();

    /**
     * Configuration for model router requests.
     */
    @JsonProperty("routingConfig")
    public abstract Optional<GenerationConfigRoutingConfig> routingConfig();

    /**
     * Configuration for model selection.
     */
    @JsonProperty("modelSelectionConfig")
    public abstract Optional<ModelSelectionConfig> modelSelectionConfig();

    /**
     * Safety settings in the request to block unsafe content in the response.
     */
    @JsonProperty("safetySettings")
    public abstract Optional<List<SafetySetting>> safetySettings();

    /**
     * Code that enables the system to interact with external systems to perform an action outside of
     * the knowledge and scope of the model.
     */
    @JsonProperty("tools")
    public abstract Optional<List<Tool>> tools();

    /**
     * Associates model output to a specific function call.
     */
    @JsonProperty("toolConfig")
    public abstract Optional<ToolConfig> toolConfig();

    /**
     * Labels with user-defined metadata to break down billed charges.
     */
    @JsonProperty("labels")
    public abstract Optional<Map<String, String>> labels();

    /**
     * Resource name of a context cache that can be used in subsequent requests.
     */
    @JsonProperty("cachedContent")
    public abstract Optional<String> cachedContent();

    /**
     * The requested modalities of the response. Represents the set of modalities that the model can
     * return.
     */
    @JsonProperty("responseModalities")
    public abstract Optional<List<String>> responseModalities();

    /**
     * If specified, the media resolution specified will be used.
     */
    @JsonProperty("mediaResolution")
    public abstract Optional<MediaResolution> mediaResolution();

    /**
     * The speech generation configuration.
     */
    @JsonProperty("speechConfig")
    public abstract Optional<SpeechConfig> speechConfig();

    /**
     * If enabled, audio timestamp will be included in the request to the model.
     */
    @JsonProperty("audioTimestamp")
    public abstract Optional<Boolean> audioTimestamp();

    /**
     * The configuration for automatic function calling.
     */
    @JsonProperty("automaticFunctionCalling")
    public abstract Optional<AutomaticFunctionCallingConfig> automaticFunctionCalling();

    /**
     * The thinking features configuration.
     */
    @JsonProperty("thinkingConfig")
    public abstract Optional<ThinkingConfig> thinkingConfig();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GenerateContentConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GenerateContentConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GenerateContentConfig.Builder();
        }

        @JsonProperty("httpOptions")
        public abstract Builder httpOptions(HttpOptions httpOptions);

        @JsonProperty("systemInstruction")
        public abstract Builder systemInstruction(Content systemInstruction);

        @JsonProperty("temperature")
        public abstract Builder temperature(Float temperature);

        @JsonProperty("topP")
        public abstract Builder topP(Float topP);

        @JsonProperty("topK")
        public abstract Builder topK(Float topK);

        @JsonProperty("candidateCount")
        public abstract Builder candidateCount(Integer candidateCount);

        @JsonProperty("maxOutputTokens")
        public abstract Builder maxOutputTokens(Integer maxOutputTokens);

        @JsonProperty("stopSequences")
        public abstract Builder stopSequences(List<String> stopSequences);

        @JsonProperty("responseLogprobs")
        public abstract Builder responseLogprobs(boolean responseLogprobs);

        @JsonProperty("logprobs")
        public abstract Builder logprobs(Integer logprobs);

        @JsonProperty("presencePenalty")
        public abstract Builder presencePenalty(Float presencePenalty);

        @JsonProperty("frequencyPenalty")
        public abstract Builder frequencyPenalty(Float frequencyPenalty);

        @JsonProperty("seed")
        public abstract Builder seed(Integer seed);

        @JsonProperty("responseMimeType")
        public abstract Builder responseMimeType(String responseMimeType);

        @JsonProperty("responseSchema")
        public abstract Builder responseSchema(Schema responseSchema);

        @JsonProperty("routingConfig")
        public abstract Builder routingConfig(GenerationConfigRoutingConfig routingConfig);

        @JsonProperty("modelSelectionConfig")
        public abstract Builder modelSelectionConfig(ModelSelectionConfig modelSelectionConfig);

        @JsonProperty("safetySettings")
        public abstract Builder safetySettings(List<SafetySetting> safetySettings);

        @JsonProperty("tools")
        public abstract Builder tools(List<Tool> tools);

        @JsonProperty("toolConfig")
        public abstract Builder toolConfig(ToolConfig toolConfig);

        @JsonProperty("labels")
        public abstract Builder labels(Map<String, String> labels);

        @JsonProperty("cachedContent")
        public abstract Builder cachedContent(String cachedContent);

        @JsonProperty("responseModalities")
        public abstract Builder responseModalities(List<String> responseModalities);

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

        @JsonProperty("speechConfig")
        public abstract Builder speechConfig(SpeechConfig speechConfig);

        @JsonProperty("audioTimestamp")
        public abstract Builder audioTimestamp(boolean audioTimestamp);

        @JsonProperty("automaticFunctionCalling")
        public abstract Builder automaticFunctionCalling(
                AutomaticFunctionCallingConfig automaticFunctionCalling);

        @JsonProperty("thinkingConfig")
        public abstract Builder thinkingConfig(ThinkingConfig thinkingConfig);

        public abstract GenerateContentConfig build();
    }
}
