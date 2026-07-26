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

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * <p>{@code XF_PORT} is the sole {@code PortType} value defined by the Fabric spec. {@code XF_PHYSICAL_PORT}
 * is retained because the same enum backs the {@code type} attribute of a physical port read response,
 * whose spec value is {@code XF_PHYSICAL_PORT}. {@code IX_PORT} and {@code XF_INTERCONNECT_PORT} come from
 * the {@code ServiceProfileAccessPointCOLO} schema, whose colo port {@code type} this enum also backs.</p>
 *
 * @author ianjones
 */
public enum PortType implements APIParam {
    XF_PORT,
    XF_PHYSICAL_PORT,
    IX_PORT,
    XF_INTERCONNECT_PORT,
    UNKNOWN;

    @JsonCreator
    public static PortType fromString(String value) {
        try { return PortType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}