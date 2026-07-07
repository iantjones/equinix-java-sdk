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

import api.equinix.javasdk.core.http.request.PaginatedPostRequest;

/**
 * A client that can fetch the next page of a POST-based search and map the response. The search
 * body (whose pagination state has been advanced by the paging pipeline) is re-serialized
 * automatically when the request is dispatched.
 *
 * @author ianjones
 */
public interface PageablePost<T> extends Pageable<T> {

    /**
     * Fetches the next page for the given POST-search request (its body's pagination has already
     * been advanced by the paging pipeline).
     *
     * @param equinixRequest the search request carrying the live search body
     * @return the next page of mapped results
     */
    PaginatedFilteredList<T> nextPage(PaginatedPostRequest<T> equinixRequest);
}
