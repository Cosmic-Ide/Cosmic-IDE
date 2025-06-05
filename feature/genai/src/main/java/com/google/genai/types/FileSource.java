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
 * Source of the File.
 */
public class FileSource {

    private final String value;
    private Known fileSourceEnum;
    @JsonCreator
    public FileSource(String value) {
        this.value = value;
        for (Known fileSourceEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(fileSourceEnum.toString(), value)) {
                this.fileSourceEnum = fileSourceEnum;
                break;
            }
        }
        if (this.fileSourceEnum == null) {
            this.fileSourceEnum = Known.FILE_SOURCE_UNSPECIFIED;
        }
    }

    public FileSource(Known knownValue) {
        this.fileSourceEnum = knownValue;
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

        if (!(o instanceof FileSource)) {
            return false;
        }

        FileSource other = (FileSource) o;

        if (this.fileSourceEnum != Known.FILE_SOURCE_UNSPECIFIED
                && other.fileSourceEnum != Known.FILE_SOURCE_UNSPECIFIED) {
            return this.fileSourceEnum == other.fileSourceEnum;
        } else if (this.fileSourceEnum == Known.FILE_SOURCE_UNSPECIFIED
                && other.fileSourceEnum == Known.FILE_SOURCE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.fileSourceEnum != Known.FILE_SOURCE_UNSPECIFIED) {
            return this.fileSourceEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.fileSourceEnum;
    }

    /**
     * Enum representing the known values for FileSource.
     */
    public enum Known {
        SOURCE_UNSPECIFIED,

        UPLOADED,

        GENERATED,

        FILE_SOURCE_UNSPECIFIED
    }
}
