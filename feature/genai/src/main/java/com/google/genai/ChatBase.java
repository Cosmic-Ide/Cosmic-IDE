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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Base class for chat sessions, used for history management.
 */
class ChatBase {
    private static final Logger logger = Logger.getLogger(ChatBase.class.getName());
    protected final List<Content> comprehensiveHistory;
    protected final List<Content> curatedHistory;
    protected ResponseStream<GenerateContentResponse> currentResponseStream;
    protected List<Content> currentUserMessage;

    ChatBase(List<Content> comprehensiveHistory, List<Content> curatedHistory) {
        this.comprehensiveHistory = new ArrayList<>();
        this.curatedHistory = new ArrayList<>();
    }

    /**
     * Records the chat history.
     *
     * @param currentHistory The current history of messages.
     */
    protected synchronized void recordHistory(
            List<Content> currentHistory, GenerateContentResponse response) {

        this.comprehensiveHistory.addAll(currentHistory);

        // This will throw for invalid history
        List<Content> validatedHistory = validateHistory(currentHistory);

        // Catch exception on checkFinishReason() and only add to curated history if checkFinishReason()
        // doesn't throw
        try {
            response.checkFinishReason();
            this.curatedHistory.addAll(validatedHistory);
        } catch (IllegalArgumentException e) {
            logger.warning(
                    "Response finished unexpectedly with reason: "
                            + response.finishReason().toString()
                            + ". Adding the response to comprehenisive history, but not to curated history.");
        }
    }

    /**
     * Validates the content of a {@link Content} object by checking that all parts are not null.
     *
     * @param content The {@link Content} object to validate.
     * @return True if the content is valid, false otherwise.
     */
    protected boolean validateContent(Content content) {
        if (content.parts().isPresent()) {
            for (Part part : content.parts().get()) {
                if (part.equals(Part.builder().build())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Validates the content of a list of {@link Content} objects by checking that all parts are not
     * null.
     *
     * @param contents The list of {@link Content} objects to validate.
     * @return True if the content is valid, false otherwise.
     */
    protected boolean validateContents(List<Content> contents) {
        for (Content content : contents) {
            if (!validateContent(content)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates the history of messages by checking that the first message is from the user, the
     * roles of subsequent messages are valid, and the content of the messages is valid.
     *
     * @param history The history of messages to validate.
     * @return The validated history of messages.
     * @throws IllegalArgumentException If the history is invalid.
     */
    protected List<Content> validateHistory(List<Content> history) {
        List<Content> validatedHistory = new ArrayList<>();
        List<Content> currentInput = new ArrayList<>();
        List<Content> currentOutput = new ArrayList<>();
        List<String> validRoles = Arrays.asList("user", "model");
        int i = 0;
        while (i < history.size()) {
            // The second condition handles the case where the history is empty and we are validating the
            // first message in a streaming call
            if ((i == 0
                    && history.get(i).role().isPresent()
                    && !history.get(i).role().get().equals("user"))
                    || (curatedHistory.isEmpty()
                    && !history.isEmpty()
                    && history.get(0).role().isPresent()
                    && !history.get(0).role().get().equals("user"))) {
                throw new IllegalArgumentException(
                        "The first message in the history must be from the user.");
            }
            if (!validRoles.contains(history.get(i).role().get())) {
                throw new IllegalArgumentException(
                        "The role of the message must be either 'user' or 'model'.");
            }
            if (history.get(i).role().isPresent() && history.get(i).role().get().equals("user")) {
                if (validateContent(history.get(i))) {
                    currentInput.add(history.get(i));
                }
                i++;
            } else {
                boolean isValid = true;
                while (i < history.size()
                        && history.get(i).role().isPresent()
                        && history.get(i).role().get().equals("model")) {
                    currentOutput.add(history.get(i));
                    if (isValid && !validateContent(history.get(i))) {
                        isValid = false;
                    }
                    i++;
                }
                if (isValid) {
                    validatedHistory.addAll(currentInput);
                    validatedHistory.addAll(currentOutput);
                    currentInput = new ArrayList<>();
                    currentOutput = new ArrayList<>();
                }
            }
        }
        return validatedHistory;
    }

    /**
     * Returns the chat history.
     *
     * @param curated Whether to return the curated history or the comprehensive history.
     *                Comprehensive history includes all messages, including empty or invalid parts. Curated
     *                history excludes empty or invalid parts.
     */
    public ImmutableList<Content> getHistory(boolean curated) {
        throwIfStreamNotConsumed();

        if (curated) {
            return ImmutableList.copyOf(this.curatedHistory);
        } else {
            return ImmutableList.copyOf(this.comprehensiveHistory);
        }
    }

    private Content aggregateStreamingResponse(List<GenerateContentResponse> responseChunks) {

        if (responseChunks == null || responseChunks.isEmpty()) {
            return Content.builder().build();
        }

        List<Part> aggregatedParts = new ArrayList<>();
        String aggregatedText = "";

        for (GenerateContentResponse responseChunk : responseChunks) {
            if (responseChunk == null) {
                continue;
            }
            Candidate candidate = responseChunk.candidates().get().get(0);
            if (candidate.content().isPresent() && candidate.content().get().parts().isPresent()) {
                List<Part> parts = candidate.content().get().parts().get();
                for (Part part : parts) {
                    if (part.text().isPresent()) {
                        aggregatedText += part.text().get();
                    } else {
                        boolean hasOtherContentParts =
                                part.functionCall().isPresent()
                                        || part.functionResponse().isPresent()
                                        || part.codeExecutionResult().isPresent()
                                        || part.executableCode().isPresent()
                                        || part.fileData().isPresent()
                                        || part.videoMetadata().isPresent()
                                        || part.thought().isPresent()
                                        || part.inlineData().isPresent();
                        if (hasOtherContentParts) {
                            aggregatedParts.add(part);
                        }
                    }
                }
            }
        }

        // Construct the final response
        aggregatedParts.add(Part.fromText(aggregatedText));
        return Content.builder().parts(aggregatedParts).role("model").build();
    }

    protected void checkStreamResponseAndUpdateHistory() {
        if (this.currentResponseStream != null && this.currentUserMessage != null) {
            throwIfStreamNotConsumed();
            List<Content> streamingResponseContents = new ArrayList<>();
            streamingResponseContents.addAll(this.currentUserMessage);
            Content aggregatedResponse = aggregateStreamingResponse(this.currentResponseStream.history);
            streamingResponseContents.add(aggregatedResponse);
            recordHistory(
                    streamingResponseContents, Iterables.getLast(this.currentResponseStream.history));
        }
        this.currentUserMessage = null;
        this.currentResponseStream = null;
    }

    protected void throwIfStreamNotConsumed() {
        if (this.currentResponseStream != null && this.currentUserMessage != null) {
            if (!this.currentResponseStream.isConsumed()) {
                throw new IllegalStateException("Response stream is not consumed");
            }
        }
    }

    protected List<Content> prepareSendMessageRequest(List<Content> newContents) {

        throwIfStreamNotConsumed();

        // Validate user input before sending to the model.
        if (!validateContents(newContents)) {
            throw new IllegalArgumentException("The content of the message is invalid.");
        }

        List<Content> requestContents = new ArrayList<>();
        requestContents.addAll(this.curatedHistory);
        requestContents.addAll(newContents);
        return requestContents;
    }

    protected void updateHistoryNonStreaming(
            GenerateContentResponse modelResponse, List<Content> userInputContents) {
        List<Content> inputContents = new ArrayList<>();
        if (modelResponse.automaticFunctionCallingHistory().isPresent()
                && !modelResponse.automaticFunctionCallingHistory().get().isEmpty()) {
            // the afc history contains the entire curated history in addition to the new user input.
            // truncate the afc history to deduplicate the existing curated history.
            inputContents.addAll(
                    modelResponse
                            .automaticFunctionCallingHistory()
                            .get()
                            .subList(
                                    this.curatedHistory.size(),
                                    modelResponse.automaticFunctionCallingHistory().get().size()));
        } else {
            inputContents.addAll(userInputContents);
        }
        List<Content> currentHistory = new ArrayList<>();
        Content modelResponseContent = modelResponse.candidates().get().get(0).content().get();
        currentHistory.addAll(inputContents);
        currentHistory.add(modelResponseContent);
        recordHistory(currentHistory, modelResponse);
    }
}
