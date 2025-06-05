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
 * None
 */
@AutoValue
@JsonDeserialize(builder = ListCachedContentsResponse.Builder.class)
public abstract class ListCachedContentsResponse extends JsonSerializable {
    /**
     * Instantiates a builder for ListCachedContentsResponse.
     */
    public static Builder builder() {
        return new AutoValue_ListCachedContentsResponse.Builder();
    }

    /**
     * Deserializes a JSON string to a ListCachedContentsResponse object.
     */
    public static ListCachedContentsResponse fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, ListCachedContentsResponse.class);
    }

    /**
     *
     */
    @JsonProperty("nextPageToken")
    public abstract Optional<String> nextPageToken();

    /**
     * List of cached contents.
     */
    @JsonProperty("cachedContents")
    public abstract Optional<List<CachedContent>> cachedContents();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for ListCachedContentsResponse.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `ListCachedContentsResponse.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_ListCachedContentsResponse.Builder();
        }

        @JsonProperty("nextPageToken")
        public abstract Builder nextPageToken(String nextPageToken);

        @JsonProperty("cachedContents")
        public abstract Builder cachedContents(List<CachedContent> cachedContents);

        public abstract ListCachedContentsResponse build();
    }
}
