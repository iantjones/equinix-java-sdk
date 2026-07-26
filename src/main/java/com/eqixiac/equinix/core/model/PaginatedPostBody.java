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

package com.eqixiac.equinix.core.model;

/**
 * A POST-search request body that carries client-side pagination state, so the shared paging
 * pipeline ({@code PaginatedList}/{@code PaginatedFilteredList} {@code next()}/{@code loadAll()})
 * can re-point the body at the next page between fetches without knowing the body's concrete type
 * or how it represents pagination.
 *
 * <p>The contract is deliberately <em>behavioral</em> rather than representational: bodies with
 * heterogeneous pagination models — the core {@code Pagination {offset, limit}} object
 * ({@link FilteredSortedPaginatedPost}, {@link FilteredPaginatedPost}), IBX SmartView's
 * {@code SearchPagination {offset, limit}}, or flat top-level {@code offset}/{@code limit} members
 * (billing accounts search) — all implement it by mapping the requested window onto their own
 * serialized shape.</p>
 *
 * <p>The paging pipeline always seeks from the <em>server-reported</em> pagination of the page it
 * holds: the next page starts at {@code serverOffset + serverLimit} with the server's (possibly
 * clamped) limit, and a failed fetch seeks back to {@code serverOffset} so a retried {@code next()}
 * re-requests the same page. Domain search bodies must implement this interface if their results
 * are exposed through a pageable list; a body that does not implement it fails paging fast with a
 * clear {@code EquinixClientException} instead of a {@code ClassCastException}.</p>
 */
public interface PaginatedPostBody {

    /**
     * Points this body's serialized pagination state at the page window starting at
     * {@code offset}, sized {@code limit}. Called by the paging pipeline both to advance to the
     * next page and to roll back to the current page after a failed fetch.
     *
     * @param offset the zero-based index of the first record of the requested page
     * @param limit the page size; implementations should ignore non-positive values
     *              (keeping their current/default limit) rather than sending an invalid one
     */
    void seekPage(long offset, long limit);
}
