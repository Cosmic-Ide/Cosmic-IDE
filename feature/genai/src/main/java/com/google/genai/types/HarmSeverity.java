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
 * Output only. Harm severity levels in the content.
 */
public class HarmSeverity {

    private final String value;
    private Known harmSeverityEnum;
    @JsonCreator
    public HarmSeverity(String value) {
        this.value = value;
        for (Known harmSeverityEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(harmSeverityEnum.toString(), value)) {
                this.harmSeverityEnum = harmSeverityEnum;
                break;
            }
        }
        if (this.harmSeverityEnum == null) {
            this.harmSeverityEnum = Known.HARM_SEVERITY_UNSPECIFIED;
        }
    }

    public HarmSeverity(Known knownValue) {
        this.harmSeverityEnum = knownValue;
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

        if (!(o instanceof HarmSeverity)) {
            return false;
        }

        HarmSeverity other = (HarmSeverity) o;

        if (this.harmSeverityEnum != Known.HARM_SEVERITY_UNSPECIFIED
                && other.harmSeverityEnum != Known.HARM_SEVERITY_UNSPECIFIED) {
            return this.harmSeverityEnum == other.harmSeverityEnum;
        } else if (this.harmSeverityEnum == Known.HARM_SEVERITY_UNSPECIFIED
                && other.harmSeverityEnum == Known.HARM_SEVERITY_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.harmSeverityEnum != Known.HARM_SEVERITY_UNSPECIFIED) {
            return this.harmSeverityEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.harmSeverityEnum;
    }

    /**
     * Enum representing the known values for HarmSeverity.
     */
    public enum Known {
        /**
         * Harm severity unspecified.
         */
        HARM_SEVERITY_UNSPECIFIED,

        /**
         * Negligible level of harm severity.
         */
        HARM_SEVERITY_NEGLIGIBLE,

        /**
         * Low level of harm severity.
         */
        HARM_SEVERITY_LOW,

        /**
         * Medium level of harm severity.
         */
        HARM_SEVERITY_MEDIUM,

        /**
         * High level of harm severity.
         */
        HARM_SEVERITY_HIGH
    }
}
