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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.customerportal.enums.ReportScheduleStatus;
import api.equinix.javasdk.customerportal.enums.ReportPeriod;
import api.equinix.javasdk.customerportal.enums.ReportScheduleType;
import api.equinix.javasdk.customerportal.model.ScheduledReport;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for a Report Center {@code scheduledReport}.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduledReportJson implements ScheduledReport {

    @JsonProperty("scheduledId")
    private String scheduledId;

    @JsonProperty("reportName")
    private String reportName;

    @JsonProperty("scheduleType")
    private ReportScheduleType scheduleType;

    @JsonProperty("period")
    private ReportPeriod period;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("lastAttemptedDate")
    private String lastAttemptedDate;

    @JsonProperty("customerOrganizationId")
    private Integer customerOrganizationId;

    @JsonProperty("forOrg")
    private Integer forOrg;

    @JsonProperty("forUser")
    private String forUser;

    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;

    @JsonProperty("lastModifiedDate")
    private String lastModifiedDate;

    @JsonProperty("numberOfFailedAttempts")
    private Integer numberOfFailedAttempts;

    @JsonProperty("status")
    private ReportScheduleStatus status;

    @JsonProperty("parameters")
    private List<ReportParameterJson> parameters;

    @JsonProperty("reports")
    private List<ReportJson> reports;
}
