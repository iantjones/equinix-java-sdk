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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.Reports;
import api.equinix.javasdk.customerportal.client.internal.ReportClient;
import api.equinix.javasdk.customerportal.model.Report;
import api.equinix.javasdk.customerportal.model.json.ReportJson;

public class ReportsImpl implements Reports {

    private final CustomerPortal serviceManager;

    private final ReportClient<Report> serviceClient;

    public ReportsImpl(ReportClient<Report> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<Report> list() {
        Page<Report, ReportJson> responsePage = this.serviceClient.list();
        PaginatedList<Report> reportList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(reportList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Report getByUuid(String uuid) {
        return this.serviceClient.getByUuid(uuid);
    }
}
