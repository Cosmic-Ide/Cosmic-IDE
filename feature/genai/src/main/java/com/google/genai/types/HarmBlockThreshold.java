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
 * Required. The harm block threshold.
 */
public class HarmBlockThreshold {

    private final String value;
    private Known harmBlockThresholdEnum;
    @JsonCreator
    public HarmBlockThreshold(String value) {
        this.value = value;
        for (Known harmBlockThresholdEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(harmBlockThresholdEnum.toString(), value)) {
                this.harmBlockThresholdEnum = harmBlockThresholdEnum;
                break;
            }
        }
        if (this.harmBlockThresholdEnum == null) {
            this.harmBlockThresholdEnum = Known.HARM_BLOCK_THRESHOLD_UNSPECIFIED;
        }
    }

    public HarmBlockThreshold(Known knownValue) {
        this.harmBlockThresholdEnum = knownValue;
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

        if (!(o instanceof HarmBlockThreshold)) {
            return false;
        }

        HarmBlockThreshold other = (HarmBlockThreshold) o;

        if (this.harmBlockThresholdEnum != Known.HARM_BLOCK_THRESHOLD_UNSPECIFIED
                && other.harmBlockThresholdEnum != Known.HARM_BLOCK_THRESHOLD_UNSPECIFIED) {
            return this.harmBlockThresholdEnum == other.harmBlockThresholdEnum;
        } else if (this.harmBlockThresholdEnum == Known.HARM_BLOCK_THRESHOLD_UNSPECIFIED
                && other.harmBlockThresholdEnum == Known.HARM_BLOCK_THRESHOLD_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.harmBlockThresholdEnum != Known.HARM_BLOCK_THRESHOLD_UNSPECIFIED) {
            return this.harmBlockThresholdEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.harmBlockThresholdEnum;
    }

    /**
     * Enum representing the known values for HarmBlockThreshold.
     */
    public enum Known {
        /**
         * Unspecified harm block threshold.
         */
        HARM_BLOCK_THRESHOLD_UNSPECIFIED,

        /**
         * Block low threshold and above (i.e. block more).
         */
        BLOCK_LOW_AND_ABOVE,

        /**
         * Block medium threshold and above.
         */
        BLOCK_MEDIUM_AND_ABOVE,

        /**
         * Block only high threshold (i.e. block less).
         */
        BLOCK_ONLY_HIGH,

        /**
         * Block none.
         */
        BLOCK_NONE,

        /**
         * Turn off the safety filter.
         */
        OFF
    }
}
