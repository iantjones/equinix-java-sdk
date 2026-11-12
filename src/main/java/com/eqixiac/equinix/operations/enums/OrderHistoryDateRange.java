/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.operations.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Relative date window for an order-history search ({@code filters.dateRange}). Values are the
 * spec enum, confirmed by the live service's HTTP 400 enum list (2026-11-11).
 */
public enum OrderHistoryDateRange {
    PAST_7_DAYS,
    PAST_14_DAYS,
    PAST_30_DAYS,
    PAST_3_MONTHS,
    PAST_6_MONTHS,
    PAST_YEAR,
    /** Read-side fallback for a value this SDK version does not know; never sent. */
    UNKNOWN;

    /**
     * Maps a wire value to a constant, falling back to {@link #UNKNOWN}.
     *
     * @param value the wire value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static OrderHistoryDateRange fromString(String value) {
        if (value != null) {
            for (OrderHistoryDateRange v : values()) {
                if (v.name().equalsIgnoreCase(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
