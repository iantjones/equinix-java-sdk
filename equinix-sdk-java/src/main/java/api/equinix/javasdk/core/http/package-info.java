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
 * HTTP communication layer for the Equinix Java SDK. Contains
 * {@link api.equinix.javasdk.core.http.EquinixHttpClient} for executing authenticated
 * HTTP requests against the Equinix API, including OAuth token management and
 * automatic error-to-exception mapping. The {@link api.equinix.javasdk.core.http.Utils}
 * class provides URI construction helpers for building API endpoint URLs with
 * path variables and query parameters. Sub-packages handle request construction
 * and response deserialization.
 *
 * @see api.equinix.javasdk.core.http.EquinixHttpClient
 * @see api.equinix.javasdk.core.http.Utils
 */
package api.equinix.javasdk.core.http;
