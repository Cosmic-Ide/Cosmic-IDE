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
 * Enum representing the mask mode of a mask reference image.
 */
public class MaskReferenceMode {

    private final String value;
    private Known maskReferenceModeEnum;
    @JsonCreator
    public MaskReferenceMode(String value) {
        this.value = value;
        for (Known maskReferenceModeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(maskReferenceModeEnum.toString(), value)) {
                this.maskReferenceModeEnum = maskReferenceModeEnum;
                break;
            }
        }
        if (this.maskReferenceModeEnum == null) {
            this.maskReferenceModeEnum = Known.MASK_REFERENCE_MODE_UNSPECIFIED;
        }
    }

    public MaskReferenceMode(Known knownValue) {
        this.maskReferenceModeEnum = knownValue;
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

        if (!(o instanceof MaskReferenceMode)) {
            return false;
        }

        MaskReferenceMode other = (MaskReferenceMode) o;

        if (this.maskReferenceModeEnum != Known.MASK_REFERENCE_MODE_UNSPECIFIED
                && other.maskReferenceModeEnum != Known.MASK_REFERENCE_MODE_UNSPECIFIED) {
            return this.maskReferenceModeEnum == other.maskReferenceModeEnum;
        } else if (this.maskReferenceModeEnum == Known.MASK_REFERENCE_MODE_UNSPECIFIED
                && other.maskReferenceModeEnum == Known.MASK_REFERENCE_MODE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.maskReferenceModeEnum != Known.MASK_REFERENCE_MODE_UNSPECIFIED) {
            return this.maskReferenceModeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.maskReferenceModeEnum;
    }

    /**
     * Enum representing the known values for MaskReferenceMode.
     */
    public enum Known {
        MASK_MODE_DEFAULT,

        MASK_MODE_USER_PROVIDED,

        MASK_MODE_BACKGROUND,

        MASK_MODE_FOREGROUND,

        MASK_MODE_SEMANTIC,

        MASK_REFERENCE_MODE_UNSPECIFIED
    }
}
