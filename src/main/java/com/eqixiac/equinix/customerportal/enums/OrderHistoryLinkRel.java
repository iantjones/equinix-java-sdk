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

package com.eqixiac.equinix.customerportal.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The relation of an order-history link ({@code link.rel} in the Order History v1 spec).
 */
public enum OrderHistoryLinkRel {
    SELF("self"),
    PREV("prev"),
    NEXT("next"),
    ORDER_FULL_DETAILS("orderFullDetails"),
    UNKNOWN("UNKNOWN");

    private final String value;

    OrderHistoryLinkRel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static OrderHistoryLinkRel fromString(String value) {
        if (value != null) {
            for (OrderHistoryLinkRel rel : values()) {
                if (rel.value.equalsIgnoreCase(value) || rel.name().equalsIgnoreCase(value)) {
                    return rel;
                }
            }
        }
        return UNKNOWN;
    }
}
