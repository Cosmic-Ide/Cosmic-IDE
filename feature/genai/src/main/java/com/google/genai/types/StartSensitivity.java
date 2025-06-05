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
 * Start of speech sensitivity.
 */
public class StartSensitivity {

    private final String value;
    private Known startSensitivityEnum;
    @JsonCreator
    public StartSensitivity(String value) {
        this.value = value;
        for (Known startSensitivityEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(startSensitivityEnum.toString(), value)) {
                this.startSensitivityEnum = startSensitivityEnum;
                break;
            }
        }
        if (this.startSensitivityEnum == null) {
            this.startSensitivityEnum = Known.START_SENSITIVITY_UNSPECIFIED;
        }
    }

    public StartSensitivity(Known knownValue) {
        this.startSensitivityEnum = knownValue;
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

        if (!(o instanceof StartSensitivity)) {
            return false;
        }

        StartSensitivity other = (StartSensitivity) o;

        if (this.startSensitivityEnum != Known.START_SENSITIVITY_UNSPECIFIED
                && other.startSensitivityEnum != Known.START_SENSITIVITY_UNSPECIFIED) {
            return this.startSensitivityEnum == other.startSensitivityEnum;
        } else if (this.startSensitivityEnum == Known.START_SENSITIVITY_UNSPECIFIED
                && other.startSensitivityEnum == Known.START_SENSITIVITY_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.startSensitivityEnum != Known.START_SENSITIVITY_UNSPECIFIED) {
            return this.startSensitivityEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.startSensitivityEnum;
    }

    /**
     * Enum representing the known values for StartSensitivity.
     */
    public enum Known {
        /**
         * The default is START_SENSITIVITY_LOW.
         */
        START_SENSITIVITY_UNSPECIFIED,

        /**
         * Automatic detection will detect the start of speech more often.
         */
        START_SENSITIVITY_HIGH,

        /**
         * Automatic detection will detect the start of speech less often.
         */
        START_SENSITIVITY_LOW
    }
}
