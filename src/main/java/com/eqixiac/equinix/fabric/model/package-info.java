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
 * {@link com.eqixiac.equinix.fabric.model.Connection},
 * {@link com.eqixiac.equinix.fabric.model.Port},
 * {@link com.eqixiac.equinix.fabric.model.CloudRouter},
 * {@link com.eqixiac.equinix.fabric.model.Network},
 * {@link com.eqixiac.equinix.fabric.model.Stream},
 * {@link com.eqixiac.equinix.fabric.model.RouteFilter},
 * {@link com.eqixiac.equinix.fabric.model.RouteAggregation},
 * {@link com.eqixiac.equinix.fabric.model.RoutingProtocol}, and
 * {@link com.eqixiac.equinix.fabric.model.PrecisionTime}. Mutable resources
 * expose update and delete operations through their wrapper implementations.
 *
 * @see com.eqixiac.equinix.fabric.model.Connection
 * @see com.eqixiac.equinix.fabric.model.Port
 */
package com.eqixiac.equinix.fabric.model;
