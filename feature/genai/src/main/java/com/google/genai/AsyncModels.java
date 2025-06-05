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

package com.google.genai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.ComputeTokensConfig;
import com.google.genai.types.ComputeTokensResponse;
import com.google.genai.types.Content;
import com.google.genai.types.CountTokensConfig;
import com.google.genai.types.CountTokensResponse;
import com.google.genai.types.DeleteModelConfig;
import com.google.genai.types.DeleteModelResponse;
import com.google.genai.types.EditImageConfig;
import com.google.genai.types.EditImageResponse;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateImagesConfig;
import com.google.genai.types.GenerateImagesResponse;
import com.google.genai.types.GenerateVideosConfig;
import com.google.genai.types.GenerateVideosOperation;
import com.google.genai.types.GetModelConfig;
import com.google.genai.types.Image;
import com.google.genai.types.ListModelsConfig;
import com.google.genai.types.Model;
import com.google.genai.types.ReferenceImage;
import com.google.genai.types.UpdateModelConfig;
import com.google.genai.types.UpscaleImageConfig;
import com.google.genai.types.UpscaleImageResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Async module of {@link Models}
 */
public final class AsyncModels {
    Models models;

    public AsyncModels(ApiClient apiClient) {
        this.models = new Models(apiClient);
    }

    /**
     * Asynchronously fetches information about a model by name.
     *
     * @example ```java Model model = client.models.get("gemini-2.0-flash"); ```
     */
    public CompletableFuture<Model> get(String model, GetModelConfig config) {
        return CompletableFuture.supplyAsync(() -> models.get(model, config));
    }

    /**
     * Asynchronously updates a tuned model by its name.
     *
     * @param model  The name of the tuned model to update
     * @param config A {@link com.google.genai.types.UpdateModelConfig} instance that specifies the
     *               optional configurations
     * @return A {@link com.google.genai.types.Model} instance
     * @example ```java Model model = client.models.update( "tunedModels/12345",
     * UpdateModelConfig.builder() .displayName("New display name") .description("New
     * description") .build()); ```
     */
    public CompletableFuture<Model> update(String model, UpdateModelConfig config) {
        return CompletableFuture.supplyAsync(() -> models.update(model, config));
    }

    /**
     * Asynchronously fetches information about a model by name.
     *
     * @example ```java Model model = client.models.delete("tunedModels/12345"); ```
     */
    public CompletableFuture<DeleteModelResponse> delete(String model, DeleteModelConfig config) {
        return CompletableFuture.supplyAsync(() -> models.delete(model, config));
    }

    /**
     * Asynchronously counts tokens given a GenAI model and a list of content.
     *
     * @param model    the name of the GenAI model to use.
     * @param contents a {@link List<com.google.genai.types.Content>} to send to count tokens for.
     * @param config   a {@link com.google.genai.types.CountTokensConfig} instance that specifies the
     *                 optional configurations
     * @return a {@link com.google.genai.types.CountTokensResponse} instance that contains tokens
     * count.
     */
    public CompletableFuture<CountTokensResponse> countTokens(
            String model, List<Content> contents, CountTokensConfig config) {
        return CompletableFuture.supplyAsync(() -> models.countTokens(model, contents, config));
    }

    /**
     * Asynchronously generates videos given a GenAI model, and a prompt or an image.
     *
     * <p>This method is experimental.
     *
     * @param model  the name of the GenAI model to use for generating videos
     * @param prompt the text prompt for generating the videos. Optional for image to video use cases.
     * @param image  the input image for generating the videos. Optional if prompt is provided.
     * @param config a {@link com.google.genai.types.GenerateVideosConfig} instance that specifies the
     *               optional configurations
     * @return a {@link com.google.genai.types.GenerateVideosOperation} instance that contains the
     * generated videos.
     */
    public CompletableFuture<GenerateVideosOperation> generateVideos(
            String model, String prompt, Image image, GenerateVideosConfig config) {
        return CompletableFuture.supplyAsync(() -> models.generateVideos(model, prompt, image, config));
    }

    /**
     * Asynchronously counts tokens given a GenAI model and a text string.
     *
     * @param model  the name of the GenAI model to use.
     * @param text   the text string to send to count tokens for.
     * @param config a {@link com.google.genai.types.CountTokensConfig} instance that specifies the
     *               optional configurations
     * @return a {@link com.google.genai.types.CountTokensResponse} instance that contains tokens
     * count.
     */
    public CompletableFuture<CountTokensResponse> countTokens(
            String model, String text, CountTokensConfig config) {
        return CompletableFuture.supplyAsync(() -> models.countTokens(model, text, config));
    }


    /**
     * Asynchronously generates content given a GenAI model and a list of content.
     *
     * @param model    the name of the GenAI model to use for generation
     * @param contents a {@link List<com.google.genai.types.Content>} to send to the generative model
     * @param config   a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                 the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public CompletableFuture<GenerateContentResponse> generateContent(
            String model, List<Content> contents, GenerateContentConfig config) {
        return CompletableFuture.supplyAsync(() -> models.generateContent(model, contents, config));
    }

    /**
     * Asynchronously generates content given a GenAI model and a content object.
     *
     * @param model   the name of the GenAI model to use for generation
     * @param content a {@link com.google.genai.types.Content} to send to the generative model
     * @param config  a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public CompletableFuture<GenerateContentResponse> generateContent(
            String model, Content content, GenerateContentConfig config) {
        return generateContent(model, Transformers.tContents(content), config);
    }

    /**
     * Asynchronously generates content given a GenAI model and a text string.
     *
     * @param model  the name of the GenAI model to use for generation
     * @param text   the text string to send to the generative model
     * @param config a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *               the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public CompletableFuture<GenerateContentResponse> generateContent(
            String model, String text, GenerateContentConfig config) {
        return generateContent(model, Transformers.tContents(text), config);
    }

    /**
     * Asynchronously generates content with streaming support given a GenAI model and a list of
     * content.
     *
     * @param model    the name of the GenAI model to use for generation
     * @param contents a {@link List<com.google.genai.types.Content>} to send to the generative model
     * @param config   a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                 the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public CompletableFuture<ResponseStream<GenerateContentResponse>> generateContentStream(
            String model, List<Content> contents, GenerateContentConfig config) {
        return CompletableFuture.supplyAsync(
                () -> models.generateContentStream(model, contents, config));
    }

    /**
     * Asynchronously generates content with streaming support given a GenAI model and a content
     * object.
     *
     * @param model   the name of the GenAI model to use for generation
     * @param content a {@link com.google.genai.types.Content} to send to the generative model
     * @param config  a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public CompletableFuture<ResponseStream<GenerateContentResponse>> generateContentStream(
            String model, Content content, GenerateContentConfig config) {
        return generateContentStream(model, Transformers.tContents(content), config);
    }

    /**
     * Asynchronously generates content with streaming support given a GenAI model and a text string.
     *
     * @param model  the name of the GenAI model to use for generation
     * @param text   the text string to send to the generative model
     * @param config a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *               the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public CompletableFuture<ResponseStream<GenerateContentResponse>> generateContentStream(
            String model, String text, GenerateContentConfig config) {
        return generateContentStream(model, Transformers.tContents(text), config);
    }

    /**
     * Asynchronously generates images given a GenAI model and a prompt.
     *
     * @param model  the name of the GenAI model to use for generating images
     * @param prompt the prompt to generate images
     * @param config a {@link com.google.genai.types.GenerateImagesConfig} instance that specifies the
     *               optional configurations
     * @return a {@link com.google.genai.types.GenerateImagesResponse} instance that contains the
     * generated images.
     */
    public CompletableFuture<GenerateImagesResponse> generateImages(
            String model, String prompt, GenerateImagesConfig config) {
        return CompletableFuture.supplyAsync(() -> models.generateImages(model, prompt, config));
    }

    /**
     * Asynchronously embeds content given a GenAI model and a text string.
     *
     * @param model the name of the GenAI model to use for embedding
     * @param text  the text string to send to the embedding model
     * @return a {@link com.google.genai.types.EmbedContentResponse} instance that contains the
     * embedding.
     */
    public CompletableFuture<EmbedContentResponse> embedContent(
            String model, String text, EmbedContentConfig config) {
        return CompletableFuture.supplyAsync(() -> models.embedContent(model, text, config));
    }

    /**
     * Asynchronously embeds content given a GenAI model and a list of text strings.
     *
     * @param model the name of the GenAI model to use for embedding
     * @param texts the list of text strings to send to the embedding model
     * @return a {@link com.google.genai.types.EmbedContentResponse} instance that contains the
     * embedding.
     */
    public CompletableFuture<EmbedContentResponse> embedContent(
            String model, List<String> texts, EmbedContentConfig config) {
        return CompletableFuture.supplyAsync(() -> models.embedContent(model, texts, config));
    }

    /**
     * Asynchronously makes an API request to list the available models.
     *
     * @param config A {@link ListModelsConfig} for configuring the list request.
     * @return A CompletableFuture that resolves to a {@link AsyncPager}. The AsyncPager has a
     * `forEach` method that can be used to asynchronously process items in the page and
     * automatically query the next page once the current page is exhausted.
     */
    @SuppressWarnings("PatternMatchingInstanceof")
    public CompletableFuture<AsyncPager<Model>> list(ListModelsConfig config) {
        if (config == null) {
            config = ListModelsConfig.builder().build();
        }
        if (config.filter().isPresent()) {
            throw new IllegalArgumentException("Filter is currently not supported for list models.");
        }
        ListModelsConfig.Builder configBuilder = config.toBuilder();
        if (!config.queryBase().isPresent()) {
            configBuilder.queryBase(true);
        }
        final ListModelsConfig updatedConfig = configBuilder.build();

        Function<JsonSerializable, CompletableFuture<JsonNode>> request =
                requestConfig -> {
                    if (!(requestConfig instanceof ListModelsConfig)) {
                        throw new GenAiIOException(
                                "Internal error: Pager expected ListModelsConfig but received "
                                        + requestConfig.getClass().getName());
                    }
                    return CompletableFuture.supplyAsync(
                            () ->
                                    JsonSerializable.toJsonNode(
                                            models.privateList((ListModelsConfig) requestConfig)));
                };
        return CompletableFuture.supplyAsync(
                () ->
                        new AsyncPager<>(
                                Pager.PagedItem.MODELS,
                                request,
                                (ObjectNode) JsonSerializable.toJsonNode(updatedConfig),
                                request.apply(updatedConfig)));
    }
}
