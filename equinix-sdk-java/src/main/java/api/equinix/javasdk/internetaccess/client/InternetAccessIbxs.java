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
import api.equinix.javasdk.internetaccess.enums.ConnectionType;
import api.equinix.javasdk.internetaccess.model.Ibx;

/**
 * Client interface for the Equinix Internet Access (EIA) v2 product-availability lookup
 * ({@code GET /internetAccess/v2/ibxs}) — the IBX data centers where Equinix Internet Access is
 * available for a given service connection type.
 */
public interface InternetAccessIbxs {

    /**
     * Returns the IBXs where Equinix Internet Access is available for the given connection type.
     *
     * @param connectionType the service connection type ({@code IA_C} physical or {@code IA_VC} virtual)
     * @return a paginated list of matching IBXs
     */
    PaginatedList<Ibx> availability(ConnectionType connectionType);

    /**
     * Returns the IBXs where Equinix Internet Access is available, narrowed by connection access
     * point type and required asset type.
     *
     * @param connectionType the service connection type ({@code IA_C} physical or {@code IA_VC} virtual)
     * @param accessPointType the connection A-side access point type (e.g. {@code COLO}, {@code VD},
     *                        {@code PORT}), or {@code null} for the default ({@code COLO})
     * @param assetType the type of asset the user must have in the IBX (e.g. {@code CABINET}),
     *                  or {@code null} for no asset constraint
     * @return a paginated list of matching IBXs
     */
    PaginatedList<Ibx> availability(ConnectionType connectionType, String accessPointType, String assetType);

    /**
     * Returns the detail for a single IBX where Equinix Internet Access is available, via the v1
     * single-IBX get ({@code GET /internetAccess/v1/ibxs/{ibx}}).
     *
     * @param ibx the IBX data center code (e.g. {@code WA1})
     * @return the matching IBX
     */
    Ibx getByCode(String ibx);

    /**
     * Returns the detail for a single IBX where Equinix Internet Access is available, via the v1
     * single-IBX get ({@code GET /internetAccess/v1/ibxs/{ibx}}), narrowed by service connection
     * type and connection access point type.
     *
     * @param ibx the IBX data center code (e.g. {@code WA1})
     * @param connectionType the service connection type ({@code IA_C} physical or {@code IA_VC}
     *                       virtual), or {@code null} for no constraint
     * @param accessPointType the connection A-side access point type (e.g. {@code COLO},
     *                        {@code VD}), or {@code null} for no constraint
     * @return the matching IBX
     */
    Ibx getByCode(String ibx, ConnectionType connectionType, String accessPointType);
}
