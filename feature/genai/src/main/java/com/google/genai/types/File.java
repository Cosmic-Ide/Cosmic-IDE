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

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * A file uploaded to the API.
 */
@AutoValue
@JsonDeserialize(builder = File.Builder.class)
public abstract class File extends JsonSerializable {
    /**
     * Instantiates a builder for File.
     */
    public static Builder builder() {
        return new AutoValue_File.Builder();
    }

    /**
     * Deserializes a JSON string to a File object.
     */
    public static File fromJson(String jsonString) {
        return JsonSerializable.fromJsonString(jsonString, File.class);
    }

    /**
     * The `File` resource name. The ID (name excluding the "files/" prefix) can contain up to 40
     * characters that are lowercase alphanumeric or dashes (-). The ID cannot start or end with a
     * dash. If the name is empty on create, a unique name will be generated. Example: `files/123-456`
     */
    @JsonProperty("name")
    public abstract Optional<String> name();

    /**
     * Optional. The human-readable display name for the `File`. The display name must be no more than
     * 512 characters in length, including spaces. Example: 'Welcome Image'
     */
    @JsonProperty("displayName")
    public abstract Optional<String> displayName();

    /**
     * Output only. MIME type of the file.
     */
    @JsonProperty("mimeType")
    public abstract Optional<String> mimeType();

    /**
     * Output only. Size of the file in bytes.
     */
    @JsonProperty("sizeBytes")
    public abstract Optional<Long> sizeBytes();

    /**
     * Output only. The timestamp of when the `File` was created.
     */
    @JsonProperty("createTime")
    public abstract Optional<Instant> createTime();

    /**
     * Output only. The timestamp of when the `File` will be deleted. Only set if the `File` is
     * scheduled to expire.
     */
    @JsonProperty("expirationTime")
    public abstract Optional<Instant> expirationTime();

    /**
     * Output only. The timestamp of when the `File` was last updated.
     */
    @JsonProperty("updateTime")
    public abstract Optional<Instant> updateTime();

    /**
     * Output only. SHA-256 hash of the uploaded bytes. The hash value is encoded in base64 format.
     */
    @JsonProperty("sha256Hash")
    public abstract Optional<String> sha256Hash();

    /**
     * Output only. The URI of the `File`.
     */
    @JsonProperty("uri")
    public abstract Optional<String> uri();

    /**
     * Output only. The URI of the `File`, only set for downloadable (generated) files.
     */
    @JsonProperty("downloadUri")
    public abstract Optional<String> downloadUri();

    /**
     * Output only. Processing state of the File.
     */
    @JsonProperty("state")
    public abstract Optional<FileState> state();

    /**
     * Output only. The source of the `File`.
     */
    @JsonProperty("source")
    public abstract Optional<FileSource> source();

    /**
     * Output only. Metadata for a video.
     */
    @JsonProperty("videoMetadata")
    public abstract Optional<Map<String, Object>> videoMetadata();

    /**
     * Output only. Error status if File processing failed.
     */
    @JsonProperty("error")
    public abstract Optional<FileStatus> error();

    /**
     * Creates a builder with the same values as this instance.
     */
    public abstract Builder toBuilder();

    /**
     * Builder for File.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * For internal usage. Please use `File.builder()` for instantiation.
         */
        @JsonCreator
        private static Builder create() {
            return new AutoValue_File.Builder();
        }

        @JsonProperty("name")
        public abstract Builder name(String name);

        @JsonProperty("displayName")
        public abstract Builder displayName(String displayName);

        @JsonProperty("mimeType")
        public abstract Builder mimeType(String mimeType);

        @JsonProperty("sizeBytes")
        public abstract Builder sizeBytes(Long sizeBytes);

        @JsonProperty("createTime")
        public abstract Builder createTime(Instant createTime);

        @JsonProperty("expirationTime")
        public abstract Builder expirationTime(Instant expirationTime);

        @JsonProperty("updateTime")
        public abstract Builder updateTime(Instant updateTime);

        @JsonProperty("sha256Hash")
        public abstract Builder sha256Hash(String sha256Hash);

        @JsonProperty("uri")
        public abstract Builder uri(String uri);

        @JsonProperty("downloadUri")
        public abstract Builder downloadUri(String downloadUri);

        @JsonProperty("state")
        public abstract Builder state(FileState state);

        @CanIgnoreReturnValue
        public Builder state(FileState.Known knownType) {
            return state(new FileState(knownType));
        }

        @CanIgnoreReturnValue
        public Builder state(String state) {
            return state(new FileState(state));
        }

        @JsonProperty("source")
        public abstract Builder source(FileSource source);

        @CanIgnoreReturnValue
        public Builder source(FileSource.Known knownType) {
            return source(new FileSource(knownType));
        }

        @CanIgnoreReturnValue
        public Builder source(String source) {
            return source(new FileSource(source));
        }

        @JsonProperty("videoMetadata")
        public abstract Builder videoMetadata(Map<String, Object> videoMetadata);

        @JsonProperty("error")
        public abstract Builder error(FileStatus error);

        public abstract File build();
    }
}
