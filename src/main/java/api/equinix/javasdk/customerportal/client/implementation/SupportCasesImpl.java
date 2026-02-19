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
import api.equinix.javasdk.customerportal.client.SupportCases;
import api.equinix.javasdk.customerportal.client.internal.SupportCaseClient;
import api.equinix.javasdk.customerportal.model.SupportCase;
import api.equinix.javasdk.customerportal.model.json.SupportCaseJson;
import api.equinix.javasdk.customerportal.model.json.creators.SupportCaseOperator;
import api.equinix.javasdk.customerportal.model.wrappers.SupportCaseWrapper;

public class SupportCasesImpl implements SupportCases {

    private final CustomerPortal serviceManager;

    private final SupportCaseClient<SupportCase> serviceClient;

    public SupportCasesImpl(SupportCaseClient<SupportCase> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<SupportCase> list() {
        Page<SupportCase, SupportCaseJson> responsePage = this.serviceClient.list();
        PaginatedList<SupportCase> supportCaseList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, SupportCaseWrapper::new);
        return new PaginatedList<>(supportCaseList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public SupportCase getByUuid(String uuid) {
        SupportCaseJson supportCaseJson = this.serviceClient.getByUuid(uuid);
        return new SupportCaseWrapper(supportCaseJson, this.serviceClient);
    }

    public SupportCaseOperator.SupportCaseBuilder define() {
        return new SupportCaseOperator(this.serviceClient).create();
    }
}
