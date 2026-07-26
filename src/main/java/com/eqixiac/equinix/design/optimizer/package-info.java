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
 * The Metro Optimization Engine: scores, ranks, and selects Equinix metros for a deployment
 * from user sites, provider requirements, workloads, and constraints, against live Fabric
 * catalog data.
 *
 * <p>{@link com.eqixiac.equinix.design.optimizer.MetroOptimizer} is the public entry point — a
 * fluent builder reached via {@code fabric.optimizeMetros()} or
 * {@code Design.over(fabric).optimizeMetros()}. Its {@code optimize()} call runs the internal
 * engine pipeline (candidate filtering, five-dimension weighted scoring, redundancy-aware
 * selection, workload placement, risk analysis, cost estimation) and returns an
 * {@link com.eqixiac.equinix.design.optimizer.model.OptimizationResult} with ranked
 * recommendations, a latency matrix, a risk assessment, and cost estimates.</p>
 *
 * <p>Scoring dimensions are latency, provider coverage, cost, redundancy, and compliance,
 * weighted per {@link com.eqixiac.equinix.design.optimizer.enums.OptimizationStrategy} and
 * overridable via {@link com.eqixiac.equinix.design.optimizer.model.ScoringWeights}. Workload
 * provider dependencies are a hard placement constraint; per-workload latency ceilings come from
 * an explicit tolerance or the
 * {@link com.eqixiac.equinix.design.optimizer.enums.LatencySensitivity} tier's threshold.</p>
 *
 * <p>Sub-packages: {@code model} (request/result value types), {@code enums} (strategies,
 * tiers, categories), {@code wizard} (turning a result into an executable deployment plan).</p>
 *
 * @see com.eqixiac.equinix.design.optimizer.MetroOptimizer
 * @see com.eqixiac.equinix.design.optimizer.model.OptimizationResult
 * @see com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard
 */
package com.eqixiac.equinix.design.optimizer;
