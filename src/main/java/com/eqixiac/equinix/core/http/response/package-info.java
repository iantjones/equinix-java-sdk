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

/**
 * HTTP response handling for the Equinix Java SDK. Contains
 * {@link com.eqixiac.equinix.core.http.response.EquinixResponse}, which wraps the raw HTTP
 * response (status code, headers and the raw body stream — deserialization happens later in the
 * response handlers). {@link com.eqixiac.equinix.core.http.response.Pagination} holds page
 * metadata (offset, limit, total), and {@link com.eqixiac.equinix.core.http.response.PaginatedList}
 * provides an iterable view over the loaded results with explicit page loading via
 * {@code next()}/{@code loadAll()}.
 *
 * @see com.eqixiac.equinix.core.http.response.EquinixResponse
 * @see com.eqixiac.equinix.core.http.response.PaginatedList
 */
package com.eqixiac.equinix.core.http.response;
