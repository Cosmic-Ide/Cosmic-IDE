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
 * The different ways of handling user activity.
 */
public class ActivityHandling {

    private final String value;
    private Known activityHandlingEnum;
    @JsonCreator
    public ActivityHandling(String value) {
        this.value = value;
        for (Known activityHandlingEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(activityHandlingEnum.toString(), value)) {
                this.activityHandlingEnum = activityHandlingEnum;
                break;
            }
        }
        if (this.activityHandlingEnum == null) {
            this.activityHandlingEnum = Known.ACTIVITY_HANDLING_UNSPECIFIED;
        }
    }

    public ActivityHandling(Known knownValue) {
        this.activityHandlingEnum = knownValue;
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

        if (!(o instanceof ActivityHandling)) {
            return false;
        }

        ActivityHandling other = (ActivityHandling) o;

        if (this.activityHandlingEnum != Known.ACTIVITY_HANDLING_UNSPECIFIED
                && other.activityHandlingEnum != Known.ACTIVITY_HANDLING_UNSPECIFIED) {
            return this.activityHandlingEnum == other.activityHandlingEnum;
        } else if (this.activityHandlingEnum == Known.ACTIVITY_HANDLING_UNSPECIFIED
                && other.activityHandlingEnum == Known.ACTIVITY_HANDLING_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.activityHandlingEnum != Known.ACTIVITY_HANDLING_UNSPECIFIED) {
            return this.activityHandlingEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.activityHandlingEnum;
    }

    /**
     * Enum representing the known values for ActivityHandling.
     */
    public enum Known {
        /**
         * If unspecified, the default behavior is `START_OF_ACTIVITY_INTERRUPTS`.
         */
        ACTIVITY_HANDLING_UNSPECIFIED,

        /**
         * If true, start of activity will interrupt the model's response (also called "barge in"). The
         * model's current response will be cut-off in the moment of the interruption. This is the
         * default behavior.
         */
        START_OF_ACTIVITY_INTERRUPTS,

        /**
         * The model's response will not be interrupted.
         */
        NO_INTERRUPTION
    }
}
