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

package com.eqixiac.equinix.operations.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.operations.client.implementation.OperationsConfigImpl;
import com.eqixiac.equinix.operations.client.internal.OrderHistoryClient;
import com.eqixiac.equinix.operations.model.OrderHistoryPage;
import com.eqixiac.equinix.operations.model.PermissibleLocation;
import com.eqixiac.equinix.operations.model.json.OrderHistorySearchResponseJson;
import com.eqixiac.equinix.operations.model.json.PermissibleLocationJson;
import com.eqixiac.equinix.operations.model.json.creators.OrderHistorySearchRequest;

import java.util.List;

public class OrderHistoryClientImpl extends ClientBase implements OrderHistoryClient {

    public OrderHistoryClientImpl(OperationsConfigImpl configClient) {
        super(configClient, "Operations", "OrderHistory");
    }

    public OrderHistoryPage searchPage(OrderHistorySearchRequest request) {
        OrderHistorySearchResponseJson response = postAs("SearchOrderHistory", request, OrderHistorySearchResponseJson.class);
        // An empty 2xx body deserializes to null: report an empty page.
        return response == null ? new OrderHistorySearchResponseJson() : response;
    }

    public List<? extends PermissibleLocation> listLocations() {
        return listAs("ListOrderHistoryLocations", null, null, PermissibleLocationJson.class);
    }
}
