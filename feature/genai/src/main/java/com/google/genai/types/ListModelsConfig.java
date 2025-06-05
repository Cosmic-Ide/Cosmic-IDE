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
 * None
 */
@AutoValue
@JsonDeserialize(builder = ListModelsConfig.Builder.class)
public abstract class ListModelsConfig extends JsonSerializable {
    /**
     * Instantiates a builder for ListModelsConfig.
     */
    public static Builder builder() {
        return new AutoValue_ListModelsConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a ListModelsConfig object.
     */
    public static ListModelsConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, ListModelsConfig.class);
    }

    /**
     * Used to override HTTP request options.
     */
    @JsonProperty("httpOptions")
    public abstract Optional<HttpOptions> httpOptions();

    /**
     *
     */
    @JsonProperty("pageSize")
    public abstract Optional<Integer> pageSize();

    /**
     *
     */
    @JsonProperty("pageToken")
    public abstract Optional<String> pageToken();

    /**
     *
     */
    @JsonProperty("filter")
    public abstract Optional<String> filter();

    /**
     * Set true to list base models, false to list tuned models.
     */
    @JsonProperty("queryBase")
    public abstract Optional<Boolean> queryBase();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for ListModelsConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `ListModelsConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_ListModelsConfig.Builder();
        }

        @JsonProperty("httpOptions")
        public abstract Builder httpOptions(HttpOptions httpOptions);

        @JsonProperty("pageSize")
        public abstract Builder pageSize(Integer pageSize);

        @JsonProperty("pageToken")
        public abstract Builder pageToken(String pageToken);

        @JsonProperty("filter")
        public abstract Builder filter(String filter);

        @JsonProperty("queryBase")
        public abstract Builder queryBase(boolean queryBase);

        public abstract ListModelsConfig build();
    }
}
