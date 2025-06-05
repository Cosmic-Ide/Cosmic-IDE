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
 * Optional. The type of the data.
 */
public class Type {

    private final String value;
    private Known typeEnum;
    @JsonCreator
    public Type(String value) {
        this.value = value;
        for (Known typeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(typeEnum.toString(), value)) {
                this.typeEnum = typeEnum;
                break;
            }
        }
        if (this.typeEnum == null) {
            this.typeEnum = Known.TYPE_UNSPECIFIED;
        }
    }

    public Type(Known knownValue) {
        this.typeEnum = knownValue;
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

        if (!(o instanceof Type)) {
            return false;
        }

        Type other = (Type) o;

        if (this.typeEnum != Known.TYPE_UNSPECIFIED && other.typeEnum != Known.TYPE_UNSPECIFIED) {
            return this.typeEnum == other.typeEnum;
        } else if (this.typeEnum == Known.TYPE_UNSPECIFIED
                && other.typeEnum == Known.TYPE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.typeEnum != Known.TYPE_UNSPECIFIED) {
            return this.typeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.typeEnum;
    }

    /**
     * Enum representing the known values for Type.
     */
    public enum Known {
        /**
         * Not specified, should not be used.
         */
        TYPE_UNSPECIFIED,

        /**
         * OpenAPI string type
         */
        STRING,

        /**
         * OpenAPI number type
         */
        NUMBER,

        /**
         * OpenAPI integer type
         */
        INTEGER,

        /**
         * OpenAPI boolean type
         */
        BOOLEAN,

        /**
         * OpenAPI array type
         */
        ARRAY,

        /**
         * OpenAPI object type
         */
        OBJECT,

        /**
         * Null type
         */
        NULL
    }
}
