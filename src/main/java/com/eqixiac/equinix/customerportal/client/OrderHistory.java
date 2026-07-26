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

import com.eqixiac.equinix.customerportal.model.OrderHistoryItem;
import com.eqixiac.equinix.customerportal.model.PermissibleLocation;
import com.eqixiac.equinix.customerportal.model.json.creators.OrderHistorySearchRequest;

import java.util.List;

/**
 * Client interface for searching order history in the Equinix Customer Portal.
 *
 * <p>Backed by the Order History v1 API at {@code /v1/retrieve-orders}. Orders are discovered by
 * posting a filter to {@link #search(OrderHistorySearchRequest)}; the IBX/cage locations the
 * current user may filter by are available via {@link #listLocations()}.</p>
 */
public interface OrderHistory {

    /**
     * Searches order history.
     *
     * <p>Maps to {@code POST /v1/retrieve-orders} ({@code POST_orders-history}).</p>
     *
     * @param request the search request body
     * @return the matching order history records
     */
    List<? extends OrderHistoryItem> search(OrderHistorySearchRequest request);

    /**
     * Lists the IBX/cage locations the current user may filter order history by.
     *
     * <p>Maps to {@code GET /v1/retrieve-orders/locations} ({@code GET_retrieve-orders-locations}).</p>
     *
     * @return the list of permissible locations
     */
    List<? extends PermissibleLocation> listLocations();
}
