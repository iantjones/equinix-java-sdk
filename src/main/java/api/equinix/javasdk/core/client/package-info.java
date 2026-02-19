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
 * Core client infrastructure for the Equinix Java SDK. Contains the
 * {@link api.equinix.javasdk.core.client.Config} interface that defines the contract
 * for domain-specific configuration classes, and the
 * {@link api.equinix.javasdk.core.client.EquinixClient} base class that manages HTTP
 * communication, OAuth token lifecycle, and API parameter loading. The
 * {@link api.equinix.javasdk.core.client.ClientBase} provides request-building
 * utilities, while {@link api.equinix.javasdk.core.client.PageableBase} adds
 * pagination support for internal client implementations.
 *
 * @see api.equinix.javasdk.core.client.Config
 * @see api.equinix.javasdk.core.client.EquinixClient
 * @see api.equinix.javasdk.core.client.ClientBase
 */
package api.equinix.javasdk.core.client;
