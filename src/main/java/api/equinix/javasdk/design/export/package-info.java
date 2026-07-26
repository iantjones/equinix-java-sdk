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
 * Exporters that render design-engine output into portable, version-controllable text formats.
 * Both exporters are stateless, thread-safe, and side-effect free — they read a plan or result
 * and return a string, provisioning nothing.
 *
 * <p>{@link api.equinix.javasdk.design.export.TerraformExporter} renders a
 * {@code DeploymentPlan} as HCL for the Equinix Terraform provider:
 * {@code equinix_fabric_cloud_router}, {@code equinix_fabric_connection} (provider connections
 * and backbone links, with the mandated {@code notifications} on every resource and
 * {@code redundancy} blocks where planned), and {@code equinix_fabric_routing_protocol} (BGP
 * emitted with {@code depends_on} its DIRECT sibling), plus {@code sensitive} input variables
 * for the customer-supplied cloud authorization keys and VLAN tags. Its honest limits: the
 * caller still supplies provider credentials and the variable values before
 * {@code terraform apply}, and physical colocation (cabinets, cross-connects) cannot be
 * expressed at all — the Equinix Terraform provider has no resources for it.</p>
 *
 * <p>{@link api.equinix.javasdk.design.export.TopologyDiagram} renders a {@code DeploymentPlan}
 * (metro subgraphs, provider edges, backbone edges; {@code graph LR}) or an
 * {@code OptimizationResult} (ranked metros and workload placements; {@code graph TD}) as
 * Mermaid graph text, which GitHub, GitLab and most docs platforms render natively. Labels are
 * HTML-escaped so real-world names cannot break the diagram.</p>
 *
 * @see api.equinix.javasdk.design.export.TerraformExporter
 * @see api.equinix.javasdk.design.export.TopologyDiagram
 */
package api.equinix.javasdk.design.export;
