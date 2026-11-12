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
 * Status of an order-history header: the {@code orderStatus} of a result and the
 * {@code filters.orderStatus} of a search ({@code POST /v1/retrieve-orders}).
 *
 * <p>Values are the enum the live service lists in its HTTP 400 {@code EQX-171-1212} response
 * (observed 2026-11-11), a superset of the spec's {@code order-header-status-enum}
 * ({@code ENTERED, SUBMITTED, IN_PROGRESS, PENDING_QA, CANCELLED, CLOSED}).</p>
 */
public enum OrderHistoryStatus {
    ENTERED,
    BOOKED,
    SUBMITTED,
    IN_PROGRESS,
    PENDING_QA,
    PENDING_INFLIGHT,
    PARTIAL_SUBMISSION,
    CANCELLED,
    PENDING_CANCELLATION,
    CLOSED,
    QUOTE_IN_PROGRESS,
    QUOTE_IN_REVIEW,
    QUOTE_ORDER_SUBMITTED,
    QUOTE_CUSTOMER_ACCEPTED,
    QUOTE_PENDING_CUSTOMER_ACCEPTANCE,
    QUOTE_CANCELLED,
    QUOTE_EXPIRED,
    EXPEDITE_AWAITING_CUSTOMER_RESPONSE,
    PENDING_CUSTOMER_INPUT,
    PII_HOLD,
    ON_HOLD,
    PENDING_SIGNATURE,
    /** Read-side fallback for a value this SDK version does not know; never sent. */
    UNKNOWN;

    /**
     * Maps a wire value to a constant, falling back to {@link #UNKNOWN}.
     *
     * @param value the wire value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static OrderHistoryStatus fromString(String value) {
        if (value != null) {
            for (OrderHistoryStatus v : values()) {
                if (v.name().equalsIgnoreCase(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
