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

package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Cable connector type for a smart hands run-jumper-cable or request-cables order
 * ({@code connectorType} / {@code connector} in the smart hands v1 spec).
 */
public enum SmartHandsConnectorType implements APIParam {
    RJ45("RJ45"),
    SC("SC"),
    LC("LC"),
    BNC("BNC"),
    OTHER("Other");

    private final String value;

    SmartHandsConnectorType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SmartHandsConnectorType fromString(String value) {
        if (value != null) {
            for (SmartHandsConnectorType connectorType : values()) {
                if (connectorType.value.equalsIgnoreCase(value) || connectorType.name().equalsIgnoreCase(value)) {
                    return connectorType;
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
