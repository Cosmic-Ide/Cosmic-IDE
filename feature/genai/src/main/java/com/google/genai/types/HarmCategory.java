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
 * Required. Harm category.
 */
public class HarmCategory {

    private final String value;
    private Known harmCategoryEnum;
    @JsonCreator
    public HarmCategory(String value) {
        this.value = value;
        for (Known harmCategoryEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(harmCategoryEnum.toString(), value)) {
                this.harmCategoryEnum = harmCategoryEnum;
                break;
            }
        }
        if (this.harmCategoryEnum == null) {
            this.harmCategoryEnum = Known.HARM_CATEGORY_UNSPECIFIED;
        }
    }

    public HarmCategory(Known knownValue) {
        this.harmCategoryEnum = knownValue;
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

        if (!(o instanceof HarmCategory)) {
            return false;
        }

        HarmCategory other = (HarmCategory) o;

        if (this.harmCategoryEnum != Known.HARM_CATEGORY_UNSPECIFIED
                && other.harmCategoryEnum != Known.HARM_CATEGORY_UNSPECIFIED) {
            return this.harmCategoryEnum == other.harmCategoryEnum;
        } else if (this.harmCategoryEnum == Known.HARM_CATEGORY_UNSPECIFIED
                && other.harmCategoryEnum == Known.HARM_CATEGORY_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.harmCategoryEnum != Known.HARM_CATEGORY_UNSPECIFIED) {
            return this.harmCategoryEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.harmCategoryEnum;
    }

    /**
     * Enum representing the known values for HarmCategory.
     */
    public enum Known {
        /**
         * The harm category is unspecified.
         */
        HARM_CATEGORY_UNSPECIFIED,

        /**
         * The harm category is hate speech.
         */
        HARM_CATEGORY_HATE_SPEECH,

        /**
         * The harm category is dangerous content.
         */
        HARM_CATEGORY_DANGEROUS_CONTENT,

        /**
         * The harm category is harassment.
         */
        HARM_CATEGORY_HARASSMENT,

        /**
         * The harm category is sexually explicit content.
         */
        HARM_CATEGORY_SEXUALLY_EXPLICIT,

        /**
         * Deprecated: Election filter is not longer supported. The harm category is civic integrity.
         */
        HARM_CATEGORY_CIVIC_INTEGRITY
    }
}
