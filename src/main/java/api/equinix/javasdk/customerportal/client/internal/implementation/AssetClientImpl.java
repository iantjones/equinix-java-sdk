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
import api.equinix.javasdk.customerportal.client.internal.AssetClient;
import api.equinix.javasdk.customerportal.model.Asset;
import api.equinix.javasdk.customerportal.model.json.AssetJson;

import java.util.Map;

public class AssetClientImpl extends PageableBase implements AssetClient<Asset> {

    public AssetClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Assets");
    }

    public Page<Asset, AssetJson> list() {
        EquinixRequest<Asset> equinixRequest = this.buildRequest("ListAssets", RequestType.PAGINATED, AssetJson.getPagedTypeRef());
        EquinixResponse<Asset> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public AssetJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<AssetJson> equinixRequest = this.buildRequestWithPathParams("GetAsset", RequestType.SINGLE, pParams, AssetJson.getSingleTypeRef());
        EquinixResponse<AssetJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<Asset> nextPage(PaginatedRequest<Asset> equinixRequest) {
        EquinixResponse<Asset> equinixResponse = this.invoke(equinixRequest);
        Page<Asset, AssetJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Asset> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, (json, client) -> json);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
