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

import com.eqixiac.equinix.core.http.request.Pagination;
import com.eqixiac.equinix.core.internal.Constants;
import lombok.Getter;

/**
 * Generic request body for paginated, filtered, sorted POST search endpoints.
 *
 * <p>Holds a domain-specific filter and sort payload plus core pagination state.
 * Parameterized over the filter ({@code F}) and sort ({@code S}) types so that core
 * carries no dependency on any domain's concrete filter/sort model (for example Fabric's
 * {@code FilterPropertyList}/{@code SortPropertyList}).</p>
 *
 * @param <F> the domain filter payload type
 * @param <S> the domain sort payload type
 */
@Getter
public class FilteredSortedPaginatedPost<F, S> implements PaginatedPostBody {

    private F filter;

    private S sort;

    private Pagination pagination = new Pagination(Constants.PAGE_OFFSET, Constants.PAGE_LIMIT);

    public FilteredSortedPaginatedPost(F filter, S sort) {
        this.filter = filter;
        this.sort = sort;
    }

    @Override
    public void seekPage(long offset, long limit) {
        if (this.pagination == null) {
            this.pagination = new Pagination(Constants.PAGE_OFFSET, Constants.PAGE_LIMIT);
        }
        this.pagination.setOffset(Math.toIntExact(offset));
        if (limit > 0) {
            this.pagination.setLimit(Math.toIntExact(limit));
        }
    }
}
