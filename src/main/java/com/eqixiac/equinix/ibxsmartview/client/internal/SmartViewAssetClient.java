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

package com.eqixiac.equinix.ibxsmartview.client.internal;

import com.eqixiac.equinix.ibxsmartview.model.json.AssetDetailsGetResponseJson;
import com.eqixiac.equinix.ibxsmartview.model.json.AssetDetailsResponseJson;
import com.eqixiac.equinix.ibxsmartview.model.json.AssetsJson;
import com.eqixiac.equinix.ibxsmartview.model.json.AssetsListJson;
import com.eqixiac.equinix.ibxsmartview.model.json.HierarchyNodeForAssetAPIJson;
import com.eqixiac.equinix.ibxsmartview.model.json.TagPointDataJson;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.AssetDetailsRequest;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.CurrentTagPointRequest;

import java.util.List;

public interface SmartViewAssetClient {

    AssetsListJson list(String accountNo, String ibx, String classification, List<String> cages);

    AssetDetailsGetResponseJson getAssetDetails(String accountNo, String ibx, String classification, String assetId);

    AssetDetailsResponseJson getMultipleAssetDetails(AssetDetailsRequest requestBody);

    AssetsJson search(String accountNo, String ibx, String searchString);

    HierarchyNodeForAssetAPIJson getAffectedAssets(String accountNo, String ibx, String assetId, String classification);

    TagPointDataJson getCurrentTagPoint(String accountNo, String ibx, String tagId);

    TagPointDataJson getMultipleCurrentTagPoints(CurrentTagPointRequest requestBody);
}
