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
 * Options for feature selection preference.
 */
public class FeatureSelectionPreference {

    private final String value;
    private Known featureSelectionPreferenceEnum;
    @JsonCreator
    public FeatureSelectionPreference(String value) {
        this.value = value;
        for (Known featureSelectionPreferenceEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(featureSelectionPreferenceEnum.toString(), value)) {
                this.featureSelectionPreferenceEnum = featureSelectionPreferenceEnum;
                break;
            }
        }
        if (this.featureSelectionPreferenceEnum == null) {
            this.featureSelectionPreferenceEnum = Known.FEATURE_SELECTION_PREFERENCE_UNSPECIFIED;
        }
    }

    public FeatureSelectionPreference(Known knownValue) {
        this.featureSelectionPreferenceEnum = knownValue;
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

        if (!(o instanceof FeatureSelectionPreference)) {
            return false;
        }

        FeatureSelectionPreference other = (FeatureSelectionPreference) o;

        if (this.featureSelectionPreferenceEnum != Known.FEATURE_SELECTION_PREFERENCE_UNSPECIFIED
                && other.featureSelectionPreferenceEnum != Known.FEATURE_SELECTION_PREFERENCE_UNSPECIFIED) {
            return this.featureSelectionPreferenceEnum == other.featureSelectionPreferenceEnum;
        } else if (this.featureSelectionPreferenceEnum == Known.FEATURE_SELECTION_PREFERENCE_UNSPECIFIED
                && other.featureSelectionPreferenceEnum == Known.FEATURE_SELECTION_PREFERENCE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.featureSelectionPreferenceEnum != Known.FEATURE_SELECTION_PREFERENCE_UNSPECIFIED) {
            return this.featureSelectionPreferenceEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.featureSelectionPreferenceEnum;
    }

    /**
     * Enum representing the known values for FeatureSelectionPreference.
     */
    public enum Known {
        FEATURE_SELECTION_PREFERENCE_UNSPECIFIED,

        PRIORITIZE_QUALITY,

        BALANCED,

        PRIORITIZE_COST
    }
}
