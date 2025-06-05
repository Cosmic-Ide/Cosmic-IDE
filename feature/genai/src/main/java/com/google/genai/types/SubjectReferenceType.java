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
 * Enum representing the subject type of a subject reference image.
 */
public class SubjectReferenceType {

    private final String value;
    private Known subjectReferenceTypeEnum;
    @JsonCreator
    public SubjectReferenceType(String value) {
        this.value = value;
        for (Known subjectReferenceTypeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(subjectReferenceTypeEnum.toString(), value)) {
                this.subjectReferenceTypeEnum = subjectReferenceTypeEnum;
                break;
            }
        }
        if (this.subjectReferenceTypeEnum == null) {
            this.subjectReferenceTypeEnum = Known.SUBJECT_REFERENCE_TYPE_UNSPECIFIED;
        }
    }

    public SubjectReferenceType(Known knownValue) {
        this.subjectReferenceTypeEnum = knownValue;
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

        if (!(o instanceof SubjectReferenceType)) {
            return false;
        }

        SubjectReferenceType other = (SubjectReferenceType) o;

        if (this.subjectReferenceTypeEnum != Known.SUBJECT_REFERENCE_TYPE_UNSPECIFIED
                && other.subjectReferenceTypeEnum != Known.SUBJECT_REFERENCE_TYPE_UNSPECIFIED) {
            return this.subjectReferenceTypeEnum == other.subjectReferenceTypeEnum;
        } else if (this.subjectReferenceTypeEnum == Known.SUBJECT_REFERENCE_TYPE_UNSPECIFIED
                && other.subjectReferenceTypeEnum == Known.SUBJECT_REFERENCE_TYPE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.subjectReferenceTypeEnum != Known.SUBJECT_REFERENCE_TYPE_UNSPECIFIED) {
            return this.subjectReferenceTypeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.subjectReferenceTypeEnum;
    }

    /**
     * Enum representing the known values for SubjectReferenceType.
     */
    public enum Known {
        SUBJECT_TYPE_DEFAULT,

        SUBJECT_TYPE_PERSON,

        SUBJECT_TYPE_ANIMAL,

        SUBJECT_TYPE_PRODUCT,

        SUBJECT_REFERENCE_TYPE_UNSPECIFIED
    }
}
