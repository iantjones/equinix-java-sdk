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
 * Jumper type for a smart hands run-jumper-cable order ({@code jumperType} in the smart hands
 * v1 spec). Several wire values contain spaces or a {@code -}, so they cannot be Java
 * identifiers; the actual transmitted value is supplied via {@link #getValue()}.
 */
public enum SmartHandsJumperType implements APIParam {
    JUMPER("Jumper"),
    PRE_WIRING("Pre-Wiring"),
    PATCH_CABLE("Patch Cable"),
    OTHER("Other");

    private final String value;

    SmartHandsJumperType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SmartHandsJumperType fromString(String value) {
        if (value != null) {
            for (SmartHandsJumperType jumperType : values()) {
                if (jumperType.value.equalsIgnoreCase(value) || jumperType.name().equalsIgnoreCase(value)) {
                    return jumperType;
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
