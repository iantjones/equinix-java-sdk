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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.OrderHistoryClient;
import api.equinix.javasdk.customerportal.model.OrderHistoryItem;
import api.equinix.javasdk.customerportal.model.json.OrderHistoryItemJson;

public class OrderHistoryClientImpl extends ResourceClientBase<OrderHistoryItem, OrderHistoryItemJson> implements OrderHistoryClient<OrderHistoryItem> {

    public OrderHistoryClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "OrderHistory", OrderHistoryItemJson.class);
    }

    @Override
    protected OrderHistoryItem wrap(OrderHistoryItemJson json) {
        return json;
    }

    public Page<OrderHistoryItem, OrderHistoryItemJson> list() {
        return listPage("ListOrderHistory");
    }

    public OrderHistoryItemJson getByUuid(String uuid) {
        return getOne("GetOrderHistoryItem", uuid);
    }
}
