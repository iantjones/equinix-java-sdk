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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Request body for scheduling a report
 * ({@code POST /v1/reportCenter/reports/scheduler}, {@code reportRequest}).
 *
 * <p>{@code name}, {@code parameters}, {@code scheduleType} and {@code period} are required.
 * {@code scheduleType} is one of {@code ONE_TIME}/{@code DAILY}/{@code WEEKLY}/{@code MONTHLY}/
 * {@code QUARTERLY}/{@code ANNUALLY}. Each parameter is a {@code {name, value, type, required}}
 * object, supplied here as a free-form map.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduleReportRequest {

    @JsonProperty("name")
    private final String name;

    @JsonProperty("parameters")
    private final List<Map<String, Object>> parameters;

    @JsonProperty("scheduleType")
    private final String scheduleType;

    @JsonProperty("period")
    private final String period;

    @JsonProperty("control")
    private final String control;

    @JsonProperty("categories")
    private final List<String> categories;

    private ScheduleReportRequest(Builder builder) {
        this.name = builder.name;
        this.parameters = builder.parameters;
        this.scheduleType = builder.scheduleType;
        this.period = builder.period;
        this.control = builder.control;
        this.categories = builder.categories;
    }

    /**
     * Returns a new builder for a schedule report request.
     *
     * @param name         the report name (required)
     * @param parameters   the report parameters (required)
     * @param scheduleType the schedule type (required)
     * @param period       the period (required)
     * @return a new builder
     */
    public static Builder builder(String name, List<Map<String, Object>> parameters, String scheduleType, String period) {
        return new Builder(name, parameters, scheduleType, period);
    }

    public static class Builder {
        private final String name;
        private final List<Map<String, Object>> parameters;
        private final String scheduleType;
        private final String period;
        private String control;
        private List<String> categories;

        private Builder(String name, List<Map<String, Object>> parameters, String scheduleType, String period) {
            this.name = name;
            this.parameters = parameters;
            this.scheduleType = scheduleType;
            this.period = period;
        }

        public Builder control(String control) {
            this.control = control;
            return this;
        }

        public Builder categories(List<String> categories) {
            this.categories = categories;
            return this;
        }

        public ScheduleReportRequest build() {
            return new ScheduleReportRequest(this);
        }
    }
}
