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
 * Trouble ticket problem code ({@code Tickets_Create.code} in the tickets v2 spec). The wire
 * values are dash-separated numeric codes (e.g. {@code 0001-0000}) that cannot be Java
 * identifiers; the actual transmitted value is supplied via {@link #getValue()}.
 */
public enum TicketCode implements APIParam {
    CODE_0000_0000("0000-0000"),
    CODE_0000_0001("0000-0001"),
    CODE_0000_0002("0000-0002"),
    CODE_0001_0000("0001-0000"),
    CODE_0001_0001("0001-0001"),
    CODE_0001_0002("0001-0002"),
    CODE_0001_0003("0001-0003"),
    CODE_0002_0000("0002-0000"),
    CODE_0002_0001("0002-0001"),
    CODE_0003_0000("0003-0000"),
    CODE_0003_0001("0003-0001"),
    CODE_0003_0002("0003-0002"),
    CODE_0003_0003("0003-0003"),
    CODE_0004_0000("0004-0000"),
    CODE_0005_0000("0005-0000"),
    CODE_0005_0001("0005-0001"),
    CODE_0007_0000("0007-0000"),
    CODE_0007_0001("0007-0001"),
    CODE_0007_0002("0007-0002"),
    CODE_0007_0003("0007-0003"),
    CODE_0007_0004("0007-0004"),
    CODE_0008_0000("0008-0000"),
    CODE_0008_0001("0008-0001"),
    CODE_0008_0002("0008-0002"),
    CODE_0008_0003("0008-0003"),
    CODE_0008_0004("0008-0004"),
    CODE_0008_0005("0008-0005"),
    CODE_0008_0006("0008-0006"),
    CODE_0008_0007("0008-0007"),
    CODE_0008_0008("0008-0008"),
    CODE_0008_0009("0008-0009"),
    CODE_0008_0010("0008-0010"),
    CODE_0012_0000("0012-0000"),
    CODE_0012_0001("0012-0001"),
    CODE_0012_0002("0012-0002"),
    CODE_0012_0003("0012-0003"),
    CODE_0012_0004("0012-0004"),
    CODE_0012_0005("0012-0005");

    private final String value;

    TicketCode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TicketCode fromString(String value) {
        if (value != null) {
            for (TicketCode code : values()) {
                if (code.value.equalsIgnoreCase(value) || code.name().equalsIgnoreCase(value)) {
                    return code;
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
