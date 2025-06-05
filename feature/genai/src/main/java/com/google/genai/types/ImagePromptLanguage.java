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
 * Enum that specifies the language of the text in the prompt.
 */
public class ImagePromptLanguage {

    private final String value;
    private Known imagePromptLanguageEnum;
    @JsonCreator
    public ImagePromptLanguage(String value) {
        this.value = value;
        for (Known imagePromptLanguageEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(imagePromptLanguageEnum.toString(), value)) {
                this.imagePromptLanguageEnum = imagePromptLanguageEnum;
                break;
            }
        }
        if (this.imagePromptLanguageEnum == null) {
            this.imagePromptLanguageEnum = Known.IMAGE_PROMPT_LANGUAGE_UNSPECIFIED;
        }
    }

    public ImagePromptLanguage(Known knownValue) {
        this.imagePromptLanguageEnum = knownValue;
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

        if (!(o instanceof ImagePromptLanguage)) {
            return false;
        }

        ImagePromptLanguage other = (ImagePromptLanguage) o;

        if (this.imagePromptLanguageEnum != Known.IMAGE_PROMPT_LANGUAGE_UNSPECIFIED
                && other.imagePromptLanguageEnum != Known.IMAGE_PROMPT_LANGUAGE_UNSPECIFIED) {
            return this.imagePromptLanguageEnum == other.imagePromptLanguageEnum;
        } else if (this.imagePromptLanguageEnum == Known.IMAGE_PROMPT_LANGUAGE_UNSPECIFIED
                && other.imagePromptLanguageEnum == Known.IMAGE_PROMPT_LANGUAGE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.imagePromptLanguageEnum != Known.IMAGE_PROMPT_LANGUAGE_UNSPECIFIED) {
            return this.imagePromptLanguageEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.imagePromptLanguageEnum;
    }

    /**
     * Enum representing the known values for ImagePromptLanguage.
     */
    public enum Known {
        AUTO,

        EN,

        JA,

        KO,

        HI,

        IMAGE_PROMPT_LANGUAGE_UNSPECIFIED
    }
}
