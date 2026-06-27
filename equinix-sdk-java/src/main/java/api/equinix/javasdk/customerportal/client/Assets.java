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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.Asset;

/**
 * Client interface for accessing asset information in the Equinix Customer Portal.
 * Provides read-only operations to list and retrieve details about physical assets
 * such as cages, cabinets, and power circuits within IBX data centers.
 */
public interface Assets {

    /**
     * Lists all assets for the current account.
     *
     * @return a paginated list of assets
     */
    PaginatedList<Asset> list();

    /**
     * Retrieves a specific asset by its unique identifier.
     *
     * @param uuid the unique identifier of the asset
     * @return the matching asset
     */
    Asset getByUuid(String uuid);
}
