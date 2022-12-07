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
import api.equinix.javasdk.fabric.client.internal.GatewayPackageClient;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.model.GatewayPackage;
import api.equinix.javasdk.fabric.model.json.GatewayPackageJson;
import api.equinix.javasdk.fabric.model.wrappers.GatewayPackageWrapper;

import java.util.Map;

public class GatewayPackageClientImpl extends PageableBase implements GatewayPackageClient<GatewayPackage> {

    public GatewayPackageClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "FabricGateways");
    }

    public Page<GatewayPackage, GatewayPackageJson> list() {
        EquinixRequest<GatewayPackage> equinixRequest = this.buildRequest("GetGatewayPackages", RequestType.PAGINATED, GatewayPackageJson.getPagedTypeRef());
        EquinixResponse<GatewayPackage> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public GatewayPackageJson getByPackageCode(GatewayPackageCode gatewayPackageCode) {
        Map<String, String> pParams = Map.of("gatewayPackageCode", gatewayPackageCode.toString());
        EquinixRequest<GatewayPackage> equinixRequest = this.buildRequestWithPathParams("GetGatewayPackage", RequestType.SINGLE, pParams, GatewayPackageJson.getSingleTypeRef());
        EquinixResponse<GatewayPackage> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<GatewayPackage> nextPage(PaginatedRequest<GatewayPackage> equinixRequest) {
        EquinixResponse<GatewayPackage> equinixResponse = this.invoke(equinixRequest);
        Page<GatewayPackage, GatewayPackageJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<GatewayPackage> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, GatewayPackageWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
