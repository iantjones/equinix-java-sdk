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
 * Cable media type for a smart hands run-jumper-cable or request-cables order
 * ({@code mediaType} in the smart hands v1 spec). Several wire values contain spaces or a
 * {@code -}, so they cannot be Java identifiers; the actual transmitted value is supplied via
 * {@link #getValue()}.
 */
public enum SmartHandsMediaType implements APIParam {
    MULTI_MODE_62_5_MIC("Multi-mode 62.5mic"),
    MULTI_MODE_50_MIC("Multi-mode 50mic"),
    SINGLE_MODE("Single-mode"),
    CAT_5("Cat-5"),
    CAT_6("Cat-6"),
    COAX("Coax"),
    POTS("POTS"),
    T1("T1"),
    E1("E1");

    private final String value;

    SmartHandsMediaType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SmartHandsMediaType fromString(String value) {
        if (value != null) {
            for (SmartHandsMediaType mediaType : values()) {
                if (mediaType.value.equalsIgnoreCase(value) || mediaType.name().equalsIgnoreCase(value)) {
                    return mediaType;
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
