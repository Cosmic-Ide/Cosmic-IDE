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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.genai.JsonSerializable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Tool details of a tool that the model may use to generate a response.
 */
@AutoValue
@JsonDeserialize(builder = Tool.Builder.class)
public abstract class Tool extends JsonSerializable {
    /**
     * Instantiates a builder for Tool.
     */
    public static Builder builder() {
        return new AutoValue_Tool.Builder();
    }

    /**
     * Deserializes a JSON string to a Tool object.
     */
    public static Tool fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, Tool.class);
    }

    /**
     * List of function declarations that the tool supports.
     */
    @JsonProperty("functionDeclarations")
    public abstract Optional<List<FunctionDeclaration>> functionDeclarations();

    /**
     * Optional. Retrieval tool type. System will always execute the provided retrieval tool(s) to get
     * external knowledge to answer the prompt. Retrieval results are presented to the model for
     * generation.
     */
    @JsonProperty("retrieval")
    public abstract Optional<Retrieval> retrieval();

    /**
     * Optional. Google Search tool type. Specialized retrieval tool that is powered by Google Search.
     */
    @JsonProperty("googleSearch")
    public abstract Optional<GoogleSearch> googleSearch();

    /**
     * Optional. GoogleSearchRetrieval tool type. Specialized retrieval tool that is powered by Google
     * search.
     */
    @JsonProperty("googleSearchRetrieval")
    public abstract Optional<GoogleSearchRetrieval> googleSearchRetrieval();

    /**
     * Optional. Enterprise web search tool type. Specialized retrieval tool that is powered by Vertex
     * AI Search and Sec4 compliance.
     */
    @JsonProperty("enterpriseWebSearch")
    public abstract Optional<EnterpriseWebSearch> enterpriseWebSearch();

    /**
     * Optional. Google Maps tool type. Specialized retrieval tool that is powered by Google Maps.
     */
    @JsonProperty("googleMaps")
    public abstract Optional<GoogleMaps> googleMaps();

    /**
     * Optional. Tool to support URL context retrieval.
     */
    @JsonProperty("urlContext")
    public abstract Optional<UrlContext> urlContext();

    /**
     * The java.lang.reflect.Method instance. If provided, it will to be parsed into a list of
     * FunctionDeclaration instances, and be assigned to the functionDeclarations field.
     */
    @JsonIgnore
    public abstract Optional<List<Method>> functions();

    /**
     * Optional. CodeExecution tool type. Enables the model to execute code as part of generation.
     */
    @JsonProperty("codeExecution")
    public abstract Optional<ToolCodeExecution> codeExecution();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for Tool.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `Tool.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_Tool.Builder();
        }

        @JsonProperty("functionDeclarations")
        public abstract Builder functionDeclarations(List<FunctionDeclaration> functionDeclarations);

        @JsonProperty("retrieval")
        public abstract Builder retrieval(Retrieval retrieval);

        @JsonProperty("googleSearch")
        public abstract Builder googleSearch(GoogleSearch googleSearch);

        @JsonProperty("googleSearchRetrieval")
        public abstract Builder googleSearchRetrieval(GoogleSearchRetrieval googleSearchRetrieval);

        @JsonProperty("enterpriseWebSearch")
        public abstract Builder enterpriseWebSearch(EnterpriseWebSearch enterpriseWebSearch);

        @JsonProperty("googleMaps")
        public abstract Builder googleMaps(GoogleMaps googleMaps);

        @JsonProperty("urlContext")
        public abstract Builder urlContext(UrlContext urlContext);

        @JsonIgnore
        public abstract Builder functions(List<Method> functions);

        @JsonProperty("codeExecution")
        public abstract Builder codeExecution(ToolCodeExecution codeExecution);

        public abstract Tool build();
    }
}
