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
 * Required. Programming language of the `code`.
 */
public class Language {

    private final String value;
    private Known languageEnum;
    @JsonCreator
    public Language(String value) {
        this.value = value;
        for (Known languageEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(languageEnum.toString(), value)) {
                this.languageEnum = languageEnum;
                break;
            }
        }
        if (this.languageEnum == null) {
            this.languageEnum = Known.LANGUAGE_UNSPECIFIED;
        }
    }

    public Language(Known knownValue) {
        this.languageEnum = knownValue;
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

        if (!(o instanceof Language)) {
            return false;
        }

        Language other = (Language) o;

        if (this.languageEnum != Known.LANGUAGE_UNSPECIFIED
                && other.languageEnum != Known.LANGUAGE_UNSPECIFIED) {
            return this.languageEnum == other.languageEnum;
        } else if (this.languageEnum == Known.LANGUAGE_UNSPECIFIED
                && other.languageEnum == Known.LANGUAGE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.languageEnum != Known.LANGUAGE_UNSPECIFIED) {
            return this.languageEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.languageEnum;
    }

    /**
     * Enum representing the known values for Language.
     */
    public enum Known {
        /**
         * Unspecified language. This value should not be used.
         */
        LANGUAGE_UNSPECIFIED,

        /**
         * Python >= 3.10, with numpy and simpy available.
         */
        PYTHON
    }
}
