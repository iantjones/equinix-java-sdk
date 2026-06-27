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

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.model.AssetDetail;
import api.equinix.javasdk.ibxsmartview.model.SmartViewAsset;
import api.equinix.javasdk.ibxsmartview.model.TagPointData;

import java.util.List;

/**
 * Client interface for managing IBX SmartView monitored assets. Provides methods to list,
 * search, and retrieve details for infrastructure assets such as cooling units and power
 * distribution equipment, as well as their associated tag point data.
 */
public interface SmartViewAssets {

    /**
     * Lists monitored assets for the specified IBX, optionally filtered by classification and cages.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param classification the asset classification to filter by
     * @param cages the list of cage identifiers to filter by
     * @return a paginated list of SmartView assets
     */
    PaginatedList<SmartViewAsset> list(String accountNo, String ibx, String classification, List<String> cages);

    /**
     * Retrieves detailed information for a specific monitored asset.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param classification the asset classification
     * @param assetId the unique identifier of the asset
     * @return the asset detail
     */
    AssetDetail getAssetDetails(String accountNo, String ibx, String classification, String assetId);

    /**
     * Retrieves details for multiple assets using a structured request body.
     *
     * @param requestBody the request body specifying the assets to query
     * @return a list of asset details
     */
    List<AssetDetail> getMultipleAssetDetails(Object requestBody);

    /**
     * Searches for monitored assets matching a search string within an IBX.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param searchString the search term to match against asset names or attributes
     * @return a paginated list of matching SmartView assets
     */
    PaginatedList<SmartViewAsset> search(String accountNo, String ibx, String searchString);

    /**
     * Retrieves tag point data for assets affected by active alerts in the specified IBX.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @return the tag point data for affected assets
     */
    TagPointData getAffectedAssets(String accountNo, String ibx);

    /**
     * Retrieves the current tag point data for a specified IBX.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @return the current tag point data
     */
    TagPointData getCurrentTagPoint(String accountNo, String ibx);

    /**
     * Retrieves current tag point data for multiple assets using a structured request body.
     *
     * @param requestBody the request body specifying the assets to query
     * @return a list of tag point data entries
     */
    List<TagPointData> getMultipleCurrentTagPoints(Object requestBody);
}
