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

package com.eqixiac.equinix.internetaccess.client;

import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.internetaccess.model.PatchPanel;

/**
 * Client interface for the Equinix Internet Access (EIA) v1 product-availability lookup
 * ({@code GET /internetAccess/v1/patchPanels}) — the patch panels a customer has in a given cage
 * and cabinet.
 */
public interface InternetAccessPatchPanels {

    /**
     * Returns the patch panels the given account has in the given cage and cabinet.
     *
     * @param ibx the IBX data center code (e.g. {@code WA1})
     * @param accountNumber the customer billing account number
     * @param cageSpaceId the cage space identifier
     * @param cabinetSpaceId the cabinet space identifier
     * @return a paginated list of matching patch panels
     */
    PaginatedList<PatchPanel> list(String ibx, String accountNumber, String cageSpaceId, String cabinetSpaceId);

    /**
     * Returns the patch panels the given account has in the given cage and cabinet, narrowed by
     * media type name.
     *
     * @param ibx the IBX data center code (e.g. {@code WA1})
     * @param accountNumber the customer billing account number
     * @param cageSpaceId the cage space identifier
     * @param cabinetSpaceId the cabinet space identifier
     * @param mediaTypesName the media type name to filter by, or {@code null} for no constraint
     * @return a paginated list of matching patch panels
     */
    PaginatedList<PatchPanel> list(String ibx, String accountNumber, String cageSpaceId, String cabinetSpaceId,
                                   String mediaTypesName);
}
