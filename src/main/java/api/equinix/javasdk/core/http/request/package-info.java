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
 * HTTP request model for the Equinix Java SDK.
 * {@link api.equinix.javasdk.core.http.request.EquinixRequest} represents a typed API request —
 * headers, query parameters, path variables, and a single
 * {@link api.equinix.javasdk.core.http.request.RequestBody} (JSON payload, form fields, or raw
 * bytes). {@link api.equinix.javasdk.core.http.request.PaginatedRequest} adds offset/limit paging
 * state for paginated GET endpoints, and
 * {@link api.equinix.javasdk.core.http.request.PaginatedPostRequest} carries POST-search bodies
 * whose paging state lives in the body itself. At dispatch,
 * {@link api.equinix.javasdk.core.http.request.RequestFactory} converts the populated request
 * into the transport (Apache HttpClient) request that goes on the wire, building the entity from
 * the {@code RequestBody} once per attempt; no transport type appears in the request model's API.
 *
 * @see api.equinix.javasdk.core.http.request.EquinixRequest
 * @see api.equinix.javasdk.core.http.request.RequestBody
 * @see api.equinix.javasdk.core.http.request.RequestFactory
 */
package api.equinix.javasdk.core.http.request;
