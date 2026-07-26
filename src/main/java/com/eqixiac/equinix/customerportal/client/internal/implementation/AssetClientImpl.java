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

package com.eqixiac.equinix.customerportal.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.customerportal.client.implementation.CustomerPortalConfigImpl;
import com.eqixiac.equinix.customerportal.client.internal.AssetClient;
import com.eqixiac.equinix.customerportal.model.Asset;
import com.eqixiac.equinix.customerportal.model.json.AssetJson;
import com.eqixiac.equinix.customerportal.model.json.creators.AssetSearchRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssetClientImpl extends ResourceClientBase<Asset, AssetJson> implements AssetClient<Asset> {

    public AssetClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Assets", AssetJson.class);
    }

    @Override
    protected Asset wrap(AssetJson json) {
        return json;
    }

    public Page<AssetJson> search(AssetSearchRequest request) {
        // assetsv1 paginates the search via offset/limit QUERY PARAMETERS (the body carries only
        // the filter), so this builds a query-paginated request: the shared paging pipeline
        // advances the query offset between pages while re-sending the same filter body.
        Map<String, List<String>> queryParams = buildQueryParams(request);
        return searchPageQueryPaginated("SearchAssets", queryParams.isEmpty() ? null : queryParams, request);
    }

    public AssetJson getByUuid(String assetId) {
        return getOne("GetAsset", Map.of("assetId", assetId));
    }

    private Map<String, List<String>> buildQueryParams(AssetSearchRequest request) {
        Map<String, List<String>> queryParams = new java.util.HashMap<>();
        if (request.getQ() != null) {
            queryParams.put("q", List.of(request.getQ()));
        }
        if (request.getExactMatch() != null) {
            queryParams.put("exactMatch", List.of(String.valueOf(request.getExactMatch())));
        }
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            queryParams.put("sorts", new ArrayList<>(request.getSorts()));
        }
        return queryParams;
    }
}
