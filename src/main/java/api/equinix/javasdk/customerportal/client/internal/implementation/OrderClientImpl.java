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
import api.equinix.javasdk.customerportal.client.internal.OrderClient;
import api.equinix.javasdk.customerportal.model.Order;
import api.equinix.javasdk.customerportal.model.json.OrderJson;
import api.equinix.javasdk.customerportal.model.json.creators.OrderCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.OrderWrapper;

import java.util.Map;

public class OrderClientImpl extends PageableBase implements OrderClient<Order> {

    public OrderClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Orders");
    }

    public Page<Order, OrderJson> list() {
        EquinixRequest<Order> equinixRequest = this.buildRequest("ListOrders", RequestType.PAGINATED, OrderJson.getPagedTypeRef());
        EquinixResponse<Order> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public OrderJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<OrderJson> equinixRequest = this.buildRequestWithPathParams("GetOrder", RequestType.SINGLE, pParams, OrderJson.getSingleTypeRef());
        EquinixResponse<OrderJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public OrderJson create(OrderCreatorJson orderCreatorJson) {
        EquinixRequest<OrderJson> equinixRequest = this.buildRequest("CreateOrder", RequestType.SINGLE, OrderJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, orderCreatorJson);
        EquinixResponse<OrderJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<Order> nextPage(PaginatedRequest<Order> equinixRequest) {
        EquinixResponse<Order> equinixResponse = this.invoke(equinixRequest);
        Page<Order, OrderJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Order> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, OrderWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
