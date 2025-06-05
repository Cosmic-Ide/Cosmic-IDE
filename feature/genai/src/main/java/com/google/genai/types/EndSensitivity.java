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
 * End of speech sensitivity.
 */
public class EndSensitivity {

    private final String value;
    private Known endSensitivityEnum;
    @JsonCreator
    public EndSensitivity(String value) {
        this.value = value;
        for (Known endSensitivityEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(endSensitivityEnum.toString(), value)) {
                this.endSensitivityEnum = endSensitivityEnum;
                break;
            }
        }
        if (this.endSensitivityEnum == null) {
            this.endSensitivityEnum = Known.END_SENSITIVITY_UNSPECIFIED;
        }
    }

    public EndSensitivity(Known knownValue) {
        this.endSensitivityEnum = knownValue;
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

        if (!(o instanceof EndSensitivity)) {
            return false;
        }

        EndSensitivity other = (EndSensitivity) o;

        if (this.endSensitivityEnum != Known.END_SENSITIVITY_UNSPECIFIED
                && other.endSensitivityEnum != Known.END_SENSITIVITY_UNSPECIFIED) {
            return this.endSensitivityEnum == other.endSensitivityEnum;
        } else if (this.endSensitivityEnum == Known.END_SENSITIVITY_UNSPECIFIED
                && other.endSensitivityEnum == Known.END_SENSITIVITY_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.endSensitivityEnum != Known.END_SENSITIVITY_UNSPECIFIED) {
            return this.endSensitivityEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.endSensitivityEnum;
    }

    /**
     * Enum representing the known values for EndSensitivity.
     */
    public enum Known {
        /**
         * The default is END_SENSITIVITY_LOW.
         */
        END_SENSITIVITY_UNSPECIFIED,

        /**
         * Automatic detection ends speech more often.
         */
        END_SENSITIVITY_HIGH,

        /**
         * Automatic detection ends speech less often.
         */
        END_SENSITIVITY_LOW
    }
}
