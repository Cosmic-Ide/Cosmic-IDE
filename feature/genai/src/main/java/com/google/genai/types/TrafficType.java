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
 * Output only. Traffic type. This shows whether a request consumes Pay-As-You-Go or Provisioned
 * Throughput quota.
 */
public class TrafficType {

    private final String value;
    private Known trafficTypeEnum;
    @JsonCreator
    public TrafficType(String value) {
        this.value = value;
        for (Known trafficTypeEnum : Known.values()) {
            if (Ascii.equalsIgnoreCase(trafficTypeEnum.toString(), value)) {
                this.trafficTypeEnum = trafficTypeEnum;
                break;
            }
        }
        if (this.trafficTypeEnum == null) {
            this.trafficTypeEnum = Known.TRAFFIC_TYPE_UNSPECIFIED;
        }
    }

    public TrafficType(Known knownValue) {
        this.trafficTypeEnum = knownValue;
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

        if (!(o instanceof TrafficType)) {
            return false;
        }

        TrafficType other = (TrafficType) o;

        if (this.trafficTypeEnum != Known.TRAFFIC_TYPE_UNSPECIFIED
                && other.trafficTypeEnum != Known.TRAFFIC_TYPE_UNSPECIFIED) {
            return this.trafficTypeEnum == other.trafficTypeEnum;
        } else if (this.trafficTypeEnum == Known.TRAFFIC_TYPE_UNSPECIFIED
                && other.trafficTypeEnum == Known.TRAFFIC_TYPE_UNSPECIFIED) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.trafficTypeEnum != Known.TRAFFIC_TYPE_UNSPECIFIED) {
            return this.trafficTypeEnum.hashCode();
        } else {
            return Objects.hashCode(this.value);
        }
    }

    public Known knownEnum() {
        return this.trafficTypeEnum;
    }

    /**
     * Enum representing the known values for TrafficType.
     */
    public enum Known {
        /**
         * Unspecified request traffic type.
         */
        TRAFFIC_TYPE_UNSPECIFIED,

        /**
         * Type for Pay-As-You-Go traffic.
         */
        ON_DEMAND,

        /**
         * Type for Provisioned Throughput traffic.
         */
        PROVISIONED_THROUGHPUT
    }
}
