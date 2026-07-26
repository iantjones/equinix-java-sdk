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

package com.eqixiac.equinix.customerportal.client;

import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.model.Asset;
import com.eqixiac.equinix.customerportal.model.json.creators.AssetSearchRequest;

/**
 * Client interface for accessing asset information in the Equinix Customer Portal.
 *
 * <p>Backed by the Assets v1 API at {@code /v1/assets}. Assets are discovered by posting a filter
 * to {@link #search(AssetSearchRequest)} and retrieved individually by id.</p>
 */
public interface Assets {

    /**
     * Searches assets.
     *
     * <p>Maps to {@code POST /v1/assets/search} ({@code searchAssets}).</p>
     *
     * @param request the asset search request body
     * @return a paginated list of matching assets
     */
    PaginatedList<Asset> search(AssetSearchRequest request);

    /**
     * Retrieves a specific asset by its identifier.
     *
     * <p>Maps to {@code GET /v1/assets/{assetId}} ({@code getAssetById}).</p>
     *
     * @param assetId the identifier of the asset
     * @return the matching asset
     */
    Asset getByUuid(String assetId);
}
