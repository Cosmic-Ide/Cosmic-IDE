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
 * Configuration for generating videos.
 */
@AutoValue
@JsonDeserialize(builder = GenerateVideosConfig.Builder.class)
public abstract class GenerateVideosConfig extends JsonSerializable {
    /**
     * Instantiates a builder for GenerateVideosConfig.
     */
    public static Builder builder() {
        return new AutoValue_GenerateVideosConfig.Builder();
    }

    /**
     * Deserializes a JSON string to a GenerateVideosConfig object.
     */
    public static GenerateVideosConfig fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, GenerateVideosConfig.class);
    }

    /**
     * Used to override HTTP request options.
     */
    @JsonProperty("httpOptions")
    public abstract Optional<HttpOptions> httpOptions();

    /**
     * Number of output videos.
     */
    @JsonProperty("numberOfVideos")
    public abstract Optional<Integer> numberOfVideos();

    /**
     * The gcs bucket where to save the generated videos.
     */
    @JsonProperty("outputGcsUri")
    public abstract Optional<String> outputGcsUri();

    /**
     * Frames per second for video generation.
     */
    @JsonProperty("fps")
    public abstract Optional<Integer> fps();

    /**
     * Duration of the clip for video generation in seconds.
     */
    @JsonProperty("durationSeconds")
    public abstract Optional<Integer> durationSeconds();

    /**
     * The RNG seed. If RNG seed is exactly same for each request with unchanged inputs, the
     * prediction results will be consistent. Otherwise, a random RNG seed will be used each time to
     * produce a different result.
     */
    @JsonProperty("seed")
    public abstract Optional<Integer> seed();

    /**
     * The aspect ratio for the generated video. 16:9 (landscape) and 9:16 (portrait) are supported.
     */
    @JsonProperty("aspectRatio")
    public abstract Optional<String> aspectRatio();

    /**
     * The resolution for the generated video. 1280x720, 1920x1080 are supported.
     */
    @JsonProperty("resolution")
    public abstract Optional<String> resolution();

    /**
     * Whether allow to generate person videos, and restrict to specific ages. Supported values are:
     * dont_allow, allow_adult.
     */
    @JsonProperty("personGeneration")
    public abstract Optional<String> personGeneration();

    /**
     * The pubsub topic where to publish the video generation progress.
     */
    @JsonProperty("pubsubTopic")
    public abstract Optional<String> pubsubTopic();

    /**
     * Optional field in addition to the text content. Negative prompts can be explicitly stated here
     * to help generate the video.
     */
    @JsonProperty("negativePrompt")
    public abstract Optional<String> negativePrompt();

    /**
     * Whether to use the prompt rewriting logic.
     */
    @JsonProperty("enhancePrompt")
    public abstract Optional<Boolean> enhancePrompt();

    /**
     * Whether to generate audio along with the video.
     */
    @JsonProperty("generateAudio")
    public abstract Optional<Boolean> generateAudio();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for GenerateVideosConfig.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `GenerateVideosConfig.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_GenerateVideosConfig.Builder();
        }

        @JsonProperty("httpOptions")
        public abstract Builder httpOptions(HttpOptions httpOptions);

        @JsonProperty("numberOfVideos")
        public abstract Builder numberOfVideos(Integer numberOfVideos);

        @JsonProperty("outputGcsUri")
        public abstract Builder outputGcsUri(String outputGcsUri);

        @JsonProperty("fps")
        public abstract Builder fps(Integer fps);

        @JsonProperty("durationSeconds")
        public abstract Builder durationSeconds(Integer durationSeconds);

        @JsonProperty("seed")
        public abstract Builder seed(Integer seed);

        @JsonProperty("aspectRatio")
        public abstract Builder aspectRatio(String aspectRatio);

        @JsonProperty("resolution")
        public abstract Builder resolution(String resolution);

        @JsonProperty("personGeneration")
        public abstract Builder personGeneration(String personGeneration);

        @JsonProperty("pubsubTopic")
        public abstract Builder pubsubTopic(String pubsubTopic);

        @JsonProperty("negativePrompt")
        public abstract Builder negativePrompt(String negativePrompt);

        @JsonProperty("enhancePrompt")
        public abstract Builder enhancePrompt(boolean enhancePrompt);

        @JsonProperty("generateAudio")
        public abstract Builder generateAudio(boolean generateAudio);

        public abstract GenerateVideosConfig build();
    }
}
