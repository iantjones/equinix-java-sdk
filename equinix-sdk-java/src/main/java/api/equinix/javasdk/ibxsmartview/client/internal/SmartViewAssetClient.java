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

package api.equinix.javasdk.ibxsmartview.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.ibxsmartview.model.SmartViewAsset;
import api.equinix.javasdk.ibxsmartview.model.json.AssetDetailJson;
import api.equinix.javasdk.ibxsmartview.model.json.SmartViewAssetJson;
import api.equinix.javasdk.ibxsmartview.model.json.TagPointDataJson;

import java.util.List;

public interface SmartViewAssetClient<T> extends Pageable<T> {

    Page<SmartViewAsset, SmartViewAssetJson> list(String accountNo, String ibx, String classification, List<String> cages);

    AssetDetailJson getAssetDetails(String accountNo, String ibx, String classification, String assetId);

    List<AssetDetailJson> getMultipleAssetDetails(Object requestBody);

    Page<SmartViewAsset, SmartViewAssetJson> search(String accountNo, String ibx, String searchString);

    TagPointDataJson getAffectedAssets(String accountNo, String ibx);

    TagPointDataJson getCurrentTagPoint(String accountNo, String ibx);

    List<TagPointDataJson> getMultipleCurrentTagPoints(Object requestBody);
}
