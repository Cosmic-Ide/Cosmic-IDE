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
 * Type of auth scheme.
 */
public class AuthType {

    private final String value;
    private Known authTypeEnum;
    @JsonCreator
    public AuthType(String value) {
        this.value = value;
        for (Known authTypeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(authTypeEnum.toString(), value)) {
                this.authTypeEnum = authTypeEnum;
                break;
            }
        }
        if (this.authTypeEnum == null) {
            this.authTypeEnum = Known.AUTH_TYPE_UNSPECIFIED;
        }
    }

    public AuthType(Known knownValue) {
        this.authTypeEnum = knownValue;
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

        if (!(o instanceof AuthType)) {
            return false;
        }

        AuthType other = (AuthType) o;

        if (this.authTypeEnum != Known.AUTH_TYPE_UNSPECIFIED
                && other.authTypeEnum != Known.AUTH_TYPE_UNSPECIFIED) {
            return this.authTypeEnum == other.authTypeEnum;
        } else if (this.authTypeEnum == Known.AUTH_TYPE_UNSPECIFIED
                && other.authTypeEnum == Known.AUTH_TYPE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.authTypeEnum != Known.AUTH_TYPE_UNSPECIFIED) {
            return this.authTypeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.authTypeEnum;
    }

    /**
     * Enum representing the known values for AuthType.
     */
    public enum Known {
        AUTH_TYPE_UNSPECIFIED,

        /**
         * No Auth.
         */
        NO_AUTH,

        /**
         * API Key Auth.
         */
        API_KEY_AUTH,

        /**
         * HTTP Basic Auth.
         */
        HTTP_BASIC_AUTH,

        /**
         * Google Service Account Auth.
         */
        GOOGLE_SERVICE_ACCOUNT_AUTH,

        /**
         * OAuth auth.
         */
        OAUTH,

        /**
         * OpenID Connect (OIDC) Auth.
         */
        OIDC_AUTH
    }
}
