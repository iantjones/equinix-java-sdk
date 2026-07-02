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

package api.equinix.javasdk.fabric.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Identifies the service type associated with a port (the Fabric v4 {@code PortServiceCode}
 * schema: {@code CX}, {@code IX}, {@code IA}, {@code MC}). Unrecognized values deserialize
 * to {@link #UNKNOWN} rather than failing the whole response.
 *
 * @author ianjones
 */
public enum PortServiceCode {
    CX,
    IX,
    IA,
    MC,
    UNKNOWN;

    /**
     * Deserializes a service code leniently: an unrecognized value maps to {@link #UNKNOWN}
     * instead of failing the enclosing response.
     *
     * @param value the raw API value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static PortServiceCode fromString(String value) {
        try { return PortServiceCode.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
