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
 * Enum representing the Imagen 3 Edit mode.
 */
public class EditMode {

    private final String value;
    private Known editModeEnum;
    @JsonCreator
    public EditMode(String value) {
        this.value = value;
        for (Known editModeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(editModeEnum.toString(), value)) {
                this.editModeEnum = editModeEnum;
                break;
            }
        }
        if (this.editModeEnum == null) {
            this.editModeEnum = Known.EDIT_MODE_UNSPECIFIED;
        }
    }

    public EditMode(Known knownValue) {
        this.editModeEnum = knownValue;
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

        if (!(o instanceof EditMode)) {
            return false;
        }

        EditMode other = (EditMode) o;

        if (this.editModeEnum != Known.EDIT_MODE_UNSPECIFIED
                && other.editModeEnum != Known.EDIT_MODE_UNSPECIFIED) {
            return this.editModeEnum == other.editModeEnum;
        } else if (this.editModeEnum == Known.EDIT_MODE_UNSPECIFIED
                && other.editModeEnum == Known.EDIT_MODE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.editModeEnum != Known.EDIT_MODE_UNSPECIFIED) {
            return this.editModeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.editModeEnum;
    }

    /**
     * Enum representing the known values for EditMode.
     */
    public enum Known {
        EDIT_MODE_DEFAULT,

        EDIT_MODE_INPAINT_REMOVAL,

        EDIT_MODE_INPAINT_INSERTION,

        EDIT_MODE_OUTPAINT,

        EDIT_MODE_CONTROLLED_EDITING,

        EDIT_MODE_STYLE,

        EDIT_MODE_BGSWAP,

        EDIT_MODE_PRODUCT_IMAGE,

        EDIT_MODE_UNSPECIFIED
    }
}
