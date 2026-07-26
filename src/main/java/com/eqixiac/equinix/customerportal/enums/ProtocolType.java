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

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Cross-connect protocol type ({@code protocol_types} in the cross-connects v2 spec). Several
 * wire values begin with a digit or contain a {@code -}, so they cannot be Java identifiers;
 * the actual transmitted value is supplied via {@link #getValue()}.
 */
public enum ProtocolType implements APIParam {
    ANTENNA("ANTENNA"),
    DS_3("DS-3"),
    E1("E1"),
    E3("E3"),
    ETHERNET("ETHERNET"),
    FAST_ETHERNET("FAST_ETHERNET"),
    GIGABIT_ETHERNET("GIGABIT_ETHERNET"),
    POTS("POTS"),
    T1("T1"),
    FIBRE_CHANNEL("FIBRE_CHANNEL"),
    GIG_ETHERNET_10("10_GIG_ETHERNET"),
    GIG_ETHERNET_100("100_GIG_ETHERNET"),
    GIG_ETHERNET_40("40_GIG_ETHERNET"),
    DARK_FIBER("DARK_FIBER"),
    DWDM("DWDM"),
    SPEED_56K("56K"),
    ISDN("ISDN"),
    OC_12("OC-12"),
    OC_192("OC-192"),
    OC_3("OC-3"),
    OC_48("OC-48"),
    STM_1("STM-1"),
    STM_16("STM-16"),
    STM_4("STM-4"),
    STM_64("STM-64"),
    NA("NA");

    private final String value;

    ProtocolType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProtocolType fromString(String value) {
        if (value != null) {
            for (ProtocolType protocolType : values()) {
                if (protocolType.value.equalsIgnoreCase(value) || protocolType.name().equalsIgnoreCase(value)) {
                    return protocolType;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
