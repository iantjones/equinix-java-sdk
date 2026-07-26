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

package com.eqixiac.equinix.core.http.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Client-side pagination state serialized into POST-search request bodies and advanced by the
 * paging pipeline between page fetches. Fields are primitives so paging arithmetic can never
 * throw a null-unboxing {@code NullPointerException}.
 */
@Getter
@Setter
public class Pagination {

    private int offset;

    private int limit;

    /**
     * Creates pagination state for a POST-search body. The parameter order is pinned by this
     * explicit constructor — {@code offset} first, then {@code limit} — deliberately not
     * Lombok-generated, so a field reorder can never silently transpose the two {@code int}
     * arguments at call sites.
     *
     * @param offset the zero-based index of the first record of the requested page
     * @param limit the page size
     */
    public Pagination(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public void nextPage() {
        this.offset = this.offset + this.limit;
    }

    /**
     * Rolls the offset back one page (floored at zero). Used by the paging pipeline to restore
     * the pre-advance state when a page fetch fails, so a retried {@code next()} re-requests the
     * same page instead of silently skipping one.
     */
    public void previousPage() {
        this.offset = Math.max(0, this.offset - this.limit);
    }
}
