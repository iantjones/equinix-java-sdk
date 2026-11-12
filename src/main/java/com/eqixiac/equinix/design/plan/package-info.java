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
 * The deployment-plan lifecycle: plan as data, drift detection, idempotent and resumable reconcile,
 * and safe rollback.
 *
 * <ul>
 *   <li><b>Plan as data.</b> A {@code DeploymentPlan} is immutable and round-trips through JSON
 *       ({@link com.eqixiac.equinix.design.plan.PlanJson}). Every planned resource carries a logical key
 *       derived from the design; {@link com.eqixiac.equinix.design.plan.PlanSteps} orders them by
 *       dependency. The plan id is a hash of its desired state; the document records the
 *       {@code DesignSpec} fingerprint it came from.</li>
 *   <li><b>Marking.</b> Resources are found again by name within the plan's project, and connections by
 *       the {@code additionalInfo} marker the API persists ({@link com.eqixiac.equinix.design.plan.PlanMarker}).</li>
 *   <li><b>Drift.</b> {@link com.eqixiac.equinix.design.plan.LiveStateReader} reads live state (read-only);
 *       {@link com.eqixiac.equinix.design.plan.DriftDetector} classifies each planned resource as
 *       {@code MISSING}, {@code PRESENT_MATCHING} or {@code PRESENT_DIFFERENT} (with the fields) and lists
 *       marked live resources that are not in the plan.</li>
 *   <li><b>Reconcile.</b> {@link com.eqixiac.equinix.design.plan.PlanExecutor} creates only what is missing,
 *       in dependency order, saving a {@link com.eqixiac.equinix.design.plan.Checkpoint} to a
 *       {@link com.eqixiac.equinix.design.plan.CheckpointStore} after every step; a re-run of a completed
 *       plan is a no-op. Updates are opt-in and limited to patchable fields.</li>
 *   <li><b>Failure and rollback.</b> One {@link com.eqixiac.equinix.design.plan.FailurePolicy} for every
 *       step; {@link com.eqixiac.equinix.design.plan.PlanRollback} waits for asynchronous connection
 *       deletes before deleting a Cloud Router and reports orphans with their uuids.</li>
 * </ul>
 *
 * <p>Fabric Interconnects (Fabric One) and native multicloud links are plan elements the SDK never
 * creates: they appear as {@code NOT_EXECUTABLE} steps with the reason.</p>
 */
package com.eqixiac.equinix.design.plan;
