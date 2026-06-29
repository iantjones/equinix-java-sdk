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

package api.equinix.javasdk.internetaccess.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.Cabinet;

/**
 * Client interface for the Equinix Internet Access (EIA) v1 product-availability lookup
 * ({@code GET /internetAccess/v1/cabinets}) — the cabinets a customer has, optionally narrowed by
 * cage, IBX and account.
 */
public interface InternetAccessCabinets {

    /**
     * Returns the cabinets the user has access to.
     *
     * @return a paginated list of cabinets
     */
    PaginatedList<Cabinet> list();

    /**
     * Returns the cabinets the user has access to, narrowed by the given criteria.
     *
     * @param cageSpaceId the cage space identifier to filter by, or {@code null} for no constraint
     * @param ibx the IBX data center code to filter by, or {@code null} for no constraint
     * @param accountNumber the customer billing account number to filter by, or {@code null} for
     *                      no constraint
     * @return a paginated list of matching cabinets
     */
    PaginatedList<Cabinet> list(String cageSpaceId, String ibx, String accountNumber);
}
