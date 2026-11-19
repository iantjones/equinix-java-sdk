/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.opencapacity.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Why a result is not {@code AVAILABLE}. A value this SDK does not list reads as {@link #UNKNOWN}.
 */
public enum Reason {
    POWER,
    SPACE,
    COOLING,
    DENSITY,
    TIMING,
    TERM,
    FEATURE,
    INTERCONNECTION,
    REDUNDANCY,
    POWER_FEED,
    OFFERING,
    LOCATION_NOT_SERVED,
    REVIEW_REQUIRED,
    OTHER,
    UNKNOWN;

    /**
     * Reads a wire value.
     *
     * @param value the wire value, or {@code null}
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static Reason fromValue(String value) {
        if (value != null) {
            for (Reason candidate : values()) {
                if (candidate.name().equals(value)) {
                    return candidate;
                }
            }
        }
        return UNKNOWN;
    }
}
