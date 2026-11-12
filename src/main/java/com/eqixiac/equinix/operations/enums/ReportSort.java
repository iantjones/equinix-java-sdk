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

package com.eqixiac.equinix.operations.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Sort order for {@code GET /v1/reportCenter/reports} ({@code sorts}, Reports v1 spec). A leading
 * {@code -} on the wire value sorts descending; the service default is {@code -GENERATED_DATE}.
 */
public enum ReportSort {
    /** {@code REPORT_NAME}, ascending. */
    REPORT_NAME_ASC("REPORT_NAME"),
    /** {@code -REPORT_NAME}, descending. */
    REPORT_NAME_DESC("-REPORT_NAME"),
    /** {@code GENERATED_DATE}, ascending. */
    GENERATED_DATE_ASC("GENERATED_DATE"),
    /** {@code -GENERATED_DATE}, descending (service default). */
    GENERATED_DATE_DESC("-GENERATED_DATE"),
    /** {@code REQUESTED_DATE}, ascending. */
    REQUESTED_DATE_ASC("REQUESTED_DATE"),
    /** {@code -REQUESTED_DATE}, descending. */
    REQUESTED_DATE_DESC("-REQUESTED_DATE");

    private final String value;

    ReportSort(String value) {
        this.value = value;
    }

    /**
     * Returns the wire value.
     *
     * @return the value sent in {@code sorts}
     */
    @JsonValue
    public String getValue() {
        return value;
    }
}
