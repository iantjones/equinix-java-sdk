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

package api.equinix.javasdk.internetaccess.client.implementation;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.internetaccess.client.InternetAccessServices;
import api.equinix.javasdk.internetaccess.client.internal.InternetAccessServiceClient;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessServiceJson;
import api.equinix.javasdk.internetaccess.model.json.creators.ChangeOperationUpdate;
import api.equinix.javasdk.internetaccess.model.json.creators.InternetAccessServiceOperator;
import api.equinix.javasdk.internetaccess.model.json.creators.ServiceSearchRequest;

import java.util.List;

public class InternetAccessServicesImpl implements InternetAccessServices {

    private final InternetAccess serviceManager;

    private final InternetAccessServiceClient serviceClient;

    public InternetAccessServicesImpl(InternetAccessServiceClient serviceClient, InternetAccess serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public InternetAccessServiceOperator.InternetAccessServiceBuilder define() {
        return new InternetAccessServiceOperator(this.serviceClient).create();
    }

    public InternetAccessService getByUuid(String serviceId) {
        return this.serviceClient.getByUuid(serviceId);
    }

    public InternetAccessService update(String serviceId, List<ChangeOperationUpdate> operations) {
        return this.serviceClient.update(serviceId, operations, false);
    }

    public InternetAccessService update(String serviceId, List<ChangeOperationUpdate> operations, boolean dryRun) {
        return this.serviceClient.update(serviceId, operations, dryRun);
    }

    public Boolean delete(String serviceId) {
        return this.serviceClient.delete(serviceId, false);
    }

    public Boolean delete(String serviceId, boolean dryRun) {
        return this.serviceClient.delete(serviceId, dryRun);
    }

    public PaginatedFilteredList<InternetAccessService> search(ServiceSearchRequest searchRequest) {
        Page<InternetAccessServiceJson> responsePage = this.serviceClient.search(searchRequest);
        return ResponseHandler.toPaginatedFilteredList(responsePage, this.serviceClient, (json, client) -> json);
    }
}
