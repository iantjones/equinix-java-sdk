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

package api.equinix.javasdk.core.http.response;

import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

/**
 * A paginated list of resources returned by Equinix API list operations.
 *
 * <p>Extends {@link ArrayList} with pagination metadata and automatic page loading.
 * All SDK list operations return this type (or {@code PaginatedFilteredList} for search operations).
 * Provides methods to check for additional pages, load the next page, or eagerly load all pages.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * PaginatedList<Port> ports = fabric.ports().list();
 *
 * // Access pagination metadata
 * Pagination pagination = ports.getPagination();
 * int total = pagination.getTotal();
 * boolean isLast = pagination.getIsLastPage();
 *
 * // Load additional pages
 * while (ports.hasNextPage()) {
 *     ports.next();
 * }
 *
 * // Or load all pages at once
 * ports.loadAll();
 * }</pre>
 *
 * @param <T> the type of resource in the list
 * @author ianjones
 * @version $Id: $Id
 * @see Pagination
 */
@Getter
@Setter
@NoArgsConstructor
public class PaginatedList<T> extends ArrayList<T> {

    private Pageable<T> pageableClient;
    private EquinixRequest<T> equinixRequest;
    private EquinixResponse<T> equinixResponse;
    private Pagination pagination;

    /**
     * <p>Constructor for PaginatedList.</p>
     *
     * @param initialItems a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     * @param pageableClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param equinixResponse a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @param pagination a {@link api.equinix.javasdk.core.http.response.Pagination} object.
     */
    public PaginatedList(PaginatedList<T> initialItems, Pageable<T> pageableClient,
                         EquinixRequest<T> equinixRequest, EquinixResponse<T> equinixResponse, Pagination pagination) {

        this.addAll(initialItems);
        this.pageableClient = pageableClient;
        this.equinixRequest = equinixRequest;
        this.equinixResponse = equinixResponse;
        this.pagination = pagination;
    }

    private PaginatedList<T> fetchNextPage() {
        ((PaginatedRequest<T>)equinixRequest).nextPage();
        return (PaginatedList<T>) this.pageableClient.nextPage((PaginatedRequest<T>)equinixRequest);
    }

    private void loadNextPage() {
        PaginatedList<T> primaryObjectList = fetchNextPage();
        this.addAll(primaryObjectList);
        this.equinixRequest = primaryObjectList.getEquinixRequest();
        this.equinixResponse = primaryObjectList.getEquinixResponse();
        this.pagination = primaryObjectList.getPagination();
    }

    /**
     * Returns {@code true} if there are more pages of results available from the API.
     *
     * @return {@code true} if a next page exists; {@code false} if this is the last page
     */
    public boolean hasNextPage() {
        return !this.pagination.getIsLastPage();
    }

    /**
     * Loads the next page of results from the API and appends them to this list.
     * Does nothing if there are no more pages. After calling this method,
     * the pagination metadata is updated to reflect the newly loaded page.
     */
    public void next() {
        if (hasNextPage()) {
            loadNextPage();
        }
    }

    /**
     * Eagerly loads all remaining pages from the API, appending all results to this list.
     * After calling this method, the list contains all available resources and
     * {@link #hasNextPage()} will return {@code false}.
     *
     * @return this list, now containing all resources across all pages
     */
    public PaginatedList<T> loadAll() {
        while (hasNextPage()) {
            loadNextPage();
        }

        return this;
    }
}
