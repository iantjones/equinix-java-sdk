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
 * Authentication and credential management for the Equinix Java SDK. Provides
 * the {@link api.equinix.javasdk.core.auth.EquinixCredentials} interface and its
 * {@link api.equinix.javasdk.core.auth.BasicEquinixCredentials} implementation for
 * supplying OAuth2 client credentials (client ID and client secret). The
 * {@link api.equinix.javasdk.core.auth.EquinixStaticCredentialsProvider} resolves
 * credentials statically for use in the OAuth2 client credentials grant flow.
 *
 * @see api.equinix.javasdk.core.auth.EquinixCredentials
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 */
package api.equinix.javasdk.core.auth;
