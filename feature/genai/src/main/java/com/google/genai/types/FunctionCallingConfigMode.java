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
 * Config for the function calling config mode.
 */
public class FunctionCallingConfigMode {

    private final String value;
    private Known functionCallingConfigModeEnum;
    @JsonCreator
    public FunctionCallingConfigMode(String value) {
        this.value = value;
        for (Known functionCallingConfigModeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(functionCallingConfigModeEnum.toString(), value)) {
                this.functionCallingConfigModeEnum = functionCallingConfigModeEnum;
                break;
            }
        }
        if (this.functionCallingConfigModeEnum == null) {
            this.functionCallingConfigModeEnum = Known.FUNCTION_CALLING_CONFIG_MODE_UNSPECIFIED;
        }
    }

    public FunctionCallingConfigMode(Known knownValue) {
        this.functionCallingConfigModeEnum = knownValue;
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

        if (!(o instanceof FunctionCallingConfigMode)) {
            return false;
        }

        FunctionCallingConfigMode other = (FunctionCallingConfigMode) o;

        if (this.functionCallingConfigModeEnum != Known.FUNCTION_CALLING_CONFIG_MODE_UNSPECIFIED
                && other.functionCallingConfigModeEnum != Known.FUNCTION_CALLING_CONFIG_MODE_UNSPECIFIED) {
            return this.functionCallingConfigModeEnum == other.functionCallingConfigModeEnum;
        } else if (this.functionCallingConfigModeEnum == Known.FUNCTION_CALLING_CONFIG_MODE_UNSPECIFIED
                && other.functionCallingConfigModeEnum == Known.FUNCTION_CALLING_CONFIG_MODE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.functionCallingConfigModeEnum != Known.FUNCTION_CALLING_CONFIG_MODE_UNSPECIFIED) {
            return this.functionCallingConfigModeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.functionCallingConfigModeEnum;
    }

    /**
     * Enum representing the known values for FunctionCallingConfigMode.
     */
    public enum Known {
        /**
         * The function calling config mode is unspecified. Should not be used.
         */
        MODE_UNSPECIFIED,

        /**
         * Default model behavior, model decides to predict either function calls or natural language
         * response.
         */
        AUTO,

        /**
         * Model is constrained to always predicting function calls only. If "allowed_function_names"
         * are set, the predicted function calls will be limited to any one of "allowed_function_names",
         * else the predicted function calls will be any one of the provided "function_declarations".
         */
        ANY,

        /**
         * Model will not predict any function calls. Model behavior is same as when not passing any
         * function declarations.
         */
        NONE,

        FUNCTION_CALLING_CONFIG_MODE_UNSPECIFIED
    }
}
