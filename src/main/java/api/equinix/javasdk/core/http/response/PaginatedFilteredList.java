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
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import lombok.Getter;
import lombok.experimental.Delegate;

import java.util.ArrayList;
import java.util.List;

/**
 * A paginated, filtered list of resources returned by Equinix API search operations.
 *
 * <p>Similar to {@link PaginatedList} but used for POST-based search endpoints that accept
 * filter and sort criteria in the request body. Returned by {@code search()} methods
 * on resource clients such as {@code fabric.connections().search()}.</p>
 *
 * <p>Like {@link PaginatedList}, it implements {@link List} by composition (delegating to an
 * internal {@code List}) rather than extending {@link ArrayList}, and provides the same pagination
 * capabilities: check for additional pages, load the next page, or eagerly load all pages.</p>
 *
 * @param <T> the type of resource in the list
 * @author ianjones
 * @version $Id: $Id
 * @see PaginatedList
 * @see Pagination
 */
@Getter
public class PaginatedFilteredList<T> implements List<T> {

    @Delegate
    private final List<T> items = new ArrayList<>();

    private PageablePost<T> pageableClient;
    private EquinixRequest<T> equinixRequest;
    private EquinixResponse<T> equinixResponse;
    private Pagination pagination;

    /**
     * No-arg constructor used when collecting mapped items (e.g. {@code Collectors.toCollection}).
     * Pagination metadata is attached afterwards via the full constructor.
     */
    public PaginatedFilteredList() {
    }

    /**
     * <p>Constructor for PaginatedFilteredList.</p>
     *
     * @param initialItems the items for the current page.
     * @param pageableClient a {@link PageablePost} object.
     * @param equinixRequest a {@link EquinixRequest} object.
     * @param equinixResponse a {@link EquinixResponse} object.
     * @param pagination a {@link Pagination} object.
     */
    public PaginatedFilteredList(List<T> initialItems, PageablePost<T> pageableClient,
                                 EquinixRequest<T> equinixRequest, EquinixResponse<T> equinixResponse, Pagination pagination) {

        this.items.addAll(initialItems);
        this.pageableClient = pageableClient;
        this.equinixRequest = equinixRequest;
        this.equinixResponse = equinixResponse;
        this.pagination = pagination;
    }

    private PaginatedFilteredList<T> fetchNextPage() {
        ((FilteredSortedPaginatedPost<?, ?>)((PaginatedPostRequest<T>)equinixRequest).getObjectToSerialize()).getPagination().nextPage();
        return (PaginatedFilteredList<T>) this.pageableClient.nextPage((PaginatedPostRequest<T>)equinixRequest);
    }

    private void loadNextPage() {
        PaginatedFilteredList<T> primaryObjectList = fetchNextPage();
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
