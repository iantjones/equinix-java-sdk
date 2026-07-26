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

/**
 * The product type of an IP block ({@code IPV4_IP_BLOCK} / {@code IPV6_IP_BLOCK}).
 */
public enum IpBlockProductType {
    IPV4_IP_BLOCK,
    IPV6_IP_BLOCK,
    UNKNOWN;

    @JsonCreator
    public static IpBlockProductType fromString(String value) {
        try { return IpBlockProductType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
