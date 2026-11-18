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
 * Decides how each destination of a design is reached and how each metro is entered.
 *
 * <p>{@link com.eqixiac.equinix.design.connectivity.ConnectivityPlanner} applies stated rules to a spec,
 * the optimizer's result, an optional Peering Intelligence result and
 * {@link com.eqixiac.equinix.design.connectivity.ProductAvailability}: a cloud over a private Fabric
 * connection from a Cloud Router, a peer over an Internet Exchange (IX), a private connection or Internet
 * Access, and each metro through redundant physical ports, a Network Edge device pair, or its Cloud
 * Router alone. It reads nothing itself and orders nothing. Metro access is decided by
 * {@code design.access.AccessPlanner}; give {@code ConnectivityPlan.accessPlan()} to the Deployment Wizard
 * and to the resilience analyzer to put the ports, cross connects and devices on the plan and into the
 * failure simulation.</p>
 */
package com.eqixiac.equinix.design.connectivity;
