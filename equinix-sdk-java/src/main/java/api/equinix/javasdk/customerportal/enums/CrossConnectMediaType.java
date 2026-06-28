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
 * Cross-connect media type ({@code media_types} in the cross-connects v2 spec). Several wire
 * values begin with a digit or contain a {@code .}, so they cannot be Java identifiers; the
 * actual transmitted value is supplied via {@link #getValue()}.
 */
public enum CrossConnectMediaType implements APIParam {
    COAX("COAX"),
    MP4_CABLE("MP4_CABLE"),
    MICRON_50_MULTI_MODE_FIBER("50_MICRON_MULTI_MODE_FIBER"),
    MICRON_50_MULTI_MODE_FIBER_OM2("50_MICRON_MULTI_MODE_FIBER_OM2"),
    MICRON_50_MULTI_MODE_FIBER_OM3("50_MICRON_MULTI_MODE_FIBER_OM3"),
    MICRON_50_MULTI_MODE_FIBER_OM4("50_MICRON_MULTI_MODE_FIBER_OM4"),
    MICRON_62_5_MULTI_MODE_FIBER("62.5_MICRON_MULTI_MODE_FIBER"),
    MICRON_62_5_MULTI_MODE_FIBER_OM1("62.5_MICRON_MULTI_MODE_FIBER_OM1"),
    ABAM("ABAM"),
    CAT3("CAT3"),
    CAT5E("CAT5E"),
    CAT6("CAT6"),
    CAT6A("CAT6A"),
    OS1("OS1"),
    SINGLE_MODE_FIBER("SINGLE_MODE_FIBER");

    private final String value;

    CrossConnectMediaType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CrossConnectMediaType fromString(String value) {
        if (value != null) {
            for (CrossConnectMediaType mediaType : values()) {
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
