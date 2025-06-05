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

package com.google.genai;

import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * An async chat session with a generative model.
 *
 * <p>This class provides a way to interact with a generative model in an async multi-turn chat
 * session. It keeps track of the chat history and uses it to provide context for subsequent
 * messages.
 */
public class AsyncChat extends ChatBase {
    private static final Logger logger = Logger.getLogger(AsyncChat.class.getName());
    private final ApiClient apiClient;
    private final AsyncModels models;
    private final String model;
    private final GenerateContentConfig config;

    AsyncChat(ApiClient apiClient, String model, GenerateContentConfig config) {
        super(new ArrayList<>(), new ArrayList<>());
        this.apiClient = apiClient;
        this.models = new AsyncModels(apiClient);
        this.model = model;
        this.config = config;
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * the model's response.
     *
     * <p>This appends the message and the model's response to the chat history, which is sent back to
     * the model in subsequent calls to sendMessage. Be sure to initialize a chat session using
     * client.async.chats.create() before calling sendMessage.
     *
     * @param text   the text to send to the generative model
     * @param config a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *               the optional configurations
     */
    public CompletableFuture<GenerateContentResponse> sendMessage(
            String text, GenerateContentConfig config) {
        return privateSendMessage(Transformers.tContents(text), config);
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * the model's response.
     *
     * <p>This appends the message and the model's response to the chat history, which is sent back to
     * the model in subsequent calls to sendMessage. Be sure to initialize a chat session using
     * client.async.chats.create() before calling sendMessage.
     *
     * @param text the text to send to the generative model
     */
    public CompletableFuture<GenerateContentResponse> sendMessage(String text) {
        return privateSendMessage(Transformers.tContents(text), null);
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * the model's response.
     *
     * <p>This appends the message and the model's response to the chat history, which is sent back to
     * the model in subsequent calls to sendMessage. Be sure to initialize a chat session using
     * cilent.async.chats.create() before calling sendMessage.
     *
     * @param content a {@link com.google.genai.types.Content} to send to the generative model
     * @param config  a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                the optional configurations
     */
    public CompletableFuture<GenerateContentResponse> sendMessage(
            Content content, GenerateContentConfig config) {
        return privateSendMessage(Transformers.tContents(content), config);
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * the model's response.
     *
     * <p>This appends the message and the model's response to the chat history, which is sent back to
     * the model in subsequent calls to sendMessage. Be sure to initialize a chat session using
     * client.async.chats.create() before calling sendMessage.
     *
     * @param content a {@link com.google.genai.types.Content} to send to the generative model
     */
    public CompletableFuture<GenerateContentResponse> sendMessage(Content content) {
        return privateSendMessage(Transformers.tContents(content), null);
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * the model's response.
     *
     * <p>This appends the message and the model's response to the chat history, which is sent back to
     * the model in subsequent calls to sendMessage. Be sure to initialize a chat session using
     * client.async.chats.create() before calling sendMessage.
     *
     * @param contents a {@link List<com.google.genai.types.Content>} to send to the generative model
     * @param config   a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                 the optional configurations
     */
    public CompletableFuture<GenerateContentResponse> sendMessage(
            List<Content> contents, GenerateContentConfig config) {
        return privateSendMessage(contents, config);
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * the model's response.
     *
     * <p>This appends the message and the model's response to the chat history, which is sent back to
     * the model in subsequent calls to sendMessage. Be sure to initialize a chat session using
     * client.async.chats.create() before calling sendMessage.
     *
     * @param contents a {@link List<com.google.genai.types.Content>} to send to the generative model
     */
    public CompletableFuture<GenerateContentResponse> sendMessage(List<Content> contents) {
        return privateSendMessage(contents, null);
    }

    private CompletableFuture<GenerateContentResponse> privateSendMessage(
            List<Content> contents, GenerateContentConfig config) {

        List<Content> requestContents = prepareSendMessageRequest(contents);

        if (this.config != null && config == null) {
            config = this.config;
        }

        final GenerateContentConfig finalConfig = config;

        CompletableFuture<GenerateContentResponse> responseFuture =
                this.models.generateContent(this.model, requestContents, finalConfig);

        return responseFuture
                .thenApply(
                        response -> {
                            updateHistoryNonStreaming(response, contents);
                            return response;
                        })
                .exceptionally(
                        exception -> {
                            logger.warning("Async chat response failed with exception: " + exception);
                            throw new GenAiIOException(exception);
                        });
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * a stream of responses.
     *
     * <p>This appends the message and the model's response to the chat history after the stream is
     * consumed. Be sure to initialize a chat session using client.async.chats.create() before calling
     * sendMessageStream.
     *
     * @param text   the text to send to the generative model
     * @param config a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *               the optional configurations *
     */
    public CompletableFuture<ResponseStream<GenerateContentResponse>> sendMessageStream(
            String text, GenerateContentConfig config) {
        return privateSendMessageStream(Transformers.tContents(text), config);
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * a stream of responses.
     *
     * <p>This appends the message and the model's response to the chat history after the stream is
     * consumed. Be sure to initialize a chat session using client.async.chats.create() before calling
     * sendMessageStream.
     *
     * @param text the text to send to the generative model
     */
    public CompletableFuture<ResponseStream<GenerateContentResponse>> sendMessageStream(String text) {
        return privateSendMessageStream(Transformers.tContents(text), null);
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * a stream of responses.
     *
     * <p>This appends the message and the model's response to the chat history after the stream is
     * consumed. Be sure to initialize a chat session using client.async.chats.create() before calling
     * sendMessageStream.
     *
     * @param content a {@link com.google.genai.types.Content} to send to the generative model
     * @param config  a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                the optional configurations *
     */
    public CompletableFuture<ResponseStream<GenerateContentResponse>> sendMessageStream(
            Content content, GenerateContentConfig config) {
        return privateSendMessageStream(Transformers.tContents(content), config);
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * a stream of responses.
     *
     * <p>This appends the message and the model's response to the chat history after the stream is
     * consumed. Be sure to initialize a chat session using client.async.chats.create() before calling
     * sendMessageStream.
     *
     * @param content a {@link com.google.genai.types.Content} to send to the generative model
     */
    public CompletableFuture<ResponseStream<GenerateContentResponse>> sendMessageStream(
            Content content) {
        return privateSendMessageStream(Transformers.tContents(content), null);
    }

    /**
     * Asynchronously sends a message to the model in the current multi-turn chat session and returns
     * a stream of responses.
     *
     * <p>This appends the message and the model's response to the chat history after the stream is
     * consumed. Be sure to initialize a chat session using client.async.chats.create() before calling
     * sendMessageStream.
     *
     * @param contents a {@link List<com.google.genai.types.Content>} to send to the generative model
     * @param config   a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                 the optional configurations
     */
    public CompletableFuture<ResponseStream<GenerateContentResponse>> sendMessageStream(
            List<Content> contents, GenerateContentConfig config) {
        return privateSendMessageStream(contents, config);
    }

    /**
     * Sends a message to the model in the current multi-turn chat session and returns the model's
     * response.
     *
     * <p>This appends the message and the model's response to the chat history after the stream is
     * consumed. Be sure to initialize a chat session using client.async.chats.create() before calling
     * sendMessage.
     *
     * @param contents a {@link List<com.google.genai.types.Content>} to send to the generative model
     *                 the optional configurations
     */
    public CompletableFuture<ResponseStream<GenerateContentResponse>> sendMessageStream(
            List<Content> contents) {
        return privateSendMessageStream(contents, null);
    }

    private CompletableFuture<ResponseStream<GenerateContentResponse>> privateSendMessageStream(
            List<Content> contents, GenerateContentConfig config) {

        List<Content> requestContents = prepareSendMessageRequest(contents);

        if (this.config != null && config == null) {
            config = this.config;
        }

        CompletableFuture<ResponseStream<GenerateContentResponse>> responseStreamFuture =
                this.models.generateContentStream(this.model, requestContents, config);

        return responseStreamFuture
                .thenApply(
                        resolvedResponseStream -> {
                            resolvedResponseStream.recordingHistory = true;
                            resolvedResponseStream.asyncChatSession = this;
                            this.currentUserMessage = contents;
                            this.currentResponseStream = resolvedResponseStream;
                            return resolvedResponseStream;
                        })
                .exceptionally(
                        exception -> {
                            logger.warning("Async streaming chat response failed with exception: " + exception);
                            throw new GenAiIOException(exception);
                        });
    }
}
