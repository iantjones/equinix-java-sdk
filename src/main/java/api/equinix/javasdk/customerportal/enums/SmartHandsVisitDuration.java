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
 * Duration of the visit for a smart hands cage-escort order ({@code durationVisit} in the smart
 * hands v1 spec). The wire values are human-readable strings, so they cannot be Java
 * identifiers; the actual transmitted value is supplied via {@link #getValue()}.
 */
public enum SmartHandsVisitDuration implements APIParam {
    THIRTY_MINUTES("30 Minutes"),
    SIXTY_MINUTES("60 Minutes"),
    NINETY_MINUTES("90 Minutes"),
    TWO_HOURS("2 Hours"),
    TWO_HOURS_THIRTY_MINUTES("2 Hours 30 Minutes"),
    THREE_HOURS("3 Hours"),
    THREE_HOURS_THIRTY_MINUTES("3 Hours 30 Minutes"),
    FOUR_HOURS("4 Hours");

    private final String value;

    SmartHandsVisitDuration(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SmartHandsVisitDuration fromString(String value) {
        if (value != null) {
            for (SmartHandsVisitDuration duration : values()) {
                if (duration.value.equalsIgnoreCase(value) || duration.name().equalsIgnoreCase(value)) {
                    return duration;
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
