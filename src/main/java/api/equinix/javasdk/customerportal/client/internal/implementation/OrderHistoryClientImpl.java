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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.OrderHistoryClient;
import api.equinix.javasdk.customerportal.model.OrderHistoryItem;
import api.equinix.javasdk.customerportal.model.json.OrderHistoryItemJson;

import java.util.Map;

public class OrderHistoryClientImpl extends PageableBase implements OrderHistoryClient<OrderHistoryItem> {

    public OrderHistoryClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "OrderHistory");
    }

    public Page<OrderHistoryItem, OrderHistoryItemJson> list() {
        EquinixRequest<OrderHistoryItem> equinixRequest = this.buildRequest("ListOrderHistory", RequestType.PAGINATED, OrderHistoryItemJson.getPagedTypeRef());
        EquinixResponse<OrderHistoryItem> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public OrderHistoryItemJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<OrderHistoryItemJson> equinixRequest = this.buildRequestWithPathParams("GetOrderHistoryItem", RequestType.SINGLE, pParams, OrderHistoryItemJson.getSingleTypeRef());
        EquinixResponse<OrderHistoryItemJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<OrderHistoryItem> nextPage(PaginatedRequest<OrderHistoryItem> equinixRequest) {
        EquinixResponse<OrderHistoryItem> equinixResponse = this.invoke(equinixRequest);
        Page<OrderHistoryItem, OrderHistoryItemJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<OrderHistoryItem> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, (json, client) -> json);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
