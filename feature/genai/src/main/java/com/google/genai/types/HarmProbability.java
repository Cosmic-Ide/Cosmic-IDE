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
 * Output only. Harm probability levels in the content.
 */
public class HarmProbability {

    private final String value;
    private Known harmProbabilityEnum;
    @JsonCreator
    public HarmProbability(String value) {
        this.value = value;
        for (Known harmProbabilityEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(harmProbabilityEnum.toString(), value)) {
                this.harmProbabilityEnum = harmProbabilityEnum;
                break;
            }
        }
        if (this.harmProbabilityEnum == null) {
            this.harmProbabilityEnum = Known.HARM_PROBABILITY_UNSPECIFIED;
        }
    }

    public HarmProbability(Known knownValue) {
        this.harmProbabilityEnum = knownValue;
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

        if (!(o instanceof HarmProbability)) {
            return false;
        }

        HarmProbability other = (HarmProbability) o;

        if (this.harmProbabilityEnum != Known.HARM_PROBABILITY_UNSPECIFIED
                && other.harmProbabilityEnum != Known.HARM_PROBABILITY_UNSPECIFIED) {
            return this.harmProbabilityEnum == other.harmProbabilityEnum;
        } else if (this.harmProbabilityEnum == Known.HARM_PROBABILITY_UNSPECIFIED
                && other.harmProbabilityEnum == Known.HARM_PROBABILITY_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.harmProbabilityEnum != Known.HARM_PROBABILITY_UNSPECIFIED) {
            return this.harmProbabilityEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.harmProbabilityEnum;
    }

    /**
     * Enum representing the known values for HarmProbability.
     */
    public enum Known {
        /**
         * Harm probability unspecified.
         */
        HARM_PROBABILITY_UNSPECIFIED,

        /**
         * Negligible level of harm.
         */
        NEGLIGIBLE,

        /**
         * Low level of harm.
         */
        LOW,

        /**
         * Medium level of harm.
         */
        MEDIUM,

        /**
         * High level of harm.
         */
        HIGH
    }
}
