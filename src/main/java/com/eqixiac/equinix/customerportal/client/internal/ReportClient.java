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

package com.eqixiac.equinix.customerportal.client.internal;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.customerportal.model.Report;
import com.eqixiac.equinix.customerportal.model.ScheduledReport;
import com.eqixiac.equinix.customerportal.model.json.ReportDefinitionJson;
import com.eqixiac.equinix.customerportal.model.json.ReportJson;
import com.eqixiac.equinix.customerportal.model.json.ScheduledReportJson;
import com.eqixiac.equinix.customerportal.model.json.creators.ScheduleReportRequest;

import java.util.List;

public interface ReportClient<T> extends Pageable<T> {

    Page<ReportJson> getReports();

    ReportJson getReportById(String reportId);

    List<? extends Report> deleteReports(List<String> reportIds);

    byte[] downloadReports(List<String> reportIds);

    List<? extends ScheduledReport> getScheduledReports();

    ScheduledReportJson scheduleReport(ScheduleReportRequest request);

    boolean deleteScheduledReports(List<String> scheduledIds);

    ScheduledReportJson getScheduledReport(String scheduledId);

    ScheduledReportJson updateScheduledReport(String scheduledId, ScheduleReportRequest request);

    ReportJson generateReport(String scheduledId);

    List<ReportDefinitionJson> getReportDefinitions();

    ReportDefinitionJson getReportDefinition(String reportName);

    byte[] downloadReport(String reportId);
}
