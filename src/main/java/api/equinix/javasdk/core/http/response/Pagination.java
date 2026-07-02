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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Pagination metadata returned by Equinix API list operations.
 *
 * <p>Contains offset-based pagination information including the current offset,
 * page size (limit), total result count, and links to previous/next pages.
 * Provides convenience methods for computing page numbers and detecting
 * first/last pages.</p>
 *
 * @author ianjones
 * @see PaginatedList
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class Pagination {

    private Long offset;

    private Long limit;

    private Long total;

    private String previous;

    private String next;

    /**
     * Returns the zero-based page number computed from the current offset and limit.
     * Returns {@code 0} when offset/limit are absent (some endpoints omit pagination metadata,
     * e.g. on empty results) so the value is never an unboxing NPE or a divide-by-zero.
     *
     * @return the current page number (0-indexed)
     */
    public int getPageNumber() {
        if (offset == null || limit == null || limit == 0) {
            return 0;
        }
        return (int) (offset / limit);
    }

    /**
     * Returns the page size (number of results per page), which equals the limit, or {@code 0} when
     * the limit is absent.
     *
     * @return the number of results per page
     */
    public int getPageSize() {
        return (limit != null) ? limit.intValue() : 0;
    }

    /**
     * Returns {@code true} if the current page is the first page of results.
     *
     * @return {@code true} if this is page 0
     */
    public Boolean getIsFirstPage() {
        return getPageNumber() == 0;
    }

    /**
     * Returns {@code true} if the current page is the last page of results. When the total or limit
     * is absent (some endpoints omit {@code total} on empty results), the current page is treated as
     * the last so paging terminates gracefully rather than NPE-ing or looping.
     *
     * @return {@code true} if there are no more pages after this one
     */
    public Boolean getIsLastPage() {
        if (total == null || limit == null || limit == 0) {
            return true;
        }
        return (getPageNumber() + 1) * limit >= total;
    }
}
