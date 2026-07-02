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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The data period a report covers ({@code period} in the Reports v1 spec).
 */
public enum ReportPeriod {
    DAY_1("1_DAY"),
    DAYS_7("7_DAYS"),
    DAYS_14("14_DAYS"),
    DAYS_30("30_DAYS"),
    DAYS_90("90_DAYS"),
    DAYS_180("180_DAYS"),
    YEAR_1("1_YEAR"),
    CUSTOM("CUSTOM"),
    NONE("NONE"),
    UNKNOWN("UNKNOWN");

    private final String value;

    ReportPeriod(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static ReportPeriod fromString(String value) {
        if (value != null) {
            for (ReportPeriod period : values()) {
                if (period.value.equalsIgnoreCase(value) || period.name().equalsIgnoreCase(value)) {
                    return period;
                }
            }
        }
        return UNKNOWN;
    }
}
