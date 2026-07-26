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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.client.Reports;
import com.eqixiac.equinix.customerportal.client.internal.ReportClient;
import com.eqixiac.equinix.customerportal.model.Report;
import com.eqixiac.equinix.customerportal.model.ScheduledReport;
import com.eqixiac.equinix.customerportal.model.json.ReportDefinitionJson;
import com.eqixiac.equinix.customerportal.model.json.ReportJson;
import com.eqixiac.equinix.customerportal.model.json.creators.ScheduleReportRequest;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReportsImpl implements Reports {

    private final ReportClient<Report> serviceClient;

    private final CustomerPortal serviceManager;

    public PaginatedList<Report> getReports() {
        Page<ReportJson> responsePage = this.serviceClient.getReports();
        PaginatedList<Report> reportList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(reportList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Report getReportById(String reportId) {
        return this.serviceClient.getReportById(reportId);
    }

    public List<? extends Report> deleteReports(List<String> reportIds) {
        return this.serviceClient.deleteReports(reportIds);
    }

    public byte[] downloadReports(List<String> reportIds) {
        return this.serviceClient.downloadReports(reportIds);
    }

    public byte[] downloadReport(String reportId) {
        return this.serviceClient.downloadReport(reportId);
    }

    public List<? extends ScheduledReport> getScheduledReports() {
        return this.serviceClient.getScheduledReports();
    }

    public ScheduledReport getScheduledReport(String scheduledId) {
        return this.serviceClient.getScheduledReport(scheduledId);
    }

    public ScheduledReport scheduleReport(ScheduleReportRequest request) {
        return this.serviceClient.scheduleReport(request);
    }

    public ScheduledReport updateScheduledReport(String scheduledId, ScheduleReportRequest request) {
        return this.serviceClient.updateScheduledReport(scheduledId, request);
    }

    public boolean deleteScheduledReports(List<String> scheduledIds) {
        return this.serviceClient.deleteScheduledReports(scheduledIds);
    }

    public Report generateReport(String scheduledId) {
        return this.serviceClient.generateReport(scheduledId);
    }

    public List<ReportDefinitionJson> getReportDefinitions() {
        return this.serviceClient.getReportDefinitions();
    }

    public ReportDefinitionJson getReportDefinition(String reportName) {
        return this.serviceClient.getReportDefinition(reportName);
    }
}
