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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.types.LiveClientContent;
import com.google.genai.types.LiveClientMessage;
import com.google.genai.types.LiveClientToolResponse;
import com.google.genai.types.LiveSendClientContentParameters;
import com.google.genai.types.LiveSendRealtimeInputParameters;
import com.google.genai.types.LiveSendToolResponseParameters;
import com.google.genai.types.LiveServerMessage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * AsyncSession manages sending and receiving messages over a WebSocket connection for a live
 * session. The live module is experimental.
 */
public final class AsyncSession {

    @SuppressWarnings("unused")
    // For future use of converters for different backends.
    private final ApiClient apiClient;

    private final AsyncLive.GenAiWebSocketClient websocket;

    AsyncSession(ApiClient apiClient, AsyncLive.GenAiWebSocketClient websocket) {
        this.apiClient = apiClient;
        this.websocket = websocket;
    }

    /**
     * Sends client content to the live session.
     *
     * @param clientContent A {@link LiveSendClientContentParameters} to send.
     * @return A {@link CompletableFuture} that completes when the client content has been sent. The
     * future will fail if the client content cannot be sent.
     */
    public CompletableFuture<Void> sendClientContent(LiveSendClientContentParameters clientContent) {
        return send(
                LiveClientMessage.builder()
                        .clientContent(LiveClientContent.fromJson(clientContent.toJson()))
                        .build());
    }

    /**
     * Sends realtime input to the live session.
     *
     * @param realtimeInput A {@link LiveSendRealtimeInputParameters} to send.
     * @return A {@link CompletableFuture} that completes when the realtime input has been sent. The
     * future will fail if the realtime input cannot be sent.
     */
    public CompletableFuture<Void> sendRealtimeInput(LiveSendRealtimeInputParameters realtimeInput) {
        LiveClientMessage msg =
                LiveClientMessage.builder().realtimeInputParameters(realtimeInput).build();
        return send(msg);
    }

    /**
     * Sends tool response to the live session.
     *
     * @param toolResponse A {@link LiveSendToolResponseParameters} to send.
     * @return A {@link CompletableFuture} that completes when the tool response has been sent. The
     * future will fail if the tool response cannot be sent.
     */
    public CompletableFuture<Void> sendToolResponse(LiveSendToolResponseParameters toolResponse) {
        return send(
                LiveClientMessage.builder()
                        .toolResponse(LiveClientToolResponse.fromJson(toolResponse.toJson()))
                        .build());
    }

    /**
     * Sends a message to the live session.
     *
     * @param input A {@link LiveClientMessage} to send.
     * @return A {@link CompletableFuture} that completes when the message has been sent. The future
     * will fail if the message cannot be sent.
     */
    private CompletableFuture<Void> send(LiveClientMessage input) {

        LiveConverters liveConverters = new LiveConverters(apiClient);
        JsonNode parameterNode = JsonSerializable.toJsonNode(input);

        ObjectNode body = liveConverters.liveClientMessageToMldev(this.apiClient, parameterNode, null);

        return CompletableFuture.runAsync(() -> websocket.send(JsonSerializable.toJsonString(body)));
    }

    /**
     * Registers a callback to receive messages from the live session. Only one callback can be
     * registered at a time.
     *
     * @param onMessage A {@link Consumer} that will be called for each {@link LiveServerMessage}
     *                  received.
     * @return A {@link CompletableFuture} that completes when the callback has been registered. Note:
     * This future doesn't represent the entire lifecycle of receiving messages, just the
     * *registration* of the callback.
     */
    public CompletableFuture<Void> receive(Consumer<LiveServerMessage> onMessage) {
        websocket.setMessageCallback(onMessage);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Closes the WebSocket connection.
     *
     * @return A {@link CompletableFuture} that completes when the connection has been closed.
     */
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(
                () -> {
                    websocket.close();
                });
    }
}
