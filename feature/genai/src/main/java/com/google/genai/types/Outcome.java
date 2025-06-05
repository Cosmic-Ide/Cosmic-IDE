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
 * Required. Outcome of the code execution.
 */
public class Outcome {

    private final String value;
    private Known outcomeEnum;
    @JsonCreator
    public Outcome(String value) {
        this.value = value;
        for (Known outcomeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(outcomeEnum.toString(), value)) {
                this.outcomeEnum = outcomeEnum;
                break;
            }
        }
        if (this.outcomeEnum == null) {
            this.outcomeEnum = Known.OUTCOME_UNSPECIFIED;
        }
    }

    public Outcome(Known knownValue) {
        this.outcomeEnum = knownValue;
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

        if (!(o instanceof Outcome)) {
            return false;
        }

        Outcome other = (Outcome) o;

        if (this.outcomeEnum != Known.OUTCOME_UNSPECIFIED
                && other.outcomeEnum != Known.OUTCOME_UNSPECIFIED) {
            return this.outcomeEnum == other.outcomeEnum;
        } else if (this.outcomeEnum == Known.OUTCOME_UNSPECIFIED
                && other.outcomeEnum == Known.OUTCOME_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.outcomeEnum != Known.OUTCOME_UNSPECIFIED) {
            return this.outcomeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.outcomeEnum;
    }

    /**
     * Enum representing the known values for Outcome.
     */
    public enum Known {
        /**
         * Unspecified status. This value should not be used.
         */
        OUTCOME_UNSPECIFIED,

        /**
         * Code execution completed successfully.
         */
        OUTCOME_OK,

        /**
         * Code execution finished but with a failure. `stderr` should contain the reason.
         */
        OUTCOME_FAILED,

        /**
         * Code execution ran for too long, and was cancelled. There may or may not be a partial output
         * present.
         */
        OUTCOME_DEADLINE_EXCEEDED
    }
}
