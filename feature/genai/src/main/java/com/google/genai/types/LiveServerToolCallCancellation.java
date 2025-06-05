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
 * Notification for the client that a previously issued `ToolCallMessage` with the specified `id`s
 * should have been not executed and should be cancelled.
 *
 * <p>If there were side-effects to those tool calls, clients may attempt to undo the tool calls.
 * This message occurs only in cases where the clients interrupt server turns.
 */
@AutoValue
@JsonDeserialize(builder = LiveServerToolCallCancellation.Builder.class)
public abstract class LiveServerToolCallCancellation extends JsonSerializable {
    /**
     * Instantiates a builder for LiveServerToolCallCancellation.
     */
    public static Builder builder() {
        return new AutoValue_LiveServerToolCallCancellation.Builder();
    }

    /**
     * Deserializes a JSON string to a LiveServerToolCallCancellation object.
     */
    public static LiveServerToolCallCancellation fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, LiveServerToolCallCancellation.class);
    }

    /**
     * The ids of the tool calls to be cancelled.
     */
    @JsonProperty("ids")
    public abstract Optional<List<String>> ids();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for LiveServerToolCallCancellation.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `LiveServerToolCallCancellation.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_LiveServerToolCallCancellation.Builder();
        }

        @JsonProperty("ids")
        public abstract Builder ids(List<String> ids);

        public abstract LiveServerToolCallCancellation build();
    }
}
