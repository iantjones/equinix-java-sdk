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
import api.equinix.javasdk.customerportal.model.CrossConnect;
import api.equinix.javasdk.customerportal.model.json.creators.CrossConnectOperator;

/**
 * Client interface for managing cross-connect resources in the Equinix Customer Portal.
 * Provides operations to list, retrieve, and create cross-connect orders between
 * cages or cabinets within an IBX data center.
 */
public interface CrossConnects {

    /**
     * Lists all cross-connects for the current account.
     *
     * @return a paginated list of cross-connects
     */
    PaginatedList<CrossConnect> list();

    /**
     * Retrieves a specific cross-connect by its unique identifier.
     *
     * @param uuid the unique identifier of the cross-connect
     * @return the matching cross-connect
     */
    CrossConnect getByUuid(String uuid);

    /**
     * Returns a builder for defining a new cross-connect order.
     *
     * @return a new CrossConnectBuilder instance
     */
    CrossConnectOperator.CrossConnectBuilder define();
}
