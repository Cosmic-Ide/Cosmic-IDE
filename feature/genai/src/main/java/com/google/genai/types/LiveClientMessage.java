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
 * Messages sent by the client in the API call.
 */
@AutoValue
@JsonDeserialize(builder = LiveClientMessage.Builder.class)
public abstract class LiveClientMessage extends JsonSerializable {
    /**
     * Instantiates a builder for LiveClientMessage.
     */
    public static Builder builder() {
        return new AutoValue_LiveClientMessage.Builder();
    }

    /**
     * Deserializes a JSON string to a LiveClientMessage object.
     */
    public static LiveClientMessage fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, LiveClientMessage.class);
    }

    /**
     * Message to be sent by the system when connecting to the API. SDK users should not send this
     * message.
     */
    @JsonProperty("setup")
    public abstract Optional<LiveClientSetup> setup();

    /**
     * Incremental update of the current conversation delivered from the client.
     */
    @JsonProperty("clientContent")
    public abstract Optional<LiveClientContent> clientContent();

    /**
     * User input that is sent in real time.
     */
    @JsonProperty("realtimeInput")
    public abstract Optional<LiveClientRealtimeInput> realtimeInput();

    /**
     * This is an alias for `realtime_input` field. Allows the inputs to sendRealtimeInput to be
     * transformed into `realtime_input` before being sent. Note: This will overwrite any
     * `realtime_input` field.
     */
    @JsonProperty("realtimeInputParameters")
    public abstract Optional<LiveSendRealtimeInputParameters> realtimeInputParameters();

    /**
     * Response to a `ToolCallMessage` received from the server.
     */
    @JsonProperty("toolResponse")
    public abstract Optional<LiveClientToolResponse> toolResponse();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for LiveClientMessage.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `LiveClientMessage.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_LiveClientMessage.Builder();
        }

        @JsonProperty("setup")
        public abstract Builder setup(LiveClientSetup setup);

        @JsonProperty("clientContent")
        public abstract Builder clientContent(LiveClientContent clientContent);

        @JsonProperty("realtimeInput")
        public abstract Builder realtimeInput(LiveClientRealtimeInput realtimeInput);

        @JsonProperty("realtimeInputParameters")
        public abstract Builder realtimeInputParameters(
                LiveSendRealtimeInputParameters realtimeInputParameters);

        @JsonProperty("toolResponse")
        public abstract Builder toolResponse(LiveClientToolResponse toolResponse);

        public abstract LiveClientMessage build();
    }
}
