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
import api.equinix.javasdk.customerportal.client.CrossConnects;
import api.equinix.javasdk.customerportal.client.internal.CrossConnectClient;
import api.equinix.javasdk.customerportal.model.CrossConnect;
import api.equinix.javasdk.customerportal.model.json.CrossConnectJson;
import api.equinix.javasdk.customerportal.model.json.creators.CrossConnectOperator;
import api.equinix.javasdk.customerportal.model.wrappers.CrossConnectWrapper;

public class CrossConnectsImpl implements CrossConnects {

    private final CustomerPortal serviceManager;

    private final CrossConnectClient<CrossConnect> serviceClient;

    public CrossConnectsImpl(CrossConnectClient<CrossConnect> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<CrossConnect> list() {
        Page<CrossConnect, CrossConnectJson> responsePage = this.serviceClient.list();
        PaginatedList<CrossConnect> crossConnectList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, CrossConnectWrapper::new);
        return new PaginatedList<>(crossConnectList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public CrossConnect getByUuid(String uuid) {
        CrossConnectJson crossConnectJson = this.serviceClient.getByUuid(uuid);
        return new CrossConnectWrapper(crossConnectJson, this.serviceClient);
    }

    public CrossConnectOperator.CrossConnectBuilder define() {
        return new CrossConnectOperator(this.serviceClient).create();
    }
}
