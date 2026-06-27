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
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A paginated, filtered list of resources returned by Equinix API search operations.
 *
 * <p>Similar to {@link PaginatedList} but used for POST-based search endpoints that accept
 * filter and sort criteria in the request body. Returned by {@code search()} methods
 * on resource clients such as {@code fabric.connections().search()}.</p>
 *
 * <p>Like {@link PaginatedList}, it is an {@link Iterable} view of the loaded results plus
 * pagination metadata — not a {@link java.util.List}. Iterate it directly, call {@link #stream()},
 * or take a snapshot with {@link #toList()}.</p>
 *
 * @param <T> the type of resource in the list
 * @author ianjones
 * @version $Id: $Id
 * @see PaginatedList
 * @see Pagination
 */
@Getter
public class PaginatedFilteredList<T> implements Iterable<T> {

    @Getter(AccessLevel.NONE)
    private final List<T> items = new ArrayList<>();

    private PageablePost<T> pageableClient;
    private EquinixRequest<T> equinixRequest;
    private EquinixResponse<T> equinixResponse;
    private Pagination pagination;

    /**
     * No-arg constructor; pagination metadata and items are attached afterwards.
     */
    public PaginatedFilteredList() {
    }

    /**
     * <p>Constructor for PaginatedFilteredList.</p>
     *
     * @param initialItems the items for the current page (any iterable; copied in).
     * @param pageableClient a {@link PageablePost} object.
     * @param equinixRequest a {@link EquinixRequest} object.
     * @param equinixResponse a {@link EquinixResponse} object.
     * @param pagination a {@link Pagination} object.
     */
    public PaginatedFilteredList(Iterable<? extends T> initialItems, PageablePost<T> pageableClient,
                                 EquinixRequest<T> equinixRequest, EquinixResponse<T> equinixResponse, Pagination pagination) {

        initialItems.forEach(this.items::add);
        this.pageableClient = pageableClient;
        this.equinixRequest = equinixRequest;
        this.equinixResponse = equinixResponse;
        this.pagination = pagination;
    }

    /** {@inheritDoc} Iterates the currently-loaded items. */
    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    /**
     * Streams the currently-loaded items.
     *
     * @return a sequential {@link Stream} over the loaded items
     */
    public Stream<T> stream() {
        return items.stream();
    }

    /**
     * The number of items currently loaded (on this and any already-loaded pages).
     *
     * @return the loaded item count
     */
    public int size() {
        return items.size();
    }

    /**
     * @return {@code true} if no items are loaded
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns the loaded item at the given index.
     *
     * @param index the zero-based index
     * @return the item at {@code index}
     */
    public T get(int index) {
        return items.get(index);
    }

    /**
     * Returns an unmodifiable snapshot of the currently-loaded items as a {@link List}.
     *
     * @return an unmodifiable copy of the loaded items
     */
    public List<T> toList() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    private PaginatedFilteredList<T> fetchNextPage() {
        ((FilteredSortedPaginatedPost<?, ?>)((PaginatedPostRequest<T>)equinixRequest).getObjectToSerialize()).getPagination().nextPage();
        return (PaginatedFilteredList<T>) this.pageableClient.nextPage((PaginatedPostRequest<T>)equinixRequest);
    }

    private void loadNextPage() {
        PaginatedFilteredList<T> primaryObjectList = fetchNextPage();
        this.items.addAll(primaryObjectList.items);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaginatedFilteredList)) {
            return false;
        }
        return items.equals(((PaginatedFilteredList<?>) o).items);
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
