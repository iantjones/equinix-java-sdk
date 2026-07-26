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

package com.eqixiac.equinix.internetaccess.enums;

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Physical port interface type of an Equinix Internet Access (EIA) v1 dedicated port configuration
 * (spec schema {@code DedicatedPortDefaultConfiguration...physicalPort.interface}, property
 * {@code type}). Wire values contain spaces (e.g. {@code 1G SMF}), so constants map to the spec
 * spelling via {@code getValue()}. {@link #UNKNOWN} is a read-side fallback for values added after
 * this SDK release — never send it.
 */
public enum PortInterfaceType implements APIParam {
    SMF_1G("1G SMF"),
    SMF_10G("10G SMF"),
    SMF_100G("100G SMF"),
    UNKNOWN("UNKNOWN");

    private final String value;

    PortInterfaceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PortInterfaceType fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (PortInterfaceType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return value;
    }
}
