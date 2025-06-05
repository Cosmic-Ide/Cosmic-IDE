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
 * Enum representing the control type of a control reference image.
 */
public class ControlReferenceType {

    private final String value;
    private Known controlReferenceTypeEnum;
    @JsonCreator
    public ControlReferenceType(String value) {
        this.value = value;
        for (Known controlReferenceTypeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(controlReferenceTypeEnum.toString(), value)) {
                this.controlReferenceTypeEnum = controlReferenceTypeEnum;
                break;
            }
        }
        if (this.controlReferenceTypeEnum == null) {
            this.controlReferenceTypeEnum = Known.CONTROL_REFERENCE_TYPE_UNSPECIFIED;
        }
    }

    public ControlReferenceType(Known knownValue) {
        this.controlReferenceTypeEnum = knownValue;
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

        if (!(o instanceof ControlReferenceType)) {
            return false;
        }

        ControlReferenceType other = (ControlReferenceType) o;

        if (this.controlReferenceTypeEnum != Known.CONTROL_REFERENCE_TYPE_UNSPECIFIED
                && other.controlReferenceTypeEnum != Known.CONTROL_REFERENCE_TYPE_UNSPECIFIED) {
            return this.controlReferenceTypeEnum == other.controlReferenceTypeEnum;
        } else if (this.controlReferenceTypeEnum == Known.CONTROL_REFERENCE_TYPE_UNSPECIFIED
                && other.controlReferenceTypeEnum == Known.CONTROL_REFERENCE_TYPE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.controlReferenceTypeEnum != Known.CONTROL_REFERENCE_TYPE_UNSPECIFIED) {
            return this.controlReferenceTypeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.controlReferenceTypeEnum;
    }

    /**
     * Enum representing the known values for ControlReferenceType.
     */
    public enum Known {
        CONTROL_TYPE_DEFAULT,

        CONTROL_TYPE_CANNY,

        CONTROL_TYPE_SCRIBBLE,

        CONTROL_TYPE_FACE_MESH,

        CONTROL_REFERENCE_TYPE_UNSPECIFIED
    }
}
