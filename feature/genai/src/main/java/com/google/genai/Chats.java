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

import com.google.genai.types.GenerateContentConfig;

/**
 * A class for creating chat sessions.
 */
public class Chats {
    private final ApiClient apiClient;

    Chats(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Creates a new chat session.
     *
     * @param model  The model to use for the chat session.
     * @param config The configuration for the chat session.
     * @return The chat session.
     */
    public Chat create(String model, GenerateContentConfig config) {
        return privateCreate(model, config);
    }

    /**
     * Creates a new chat session.
     *
     * @param model The model to use for the chat session.
     * @return The chat session.
     */
    public Chat create(String model) {
        return privateCreate(model, null);
    }

    private Chat privateCreate(String model, GenerateContentConfig config) {
        return new Chat(apiClient, model, config);
    }
}
