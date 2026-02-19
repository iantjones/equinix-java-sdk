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

package api.equinix.javasdk.internetaccess.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.RoutingConfigClient;
import api.equinix.javasdk.internetaccess.model.RoutingConfig;
import api.equinix.javasdk.internetaccess.model.json.RoutingConfigJson;
import api.equinix.javasdk.internetaccess.model.json.creators.RoutingConfigCreatorJson;
import api.equinix.javasdk.internetaccess.model.wrappers.RoutingConfigWrapper;

import java.util.Map;

public class RoutingConfigClientImpl extends PageableBase implements RoutingConfigClient<RoutingConfig> {

    public RoutingConfigClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "RoutingConfigs");
    }

    public Page<RoutingConfig, RoutingConfigJson> list() {
        EquinixRequest<RoutingConfig> equinixRequest = this.buildRequest("ListRoutingConfigs", RequestType.PAGINATED, RoutingConfigJson.getPagedTypeRef());
        EquinixResponse<RoutingConfig> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public RoutingConfigJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<RoutingConfigJson> equinixRequest = this.buildRequestWithPathParams("GetRoutingConfig", RequestType.SINGLE, pParams, RoutingConfigJson.getSingleTypeRef());
        EquinixResponse<RoutingConfigJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RoutingConfigJson create(RoutingConfigCreatorJson routingConfigCreatorJson) {
        EquinixRequest<RoutingConfigJson> equinixRequest = this.buildRequest("CreateRoutingConfig", RequestType.SINGLE, RoutingConfigJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, routingConfigCreatorJson);
        EquinixResponse<RoutingConfigJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RoutingConfigJson update(String uuid, RoutingConfigCreatorJson routingConfigCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<RoutingConfigJson> equinixRequest = this.buildRequestWithPathParams("UpdateRoutingConfig", RequestType.SINGLE, pParams, RoutingConfigJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, routingConfigCreatorJson);
        EquinixResponse<RoutingConfigJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RoutingConfigJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<RoutingConfig> nextPage(PaginatedRequest<RoutingConfig> equinixRequest) {
        EquinixResponse<RoutingConfig> equinixResponse = this.invoke(equinixRequest);
        Page<RoutingConfig, RoutingConfigJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<RoutingConfig> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, RoutingConfigWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
