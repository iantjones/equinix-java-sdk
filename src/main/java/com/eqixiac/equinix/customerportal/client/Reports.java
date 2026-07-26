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

package com.eqixiac.equinix.customerportal.client;

import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.model.Report;
import com.eqixiac.equinix.customerportal.model.ScheduledReport;
import com.eqixiac.equinix.customerportal.model.json.ReportDefinitionJson;
import com.eqixiac.equinix.customerportal.model.json.creators.ScheduleReportRequest;

import java.util.List;

/**
 * Client interface for the Report Center in the Equinix Customer Portal.
 *
 * <p>Backed by the Reports v1 API at {@code /v1/reportCenter/reports}. Generated reports are
 * listed, retrieved, downloaded and deleted; scheduled report definitions can be listed, created,
 * fetched, updated and deleted; and the available report definitions can be enumerated.</p>
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
     * Deletes generated reports in bulk.
     *
     * <p>Maps to {@code DELETE /v1/reportCenter/reports} ({@code deleteReports}). This is a
     * best-effort bulk delete: the response carries one result per requested report id, each with a
     * {@code reportId} and a per-report {@code status} ({@code SUCCESS} or {@code ERROR}), so callers
     * can detect partial failures.</p>
     *
     * @param reportIds the report ids to delete
     * @return the per-report delete results
     */
    List<? extends Report> deleteReports(List<String> reportIds);

    /**
     * Downloads one or more generated reports as a combined file (zip).
     *
     * <p>Maps to {@code GET /v1/reportCenter/reports/files} ({@code downloadReports}).</p>
     *
     * @param reportIds the report ids to download
     * @return the downloaded file bytes
     */
    byte[] downloadReports(List<String> reportIds);

    /**
     * Downloads a single generated report's file.
     *
     * <p>Maps to {@code GET /v1/reportCenter/reports/{reportId}/file} ({@code downloadReport}).</p>
     *
     * @param reportId the report id
     * @return the report file bytes
     */
    byte[] downloadReport(String reportId);

    /**
     * Lists scheduled report definitions.
     *
     * <p>Maps to {@code GET /v1/reportCenter/reports/scheduler} ({@code getScheduledReports}).</p>
     *
     * @return the list of scheduled reports
     */
    List<? extends ScheduledReport> getScheduledReports();

    /**
     * Retrieves a scheduled report by id.
     *
     * <p>Maps to {@code GET /v1/reportCenter/reports/scheduler/{scheduledId}}
     * ({@code getScheduledReport}).</p>
     *
     * @param scheduledId the scheduled report id
     * @return the matching scheduled report
     */
    ScheduledReport getScheduledReport(String scheduledId);

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
     * Updates an existing scheduled report definition.
     *
     * <p>Maps to {@code PUT /v1/reportCenter/reports/scheduler/{scheduledId}}
     * ({@code updateScheduledReport}).</p>
     *
     * @param scheduledId the scheduled report id
     * @param request     the updated schedule request body
     * @return the updated scheduled report
     */
    ScheduledReport updateScheduledReport(String scheduledId, ScheduleReportRequest request);

    /**
     * Deletes scheduled report definitions in bulk.
     *
     * <p>Maps to {@code DELETE /v1/reportCenter/reports/scheduler}
     * ({@code deleteScheduledReports}).</p>
     *
     * @param scheduledIds the scheduled report ids to delete
     * @return {@code true} if the request succeeded
     */
    boolean deleteScheduledReports(List<String> scheduledIds);

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
     * Lists the available report definitions.
     *
     * <p>Maps to {@code GET /v1/reportCenter/reports/definitions}
     * ({@code getReportDefinitions}).</p>
     *
     * @return the list of report definitions
     */
    List<ReportDefinitionJson> getReportDefinitions();

    /**
     * Retrieves a single report definition by name.
     *
     * <p>Maps to {@code GET /v1/reportCenter/reports/definitions/{reportName}}
     * ({@code getReportDefinition}).</p>
     *
     * @param reportName the report definition name
     * @return the matching report definition
     */
    ReportDefinitionJson getReportDefinition(String reportName);
}
