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
 * Sort field for an order-history search ({@code sorts[].name}).
 *
 * <p>Values are the enum the live service lists in its HTTP 400 response (observed 2026-11-11), a
 * superset of the spec's {@code ORDERING_CONTACT, ACCOUNT_NUMBER, CREATED_ON}.
 * {@code ORDERING_CONTACT} sorts by the ordering contact's last name.</p>
 */
public enum OrderHistorySortField {
    SOURCE,
    ORDERING_CONTACT,
    ACCOUNT_NUMBER,
    ACCOUNT_NAME,
    STATUS,
    CREATED_ON,
    SR_NUMBER,
    /** Read-side fallback for a value this SDK version does not know; never sent. */
    UNKNOWN;

    /**
     * Maps a wire value to a constant, falling back to {@link #UNKNOWN}.
     *
     * @param value the wire value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static OrderHistorySortField fromString(String value) {
        if (value != null) {
            for (OrderHistorySortField v : values()) {
                if (v.name().equalsIgnoreCase(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
