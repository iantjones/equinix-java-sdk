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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.client.Assets;
import com.eqixiac.equinix.customerportal.client.internal.AssetClient;
import com.eqixiac.equinix.customerportal.model.Asset;
import com.eqixiac.equinix.customerportal.model.json.AssetJson;
import com.eqixiac.equinix.customerportal.model.json.creators.AssetSearchRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AssetsImpl implements Assets {

    private final AssetClient<Asset> serviceClient;

    private final CustomerPortal serviceManager;

    public PaginatedList<Asset> search(AssetSearchRequest request) {
        Page<AssetJson> responsePage = this.serviceClient.search(request);
        PaginatedList<Asset> assetList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(assetList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Asset getByUuid(String assetId) {
        return this.serviceClient.getByUuid(assetId);
    }
}
