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

/**
 * Cross-connect connector type ({@code connector_types} in the cross-connects v2 and lookup v2 specs).
 * {@link #UNKNOWN} is a read-side fallback — never send it.
 */
public enum ConnectorType implements APIParam {
    BNC,
    LC,
    FC,
    SC,
    ST,
    WIRE_WRAP,
    RJ45,
    E2000,
    RJ11,
    UNKNOWN;

    @JsonCreator
    public static ConnectorType fromString(String value) {
        try { return ConnectorType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
