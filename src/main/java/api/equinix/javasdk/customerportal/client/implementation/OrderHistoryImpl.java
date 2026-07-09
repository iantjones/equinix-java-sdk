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
import api.equinix.javasdk.customerportal.client.OrderHistory;
import api.equinix.javasdk.customerportal.client.internal.OrderHistoryClient;
import api.equinix.javasdk.customerportal.model.OrderHistoryItem;
import api.equinix.javasdk.customerportal.model.PermissibleLocation;
import api.equinix.javasdk.customerportal.model.json.creators.OrderHistorySearchRequest;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderHistoryImpl implements OrderHistory {

    private final OrderHistoryClient serviceClient;

    private final CustomerPortal serviceManager;

    public List<? extends OrderHistoryItem> search(OrderHistorySearchRequest request) {
        return this.serviceClient.search(request);
    }

    public List<? extends PermissibleLocation> listLocations() {
        return this.serviceClient.listLocations();
    }
}
