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
 * Workload placement on a chosen set of metros: {@link com.eqixiac.equinix.design.placement.WorkloadPlacer}
 * decides where each workload runs (honouring {@code pinnedMetro}, per-workload {@code maxLatencyMs} and
 * cloud reachability) and which cloud connections that implies, as a
 * {@link com.eqixiac.equinix.design.placement.PlacementPlan}. The cost model, the resilience objective,
 * the workload constraints and the Deployment Wizard all read the same plan.
 */
package com.eqixiac.equinix.design.placement;
