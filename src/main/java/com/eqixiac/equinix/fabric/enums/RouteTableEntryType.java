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

package com.eqixiac.equinix.fabric.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RouteTableEntryType {
    IPv4_BGP_ROUTE,
    IPv4_STATIC_ROUTE,
    IPv4_DIRECT_ROUTE,
    IPv6_BGP_ROUTE,
    IPv6_STATIC_ROUTE,
    IPv6_DIRECT_ROUTE,
    UNKNOWN;

    @JsonCreator
    public static RouteTableEntryType fromString(String value) {
        try { return RouteTableEntryType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
