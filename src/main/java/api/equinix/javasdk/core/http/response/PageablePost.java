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
import api.equinix.javasdk.core.http.request.PaginatedRequest;

/**
 * <p>Pageable interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface PageablePost<T> extends Pageable<T> {

    /**
     * <p>nextPage.</p>
     *
     * @param equinixRequest a {@link PaginatedRequest} object.
     * @return a {@link PaginatedList} object.
     */
    PaginatedFilteredList<T> nextPage(PaginatedPostRequest<T> equinixRequest);
}