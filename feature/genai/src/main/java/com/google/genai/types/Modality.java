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
public class Modality {

    private final String value;
    private Known modalityEnum;
    @JsonCreator
    public Modality(String value) {
        this.value = value;
        for (Known modalityEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(modalityEnum.toString(), value)) {
                this.modalityEnum = modalityEnum;
                break;
            }
        }
        if (this.modalityEnum == null) {
            this.modalityEnum = Known.MODALITY_UNSPECIFIED;
        }
    }

    public Modality(Known knownValue) {
        this.modalityEnum = knownValue;
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

        if (!(o instanceof Modality)) {
            return false;
        }

        Modality other = (Modality) o;

        if (this.modalityEnum != Known.MODALITY_UNSPECIFIED
                && other.modalityEnum != Known.MODALITY_UNSPECIFIED) {
            return this.modalityEnum == other.modalityEnum;
        } else if (this.modalityEnum == Known.MODALITY_UNSPECIFIED
                && other.modalityEnum == Known.MODALITY_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.modalityEnum != Known.MODALITY_UNSPECIFIED) {
            return this.modalityEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.modalityEnum;
    }

    /**
     * Enum representing the known values for Modality.
     */
    public enum Known {
        /**
         * The modality is unspecified.
         */
        MODALITY_UNSPECIFIED,

        /**
         * Indicates the model should return text
         */
        TEXT,

        /**
         * Indicates the model should return images.
         */
        IMAGE,

        /**
         * Indicates the model should return audio.
         */
        AUDIO
    }
}
