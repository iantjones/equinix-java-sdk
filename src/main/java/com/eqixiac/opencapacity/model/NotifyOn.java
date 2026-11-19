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
 * Which changes of an answer produce an event. A value this SDK does not list reads as {@link #UNKNOWN}.
 */
public enum NotifyOn {
    GRADE_CHANGE,
    ANY_CHANGE,
    UNKNOWN;

    /**
     * Reads a wire value.
     *
     * @param value the wire value, or {@code null}
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static NotifyOn fromValue(String value) {
        if (value != null) {
            for (NotifyOn candidate : values()) {
                if (candidate.name().equals(value)) {
                    return candidate;
                }
            }
        }
        return UNKNOWN;
    }
}
