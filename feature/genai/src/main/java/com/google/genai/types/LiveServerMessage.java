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
import com.google.genai.JsonSerializable;

import java.util.Optional;

/**
 * Response message for API call.
 */
@AutoValue
@JsonDeserialize(builder = LiveServerMessage.Builder.class)
public abstract class LiveServerMessage extends JsonSerializable {
    /**
     * Instantiates a builder for LiveServerMessage.
     */
    public static Builder builder() {
        return new AutoValue_LiveServerMessage.Builder();
    }

    /**
     * Deserializes a JSON string to a LiveServerMessage object.
     */
    public static LiveServerMessage fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, LiveServerMessage.class);
    }

    /**
     * Sent in response to a `LiveClientSetup` message from the client.
     */
    @JsonProperty("setupComplete")
    public abstract Optional<LiveServerSetupComplete> setupComplete();

    /**
     * Content generated by the model in response to client messages.
     */
    @JsonProperty("serverContent")
    public abstract Optional<LiveServerContent> serverContent();

    /**
     * Request for the client to execute the `function_calls` and return the responses with the
     * matching `id`s.
     */
    @JsonProperty("toolCall")
    public abstract Optional<LiveServerToolCall> toolCall();

    /**
     * Notification for the client that a previously issued `ToolCallMessage` with the specified `id`s
     * should have been not executed and should be cancelled.
     */
    @JsonProperty("toolCallCancellation")
    public abstract Optional<LiveServerToolCallCancellation> toolCallCancellation();

    /**
     * Usage metadata about model response(s).
     */
    @JsonProperty("usageMetadata")
    public abstract Optional<UsageMetadata> usageMetadata();

    /**
     * Server will disconnect soon.
     */
    @JsonProperty("goAway")
    public abstract Optional<LiveServerGoAway> goAway();

    /**
     * Update of the session resumption state.
     */
    @JsonProperty("sessionResumptionUpdate")
    public abstract Optional<LiveServerSessionResumptionUpdate> sessionResumptionUpdate();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for LiveServerMessage.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `LiveServerMessage.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_LiveServerMessage.Builder();
        }

        @JsonProperty("setupComplete")
        public abstract Builder setupComplete(LiveServerSetupComplete setupComplete);

        @JsonProperty("serverContent")
        public abstract Builder serverContent(LiveServerContent serverContent);

        @JsonProperty("toolCall")
        public abstract Builder toolCall(LiveServerToolCall toolCall);

        @JsonProperty("toolCallCancellation")
        public abstract Builder toolCallCancellation(
                LiveServerToolCallCancellation toolCallCancellation);

        @JsonProperty("usageMetadata")
        public abstract Builder usageMetadata(UsageMetadata usageMetadata);

        @JsonProperty("goAway")
        public abstract Builder goAway(LiveServerGoAway goAway);

        @JsonProperty("sessionResumptionUpdate")
        public abstract Builder sessionResumptionUpdate(
                LiveServerSessionResumptionUpdate sessionResumptionUpdate);

        public abstract LiveServerMessage build();
    }
}
