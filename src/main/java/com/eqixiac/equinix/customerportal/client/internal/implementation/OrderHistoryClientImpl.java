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

package com.eqixiac.equinix.customerportal.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.customerportal.client.implementation.CustomerPortalConfigImpl;
import com.eqixiac.equinix.customerportal.client.internal.OrderHistoryClient;
import com.eqixiac.equinix.customerportal.model.OrderHistoryItem;
import com.eqixiac.equinix.customerportal.model.PermissibleLocation;
import com.eqixiac.equinix.customerportal.model.json.OrderHistorySearchResponseJson;
import com.eqixiac.equinix.customerportal.model.json.PermissibleLocationJson;
import com.eqixiac.equinix.customerportal.model.json.creators.OrderHistorySearchRequest;

import java.util.List;

public class OrderHistoryClientImpl extends ClientBase implements OrderHistoryClient {

    public OrderHistoryClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "OrderHistory");
    }

    public List<? extends OrderHistoryItem> search(OrderHistorySearchRequest request) {
        OrderHistorySearchResponseJson response = postAs("SearchOrderHistory", request, OrderHistorySearchResponseJson.class);
        return response.getContent();
    }

    public List<? extends PermissibleLocation> listLocations() {
        return listAs("ListOrderHistoryLocations", null, null, PermissibleLocationJson.class);
    }
}
