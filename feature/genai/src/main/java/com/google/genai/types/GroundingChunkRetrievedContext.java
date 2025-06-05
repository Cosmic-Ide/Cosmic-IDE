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
 * Chunk from context retrieved by the retrieval tools.
 */
@AutoValue
@JsonDeserialize(builder = GroundingChunkRetrievedContext.Builder.class)
public abstract class GroundingChunkRetrievedContext extends JsonSerializable {
    /**
     * Instantiates a builder for GroundingChunkRetrievedContext.
     */
    public static Builder builder() {
        return new AutoValue_GroundingChunkRetrievedContext.Builder();
    }

    /**
     * Deserializes a JSON string to a GroundingChunkRetrievedContext object.
     */
    public static GroundingChunkRetrievedContext fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GroundingChunkRetrievedContext.class);
    }

    /**
     * Additional context for the RAG retrieval result. This is only populated when using the RAG
     * retrieval tool.
     */
    @JsonProperty("ragChunk")
    public abstract Optional<RagChunk> ragChunk();

    /**
     * Text of the attribution.
     */
    @JsonProperty("text")
    public abstract Optional<String> text();

    /**
     * Title of the attribution.
     */
    @JsonProperty("title")
    public abstract Optional<String> title();

    /**
     * URI reference of the attribution.
     */
    @JsonProperty("uri")
    public abstract Optional<String> uri();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GroundingChunkRetrievedContext.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GroundingChunkRetrievedContext.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GroundingChunkRetrievedContext.Builder();
        }

        @JsonProperty("ragChunk")
        public abstract Builder ragChunk(RagChunk ragChunk);

        @JsonProperty("text")
        public abstract Builder text(String text);

        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("uri")
        public abstract Builder uri(String uri);

        public abstract GroundingChunkRetrievedContext build();
    }
}
