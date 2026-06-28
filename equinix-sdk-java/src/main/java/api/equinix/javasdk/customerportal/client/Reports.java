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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.Report;
import api.equinix.javasdk.customerportal.model.ScheduledReport;
import api.equinix.javasdk.customerportal.model.json.creators.ScheduleReportRequest;

import java.util.List;

/**
 * Client interface for the Report Center in the Equinix Customer Portal.
 *
 * <p>Backed by the Reports v1 API at {@code /v1/reportCenter/reports}. Generated reports are
 * listed and retrieved by id, downloaded as files, and produced from scheduled report
 * definitions which can themselves be listed and created.</p>
 */
public interface Reports {

    /**
     * Lists generated reports.
     *
     * <p>Maps to {@code GET /v1/reportCenter/reports} ({@code getReports}).</p>
     *
     * @return a paginated list of reports
     */
    PaginatedList<Report> getReports();

    /**
     * Retrieves a generated report by id.
     *
     * <p>Maps to {@code GET /v1/reportCenter/reports/{reportId}} ({@code getReportById}).</p>
     *
     * @param reportId the report id
     * @return the matching report
     */
    Report getReportById(String reportId);

    /**
     * Lists scheduled report definitions.
     *
     * <p>Maps to {@code GET /v1/reportCenter/reports/scheduler} ({@code getScheduledReports}).</p>
     *
     * @return the list of scheduled reports
     */
    List<? extends ScheduledReport> getScheduledReports();

    /**
     * Creates a scheduled report definition.
     *
     * <p>Maps to {@code POST /v1/reportCenter/reports/scheduler} ({@code scheduleReport}).</p>
     *
     * @param request the schedule request body
     * @return the created scheduled report
     */
    ScheduledReport scheduleReport(ScheduleReportRequest request);

    /**
     * Generates a report from a scheduled report definition.
     *
     * <p>Maps to {@code POST /v1/reportCenter/reports/scheduler/{scheduledId}/report}
     * ({@code generateReport}).</p>
     *
     * @param scheduledId the scheduled report id
     * @return the generated report
     */
    Report generateReport(String scheduledId);

    /**
     * Downloads a generated report's file.
     *
     * <p>Maps to {@code GET /v1/reportCenter/reports/{reportId}/file} ({@code downloadReport}).</p>
     *
     * @param reportId the report id
     * @return the report file bytes
     */
    byte[] downloadReport(String reportId);
}
