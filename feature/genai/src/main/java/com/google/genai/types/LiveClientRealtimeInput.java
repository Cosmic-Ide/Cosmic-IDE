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

import java.util.List;
import java.util.Optional;

/**
 * User input that is sent in real time.
 *
 * <p>This is different from `LiveClientContent` in a few ways:
 *
 * <p>- Can be sent continuously without interruption to model generation. - If there is a need to
 * mix data interleaved across the `LiveClientContent` and the `LiveClientRealtimeInput`, server
 * attempts to optimize for best response, but there are no guarantees. - End of turn is not
 * explicitly specified, but is rather derived from user activity (for example, end of speech). -
 * Even before the end of turn, the data is processed incrementally to optimize for a fast start of
 * the response from the model. - Is always assumed to be the user's input (cannot be used to
 * populate conversation history).
 */
@AutoValue
@JsonDeserialize(builder = LiveClientRealtimeInput.Builder.class)
public abstract class LiveClientRealtimeInput extends JsonSerializable {
    /**
     * Instantiates a builder for LiveClientRealtimeInput.
     */
    public static Builder builder() {
        return new AutoValue_LiveClientRealtimeInput.Builder();
    }

    /**
     * Deserializes a JSON string to a LiveClientRealtimeInput object.
     */
    public static LiveClientRealtimeInput fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, LiveClientRealtimeInput.class);
    }

    /**
     * Inlined bytes data for media input.
     */
    @JsonProperty("mediaChunks")
    public abstract Optional<List<Blob>> mediaChunks();

    /**
     * The realtime audio input stream.
     */
    @JsonProperty("audio")
    public abstract Optional<Blob> audio();

    /**
     * Indicates that the audio stream has ended, e.g. because the microphone was turned off.
     *
     * <p>This should only be sent when automatic activity detection is enabled (which is the
     * default).
     *
     * <p>The client can reopen the stream by sending an audio message.
     */
    @JsonProperty("audioStreamEnd")
    public abstract Optional<Boolean> audioStreamEnd();

    /**
     * The realtime video input stream.
     */
    @JsonProperty("video")
    public abstract Optional<Blob> video();

    /**
     * The realtime text input stream.
     */
    @JsonProperty("text")
    public abstract Optional<String> text();

    /**
     * Marks the start of user activity.
     */
    @JsonProperty("activityStart")
    public abstract Optional<ActivityStart> activityStart();

    /**
     * Marks the end of user activity.
     */
    @JsonProperty("activityEnd")
    public abstract Optional<ActivityEnd> activityEnd();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for LiveClientRealtimeInput.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `LiveClientRealtimeInput.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_LiveClientRealtimeInput.Builder();
        }

        @JsonProperty("mediaChunks")
        public abstract Builder mediaChunks(List<Blob> mediaChunks);

        @JsonProperty("audio")
        public abstract Builder audio(Blob audio);

        @JsonProperty("audioStreamEnd")
        public abstract Builder audioStreamEnd(boolean audioStreamEnd);

        @JsonProperty("video")
        public abstract Builder video(Blob video);

        @JsonProperty("text")
        public abstract Builder text(String text);

        @JsonProperty("activityStart")
        public abstract Builder activityStart(ActivityStart activityStart);

        @JsonProperty("activityEnd")
        public abstract Builder activityEnd(ActivityEnd activityEnd);

        public abstract LiveClientRealtimeInput build();
    }
}
