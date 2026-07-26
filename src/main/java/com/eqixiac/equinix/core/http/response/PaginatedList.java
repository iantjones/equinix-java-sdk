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

package com.eqixiac.equinix.core.http.response;

import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.PaginatedPostRequest;
import com.eqixiac.equinix.core.http.request.PaginatedRequest;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A paginated list of resources returned by Equinix API list operations.
 *
 * <p>This is an {@link Iterable} view of the loaded results plus pagination metadata and automatic
 * page loading — it is deliberately <em>not</em> a {@link java.util.List}. A server response is not a
 * mutable collection: modeling it as one (the old {@code extends ArrayList}) exposed {@code add}/
 * {@code remove}/{@code clear}/{@code ensureCapacity}/… that make no sense for a page of API data.
 * Instead it follows the idiom used by the major cloud SDKs (AWS SDK v2, Google Cloud, Stripe):
 * iterate it directly, call {@link #stream()}, or take a snapshot with {@link #toList()}.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * PaginatedList<Port> ports = fabric.ports().list();
 *
 * // Iterate or stream the current page
 * for (Port p : ports) { ... }
 * ports.stream().filter(...).forEach(...);
 * Port first = ports.get(0);
 *
 * // Pagination metadata
 * Pagination pagination = ports.getPagination();
 *
 * // Load additional pages
 * while (ports.hasNextPage()) { ports.next(); }
 *
 * // Or load all pages at once, then take a List snapshot
 * List<Port> all = ports.loadAll().toList();
 * }</pre>
 *
 * @param <T> the type of resource in the list
 * @author ianjones
 * @see Pagination
 */
public class PaginatedList<T> implements Iterable<T> {

    private final List<T> items = new ArrayList<>();

    // Internal paging machinery — deliberately not exposed on the public list type. The
    // request/response element types are unbounded: the carried request may be typed over
    // the public model or the resource's JSON model (see Page), and paging never needs it.
    @Getter(AccessLevel.PACKAGE)
    private Pageable<T> pageableClient;
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
    public PaginatedList(Iterable<? extends T> initialItems, Pageable<T> pageableClient,
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
    private PaginatedList<T> fetchNextPage() {
        // One shared advance path with PaginatedFilteredList: seek the request's pagination —
        // whether it lives in the POST body or in offset/limit query parameters — from the
        // SERVER-reported pagination of the page we hold (clamp-safe), or fail fast with a clear
        // unsupported-pagination error (previously a hard cast here threw ClassCastException on
        // page 2 of POST searches wrapped in a PaginatedList).
        PagingDispatcher.advance(equinixRequest, pagination);
        try {
            if (equinixRequest instanceof PaginatedPostRequest) {
                if (!(pageableClient instanceof PageablePost)) {
                    throw new EquinixClientException("Cannot fetch the next page for search endpoint '"
                            + equinixRequest.getServiceEndpoint() + "': the paging client "
                            + (pageableClient != null ? pageableClient.getClass().getName() : "<none>")
                            + " cannot re-invoke POST searches (it does not implement PageablePost).");
                }
                PaginatedFilteredList<T> nextPage =
                        ((PageablePost<T>) pageableClient).nextPage((PaginatedPostRequest<T>) equinixRequest);
                return new PaginatedList<>(nextPage, this.pageableClient, nextPage.getEquinixRequest(),
                        nextPage.getEquinixResponse(), nextPage.getPagination());
            }
            return (PaginatedList<T>) this.pageableClient.nextPage((PaginatedRequest<T>) equinixRequest);
        }
        catch (RuntimeException e) {
            // Seek the shared request's pagination back so a caller that catches the failure and
            // retries next() re-fetches the SAME page instead of silently skipping one.
            PagingDispatcher.rollback(equinixRequest, pagination);
            throw e;
        }
    }

    private void loadNextPage() {
        PaginatedList<T> primaryObjectList = fetchNextPage();
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

    // No equals()/hashCode(): a live, page-growing view has identity semantics (its loaded-item
    // set mutates on next()/loadAll(), which would silently break hash-keyed usage).

    @Override
    public String toString() {
        return items.toString();
    }
}
