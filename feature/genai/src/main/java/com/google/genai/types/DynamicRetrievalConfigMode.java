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
 * Config for the dynamic retrieval config mode.
 */
public class DynamicRetrievalConfigMode {

    private final String value;
    private Known dynamicRetrievalConfigModeEnum;
    @JsonCreator
    public DynamicRetrievalConfigMode(String value) {
        this.value = value;
        for (Known dynamicRetrievalConfigModeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(dynamicRetrievalConfigModeEnum.toString(), value)) {
                this.dynamicRetrievalConfigModeEnum = dynamicRetrievalConfigModeEnum;
                break;
            }
        }
        if (this.dynamicRetrievalConfigModeEnum == null) {
            this.dynamicRetrievalConfigModeEnum = Known.DYNAMIC_RETRIEVAL_CONFIG_MODE_UNSPECIFIED;
        }
    }

    public DynamicRetrievalConfigMode(Known knownValue) {
        this.dynamicRetrievalConfigModeEnum = knownValue;
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

        if (!(o instanceof DynamicRetrievalConfigMode)) {
            return false;
        }

        DynamicRetrievalConfigMode other = (DynamicRetrievalConfigMode) o;

        if (this.dynamicRetrievalConfigModeEnum != Known.DYNAMIC_RETRIEVAL_CONFIG_MODE_UNSPECIFIED
                && other.dynamicRetrievalConfigModeEnum
                != Known.DYNAMIC_RETRIEVAL_CONFIG_MODE_UNSPECIFIED) {
            return this.dynamicRetrievalConfigModeEnum == other.dynamicRetrievalConfigModeEnum;
        } else if (this.dynamicRetrievalConfigModeEnum
                == Known.DYNAMIC_RETRIEVAL_CONFIG_MODE_UNSPECIFIED
                && other.dynamicRetrievalConfigModeEnum
                == Known.DYNAMIC_RETRIEVAL_CONFIG_MODE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.dynamicRetrievalConfigModeEnum != Known.DYNAMIC_RETRIEVAL_CONFIG_MODE_UNSPECIFIED) {
            return this.dynamicRetrievalConfigModeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.dynamicRetrievalConfigModeEnum;
    }

    /**
     * Enum representing the known values for DynamicRetrievalConfigMode.
     */
    public enum Known {
        /**
         * Always trigger retrieval.
         */
        MODE_UNSPECIFIED,

        /**
         * Run retrieval only when system decides it is necessary.
         */
        MODE_DYNAMIC,

        DYNAMIC_RETRIEVAL_CONFIG_MODE_UNSPECIFIED
    }
}
