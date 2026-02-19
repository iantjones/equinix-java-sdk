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
import api.equinix.javasdk.core.http.request.PaginatedPostRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

/**
 * A paginated, filtered list of resources returned by Equinix API search operations.
 *
 * <p>Similar to {@link PaginatedList} but used for POST-based search endpoints that accept
 * filter and sort criteria in the request body. Returned by {@code search()} methods
 * on resource clients such as {@code fabric.connections().search()}.</p>
 *
 * <p>Provides the same pagination capabilities as {@link PaginatedList}: check for
 * additional pages, load the next page, or eagerly load all pages.</p>
 *
 * @param <T> the type of resource in the list
 * @author ianjones
 * @version $Id: $Id
 * @see PaginatedList
 * @see Pagination
 */
@Getter
@Setter
@NoArgsConstructor
public class PaginatedFilteredList<T> extends ArrayList<T> {

    private PageablePost<T> pageableClient;
    private EquinixRequest<T> equinixRequest;
    private EquinixResponse<T> equinixResponse;
    private Pagination pagination;

    /**
     * <p>Constructor for PaginatedList.</p>
     *
     * @param initialItems a {@link PaginatedFilteredList} object.
     * @param pageableClient a {@link Pageable} object.
     * @param equinixRequest a {@link EquinixRequest} object.
     * @param equinixResponse a {@link EquinixResponse} object.
     * @param pagination a {@link Pagination} object.
     */
    public PaginatedFilteredList(PaginatedFilteredList<T> initialItems, PageablePost<T> pageableClient,
                                 EquinixRequest<T> equinixRequest, EquinixResponse<T> equinixResponse, Pagination pagination) {

        this.addAll(initialItems);
        this.pageableClient = pageableClient;
        this.equinixRequest = equinixRequest;
        this.equinixResponse = equinixResponse;
        this.pagination = pagination;
    }

    private PaginatedFilteredList<T> fetchNextPage() {
        ((FilteredSortedPaginatedPost)((PaginatedPostRequest<T>)equinixRequest).getObjectToSerialize()).getPagination().nextPage();
        return (PaginatedFilteredList<T>) this.pageableClient.nextPage((PaginatedPostRequest<T>)equinixRequest);
    }

    private void loadNextPage() {
        PaginatedFilteredList<T> primaryObjectList = fetchNextPage();
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
     * Does nothing if there are no more pages.
     */
    public void next() {
        if (hasNextPage()) {
            loadNextPage();
        }
    }

    /**
     * Eagerly loads all remaining pages from the API, appending all results to this list.
     *
     * @return this list, now containing all resources across all pages
     */
    public PaginatedFilteredList<T> loadAll() {
        while (hasNextPage()) {
            loadNextPage();
        }

        return this;
    }
}
