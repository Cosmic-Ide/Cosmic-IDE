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
 * The model routing preference.
 */
public class ModelRoutingPreference {

    private final String value;
    private Known modelRoutingPreferenceEnum;
    @JsonCreator
    public ModelRoutingPreference(String value) {
        this.value = value;
        for (Known modelRoutingPreferenceEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(modelRoutingPreferenceEnum.toString(), value)) {
                this.modelRoutingPreferenceEnum = modelRoutingPreferenceEnum;
                break;
            }
        }
        if (this.modelRoutingPreferenceEnum == null) {
            this.modelRoutingPreferenceEnum = Known.MODEL_ROUTING_PREFERENCE_UNSPECIFIED;
        }
    }

    public ModelRoutingPreference(Known knownValue) {
        this.modelRoutingPreferenceEnum = knownValue;
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

        if (!(o instanceof ModelRoutingPreference)) {
            return false;
        }

        ModelRoutingPreference other = (ModelRoutingPreference) o;

        if (this.modelRoutingPreferenceEnum != Known.MODEL_ROUTING_PREFERENCE_UNSPECIFIED
                && other.modelRoutingPreferenceEnum != Known.MODEL_ROUTING_PREFERENCE_UNSPECIFIED) {
            return this.modelRoutingPreferenceEnum == other.modelRoutingPreferenceEnum;
        } else if (this.modelRoutingPreferenceEnum == Known.MODEL_ROUTING_PREFERENCE_UNSPECIFIED
                && other.modelRoutingPreferenceEnum == Known.MODEL_ROUTING_PREFERENCE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.modelRoutingPreferenceEnum != Known.MODEL_ROUTING_PREFERENCE_UNSPECIFIED) {
            return this.modelRoutingPreferenceEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.modelRoutingPreferenceEnum;
    }

    /**
     * Enum representing the known values for ModelRoutingPreference.
     */
    public enum Known {
        UNKNOWN,

        PRIORITIZE_QUALITY,

        BALANCED,

        PRIORITIZE_COST,

        MODEL_ROUTING_PREFERENCE_UNSPECIFIED
    }
}
