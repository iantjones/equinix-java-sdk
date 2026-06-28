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

package api.equinix.javasdk.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Installed-date range filter on an asset search ({@code filter.dateRange}). Both {@code fromDate}
 * and {@code toDate} are required (ISO 8601 date-time).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetSearchDateRange {

    @JsonProperty("fromDate")
    private final String fromDate;

    @JsonProperty("toDate")
    private final String toDate;

    /**
     * Creates an installed-date range filter.
     *
     * @param fromDate the start date (required)
     * @param toDate   the end date (required)
     */
    public AssetSearchDateRange(String fromDate, String toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
    }
}
