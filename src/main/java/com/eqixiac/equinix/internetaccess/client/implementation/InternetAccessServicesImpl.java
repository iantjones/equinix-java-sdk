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

package com.eqixiac.equinix.internetaccess.client.implementation;

import com.eqixiac.equinix.InternetAccess;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.internetaccess.client.InternetAccessServices;
import com.eqixiac.equinix.internetaccess.client.internal.InternetAccessServiceClient;
import com.eqixiac.equinix.internetaccess.model.InternetAccessService;
import com.eqixiac.equinix.internetaccess.model.json.InternetAccessServiceJson;
import com.eqixiac.equinix.internetaccess.model.json.creators.ChangeOperationUpdate;
import com.eqixiac.equinix.internetaccess.model.json.creators.InternetAccessServiceOperator;
import com.eqixiac.equinix.internetaccess.model.json.creators.ServiceSearchRequest;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternetAccessServicesImpl implements InternetAccessServices {

    private final InternetAccessServiceClient serviceClient;

    private final InternetAccess serviceManager;

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
