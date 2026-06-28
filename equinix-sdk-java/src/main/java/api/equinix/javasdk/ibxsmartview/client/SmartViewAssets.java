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

package api.equinix.javasdk.ibxsmartview.client;

import api.equinix.javasdk.ibxsmartview.model.AssetDetail;
import api.equinix.javasdk.ibxsmartview.model.AssetDetailsResponse;
import api.equinix.javasdk.ibxsmartview.model.Assets;
import api.equinix.javasdk.ibxsmartview.model.AssetsList;
import api.equinix.javasdk.ibxsmartview.model.HierarchyNodeForAssetAPI;
import api.equinix.javasdk.ibxsmartview.model.TagPointData;
import api.equinix.javasdk.ibxsmartview.model.json.creators.AssetDetailsRequest;
import api.equinix.javasdk.ibxsmartview.model.json.creators.CurrentTagPointRequest;

import java.util.List;

/**
 * Client interface for the IBX SmartView Assets API. Provides methods to list assets as a
 * category/template/asset hierarchy, search assets, retrieve asset details and their tag points,
 * and read current tag-point values.
 */
public interface SmartViewAssets {

    /**
     * Lists monitored assets for the specified account/IBX as a category/template/asset hierarchy.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param classification the asset classification to filter by ({@code Electrical} or {@code Mechanical})
     * @param cages the list of cage unique space ids to filter by, or {@code null} for all
     * @return the assets list hierarchy
     */
    AssetsList list(String accountNo, String ibx, String classification, List<String> cages);

    /**
     * Retrieves detailed information (including tag points) for a single monitored asset.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param classification the asset classification ({@code Electrical} or {@code Mechanical})
     * @param assetId the unique identifier of the asset
     * @return the asset detail envelope
     */
    AssetDetail getAssetDetails(String accountNo, String ibx, String classification, String assetId);

    /**
     * Retrieves details for multiple assets using a typed request body.
     *
     * @param requestBody the typed request body specifying the account, IBX, classification and asset ids
     * @return the multi-asset details response
     */
    AssetDetailsResponse getMultipleAssetDetails(AssetDetailsRequest requestBody);

    /**
     * Searches for monitored assets matching a wildcard search string within an account/IBX.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param searchString the search term to match against asset identifiers
     * @return the asset search results
     */
    Assets search(String accountNo, String ibx, String searchString);

    /**
     * Retrieves the hierarchy of customer assets affected by a given asset.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param assetId the unique identifier of the asset
     * @param classification the asset classification ({@code Electrical} or {@code Mechanical})
     * @return the affected-assets hierarchy
     */
    HierarchyNodeForAssetAPI getAffectedAssets(String accountNo, String ibx, String assetId, String classification);

    /**
     * Retrieves the most recent tag-point data for a given tag identifier.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param tagId the unique identifier of the tag point
     * @return the current tag-point data
     */
    TagPointData getCurrentTagPoint(String accountNo, String ibx, String tagId);

    /**
     * Retrieves current tag-point data for multiple tag identifiers using a typed request body.
     *
     * @param requestBody the typed request body specifying the account, tag ids and IBX
     * @return the current tag-point data
     */
    TagPointData getMultipleCurrentTagPoints(CurrentTagPointRequest requestBody);
}
