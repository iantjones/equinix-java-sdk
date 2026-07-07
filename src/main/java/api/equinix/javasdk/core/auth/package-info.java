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
 * Authentication and credential management for the Equinix Java SDK.
 *
 * <p>{@link api.equinix.javasdk.core.auth.EquinixCredentials} supplies the OAuth2 client
 * credentials (Client ID and Client Secret); {@link api.equinix.javasdk.core.auth.BasicEquinixCredentials}
 * is the standard implementation. Credentials are resolved through the
 * {@link api.equinix.javasdk.core.auth.EquinixCredentialsProvider} extension point — implement it
 * to source credentials from a vault, environment, or rotation scheme; the SDK ships
 * {@link api.equinix.javasdk.core.auth.EquinixStaticCredentialsProvider} for the common static case.
 * At authentication time the SDK builds an {@link api.equinix.javasdk.core.auth.Oauth2TokenRequest}
 * from the provided credentials and performs the OAuth2 {@code client_credentials} grant
 * ({@code POST /oauth2/v1/token}) — the only grant the Equinix token endpoint supports.</p>
 *
 * @see api.equinix.javasdk.core.auth.EquinixCredentials
 * @see api.equinix.javasdk.core.auth.EquinixCredentialsProvider
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 */
package api.equinix.javasdk.core.auth;
