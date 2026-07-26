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

package com.eqixiac.equinix.ibxsmartview.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Comparison of the current power reading for a datapoint against historical values
 * (yesterday, last week, last month and last quarter).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComparisonData {

    @JsonProperty("datapoint")
    private String datapoint;

    @JsonProperty("yesterday")
    private Double yesterday;

    @JsonProperty("lastWeek")
    private Double lastWeek;

    @JsonProperty("lastMonth")
    private Double lastMonth;

    @JsonProperty("lastQuarter")
    private Double lastQuarter;
}
