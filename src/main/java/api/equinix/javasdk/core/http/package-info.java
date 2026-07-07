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
 * automatic error-to-exception mapping. Request assembly and response handling are split into
 * cohesive helpers: {@link api.equinix.javasdk.core.http.RequestAssembler} (request construction
 * and URI-template resolution), {@link api.equinix.javasdk.core.http.ResponseHandler} (body
 * deserialization and model mapping), {@link api.equinix.javasdk.core.http.ParameterMapper}
 * (query-parameter maps and wire formatting) and
 * {@link api.equinix.javasdk.core.http.SerializationHelper} (JSON request-body attachment; the
 * wire entity itself is built by the request factory at dispatch).
 * Sub-packages handle request and response types.
 *
 * @see api.equinix.javasdk.core.http.EquinixHttpClient
 * @see api.equinix.javasdk.core.http.RequestAssembler
 * @see api.equinix.javasdk.core.http.ResponseHandler
 */
package api.equinix.javasdk.core.http;
