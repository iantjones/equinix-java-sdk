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
 * Product (order) type for an order-history search ({@code filters.productTypes},
 * {@code POST /v1/retrieve-orders}).
 *
 * <p>Values are the enum the live service lists in its HTTP 400 {@code EQX-171-1212} response
 * (observed 2026-11-11). The spec's {@code product-types-enum} is out of date: it has a single
 * {@code SHIPMENT}, which the service rejects, where live has {@code INBOUND_SHIPMENT} and
 * {@code OUTBOUND_SHIPMENT}, and it lacks {@code PATCH_PANEL}, {@code NETWORK_PRODUCT_ACQUISITION}
 * and {@code OTHER}. Trouble tickets are {@code TROUBLE}; {@code TROUBLE_TICKET} is rejected.</p>
 */
public enum OrderHistoryProductType {
    SMART_HANDS,
    TROUBLE,
    INBOUND_SHIPMENT,
    OUTBOUND_SHIPMENT,
    WORK_VISIT,
    SECURITY_ACCESS,
    CONFERENCE_ROOM,
    TOUR_REQUESTS,
    CROSS_CONNECT,
    CLOUD_EXCHANGE_PORT,
    INTERNET_EXCHANGE_PORT,
    METRO_CONNECT,
    POWER,
    IBX_SMARTVIEW_SENSOR,
    ACCESSORY,
    CAGE,
    CABINET,
    PATCH_PANEL,
    IBX_SMART_VIEW,
    NETWORK_PRODUCT_ACQUISITION,
    OTHER,
    /** Read-side fallback for a value this SDK version does not know; never sent. */
    UNKNOWN;

    /**
     * Maps a wire value to a constant, falling back to {@link #UNKNOWN}.
     *
     * @param value the wire value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static OrderHistoryProductType fromString(String value) {
        if (value != null) {
            for (OrderHistoryProductType v : values()) {
                if (v.name().equalsIgnoreCase(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
