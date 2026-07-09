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
import api.equinix.javasdk.ibxsmartview.client.SmartViewAssets;
import api.equinix.javasdk.ibxsmartview.client.internal.SmartViewAssetClient;
import api.equinix.javasdk.ibxsmartview.model.AssetDetail;
import api.equinix.javasdk.ibxsmartview.model.AssetDetailsResponse;
import api.equinix.javasdk.ibxsmartview.model.Assets;
import api.equinix.javasdk.ibxsmartview.model.AssetsList;
import api.equinix.javasdk.ibxsmartview.model.HierarchyNodeForAssetAPI;
import api.equinix.javasdk.ibxsmartview.model.TagPointData;
import api.equinix.javasdk.ibxsmartview.model.json.creators.AssetDetailsRequest;
import api.equinix.javasdk.ibxsmartview.model.json.creators.CurrentTagPointRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SmartViewAssetsImpl implements SmartViewAssets {

    private final SmartViewAssetClient serviceClient;

    private final IBXSmartView serviceManager;

    public AssetsList list(String accountNo, String ibx, String classification, List<String> cages) {
        return serviceClient.list(accountNo, ibx, classification, cages);
    }

    public AssetDetail getAssetDetails(String accountNo, String ibx, String classification, String assetId) {
        return serviceClient.getAssetDetails(accountNo, ibx, classification, assetId);
    }

    public AssetDetailsResponse getMultipleAssetDetails(AssetDetailsRequest requestBody) {
        return serviceClient.getMultipleAssetDetails(requestBody);
    }

    public Assets search(String accountNo, String ibx, String searchString) {
        return serviceClient.search(accountNo, ibx, searchString);
    }

    public HierarchyNodeForAssetAPI getAffectedAssets(String accountNo, String ibx, String assetId, String classification) {
        return serviceClient.getAffectedAssets(accountNo, ibx, assetId, classification);
    }

    public TagPointData getCurrentTagPoint(String accountNo, String ibx, String tagId) {
        return serviceClient.getCurrentTagPoint(accountNo, ibx, tagId);
    }

    public TagPointData getMultipleCurrentTagPoints(CurrentTagPointRequest requestBody) {
        return serviceClient.getMultipleCurrentTagPoints(requestBody);
    }
}
