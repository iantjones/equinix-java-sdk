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
import api.equinix.javasdk.customerportal.client.SmartHandsRequests;
import api.equinix.javasdk.customerportal.client.internal.SmartHandsClient;
import api.equinix.javasdk.customerportal.model.SmartHands;
import api.equinix.javasdk.customerportal.model.json.SmartHandsJson;
import api.equinix.javasdk.customerportal.model.json.creators.SmartHandsOperator;
import api.equinix.javasdk.customerportal.model.wrappers.SmartHandsWrapper;

public class SmartHandsRequestsImpl implements SmartHandsRequests {

    private final CustomerPortal serviceManager;

    private final SmartHandsClient<SmartHands> serviceClient;

    public SmartHandsRequestsImpl(SmartHandsClient<SmartHands> serviceClient, CustomerPortal serviceManager) {
        this.serviceClient = serviceClient;
        this.serviceManager = serviceManager;
    }

    public PaginatedList<SmartHands> list() {
        Page<SmartHands, SmartHandsJson> responsePage = this.serviceClient.list();
        PaginatedList<SmartHands> smartHandsList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, SmartHandsWrapper::new);
        return new PaginatedList<>(smartHandsList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public SmartHands getByUuid(String uuid) {
        SmartHandsJson smartHandsJson = this.serviceClient.getByUuid(uuid);
        return new SmartHandsWrapper(smartHandsJson, this.serviceClient);
    }

    public SmartHandsOperator.SmartHandsBuilder define() {
        return new SmartHandsOperator(this.serviceClient).create();
    }
}
