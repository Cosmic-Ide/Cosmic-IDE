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
 * Optional. Specify if the threshold is used for probability or severity score. If not specified,
 * the threshold is used for probability score.
 */
public class HarmBlockMethod {

    private final String value;
    private Known harmBlockMethodEnum;
    @JsonCreator
    public HarmBlockMethod(String value) {
        this.value = value;
        for (Known harmBlockMethodEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(harmBlockMethodEnum.toString(), value)) {
                this.harmBlockMethodEnum = harmBlockMethodEnum;
                break;
            }
        }
        if (this.harmBlockMethodEnum == null) {
            this.harmBlockMethodEnum = Known.HARM_BLOCK_METHOD_UNSPECIFIED;
        }
    }

    public HarmBlockMethod(Known knownValue) {
        this.harmBlockMethodEnum = knownValue;
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

        if (!(o instanceof HarmBlockMethod)) {
            return false;
        }

        HarmBlockMethod other = (HarmBlockMethod) o;

        if (this.harmBlockMethodEnum != Known.HARM_BLOCK_METHOD_UNSPECIFIED
                && other.harmBlockMethodEnum != Known.HARM_BLOCK_METHOD_UNSPECIFIED) {
            return this.harmBlockMethodEnum == other.harmBlockMethodEnum;
        } else if (this.harmBlockMethodEnum == Known.HARM_BLOCK_METHOD_UNSPECIFIED
                && other.harmBlockMethodEnum == Known.HARM_BLOCK_METHOD_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.harmBlockMethodEnum != Known.HARM_BLOCK_METHOD_UNSPECIFIED) {
            return this.harmBlockMethodEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.harmBlockMethodEnum;
    }

    /**
     * Enum representing the known values for HarmBlockMethod.
     */
    public enum Known {
        /**
         * The harm block method is unspecified.
         */
        HARM_BLOCK_METHOD_UNSPECIFIED,

        /**
         * The harm block method uses both probability and severity scores.
         */
        SEVERITY,

        /**
         * The harm block method uses the probability score.
         */
        PROBABILITY
    }
}
