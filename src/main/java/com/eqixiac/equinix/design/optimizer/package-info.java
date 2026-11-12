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
 * The Metro Optimizer: which set of Equinix metros best serves a
 * {@link com.eqixiac.equinix.design.spec.DesignSpec}, as a multi-objective search.
 *
 * <p>{@link com.eqixiac.equinix.design.optimizer.MetroOptimizer#forSpec} is the entry point
 * ({@code Design.over(fabric).optimizeMetros(spec)} is the facade equivalent; the fluent
 * {@code MetroOptimizer.builder(fabric)} only assembles a spec). A run gathers the facts once
 * ({@code context} sub-package), measures the built-in objectives ({@code design.objective}: latency,
 * cost, provider coverage, resilience, compliance), applies the built-in constraints
 * ({@code design.constraint}), searches every admissible set of metros
 * ({@code design.solve.ExhaustiveSolver}), analyses the answer's sensitivity
 * ({@code design.solve.SensitivityAnalysis}) and places the workloads ({@code design.placement}).
 * Money comes from one cost model ({@code design.cost.RateCardCostModel}).</p>
 *
 * <p>The {@link com.eqixiac.equinix.design.optimizer.model.OptimizationResult} carries the recommended
 * option, the Pareto frontier, every constraint verdict, the sensitivity report, the placement and the
 * calibrated latency factor, plus the views the Deployment Wizard, the exporters and the MCP server read.</p>
 *
 * <p><b>Beta.</b> When a workload depends on two clouds for which
 * {@link com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog} lists a native
 * provider-to-provider environment, the result carries an informational
 * {@code NATIVE_MULTICLOUD_ALTERNATIVE} finding.</p>
 *
 * @see com.eqixiac.equinix.design.optimizer.MetroOptimizer
 * @see com.eqixiac.equinix.design.optimizer.model.OptimizationResult
 * @see com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard
 */
package com.eqixiac.equinix.design.optimizer;
