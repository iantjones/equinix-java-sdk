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
 * <h2>Cloud-to-cloud flows (Beta)</h2>
 * <p>A workload that depends on two or more clouds implies a flow between each pair of them. Two
 * cloud providers can carry such a flow over a native provider-to-provider link (AWS Interconnect -
 * multicloud with a Google Cloud Partner Cross-Cloud Interconnect transport, or with an Oracle
 * FastConnect interconnect virtual circuit) that uses no Equinix resource. The wizard plans and
 * prices that link next to the Equinix path and never provisions it: this SDK calls no
 * cloud-provider API.</p>
 *
 * <table>
 *   <caption>{@link com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy},
 *   set with {@code DeploymentWizard.Builder.cloudToCloudStrategy(...)}</caption>
 *   <tr><th>Strategy</th><th>Equinix connections for the flow</th><th>Native link on the plan</th></tr>
 *   <tr><td>{@code COMPARE} (default)</td><td>planned as under {@code EQUINIX_ONLY}</td>
 *       <td>{@code ALTERNATIVE}, when the catalog lists the planned region pair</td></tr>
 *   <tr><td>{@code EQUINIX_ONLY}</td><td>planned</td><td>none; the catalog is not read</td></tr>
 *   <tr><td>{@code NATIVE_WHEN_AVAILABLE}</td><td>omitted only where the replacement rule allows:
 *       a {@code GA} environment with a covering size, no user site in the request, the cloud not
 *       a request-level requirement, and no other workload using the connection</td>
 *       <td>{@code REPLACEMENT} where a connection was omitted, otherwise {@code ALTERNATIVE}</td></tr>
 *   <tr><td>{@code NATIVE_ONLY}</td><td>as {@code NATIVE_WHEN_AVAILABLE}</td>
 *       <td>as above; a flow with no usable environment is an {@code UNAVAILABLE} entry and a
 *       Layer-1 validation error</td></tr>
 * </table>
 *
 * <table>
 *   <caption>What each stage does with a
 *   {@link com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect}</caption>
 *   <tr><th>Stage</th><th>Behavior</th></tr>
 *   <tr><td>Plan</td><td>Region pairs are matched against
 *       {@link com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog}, a bundled,
 *       dated copy of provider documentation that goes stale; supply a newer one with
 *       {@code multicloudEnvironments(...)}. The requested bandwidth (the sum of the workloads'
 *       effective bandwidths, Mbps) rounds up to the smallest size the environment lists. No
 *       routing protocol, /30 subnet or redundancy group is planned for a link.</td></tr>
 *   <tr><td>Price</td><td>Each link carries a
 *       {@link com.eqixiac.equinix.design.optimizer.wizard.model.MulticloudLinkPricing}: the
 *       two-sided native quote (hourly list prices at 730 h per month; an unpublished size is
 *       unpriced, never zero), the Equinix-path fixed cost for the same flow, and the break-even
 *       sustained rate from {@code MulticloudPathComparison.breakEvenSustainedMbps(...)}.
 *       {@code PlanPricing} reports native figures in separate {@code native*} fields; none is
 *       part of the Equinix totals. {@code reprice(plan)} refreshes both.</td></tr>
 *   <tr><td>Validate</td><td>Recorded as <em>skipped</em> with a reason naming the link: nothing
 *       can dry-run it now or at provisioning, so it is not <em>deferred</em>. Errors: a malformed
 *       entry, or a {@code NATIVE_ONLY} flow without a usable environment.</td></tr>
 *   <tr><td>Execute</td><td>No request is sent. {@code DeploymentOutcome.getInformational()}
 *       carries one non-recoverable entry per link ("created outside Fabric: follow the
 *       create-then-accept recipe"); it is not an error and does not affect
 *       {@code isFullySuccessful()}. {@code totalResourceCount()} excludes links.</td></tr>
 * </table>
 *
 * <p>Sub-packages: {@code model} (the plan and its parts),
 * {@code enums} (topology, bandwidth strategy, connection purpose, cloud-to-cloud strategy, native
 * link role).</p>
 *
 * @see com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard
 * @see com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan
 * @see com.eqixiac.equinix.design.optimizer.wizard.PlanValidator
 */
package com.eqixiac.equinix.design.optimizer.wizard;
