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

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.SmartViewAssetClient;
import api.equinix.javasdk.ibxsmartview.model.json.AssetDetailsGetResponseJson;
import api.equinix.javasdk.ibxsmartview.model.json.AssetDetailsResponseJson;
import api.equinix.javasdk.ibxsmartview.model.json.AssetsJson;
import api.equinix.javasdk.ibxsmartview.model.json.AssetsListJson;
import api.equinix.javasdk.ibxsmartview.model.json.HierarchyNodeForAssetAPIJson;
import api.equinix.javasdk.ibxsmartview.model.json.TagPointDataJson;
import api.equinix.javasdk.ibxsmartview.model.json.creators.AssetDetailsRequest;
import api.equinix.javasdk.ibxsmartview.model.json.creators.CurrentTagPointRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SmartViewAssetClientImpl extends ClientBase implements SmartViewAssetClient {

    public SmartViewAssetClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "Assets");
    }

    public AssetsListJson list(String accountNo, String ibx, String classification, List<String> cages) {
        Map<String, List<String>> qParams = new LinkedHashMap<>();
        qParams.put("accountNo", List.of(accountNo));
        qParams.put("ibx", List.of(ibx));
        qParams.put("classification", List.of(classification));
        if (cages != null && !cages.isEmpty()) {
            qParams.put("cages", cages);
        }
        return getAs("ListAssets", Map.of(), qParams, AssetsListJson.class);
    }

    public AssetDetailsGetResponseJson getAssetDetails(String accountNo, String ibx, String classification, String assetId) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx),
                "classification", List.of(classification),
                "assetId", List.of(assetId)
        );
        return getAs("GetAssetDetails", Map.of(), qParams, AssetDetailsGetResponseJson.class);
    }

    public AssetDetailsResponseJson getMultipleAssetDetails(AssetDetailsRequest requestBody) {
        return postAs("PostAssetDetails", requestBody, AssetDetailsResponseJson.class);
    }

    public AssetsJson search(String accountNo, String ibx, String searchString) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx),
                "searchString", List.of(searchString)
        );
        return getAs("SearchAssets", Map.of(), qParams, AssetsJson.class);
    }

    public HierarchyNodeForAssetAPIJson getAffectedAssets(String accountNo, String ibx, String assetId, String classification) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx),
                "assetId", List.of(assetId),
                "classification", List.of(classification)
        );
        return getAs("GetAffectedAssets", Map.of(), qParams, HierarchyNodeForAssetAPIJson.class);
    }

    public TagPointDataJson getCurrentTagPoint(String accountNo, String ibx, String tagId) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx),
                "tagid", List.of(tagId)
        );
        return getAs("GetCurrentTagPoint", Map.of(), qParams, TagPointDataJson.class);
    }

    public TagPointDataJson getMultipleCurrentTagPoints(CurrentTagPointRequest requestBody) {
        return postAs("PostCurrentTagPoints", requestBody, TagPointDataJson.class);
    }
}
