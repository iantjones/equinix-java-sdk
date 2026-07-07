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

package api.equinix.javasdk.core.model;

import api.equinix.javasdk.core.http.request.Pagination;

/**
 * A POST-search request body that carries client-side pagination state, so the shared paging
 * pipeline ({@code PaginatedFilteredList.next()}/{@code loadAll()}) can advance the body's
 * {@code offset} between page fetches without knowing the body's concrete type.
 *
 * <p>Implemented by the core body shapes ({@link FilteredSortedPaginatedPost},
 * {@link FilteredPaginatedPost}); domain-specific search bodies must implement it too if their
 * results are exposed through a pageable {@code PaginatedFilteredList}.</p>
 */
public interface PaginatedPostBody {

    /**
     * The mutable pagination state serialized into the search body and advanced between pages.
     *
     * @return the body's pagination state; never {@code null} for a pageable body
     */
    Pagination getPagination();
}
