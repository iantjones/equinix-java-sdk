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
 * Value types for the Deployment Wizard — the plan itself and everything it is made of, plus the
 * execution-side inputs and outcomes.
 *
 * <p>The centrepiece is {@link api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan}
 * (immutable; {@code dryRun()} returns a refreshed copy, {@code execute(ExecutionInputs)} does the
 * provisioning). Its parts are the planned resources —
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.PlannedCloudRouter},
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.PlannedConnection},
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.PlannedBackboneLink},
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.PlannedRoutingProtocol} — with the
 * sizing/selection evidence
 * ({@link api.equinix.javasdk.design.optimizer.wizard.model.BandwidthAllocation},
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.ProfileSelection},
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.ProfileCandidate}) and
 * currency-honest pricing
 * ({@link api.equinix.javasdk.design.optimizer.wizard.model.PlanPricing} — cross-currency totals
 * are withheld, never fabricated).</p>
 *
 * <p>Execution consumes {@link api.equinix.javasdk.design.optimizer.wizard.model.ExecutionInputs}
 * (the customer-supplied authorization keys enumerated by
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.ConnectionInputRequirement}) and yields
 * a {@link api.equinix.javasdk.design.optimizer.wizard.model.DeploymentOutcome} of
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.ProvisionedResource}s and
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.ProvisioningError}s.
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.RouterBodies} and
 * {@link api.equinix.javasdk.design.optimizer.wizard.model.ConnectionBodies} are the single home
 * of the wire-body shapes, so dry-runs and real creates send identical requests.</p>
 *
 * <p>Most types here are Lombok {@code @Value} (get-prefixed accessors, builders, no setters).</p>
 *
 * @see api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan
 * @see api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard
 */
package api.equinix.javasdk.design.optimizer.wizard.model;
