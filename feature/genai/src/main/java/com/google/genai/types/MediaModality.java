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
 * Server content modalities.
 */
public class MediaModality {

    private final String value;
    private Known mediaModalityEnum;
    @JsonCreator
    public MediaModality(String value) {
        this.value = value;
        for (Known mediaModalityEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(mediaModalityEnum.toString(), value)) {
                this.mediaModalityEnum = mediaModalityEnum;
                break;
            }
        }
        if (this.mediaModalityEnum == null) {
            this.mediaModalityEnum = Known.MEDIA_MODALITY_UNSPECIFIED;
        }
    }

    public MediaModality(Known knownValue) {
        this.mediaModalityEnum = knownValue;
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

        if (!(o instanceof MediaModality)) {
            return false;
        }

        MediaModality other = (MediaModality) o;

        if (this.mediaModalityEnum != Known.MEDIA_MODALITY_UNSPECIFIED
                && other.mediaModalityEnum != Known.MEDIA_MODALITY_UNSPECIFIED) {
            return this.mediaModalityEnum == other.mediaModalityEnum;
        } else if (this.mediaModalityEnum == Known.MEDIA_MODALITY_UNSPECIFIED
                && other.mediaModalityEnum == Known.MEDIA_MODALITY_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.mediaModalityEnum != Known.MEDIA_MODALITY_UNSPECIFIED) {
            return this.mediaModalityEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.mediaModalityEnum;
    }

    /**
     * Enum representing the known values for MediaModality.
     */
    public enum Known {
        /**
         * The modality is unspecified.
         */
        MODALITY_UNSPECIFIED,

        /**
         * Plain text.
         */
        TEXT,

        /**
         * Images.
         */
        IMAGE,

        /**
         * Video.
         */
        VIDEO,

        /**
         * Audio.
         */
        AUDIO,

        /**
         * Document, e.g. PDF.
         */
        DOCUMENT,

        MEDIA_MODALITY_UNSPECIFIED
    }
}
