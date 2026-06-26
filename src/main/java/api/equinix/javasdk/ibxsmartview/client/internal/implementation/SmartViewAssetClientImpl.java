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

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.SmartViewAssetClient;
import api.equinix.javasdk.ibxsmartview.model.SmartViewAsset;
import api.equinix.javasdk.ibxsmartview.model.json.AssetDetailJson;
import api.equinix.javasdk.ibxsmartview.model.json.SmartViewAssetJson;
import api.equinix.javasdk.ibxsmartview.model.json.TagPointDataJson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SmartViewAssetClientImpl extends ResourceClientBase<SmartViewAsset, SmartViewAssetJson> implements SmartViewAssetClient<SmartViewAsset> {

    public SmartViewAssetClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "Assets", SmartViewAssetJson.class);
    }

    @Override
    protected SmartViewAsset wrap(SmartViewAssetJson json) {
        return json;
    }

    public Page<SmartViewAsset, SmartViewAssetJson> list(String accountNo, String ibx, String classification, List<String> cages) {
        Map<String, List<String>> qParams = new LinkedHashMap<>();
        qParams.put("accountNo", List.of(accountNo));
        qParams.put("ibx", List.of(ibx));
        qParams.put("classification", List.of(classification));
        if (cages != null && !cages.isEmpty()) {
            qParams.put("cages", cages);
        }
        return listPage("ListAssets", qParams);
    }

    public AssetDetailJson getAssetDetails(String accountNo, String ibx, String classification, String assetId) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx),
                "classification", List.of(classification),
                "assetId", List.of(assetId)
        );
        return getAs("GetAssetDetails", Map.of(), qParams, AssetDetailJson.class);
    }

    public List<AssetDetailJson> getMultipleAssetDetails(Object requestBody) {
        return postForType("PostAssetDetails", requestBody, AssetDetailJson.getListTypeRef());
    }

    public Page<SmartViewAsset, SmartViewAssetJson> search(String accountNo, String ibx, String searchString) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx),
                "searchString", List.of(searchString)
        );
        return listPage("SearchAssets", qParams);
    }

    public TagPointDataJson getAffectedAssets(String accountNo, String ibx) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx)
        );
        return getAs("GetAffectedAssets", Map.of(), qParams, TagPointDataJson.class);
    }

    public TagPointDataJson getCurrentTagPoint(String accountNo, String ibx) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx)
        );
        return getAs("GetCurrentTagPoint", Map.of(), qParams, TagPointDataJson.class);
    }

    public List<TagPointDataJson> getMultipleCurrentTagPoints(Object requestBody) {
        return postForType("PostCurrentTagPoints", requestBody, TagPointDataJson.getListTypeRef());
    }
}
