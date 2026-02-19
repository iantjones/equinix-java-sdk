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
import api.equinix.javasdk.customerportal.client.WorkVisits;
import api.equinix.javasdk.customerportal.client.internal.WorkVisitClient;
import api.equinix.javasdk.customerportal.model.WorkVisit;
import api.equinix.javasdk.customerportal.model.json.WorkVisitJson;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitOperator;
import api.equinix.javasdk.customerportal.model.wrappers.WorkVisitWrapper;

public class WorkVisitsImpl implements WorkVisits {

    private final CustomerPortal serviceManager;

    private final WorkVisitClient<WorkVisit> serviceClient;

    public WorkVisitsImpl(WorkVisitClient<WorkVisit> serviceClient, CustomerPortal serviceManager) {
        this.serviceClient = serviceClient;
        this.serviceManager = serviceManager;
    }

    public PaginatedList<WorkVisit> list() {
        Page<WorkVisit, WorkVisitJson> responsePage = this.serviceClient.list();
        PaginatedList<WorkVisit> workVisitList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, WorkVisitWrapper::new);
        return new PaginatedList<>(workVisitList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public WorkVisit getByUuid(String uuid) {
        WorkVisitJson workVisitJson = this.serviceClient.getByUuid(uuid);
        return new WorkVisitWrapper(workVisitJson, this.serviceClient);
    }

    public WorkVisitOperator.WorkVisitBuilder define() {
        return new WorkVisitOperator(this.serviceClient).create();
    }
}
