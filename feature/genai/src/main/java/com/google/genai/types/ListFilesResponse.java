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
 * Response for the list files method.
 */
@AutoValue
@JsonDeserialize(builder = ListFilesResponse.Builder.class)
public abstract class ListFilesResponse extends JsonSerializable {
    /**
     * Instantiates a builder for ListFilesResponse.
     */
    public static Builder builder() {
        return new AutoValue_ListFilesResponse.Builder();
    }

    /**
     * Deserializes a JSON string to a ListFilesResponse object.
     */
    public static ListFilesResponse fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, ListFilesResponse.class);
    }

    /**
     * A token to retrieve next page of results.
     */
    @JsonProperty("nextPageToken")
    public abstract Optional<String> nextPageToken();

    /**
     * The list of files.
     */
    @JsonProperty("files")
    public abstract Optional<List<File>> files();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for ListFilesResponse.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `ListFilesResponse.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_ListFilesResponse.Builder();
        }

        @JsonProperty("nextPageToken")
        public abstract Builder nextPageToken(String nextPageToken);

        @JsonProperty("files")
        public abstract Builder files(List<File> files);

        public abstract ListFilesResponse build();
    }
}
