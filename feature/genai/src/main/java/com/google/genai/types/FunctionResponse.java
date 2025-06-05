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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.genai.JsonSerializable;

import java.util.Map;
import java.util.Optional;

/**
 * A function response.
 */
@AutoValue
@JsonDeserialize(builder = FunctionResponse.Builder.class)
public abstract class FunctionResponse extends JsonSerializable {
    /**
     * Instantiates a builder for FunctionResponse.
     */
    public static Builder builder() {
        return new AutoValue_FunctionResponse.Builder();
    }

    /**
     * Deserializes a JSON string to a FunctionResponse object.
     */
    public static FunctionResponse fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, FunctionResponse.class);
    }

    /**
     * Signals that function call continues, and more responses will be returned, turning the function
     * call into a generator. Is only applicable to NON_BLOCKING function calls (see
     * FunctionDeclaration.behavior for details), ignored otherwise. If false, the default, future
     * responses will not be considered. Is only applicable to NON_BLOCKING function calls, is ignored
     * otherwise. If set to false, future responses will not be considered. It is allowed to return
     * empty `response` with `will_continue=False` to signal that the function call is finished.
     */
    @JsonProperty("willContinue")
    public abstract Optional<Boolean> willContinue();

    /**
     * Specifies how the response should be scheduled in the conversation. Only applicable to
     * NON_BLOCKING function calls, is ignored otherwise. Defaults to WHEN_IDLE.
     */
    @JsonProperty("scheduling")
    public abstract Optional<FunctionResponseScheduling> scheduling();

    /**
     * Optional. The id of the function call this response is for. Populated by the client to match
     * the corresponding function call `id`.
     */
    @JsonProperty("id")
    public abstract Optional<String> id();

    /**
     * Required. The name of the function to call. Matches [FunctionDeclaration.name] and
     * [FunctionCall.name].
     */
    @JsonProperty("name")
    public abstract Optional<String> name();

    /**
     * Required. The function response in JSON object format. Use "output" key to specify function
     * output and "error" key to specify error details (if any). If "output" and "error" keys are not
     * specified, then whole "response" is treated as function output.
     */
    @JsonProperty("response")
    public abstract Optional<Map<String, Object>> response();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for FunctionResponse.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `FunctionResponse.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_FunctionResponse.Builder();
        }

        @JsonProperty("willContinue")
        public abstract Builder willContinue(boolean willContinue);

        @JsonProperty("scheduling")
        public abstract Builder scheduling(FunctionResponseScheduling scheduling);

        @CanIgnoreReturnValue
        public Builder scheduling(FunctionResponseScheduling.Known knownType) {
            return scheduling(new FunctionResponseScheduling(knownType));
        }

        @CanIgnoreReturnValue
        public Builder scheduling(String scheduling) {
            return scheduling(new FunctionResponseScheduling(scheduling));
        }

        @JsonProperty("id")
        public abstract Builder id(String id);

        @JsonProperty("name")
        public abstract Builder name(String name);

        @JsonProperty("response")
        public abstract Builder response(Map<String, Object> response);

        public abstract FunctionResponse build();
    }
}
