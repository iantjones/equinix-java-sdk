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
 * Configuration enums for the Deployment Wizard:
 * {@link api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology} (how the recommended
 * metros are meshed with inter-metro backbone links),
 * {@link api.equinix.javasdk.design.optimizer.wizard.enums.BandwidthStrategy} (how provider
 * connections are sized from workload requirements), and
 * {@link api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose} (what role a planned
 * connection plays in the plan). The first two are levers on
 * {@link api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard.Builder}; the third is
 * stamped on each {@code PlannedConnection} by the wizard.
 *
 * @see api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard
 */
package api.equinix.javasdk.design.optimizer.wizard.enums;
