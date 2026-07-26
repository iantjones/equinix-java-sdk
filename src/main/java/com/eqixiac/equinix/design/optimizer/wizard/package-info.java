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
 * The Deployment Wizard: turns a metro {@code OptimizationResult} into an executable
 * {@link com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan} — Cloud Routers,
 * provider connections, inter-metro backbone links, and routing protocols, with bandwidth-aware
 * profile selection and honestly-reconciled pricing.
 *
 * <p>The pipeline is <b>plan &rarr; validate &rarr; execute</b>:</p>
 * <ul>
 *   <li><b>Plan</b> — {@link com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard.Builder}
 *       (reached via {@code fabric.deploymentWizard(result)}) configures packages, topology,
 *       bandwidth strategy, ASN/BFD, notifications, and pricing; {@code plan()} produces an
 *       immutable plan. Nothing is provisioned by planning.</li>
 *   <li><b>Validate</b> — {@link com.eqixiac.equinix.design.optimizer.wizard.PlanValidator} runs in
 *       three layers (structural/catalog checks, a live self-contained Cloud Router dry-run, and
 *       the connection endpoint dry-run where an endpoint already exists) and sorts every finding
 *       into three buckets that are never conflated: <em>errors</em> (validated now; a hard defect
 *       or genuine API rejection — invalidates the plan), <em>deferred to provisioning</em> (the
 *       connection endpoint dry-run that must wait for the A-side router to exist), and
 *       <em>skipped</em> (could not be attempted — offline gateway or a non-rejection API failure —
 *       each with a reason, never invalidating the plan). {@code DeploymentPlan.dryRun()} re-runs
 *       validation and returns a <em>new</em> plan with refreshed state.</li>
 *   <li><b>Execute</b> — {@code DeploymentPlan.execute(ExecutionInputs)} refuses an invalid plan
 *       ({@code IllegalStateException}), fails fast on missing customer authorization keys, then
 *       provisions in dependency order: Cloud Routers (awaiting {@code PROVISIONED}), provider
 *       connections (each pre-flighted with a live dry-run against its now-real router), backbone
 *       links, then routing protocols. State waits distinguish ready / real terminal failure /
 *       timeout; a genuine dry-run or create rejection aborts the run and unwinds everything
 *       already created in LIFO order, and {@code rollback(outcome)} offers the same best-effort
 *       teardown for partial deployments.</li>
 * </ul>
 *
 * <p>The safety model is <b>plan-only-until-execute</b>: plans, dry-runs, repricing, value
 * realization, and export are all side-effect free; only {@code execute()} creates billable
 * resources, and only after validation, input checks, and per-connection pre-flights.</p>
 *
 * <p>Sub-packages: {@code model} (the plan and its parts),
 * {@code enums} (topology, bandwidth strategy, connection purpose).</p>
 *
 * @see com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard
 * @see com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan
 * @see com.eqixiac.equinix.design.optimizer.wizard.PlanValidator
 */
package com.eqixiac.equinix.design.optimizer.wizard;
