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
 * Physical media/optic type of a Fabric physical port (the {@code physicalPortsType} attribute of
 * {@code PortRequest}). {@code _10GBASE_SMF} is only applicable for IX ports. The leading underscores
 * are required because Java identifiers may not begin with a digit; each constant carries its spec
 * wire value (e.g. {@code 10GBASE_LR}), which Jackson uses for (de)serialization via
 * {@link com.fasterxml.jackson.annotation.JsonValue} and {@link com.fasterxml.jackson.annotation.JsonCreator}.
 * Unrecognized values deserialize to {@link #UNKNOWN}.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public enum PhysicalPortType {

    _1000BASE_LX("1000BASE_LX"),
    _10GBASE_LR("10GBASE_LR"),
    _100GBASE_LR4("100GBASE_LR4"),
    _10GBASE_ER("10GBASE_ER"),
    _1000BASE_SX("1000BASE_SX"),
    _10GBASE_SMF("10GBASE_SMF"),
    _400GBASE_LR4("400GBASE_LR4"),
    UNKNOWN("UNKNOWN");

    private final String value;

    PhysicalPortType(String value) {
        this.value = value;
    }

    /**
     * The wire value, with the digit-leading underscore stripped (e.g. {@code 10GBASE_LR}).
     *
     * @return the JSON wire value for this physical port type
     */
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Resolves a wire value to a constant, falling back to {@link #UNKNOWN} for unrecognized inputs.
     *
     * @param value the wire value (e.g. {@code 10GBASE_LR})
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static PhysicalPortType fromString(String value) {
        if (value != null) {
            for (PhysicalPortType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
        }
        return UNKNOWN;
    }
}
