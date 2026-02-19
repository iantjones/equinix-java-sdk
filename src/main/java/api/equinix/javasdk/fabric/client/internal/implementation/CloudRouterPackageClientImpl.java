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
import api.equinix.javasdk.fabric.client.internal.CloudRouterPackageClient;
import api.equinix.javasdk.fabric.enums.CloudRouterPackageCode;
import api.equinix.javasdk.fabric.model.CloudRouterPackage;
import api.equinix.javasdk.fabric.model.json.CloudRouterPackageJson;

import java.util.Map;

public class CloudRouterPackageClientImpl extends PageableBase implements CloudRouterPackageClient<CloudRouterPackage> {

    public CloudRouterPackageClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "CloudRouters");
    }

    public Page<CloudRouterPackage, CloudRouterPackageJson> list() {
        EquinixRequest<CloudRouterPackage> equinixRequest = this.buildRequest("GetCloudRouterPackages", RequestType.PAGINATED, CloudRouterPackageJson.getPagedTypeRef());
        EquinixResponse<CloudRouterPackage> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public CloudRouterPackageJson getByPackageCode(CloudRouterPackageCode packageCode) {
        Map<String, String> pParams = Map.of("routerPackageCode", packageCode.toString());
        EquinixRequest<CloudRouterPackage> equinixRequest = this.buildRequestWithPathParams("GetCloudRouterPackage", RequestType.SINGLE, pParams, CloudRouterPackageJson.getSingleTypeRef());
        EquinixResponse<CloudRouterPackage> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<CloudRouterPackage> nextPage(PaginatedRequest<CloudRouterPackage> equinixRequest) {
        EquinixResponse<CloudRouterPackage> equinixResponse = this.invoke(equinixRequest);
        Page<CloudRouterPackage, CloudRouterPackageJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<CloudRouterPackage> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, (json, client) -> json);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
