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
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Ascii;

import java.util.Objects;

/**
 * The media resolution to use.
 */
public class MediaResolution {

    private final String value;
    private Known mediaResolutionEnum;
    @JsonCreator
    public MediaResolution(String value) {
        this.value = value;
        for (Known mediaResolutionEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(mediaResolutionEnum.toString(), value)) {
                this.mediaResolutionEnum = mediaResolutionEnum;
                break;
            }
        }
        if (this.mediaResolutionEnum == null) {
            this.mediaResolutionEnum = Known.MEDIA_RESOLUTION_UNSPECIFIED;
        }
    }

    public MediaResolution(Known knownValue) {
        this.mediaResolutionEnum = knownValue;
        this.value = knownValue.toString();
    }

    @Override
    @JsonValue
    public String toString() {
        return this.value;
    }

    @SuppressWarnings("PatternMatchingInstanceof")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }

        if (!(o instanceof MediaResolution)) {
            return false;
        }

        MediaResolution other = (MediaResolution) o;

        if (this.mediaResolutionEnum != Known.MEDIA_RESOLUTION_UNSPECIFIED
                && other.mediaResolutionEnum != Known.MEDIA_RESOLUTION_UNSPECIFIED) {
            return this.mediaResolutionEnum == other.mediaResolutionEnum;
        } else if (this.mediaResolutionEnum == Known.MEDIA_RESOLUTION_UNSPECIFIED
                && other.mediaResolutionEnum == Known.MEDIA_RESOLUTION_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.mediaResolutionEnum != Known.MEDIA_RESOLUTION_UNSPECIFIED) {
            return this.mediaResolutionEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.mediaResolutionEnum;
    }

    /**
     * Enum representing the known values for MediaResolution.
     */
    public enum Known {
        /**
         * Media resolution has not been set
         */
        MEDIA_RESOLUTION_UNSPECIFIED,

        /**
         * Media resolution set to low (64 tokens).
         */
        MEDIA_RESOLUTION_LOW,

        /**
         * Media resolution set to medium (256 tokens).
         */
        MEDIA_RESOLUTION_MEDIUM,

        /**
         * Media resolution set to high (zoomed reframing with 256 tokens).
         */
        MEDIA_RESOLUTION_HIGH
    }
}
