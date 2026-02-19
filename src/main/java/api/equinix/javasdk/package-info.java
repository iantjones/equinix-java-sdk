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
 * Root package for the Equinix Java SDK. Contains the main entry point classes
 * for each supported Equinix platform domain: {@link api.equinix.javasdk.Fabric},
 * {@link api.equinix.javasdk.NetworkEdge}, {@link api.equinix.javasdk.CustomerPortal},
 * {@link api.equinix.javasdk.IBXSmartView}, {@link api.equinix.javasdk.InternetAccess},
 * {@link api.equinix.javasdk.Projects}, and {@link api.equinix.javasdk.Messaging}.
 * Each entry point extends {@link api.equinix.javasdk.EquinixClient} and provides
 * lazy-initialized accessor methods for domain-specific resource clients.
 *
 * @see api.equinix.javasdk.EquinixClient
 */
package api.equinix.javasdk;
