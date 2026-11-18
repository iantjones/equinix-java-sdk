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
 * How each metro of a design is entered: Fabric ports with cross connects, Network Edge devices, and
 * the virtual connections from them to the Cloud Router.
 *
 * <p>{@link com.eqixiac.equinix.design.access.AccessInventory} reads the ports and devices a project
 * already has. {@link com.eqixiac.equinix.design.access.AccessPlanner} uses a suitable existing
 * resource before it plans a new order, and returns an
 * {@link com.eqixiac.equinix.design.access.AccessPlan}. The Deployment Wizard puts that plan on the
 * {@code DeploymentPlan}, and the resilience simulation fails its ports, devices, cross connects and
 * links one at a time.</p>
 *
 * <p>The SDK orders no port, cross connect or Network Edge device from a plan. A resource the plan
 * names by UUID is used as it is. A resource that does not exist yet is listed as an order to place.</p>
 */
package com.eqixiac.equinix.design.access;
