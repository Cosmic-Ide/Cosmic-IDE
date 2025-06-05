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
 * Status of the url retrieval.
 */
public class UrlRetrievalStatus {

    private final String value;
    private Known urlRetrievalStatusEnum;
    @JsonCreator
    public UrlRetrievalStatus(String value) {
        this.value = value;
        for (Known urlRetrievalStatusEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(urlRetrievalStatusEnum.toString(), value)) {
                this.urlRetrievalStatusEnum = urlRetrievalStatusEnum;
                break;
            }
        }
        if (this.urlRetrievalStatusEnum == null) {
            this.urlRetrievalStatusEnum = Known.URL_RETRIEVAL_STATUS_UNSPECIFIED;
        }
    }

    public UrlRetrievalStatus(Known knownValue) {
        this.urlRetrievalStatusEnum = knownValue;
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

        if (!(o instanceof UrlRetrievalStatus)) {
            return false;
        }

        UrlRetrievalStatus other = (UrlRetrievalStatus) o;

        if (this.urlRetrievalStatusEnum != Known.URL_RETRIEVAL_STATUS_UNSPECIFIED
                && other.urlRetrievalStatusEnum != Known.URL_RETRIEVAL_STATUS_UNSPECIFIED) {
            return this.urlRetrievalStatusEnum == other.urlRetrievalStatusEnum;
        } else if (this.urlRetrievalStatusEnum == Known.URL_RETRIEVAL_STATUS_UNSPECIFIED
                && other.urlRetrievalStatusEnum == Known.URL_RETRIEVAL_STATUS_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.urlRetrievalStatusEnum != Known.URL_RETRIEVAL_STATUS_UNSPECIFIED) {
            return this.urlRetrievalStatusEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.urlRetrievalStatusEnum;
    }

    /**
     * Enum representing the known values for UrlRetrievalStatus.
     */
    public enum Known {
        /**
         * Default value. This value is unused
         */
        URL_RETRIEVAL_STATUS_UNSPECIFIED,

        /**
         * Url retrieval is successful.
         */
        URL_RETRIEVAL_STATUS_SUCCESS,

        /**
         * Url retrieval is failed due to error.
         */
        URL_RETRIEVAL_STATUS_ERROR
    }
}
