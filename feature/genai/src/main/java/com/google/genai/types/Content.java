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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.jspecify.annotations.Nullable;

/**
 * Contains the multi-part content of a message.
 */
@AutoValue
@JsonDeserialize(builder = Content.Builder.class)
public abstract class Content extends JsonSerializable {
    private static final Logger logger = Logger.getLogger(Content.class.getName());

    /**
     * Instantiates a builder for Content.
     */
    public static Builder builder() {
        return new AutoValue_Content.Builder();
    }

    /**
     * Deserializes a JSON string to a Content object.
     */
    public static Content fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, Content.class);
    }

    /**
     * Constructs a Content from parts, assuming the role is "user".
     */
    public static Content fromParts(Part... parts) {
        return builder().role("user").parts(Arrays.asList(parts)).build();
    }

    /**
     * Aggregates all text parts in a list of parts.
     *
     * <p>Returns null if there are no parts in the list. Returns an empty string if parts exists but
     * none of the parts contain text.
     */
    static @Nullable String aggregateTextFromParts(List<Part> parts) {
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        ArrayList<String> nonTextParts = new ArrayList<>();
        for (Part part : parts) {
            if (part.inlineData().isPresent()) {
                nonTextParts.add("inlineData");
            }
            if (part.codeExecutionResult().isPresent()) {
                nonTextParts.add("codeExecutionResult");
            }
            if (part.executableCode().isPresent()) {
                nonTextParts.add("executableCode");
            }
            if (part.fileData().isPresent()) {
                nonTextParts.add("fileData");
            }
            if (part.functionCall().isPresent()) {
                nonTextParts.add("functionCall");
            }
            if (part.functionResponse().isPresent()) {
                nonTextParts.add("functionResponse");
            }
            if (part.videoMetadata().isPresent()) {
                nonTextParts.add("videoMetadata");
            }
            if (part.thought().orElse(false)) {
                continue;
            }
            sb.append(part.text().orElse(""));
        }

        if (!nonTextParts.isEmpty()) {
            logger.warning(
                    String.format(
                            "There are non-text parts %s in the content, returning concatenation of all text"
                                    + " parts. Please refer to the non text parts for a full response from model.",
                            String.join(", ", nonTextParts)));
        }

        return sb.toString();
    }

    /**
     * List of parts that constitute a single message. Each part may have a different IANA MIME type.
     */
    @JsonProperty("parts")
    public abstract Optional<List<Part>> parts();

    /**
     * Optional. The producer of the content. Must be either 'user' or 'model'. Useful to set for
     * multi-turn conversations, otherwise can be empty. If role is not specified, SDK will determine
     * the role.
     */
    @JsonProperty("role")
    public abstract Optional<String> role();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Returns the concatenation of all text parts in this content.
     *
     * <p>Returns null if there are no parts in the content. Returns an empty string if parts exists
     * but none of the parts contain text.
     */
    public @Nullable String text() {
        return aggregateTextFromParts(parts().orElse(null));
    }

    /**
     * Builder for Content.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `Content.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_Content.Builder();
        }

        @JsonProperty("parts")
        public abstract Builder parts(List<Part> parts);

        @JsonProperty("role")
        public abstract Builder role(String role);

        public abstract Content build();
    }
}
