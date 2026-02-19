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
import api.equinix.javasdk.customerportal.client.SecureCabinets;
import api.equinix.javasdk.customerportal.client.internal.SecureCabinetClient;
import api.equinix.javasdk.customerportal.model.SecureCabinet;
import api.equinix.javasdk.customerportal.model.json.SecureCabinetJson;
import api.equinix.javasdk.customerportal.model.json.creators.SecureCabinetOperator;
import api.equinix.javasdk.customerportal.model.wrappers.SecureCabinetWrapper;

public class SecureCabinetsImpl implements SecureCabinets {

    private final CustomerPortal serviceManager;

    private final SecureCabinetClient<SecureCabinet> serviceClient;

    public SecureCabinetsImpl(SecureCabinetClient<SecureCabinet> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<SecureCabinet> list() {
        Page<SecureCabinet, SecureCabinetJson> responsePage = this.serviceClient.list();
        PaginatedList<SecureCabinet> secureCabinetList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, SecureCabinetWrapper::new);
        return new PaginatedList<>(secureCabinetList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public SecureCabinet getByUuid(String uuid) {
        SecureCabinetJson secureCabinetJson = this.serviceClient.getByUuid(uuid);
        return new SecureCabinetWrapper(secureCabinetJson, this.serviceClient);
    }

    public SecureCabinetOperator.SecureCabinetBuilder define() {
        return new SecureCabinetOperator(this.serviceClient).create();
    }
}
