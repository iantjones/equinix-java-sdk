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
 * Fabric domain model interfaces for the Equinix Java SDK. Defines read-only
 * data access contracts for Fabric resources including
 * {@link api.equinix.javasdk.fabric.model.Connection},
 * {@link api.equinix.javasdk.fabric.model.Port},
 * {@link api.equinix.javasdk.fabric.model.CloudRouter},
 * {@link api.equinix.javasdk.fabric.model.Network},
 * {@link api.equinix.javasdk.fabric.model.Stream},
 * {@link api.equinix.javasdk.fabric.model.RouteFilter},
 * {@link api.equinix.javasdk.fabric.model.RouteAggregation},
 * {@link api.equinix.javasdk.fabric.model.RoutingProtocol}, and
 * {@link api.equinix.javasdk.fabric.model.PrecisionTime}. Mutable resources
 * expose update and delete operations through their wrapper implementations.
 *
 * @see api.equinix.javasdk.fabric.model.Connection
 * @see api.equinix.javasdk.fabric.model.Port
 */
package api.equinix.javasdk.fabric.model;
