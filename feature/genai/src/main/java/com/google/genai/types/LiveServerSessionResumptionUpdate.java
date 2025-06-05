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
 * Update of the session resumption state.
 *
 * <p>Only sent if `session_resumption` was set in the connection config.
 */
@AutoValue
@JsonDeserialize(builder = LiveServerSessionResumptionUpdate.Builder.class)
public abstract class LiveServerSessionResumptionUpdate extends JsonSerializable {
    /**
     * Instantiates a builder for LiveServerSessionResumptionUpdate.
     */
    public static Builder builder() {
        return new AutoValue_LiveServerSessionResumptionUpdate.Builder();
    }

    /**
     * Deserializes a JSON string to a LiveServerSessionResumptionUpdate object.
     */
    public static LiveServerSessionResumptionUpdate fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, LiveServerSessionResumptionUpdate.class);
    }

    /**
     * New handle that represents state that can be resumed. Empty if `resumable`=false.
     */
    @JsonProperty("newHandle")
    public abstract Optional<String> newHandle();

    /**
     * True if session can be resumed at this point. It might be not possible to resume session at
     * some points. In that case we send update empty new_handle and resumable=false. Example of such
     * case could be model executing function calls or just generating. Resuming session (using
     * previous session token) in such state will result in some data loss.
     */
    @JsonProperty("resumable")
    public abstract Optional<Boolean> resumable();

    /**
     * Index of last message sent by client that is included in state represented by this
     * SessionResumptionToken. Only sent when `SessionResumptionConfig.transparent` is set.
     *
     * <p>Presence of this index allows users to transparently reconnect and avoid issue of losing
     * some part of realtime audio input/video. If client wishes to temporarily disconnect (for
     * example as result of receiving GoAway) they can do it without losing state by buffering
     * messages sent since last `SessionResmumptionTokenUpdate`. This field will enable them to limit
     * buffering (avoid keeping all requests in RAM).
     *
     * <p>Note: This should not be used for when resuming a session at some time later -- in those
     * cases partial audio and video frames arelikely not needed.
     */
    @JsonProperty("lastConsumedClientMessageIndex")
    public abstract Optional<Long> lastConsumedClientMessageIndex();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for LiveServerSessionResumptionUpdate.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `LiveServerSessionResumptionUpdate.builder()` for
         * instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_LiveServerSessionResumptionUpdate.Builder();
        }

        @JsonProperty("newHandle")
        public abstract Builder newHandle(String newHandle);

        @JsonProperty("resumable")
        public abstract Builder resumable(boolean resumable);

        @JsonProperty("lastConsumedClientMessageIndex")
        public abstract Builder lastConsumedClientMessageIndex(Long lastConsumedClientMessageIndex);

        public abstract LiveServerSessionResumptionUpdate build();
    }
}
