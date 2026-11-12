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

package com.eqixiac.equinix.operations.client;

import com.eqixiac.equinix.operations.model.OrderHistoryItem;
import com.eqixiac.equinix.operations.model.OrderHistoryPage;
import com.eqixiac.equinix.operations.model.PermissibleLocation;
import com.eqixiac.equinix.operations.model.json.creators.OrderHistorySearchRequest;

import java.util.List;

/**
 * Client interface for searching order history.
 *
 * <p>Backed by the Order History v1 API at {@code /v1/retrieve-orders}. Orders are discovered by
 * posting a filter to {@link #search(OrderHistorySearchRequest)}; the IBX/cage locations the
 * current user may filter by are available via {@link #listLocations()}.</p>
 */
public interface OrderHistory {

    /**
     * Searches order history and returns the records of the requested page.
     *
     * <p>Maps to {@code POST /v1/retrieve-orders} ({@code POST_orders-history}). Same as
     * {@code searchPage(request).getContent()}; use {@link #searchPage(OrderHistorySearchRequest)}
     * for the paging metadata.</p>
     *
     * @param request the search request body
     * @return the matching order history records on the requested page
     */
    List<? extends OrderHistoryItem> search(OrderHistorySearchRequest request);

    /**
     * Searches order history and returns one page with its paging metadata.
     *
     * <p>Maps to {@code POST /v1/retrieve-orders} ({@code POST_orders-history}); the response
     * envelope is {@code {links, content, page{size, totalElements, totalPages, number}}}.</p>
     *
     * @param request the search request body
     * @return the page of records with {@code links} and {@code page} metadata
     */
    OrderHistoryPage searchPage(OrderHistorySearchRequest request);

    /**
     * Lists the IBX/cage locations the current user may filter order history by.
     *
     * <p>Maps to {@code GET /v1/retrieve-orders/locations} ({@code GET_retrieve-orders-locations}).</p>
     *
     * @return the list of permissible locations
     */
    List<? extends PermissibleLocation> listLocations();
}
