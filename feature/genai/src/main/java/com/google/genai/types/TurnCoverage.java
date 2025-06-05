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
 * Options about which input is included in the user's turn.
 */
public class TurnCoverage {

    private final String value;
    private Known turnCoverageEnum;
    @JsonCreator
    public TurnCoverage(String value) {
        this.value = value;
        for (Known turnCoverageEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(turnCoverageEnum.toString(), value)) {
                this.turnCoverageEnum = turnCoverageEnum;
                break;
            }
        }
        if (this.turnCoverageEnum == null) {
            this.turnCoverageEnum = Known.TURN_COVERAGE_UNSPECIFIED;
        }
    }

    public TurnCoverage(Known knownValue) {
        this.turnCoverageEnum = knownValue;
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

        if (!(o instanceof TurnCoverage)) {
            return false;
        }

        TurnCoverage other = (TurnCoverage) o;

        if (this.turnCoverageEnum != Known.TURN_COVERAGE_UNSPECIFIED
                && other.turnCoverageEnum != Known.TURN_COVERAGE_UNSPECIFIED) {
            return this.turnCoverageEnum == other.turnCoverageEnum;
        } else if (this.turnCoverageEnum == Known.TURN_COVERAGE_UNSPECIFIED
                && other.turnCoverageEnum == Known.TURN_COVERAGE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.turnCoverageEnum != Known.TURN_COVERAGE_UNSPECIFIED) {
            return this.turnCoverageEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.turnCoverageEnum;
    }

    /**
     * Enum representing the known values for TurnCoverage.
     */
    public enum Known {
        /**
         * If unspecified, the default behavior is `TURN_INCLUDES_ONLY_ACTIVITY`.
         */
        TURN_COVERAGE_UNSPECIFIED,

        /**
         * The users turn only includes activity since the last turn, excluding inactivity (e.g. silence
         * on the audio stream). This is the default behavior.
         */
        TURN_INCLUDES_ONLY_ACTIVITY,

        /**
         * The users turn includes all realtime input since the last turn, including inactivity (e.g.
         * silence on the audio stream).
         */
        TURN_INCLUDES_ALL_INPUT
    }
}
