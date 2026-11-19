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
 * The graded answer used for a location, a feature, and the presence of an interconnection party.
 * A value this SDK does not list reads as {@link #NOT_DISCLOSED}, as the specification requires.
 */
public enum Grade {
    /** The requirement is met, or will be met by the date it is needed for service. */
    AVAILABLE,
    /** It can be met with conditions, or the provider has to review it. */
    POSSIBLE,
    /** It is not met and will not be met by the date it is needed for service. */
    UNAVAILABLE,
    /** The provider does not answer this through the API. */
    NOT_DISCLOSED;

    /**
     * Reads a wire value.
     *
     * @param value the wire value, or {@code null}
     * @return the matching constant, or {@link #NOT_DISCLOSED}
     */
    @JsonCreator
    public static Grade fromValue(String value) {
        if (value != null) {
            for (Grade candidate : values()) {
                if (candidate.name().equals(value)) {
                    return candidate;
                }
            }
        }
        return NOT_DISCLOSED;
    }

    /**
     * Says whether the grade is {@link #AVAILABLE} or {@link #POSSIBLE}.
     *
     * @return {@code true} for a grade that does not rule the requirement out
     */
    public boolean isPositive() {
        return this == AVAILABLE || this == POSSIBLE;
    }
}
