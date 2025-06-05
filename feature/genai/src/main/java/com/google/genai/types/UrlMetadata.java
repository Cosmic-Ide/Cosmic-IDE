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

import java.util.Optional;

/**
 * Context for a single url retrieval.
 */
@AutoValue
@JsonDeserialize(builder = UrlMetadata.Builder.class)
public abstract class UrlMetadata extends JsonSerializable {
    /**
     * Instantiates a builder for UrlMetadata.
     */
    public static Builder builder() {
        return new AutoValue_UrlMetadata.Builder();
    }

    /**
     * Deserializes a JSON string to a UrlMetadata object.
     */
    public static UrlMetadata fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, UrlMetadata.class);
    }

    /**
     * The URL retrieved by the tool.
     */
    @JsonProperty("retrievedUrl")
    public abstract Optional<String> retrievedUrl();

    /**
     * Status of the url retrieval.
     */
    @JsonProperty("urlRetrievalStatus")
    public abstract Optional<UrlRetrievalStatus> urlRetrievalStatus();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for UrlMetadata.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `UrlMetadata.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_UrlMetadata.Builder();
        }

        @JsonProperty("retrievedUrl")
        public abstract Builder retrievedUrl(String retrievedUrl);

        @JsonProperty("urlRetrievalStatus")
        public abstract Builder urlRetrievalStatus(UrlRetrievalStatus urlRetrievalStatus);

        @CanIgnoreReturnValue
        public Builder urlRetrievalStatus(UrlRetrievalStatus.Known knownType) {
            return urlRetrievalStatus(new UrlRetrievalStatus(knownType));
        }

        @CanIgnoreReturnValue
        public Builder urlRetrievalStatus(String urlRetrievalStatus) {
            return urlRetrievalStatus(new UrlRetrievalStatus(urlRetrievalStatus));
        }

        public abstract UrlMetadata build();
    }
}
