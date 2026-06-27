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

package api.equinix.javasdk.samples;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.export.TerraformExporter;
import api.equinix.javasdk.design.export.TopologyDiagram;
import api.equinix.javasdk.design.optimizer.enums.OptimizationStrategy;
import api.equinix.javasdk.design.optimizer.enums.RedundancyTier;
import api.equinix.javasdk.design.optimizer.enums.SiteRole;
import api.equinix.javasdk.design.optimizer.enums.WorkloadType;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology;
import api.equinix.javasdk.design.optimizer.wizard.enums.BandwidthStrategy;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;

/**
 * Runs the Metro Optimization Engine to rank Equinix metros for a hypothetical
 * multi-site deployment, then turns the winning plan into a Mermaid topology diagram
 * and Equinix Terraform HCL.
 *
 * <p>The flow is: describe your sites / providers / workloads / constraints via the
 * fluent {@code fabric.optimizeMetros()} builder and call {@code optimize()}; feed the
 * {@link OptimizationResult} into {@code fabric.deploymentWizard(...)} to get an
 * executable {@link DeploymentPlan}; finally render the plan with
 * {@link TopologyDiagram} (Mermaid) and {@link TerraformExporter} (HCL).</p>
 *
 * <h3>Running</h3>
 * <pre>{@code
 * export EQUINIX_CLIENT_ID=...
 * export EQUINIX_CLIENT_SECRET=...
 * }</pre>
 *
 * <p>This program is illustrative; it is not executed by CI.</p>
 */
public final class OptimizeMetrosSample {

    private OptimizeMetrosSample() {
    }

    public static void main(String[] args) {
        BasicEquinixCredentials credentials = new BasicEquinixCredentials(
                requireEnv("EQUINIX_CLIENT_ID"),
                requireEnv("EQUINIX_CLIENT_SECRET"));

        try (Fabric fabric = new Fabric(credentials)) {

            // 1) Describe the deployment and optimize across candidate metros.
            OptimizationResult result = fabric.optimizeMetros()
                    .addSite("NYC HQ")
                        .nearestMetro(MetroCode.NY)
                        .role(SiteRole.HEADQUARTERS)
                        .headcount(500)
                        .done()
                    .addSite("Silicon Valley Eng")
                        .nearestMetro(MetroCode.SV)
                        .role(SiteRole.RESEARCH_LAB)
                        .headcount(250)
                        .done()
                    .requireProvider(CloudProviderType.AWS)
                        .sellerRegions("us-east-1")
                        .done()
                    .preferProvider(CloudProviderType.AZURE)
                        .done()
                    .addWorkload("ML Training")
                        .type(WorkloadType.AI_ML_TRAINING)
                        .bandwidthMbps(10_000)
                        .done()
                    .addWorkload("Transactional API")
                        .type(WorkloadType.TRANSACTIONAL)
                        .bandwidthMbps(1_000)
                        .done()
                    .constraints()
                        .monthlyBudget(50_000, 100_000)
                        .redundancy(RedundancyTier.MULTI_METRO)
                        .done()
                    .strategy(OptimizationStrategy.BALANCED)
                    .optimize();

            System.out.println(result.toSummary());
            System.out.println();

            // 2) Convert the optimization into an executable deployment plan.
            DeploymentPlan plan = fabric.deploymentWizard(result)
                    .routerPackage("STANDARD")
                    .routerNamePrefix("FCR")
                    .backboneBandwidthMbps(10_000)
                    .backboneTopology(BackboneTopology.FULL_MESH)
                    .bandwidthStrategy(BandwidthStrategy.PER_WORKLOAD)
                    .customerAsn(65_100L)
                    .withBFD(true, 300)
                    .plan();

            System.out.println(plan.toSummary());
            System.out.println();

            // 3a) Render the plan as a Mermaid topology diagram.
            System.out.println("=== Mermaid topology ===");
            System.out.println(new TopologyDiagram().toMermaid(plan));
            System.out.println();

            // 3b) Export the plan as Equinix Terraform provider HCL.
            System.out.println("=== Terraform (HCL) ===");
            System.out.println(new TerraformExporter().export(plan));

            // To provision for real: DeploymentOutcome outcome = plan.execute();
        } catch (Exception e) {
            System.err.println("Metro optimization sample failed: " + e.getMessage());
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
