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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.ReportClient;
import api.equinix.javasdk.customerportal.model.Report;
import api.equinix.javasdk.customerportal.model.ScheduledReport;
import api.equinix.javasdk.customerportal.model.json.ReportJson;
import api.equinix.javasdk.customerportal.model.json.ScheduledReportJson;
import api.equinix.javasdk.customerportal.model.json.ScheduledReportsResponseJson;
import api.equinix.javasdk.customerportal.model.json.creators.ScheduleReportRequest;

import java.util.List;
import java.util.Map;

public class ReportClientImpl extends ResourceClientBase<Report, ReportJson> implements ReportClient<Report> {

    public ReportClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Reports", ReportJson.class);
    }

    @Override
    protected Report wrap(ReportJson json) {
        return json;
    }

    public Page<Report, ReportJson> getReports() {
        return listPage("GetReports");
    }

    public ReportJson getReportById(String reportId) {
        return getOne("GetReport", Map.of("reportId", reportId));
    }

    public List<? extends ScheduledReport> getScheduledReports() {
        ScheduledReportsResponseJson response = getAs("GetScheduledReports", ScheduledReportsResponseJson.class);
        return response.getData();
    }

    public ScheduledReportJson scheduleReport(ScheduleReportRequest request) {
        return postAs("ScheduleReport", request, ScheduledReportJson.class);
    }

    public ReportJson generateReport(String scheduledId) {
        EquinixRequest<ReportJson> request = buildRequestWithPathParams("GenerateReport", RequestType.SINGLE,
                Map.of("scheduledId", scheduledId), ReportJson.class);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    public byte[] downloadReport(String reportId) {
        return bytesOp("DownloadReport", Map.of("reportId", reportId), null);
    }
}
