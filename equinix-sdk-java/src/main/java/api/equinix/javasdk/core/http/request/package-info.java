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
 * HTTP request construction for the Equinix Java SDK. Contains
 * {@link api.equinix.javasdk.core.http.request.EquinixRequest} for representing
 * typed API requests with query parameters, path variables, and request bodies.
 * The {@link api.equinix.javasdk.core.http.request.RequestFactory} builds
 * {@code EquinixRequest} instances from API parameter definitions loaded at
 * startup. {@link api.equinix.javasdk.core.http.request.PaginatedRequest} extends
 * the base request to support offset and limit parameters for paginated endpoints.
 *
 * @see api.equinix.javasdk.core.http.request.EquinixRequest
 * @see api.equinix.javasdk.core.http.request.RequestFactory
 */
package api.equinix.javasdk.core.http.request;
