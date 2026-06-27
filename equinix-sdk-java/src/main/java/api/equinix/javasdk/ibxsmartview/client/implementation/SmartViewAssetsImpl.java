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

package api.equinix.javasdk.ibxsmartview.client.implementation;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.client.SmartViewAssets;
import api.equinix.javasdk.ibxsmartview.client.internal.SmartViewAssetClient;
import api.equinix.javasdk.ibxsmartview.model.AssetDetail;
import api.equinix.javasdk.ibxsmartview.model.SmartViewAsset;
import api.equinix.javasdk.ibxsmartview.model.TagPointData;
import api.equinix.javasdk.ibxsmartview.model.json.AssetDetailJson;
import api.equinix.javasdk.ibxsmartview.model.json.SmartViewAssetJson;
import api.equinix.javasdk.ibxsmartview.model.json.TagPointDataJson;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SmartViewAssetsImpl implements SmartViewAssets {

    private final IBXSmartView serviceManager;

    private final SmartViewAssetClient<SmartViewAsset> serviceClient;

    public SmartViewAssetsImpl(SmartViewAssetClient<SmartViewAsset> serviceClient, IBXSmartView serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<SmartViewAsset> list(String accountNo, String ibx, String classification, List<String> cages) {
        Page<SmartViewAsset, SmartViewAssetJson> responsePage = serviceClient.list(accountNo, ibx, classification, cages);
        PaginatedList<SmartViewAsset> assetList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(assetList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public AssetDetail getAssetDetails(String accountNo, String ibx, String classification, String assetId) {
        AssetDetailJson assetDetailJson = serviceClient.getAssetDetails(accountNo, ibx, classification, assetId);
        return assetDetailJson;
    }

    public List<AssetDetail> getMultipleAssetDetails(Object requestBody) {
        List<AssetDetailJson> results = serviceClient.getMultipleAssetDetails(requestBody);
        return new ArrayList<>(results);
    }

    public PaginatedList<SmartViewAsset> search(String accountNo, String ibx, String searchString) {
        Page<SmartViewAsset, SmartViewAssetJson> responsePage = serviceClient.search(accountNo, ibx, searchString);
        PaginatedList<SmartViewAsset> assetList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(assetList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public TagPointData getAffectedAssets(String accountNo, String ibx) {
        TagPointDataJson tagPointDataJson = serviceClient.getAffectedAssets(accountNo, ibx);
        return tagPointDataJson;
    }

    public TagPointData getCurrentTagPoint(String accountNo, String ibx) {
        TagPointDataJson tagPointDataJson = serviceClient.getCurrentTagPoint(accountNo, ibx);
        return tagPointDataJson;
    }

    public List<TagPointData> getMultipleCurrentTagPoints(Object requestBody) {
        List<TagPointDataJson> results = serviceClient.getMultipleCurrentTagPoints(requestBody);
        return new ArrayList<>(results);
    }
}
