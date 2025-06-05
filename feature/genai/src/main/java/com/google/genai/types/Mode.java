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
 * The mode of the predictor to be used in dynamic retrieval.
 */
public class Mode {

    private final String value;
    private Known modeEnum;
    @JsonCreator
    public Mode(String value) {
        this.value = value;
        for (Known modeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(modeEnum.toString(), value)) {
                this.modeEnum = modeEnum;
                break;
            }
        }
        if (this.modeEnum == null) {
            this.modeEnum = Known.MODE_UNSPECIFIED;
        }
    }

    public Mode(Known knownValue) {
        this.modeEnum = knownValue;
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

        if (!(o instanceof Mode)) {
            return false;
        }

        Mode other = (Mode) o;

        if (this.modeEnum != Known.MODE_UNSPECIFIED && other.modeEnum != Known.MODE_UNSPECIFIED) {
            return this.modeEnum == other.modeEnum;
        } else if (this.modeEnum == Known.MODE_UNSPECIFIED
                && other.modeEnum == Known.MODE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.modeEnum != Known.MODE_UNSPECIFIED) {
            return this.modeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.modeEnum;
    }

    /**
     * Enum representing the known values for Mode.
     */
    public enum Known {
        /**
         * Always trigger retrieval.
         */
        MODE_UNSPECIFIED,

        /**
         * Run retrieval only when system decides it is necessary.
         */
        MODE_DYNAMIC
    }
}
