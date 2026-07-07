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
import api.equinix.javasdk.customerportal.client.WorkVisits;
import api.equinix.javasdk.customerportal.client.internal.WorkVisitClient;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.WorkVisitLocation;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitUpdateRequest;

import java.util.List;

public class WorkVisitsImpl implements WorkVisits {

    private final CustomerPortal serviceManager;

    private final WorkVisitClient serviceClient;

    public WorkVisitsImpl(WorkVisitClient serviceClient, CustomerPortal serviceManager) {
        this.serviceClient = serviceClient;
        this.serviceManager = serviceManager;
    }

    public OrderResponse order(WorkVisitOrderRequest request) {
        return this.serviceClient.order(request);
    }

    public OrderResponse update(String orderId, WorkVisitUpdateRequest request) {
        return this.serviceClient.update(orderId, request);
    }

    public List<? extends WorkVisitLocation> listLocations() {
        return this.serviceClient.listLocations(null, null, null);
    }

    public List<? extends WorkVisitLocation> listLocations(Boolean detail, String ibxs, String cages) {
        return this.serviceClient.listLocations(detail, ibxs, cages);
    }
}
