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

package api.equinix.javasdk.ibxsmartview.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.SmartViewAssetClient;
import api.equinix.javasdk.ibxsmartview.model.SmartViewAsset;
import api.equinix.javasdk.ibxsmartview.model.json.AssetDetailJson;
import api.equinix.javasdk.ibxsmartview.model.json.SmartViewAssetJson;
import api.equinix.javasdk.ibxsmartview.model.json.TagPointDataJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SmartViewAssetClientImpl extends PageableBase implements SmartViewAssetClient<SmartViewAsset> {

    public SmartViewAssetClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "Assets");
    }

    public Page<SmartViewAsset, SmartViewAssetJson> list(String accountNo, String ibx, String classification, List<String> cages) {
        Map<String, List<String>> qParams = new LinkedHashMap<>();
        qParams.put("accountNo", List.of(accountNo));
        qParams.put("ibx", List.of(ibx));
        qParams.put("classification", List.of(classification));
        if (cages != null && !cages.isEmpty()) {
            qParams.put("cages", cages);
        }
        EquinixRequest<SmartViewAsset> equinixRequest = this.buildRequestWithQueryParams("ListAssets", RequestType.PAGINATED, qParams, SmartViewAssetJson.getPagedTypeRef());
        EquinixResponse<SmartViewAsset> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public AssetDetailJson getAssetDetails(String accountNo, String ibx, String classification, String assetId) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx),
                "classification", List.of(classification),
                "assetId", List.of(assetId)
        );
        EquinixRequest<SmartViewAsset> equinixRequest = this.buildRequestWithQueryParams("GetAssetDetails", RequestType.SINGLE, qParams, AssetDetailJson.getSingleTypeRef());
        EquinixResponse<SmartViewAsset> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    @SuppressWarnings("unchecked")
    public List<AssetDetailJson> getMultipleAssetDetails(Object requestBody) {
        EquinixRequest<SmartViewAsset> equinixRequest = this.buildRequest("PostAssetDetails", RequestType.SINGLE, AssetDetailJson.getListTypeRef());
        Utils.serializeJson(equinixRequest, requestBody);
        EquinixResponse<SmartViewAsset> equinixResponse = this.invoke(equinixRequest);
        return (List<AssetDetailJson>) (Object) Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public Page<SmartViewAsset, SmartViewAssetJson> search(String accountNo, String ibx, String searchString) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx),
                "searchString", List.of(searchString)
        );
        EquinixRequest<SmartViewAsset> equinixRequest = this.buildRequestWithQueryParams("SearchAssets", RequestType.PAGINATED, qParams, SmartViewAssetJson.getPagedTypeRef());
        EquinixResponse<SmartViewAsset> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public TagPointDataJson getAffectedAssets(String accountNo, String ibx) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx)
        );
        EquinixRequest<SmartViewAsset> equinixRequest = this.buildRequestWithQueryParams("GetAffectedAssets", RequestType.SINGLE, qParams, TagPointDataJson.getSingleTypeRef());
        EquinixResponse<SmartViewAsset> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public TagPointDataJson getCurrentTagPoint(String accountNo, String ibx) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx)
        );
        EquinixRequest<SmartViewAsset> equinixRequest = this.buildRequestWithQueryParams("GetCurrentTagPoint", RequestType.SINGLE, qParams, TagPointDataJson.getSingleTypeRef());
        EquinixResponse<SmartViewAsset> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    @SuppressWarnings("unchecked")
    public List<TagPointDataJson> getMultipleCurrentTagPoints(Object requestBody) {
        EquinixRequest<SmartViewAsset> equinixRequest = this.buildRequest("PostCurrentTagPoints", RequestType.SINGLE, TagPointDataJson.getListTypeRef());
        Utils.serializeJson(equinixRequest, requestBody);
        EquinixResponse<SmartViewAsset> equinixResponse = this.invoke(equinixRequest);
        return (List<TagPointDataJson>) (Object) Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<SmartViewAsset> nextPage(PaginatedRequest<SmartViewAsset> equinixRequest) {
        EquinixResponse<SmartViewAsset> equinixResponse = this.invoke(equinixRequest);
        Page<SmartViewAsset, SmartViewAssetJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<SmartViewAsset> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, (json, client) -> json);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
