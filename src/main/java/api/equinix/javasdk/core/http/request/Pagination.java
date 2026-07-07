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

package api.equinix.javasdk.core.http.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Client-side pagination state serialized into POST-search request bodies and advanced by the
 * paging pipeline between page fetches. Fields are primitives so paging arithmetic can never
 * throw a null-unboxing {@code NullPointerException}.
 */
@Getter
@Setter
@AllArgsConstructor
public class Pagination {

    private int offset;

    private int limit;

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
