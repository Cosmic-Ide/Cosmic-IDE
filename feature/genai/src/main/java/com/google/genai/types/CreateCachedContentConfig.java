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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Optional configuration for cached content creation.
 */
@AutoValue
@JsonDeserialize(builder = CreateCachedContentConfig.Builder.class)
public abstract class CreateCachedContentConfig extends JsonSerializable {
    /**
     * Instantiates a builder for CreateCachedContentConfig.
     */
    public static Builder builder() {
        return new AutoValue_CreateCachedContentConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a CreateCachedContentConfig object.
     */
    public static CreateCachedContentConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, CreateCachedContentConfig.class);
    }

    /**
     * Used to override HTTP request options.
     */
    @JsonProperty("httpOptions")
    public abstract Optional<HttpOptions> httpOptions();

    /**
     * The TTL for this resource. The expiration time is computed: now + TTL. It is a duration string,
     * with up to nine fractional digits, terminated by 's'. Example: "3.5s".
     */
    @JsonProperty("ttl")
    public abstract Optional<Duration> ttl();

    /**
     * Timestamp of when this resource is considered expired. Uses RFC 3339 format, Example:
     * 2014-10-02T15:01:23Z.
     */
    @JsonProperty("expireTime")
    public abstract Optional<Instant> expireTime();

    /**
     * The user-generated meaningful display name of the cached content.
     */
    @JsonProperty("displayName")
    public abstract Optional<String> displayName();

    /**
     * The content to cache.
     */
    @JsonProperty("contents")
    public abstract Optional<List<Content>> contents();

    /**
     * Developer set system instruction.
     */
    @JsonProperty("systemInstruction")
    public abstract Optional<Content> systemInstruction();

    /**
     * A list of `Tools` the model may use to generate the next response.
     */
    @JsonProperty("tools")
    public abstract Optional<List<Tool>> tools();

    /**
     * Configuration for the tools to use. This config is shared for all tools.
     */
    @JsonProperty("toolConfig")
    public abstract Optional<ToolConfig> toolConfig();

    /**
     * The Cloud KMS resource identifier of the customer managed encryption key used to protect a
     * resource. The key needs to be in the same region as where the compute resource is created. See
     * https://cloud.google.com/vertex-ai/docs/general/cmek for more details. If this is set, then all
     * created CachedContent objects will be encrypted with the provided encryption key. Allowed
     * formats: projects/{project}/locations/{location}/keyRings/{key_ring}/cryptoKeys/{crypto_key}
     */
    @JsonProperty("kmsKeyName")
    public abstract Optional<String> kmsKeyName();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for CreateCachedContentConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `CreateCachedContentConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_CreateCachedContentConfig.Builder();
        }

        @JsonProperty("httpOptions")
        public abstract Builder httpOptions(HttpOptions httpOptions);

        @JsonProperty("ttl")
        public abstract Builder ttl(Duration ttl);

        @JsonProperty("expireTime")
        public abstract Builder expireTime(Instant expireTime);

        @JsonProperty("displayName")
        public abstract Builder displayName(String displayName);

        @JsonProperty("contents")
        public abstract Builder contents(List<Content> contents);

        @JsonProperty("systemInstruction")
        public abstract Builder systemInstruction(Content systemInstruction);

        @JsonProperty("tools")
        public abstract Builder tools(List<Tool> tools);

        @JsonProperty("toolConfig")
        public abstract Builder toolConfig(ToolConfig toolConfig);

        @JsonProperty("kmsKeyName")
        public abstract Builder kmsKeyName(String kmsKeyName);

        public abstract CreateCachedContentConfig build();
    }
}
