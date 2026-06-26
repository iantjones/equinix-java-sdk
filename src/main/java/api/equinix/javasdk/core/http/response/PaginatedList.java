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
import lombok.experimental.Delegate;

import java.util.ArrayList;
import java.util.List;

/**
 * A paginated list of resources returned by Equinix API list operations.
 *
 * <p>Implements {@link List} by composition (delegating to an internal {@code List}) rather than
 * extending {@link ArrayList}, so it exposes the full {@code List} contract — {@code get(int)},
 * {@code size()}, iteration, {@code stream()}, {@code new ArrayList<>(list)} — without inheriting
 * {@code ArrayList}'s implementation surface. It adds pagination metadata and automatic page
 * loading on top. All SDK list operations return this type (or {@code PaginatedFilteredList} for
 * search operations); methods are provided to check for additional pages, load the next page, or
 * eagerly load all pages.</p>
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
public class PaginatedList<T> implements List<T> {

    @Delegate
    private final List<T> items = new ArrayList<>();

    private Pageable<T> pageableClient;
    private EquinixRequest<T> equinixRequest;
    private EquinixResponse<T> equinixResponse;
    private Pagination pagination;

    /**
     * No-arg constructor used when collecting mapped items (e.g. {@code Collectors.toCollection}).
     * Pagination metadata is attached afterwards via the full constructor.
     */
    public PaginatedList() {
    }

    /**
     * <p>Constructor for PaginatedList.</p>
     *
     * @param initialItems the items for the current page.
     * @param pageableClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param equinixResponse a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @param pagination a {@link api.equinix.javasdk.core.http.response.Pagination} object.
     */
    public PaginatedList(List<T> initialItems, Pageable<T> pageableClient,
                         EquinixRequest<T> equinixRequest, EquinixResponse<T> equinixResponse, Pagination pagination) {

        this.items.addAll(initialItems);
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
        this.items.addAll(primaryObjectList);
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

    // equals/hashCode/toString delegate to the backing list so the List contract (element-based
    // equality, per java.util.List) is honored — @Delegate does not generate Object methods.

    @Override
    public boolean equals(Object o) {
        return items.equals(o);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
