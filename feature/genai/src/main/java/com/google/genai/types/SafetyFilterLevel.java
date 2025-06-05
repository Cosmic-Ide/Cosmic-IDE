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
 * Enum that controls the safety filter level for objectionable content.
 */
public class SafetyFilterLevel {

    private final String value;
    private Known safetyFilterLevelEnum;
    @JsonCreator
    public SafetyFilterLevel(String value) {
        this.value = value;
        for (Known safetyFilterLevelEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(safetyFilterLevelEnum.toString(), value)) {
                this.safetyFilterLevelEnum = safetyFilterLevelEnum;
                break;
            }
        }
        if (this.safetyFilterLevelEnum == null) {
            this.safetyFilterLevelEnum = Known.SAFETY_FILTER_LEVEL_UNSPECIFIED;
        }
    }

    public SafetyFilterLevel(Known knownValue) {
        this.safetyFilterLevelEnum = knownValue;
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

        if (!(o instanceof SafetyFilterLevel)) {
            return false;
        }

        SafetyFilterLevel other = (SafetyFilterLevel) o;

        if (this.safetyFilterLevelEnum != Known.SAFETY_FILTER_LEVEL_UNSPECIFIED
                && other.safetyFilterLevelEnum != Known.SAFETY_FILTER_LEVEL_UNSPECIFIED) {
            return this.safetyFilterLevelEnum == other.safetyFilterLevelEnum;
        } else if (this.safetyFilterLevelEnum == Known.SAFETY_FILTER_LEVEL_UNSPECIFIED
                && other.safetyFilterLevelEnum == Known.SAFETY_FILTER_LEVEL_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.safetyFilterLevelEnum != Known.SAFETY_FILTER_LEVEL_UNSPECIFIED) {
            return this.safetyFilterLevelEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.safetyFilterLevelEnum;
    }

    /**
     * Enum representing the known values for SafetyFilterLevel.
     */
    public enum Known {
        BLOCK_LOW_AND_ABOVE,

        BLOCK_MEDIUM_AND_ABOVE,

        BLOCK_ONLY_HIGH,

        BLOCK_NONE,

        SAFETY_FILTER_LEVEL_UNSPECIFIED
    }
}
