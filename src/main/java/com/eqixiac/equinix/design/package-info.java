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
 * Value-add design engines built on top of the Equinix API clients: placement optimization,
 * deployment planning, pricing/TCO analysis, peering intelligence, latency physics, and
 * infrastructure-as-code export.
 *
 * <h2>Module map</h2>
 * <ul>
 *   <li>{@code design.optimizer} — <b>placement</b>: scores and ranks Equinix metros against
 *       sites, workloads, providers, and constraints ({@code MetroOptimizer} →
 *       {@code OptimizationResult}).</li>
 *   <li>{@code design.optimizer.wizard} — <b>plan &amp; execute</b>: turns an
 *       {@code OptimizationResult} into a validated {@code DeploymentPlan} (Cloud Routers,
 *       connections, backbone links, routing protocols) that can be dry-run, executed, and
 *       rolled back.</li>
 *   <li>{@code design.value} — <b>pricing and TCO</b>: layered {@code RateCard}s (live Equinix,
 *       custom, reference, cloud-provider adapters), egress savings, and total-cost-of-ownership
 *       comparison.</li>
 *   <li>{@code design.peering} — <b>PeeringDB intelligence</b>: IX/facility presence matrices,
 *       Fabric on-ramp cross-referencing, resiliency and blast-radius analysis, peering
 *       opportunities.</li>
 *   <li>{@code design.geo} — <b>latency physics</b>: speed-of-light-in-fibre latency floors and
 *       haversine distances between IBXes, metros, and raw coordinates.</li>
 *   <li>{@code design.export} — <b>export</b>: Terraform HCL and Mermaid topology diagrams from
 *       plans and results.</li>
 * </ul>
 *
 * <p>A typical flow is optimizer → wizard → export, with {@code design.value} pricing feeding
 * both the optimizer's cost dimension and the wizard's plan pricing, and {@code design.geo}
 * supplying the distance/latency estimates used throughout.</p>
 *
 * <h2>Composition boundary</h2>
 * <p>Every engine runs over {@link com.eqixiac.equinix.FabricGateway} — the narrow capability
 * view of the Fabric client (metros, service profiles, cloud routers, connections, routing
 * protocols, prices) — never over the full concrete client. Enter through
 * {@link com.eqixiac.equinix.Design#over(com.eqixiac.equinix.FabricGateway)}, the equivalent
 * accessors on {@link com.eqixiac.equinix.Fabric} ({@code optimizeMetros()},
 * {@code deploymentWizard(...)}, {@code peeringIntelligence()}, {@code savingsCalculator()},
 * {@code tcoComparison()}), or a shared {@link com.eqixiac.equinix.Equinix} session's
 * {@code design()}.</p>
 *
 * @see com.eqixiac.equinix.Design
 * @see com.eqixiac.equinix.FabricGateway
 */
package com.eqixiac.equinix.design;
