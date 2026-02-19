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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.RoutingProtocolClient;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.json.RoutingProtocolJson;
import api.equinix.javasdk.fabric.model.json.creators.RoutingProtocolCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.RoutingProtocolWrapper;

import java.util.Map;

public class RoutingProtocolClientImpl extends PageableBase implements RoutingProtocolClient<RoutingProtocol> {

    public RoutingProtocolClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RoutingProtocols");
    }

    public Page<RoutingProtocol, RoutingProtocolJson> list(String connectionId) {
        Map<String, String> pParams = Map.of("connectionId", connectionId);
        EquinixRequest<RoutingProtocol> equinixRequest = this.buildRequestWithPathParams("GetRoutingProtocols", RequestType.PAGINATED, pParams, RoutingProtocolJson.getPagedTypeRef());
        EquinixResponse<RoutingProtocol> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public RoutingProtocolJson getByUuid(String connectionId, String uuid) {
        Map<String, String> pParams = Map.of("connectionId", connectionId, "uuid", uuid);
        EquinixRequest<RoutingProtocolJson> equinixRequest = this.buildRequestWithPathParams("GetRoutingProtocol", RequestType.SINGLE, pParams, RoutingProtocolJson.getSingleTypeRef());
        EquinixResponse<RoutingProtocolJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RoutingProtocolJson create(String connectionId, RoutingProtocolCreatorJson routingProtocolCreatorJson) {
        Map<String, String> pParams = Map.of("connectionId", connectionId);
        EquinixRequest<RoutingProtocolJson> equinixRequest = this.buildRequestWithPathParams("PostRoutingProtocol", RequestType.SINGLE, pParams, RoutingProtocolJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, routingProtocolCreatorJson);
        EquinixResponse<RoutingProtocolJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RoutingProtocolJson update(String connectionId, String uuid, Object updates) {
        Map<String, String> pParams = Map.of("connectionId", connectionId, "uuid", uuid);
        EquinixRequest<RoutingProtocolJson> equinixRequest = this.buildRequestWithPathParams("PatchRoutingProtocol", RequestType.SINGLE, pParams, RoutingProtocolJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, updates);
        EquinixResponse<RoutingProtocolJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RoutingProtocolJson delete(String connectionId, String uuid) {
        Map<String, String> pParams = Map.of("connectionId", connectionId, "uuid", uuid);
        EquinixRequest<RoutingProtocol> equinixRequest = this.buildRequestWithPathParams("DeleteRoutingProtocol", RequestType.SINGLE, pParams, RoutingProtocolJson.getSingleTypeRef());
        EquinixResponse<RoutingProtocol> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RoutingProtocolJson refresh(String connectionId, String uuid) {
        return this.getByUuid(connectionId, uuid);
    }

    public PaginatedList<RoutingProtocol> nextPage(PaginatedRequest<RoutingProtocol> equinixRequest) {
        EquinixResponse<RoutingProtocol> equinixResponse = this.invoke(equinixRequest);
        Page<RoutingProtocol, RoutingProtocolJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<RoutingProtocol> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, RoutingProtocolWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
