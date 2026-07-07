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

import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedPostRequest;
import api.equinix.javasdk.core.model.PaginatedPostBody;
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
 * @see PaginatedList
 * @see Pagination
 */
public class PaginatedFilteredList<T> implements Iterable<T> {

    private final List<T> items = new ArrayList<>();

    // Internal paging machinery — deliberately not exposed on the public list type. The
    // request/response element types are unbounded: the carried request may be typed over
    // the public model or the resource's JSON model (see Page), and paging never needs it.
    @Getter(AccessLevel.PACKAGE)
    private PageablePost<T> pageableClient;
    @Getter(AccessLevel.PACKAGE)
    private EquinixRequest<?> equinixRequest;
    @Getter(AccessLevel.PACKAGE)
    private EquinixResponse<?> equinixResponse;

    @Getter
    private Pagination pagination;

    /**
     *
     * @param initialItems the items for the current page (any iterable; copied in).
     */
    public PaginatedFilteredList(Iterable<? extends T> initialItems, PageablePost<T> pageableClient,
                                 EquinixRequest<?> equinixRequest, EquinixResponse<?> equinixResponse, Pagination pagination) {

        initialItems.forEach(this.items::add);
        this.pageableClient = pageableClient;
        this.equinixRequest = equinixRequest;
        this.equinixResponse = equinixResponse;
        this.pagination = pagination;
    }

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

    @SuppressWarnings("unchecked") // the request's element type is erased and never used by paging
    private PaginatedFilteredList<T> fetchNextPage() {
        PaginatedPostRequest<T> postRequest = (PaginatedPostRequest<T>) equinixRequest;
        Object body = postRequest.getSearchBody();

        // Dispatch on the PaginatedPostBody contract instead of hard-casting to a concrete body
        // type: any search body that carries pagination state can page, and a body that cannot
        // page fails fast with a clear message instead of a bare ClassCastException/NPE.
        if (!(body instanceof PaginatedPostBody paginatedBody) || paginatedBody.getPagination() == null) {
            throw new EquinixClientException("Cannot fetch the next page for search endpoint '"
                    + postRequest.getServiceEndpoint() + "': the request body"
                    + (body != null ? " of type " + body.getClass().getName() : "")
                    + " does not carry pagination state. Search bodies must implement "
                    + PaginatedPostBody.class.getName() + " with a non-null pagination to be pageable.");
        }

        api.equinix.javasdk.core.http.request.Pagination bodyPagination = paginatedBody.getPagination();
        bodyPagination.nextPage();
        try {
            return (PaginatedFilteredList<T>) this.pageableClient.nextPage(postRequest);
        }
        catch (RuntimeException e) {
            // Roll the shared body's offset back so a caller that catches the failure and
            // retries next() re-fetches the SAME page instead of silently skipping one.
            bodyPagination.previousPage();
            throw e;
        }
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
        return this.pagination != null && !this.pagination.isLastPage();
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

    // No equals()/hashCode(): a live, page-growing view has identity semantics (its loaded-item
    // set mutates on next()/loadAll(), which would silently break hash-keyed usage).

    @Override
    public String toString() {
        return items.toString();
    }
}
