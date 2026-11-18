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
package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.RiskFinding;
import com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.enums.GeoScopeType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A spec that names a cloud region: the placement prefers a metro whose on-ramp serves that region, and a
 * design that has to use another region says so in its findings and in the plan.
 */
class CloudRegionPreferenceTest {

    /** The site is in Toronto, where the AWS on-ramp serves ca-central-1; Ashburn serves us-east-1. */
    private static FabricGateway fabric() {
        return OptimizerFixtures.gateway(List.of(
                OptimizerFixtures.metro("DC", "Ashburn", Region.AMER, 39.0438, -77.4874,
                        List.of(GeoScopeType.CONUS), OptimizerFixtures.link("TR", 14.0)),
                OptimizerFixtures.metro("TR", "Toronto", Region.AMER, 43.6532, -79.3832,
                        List.of(GeoScopeType.CANADA), OptimizerFixtures.link("DC", 14.0))),
                List.of(OptimizerFixtures.profile("sp-aws", "AWS Direct Connect", "DC:us-east-1",
                        "TR:ca-central-1")));
    }

    private static DesignSpec.DesignSpecBuilder base(String region) {
        return DesignSpec.builder().name("region")
                .site(SiteIntent.builder().label("Toronto office").metro("TR").build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region(region).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(500).clouds(List.of("aws")).build());
    }

    private static OptimizationResult run(DesignSpec spec) {
        return MetroOptimizer.forSpec(fabric(), spec).rateCard(ReferenceRateCard.standard()).run();
    }

    private static PlacementPlan.Placement api(OptimizationResult result) {
        return result.getPlacement().placement("api").orElseThrow();
    }

    @Test
    @DisplayName("with both metros in the design the workload runs where the on-ramp serves the named region")
    void prefersTheMetroThatServesTheRegion() {
        OptimizationResult result = run(base("us-east-1").constraints(DesignConstraints.builder()
                .requiredMetros(List.of("DC", "TR")).build()).build());

        assertEquals(MetroId.of("DC"), api(result).primary(), "TR is closer to the site, DC serves us-east-1");
        assertEquals(MetroId.of("TR"), api(result).standby());
        assertTrue(result.getRiskAssessment().getFindings().stream().map(RiskFinding::getCategory)
                .anyMatch("CLOUD_REGION_NOT_LOCAL"::equals), "the standby uses another region and says so");
    }

    @Test
    @DisplayName("with no region named, latency alone decides and nothing is flagged")
    void noRegionNamed() {
        OptimizationResult result = run(base(null).constraints(DesignConstraints.builder()
                .requiredMetros(List.of("DC", "TR")).build()).build());

        assertEquals(MetroId.of("TR"), api(result).primary());
        assertFalse(result.getRiskAssessment().getFindings().stream().map(RiskFinding::getCategory)
                .anyMatch("CLOUD_REGION_NOT_LOCAL"::equals));
    }

    @Test
    @DisplayName("a plan states the region each connection uses and the region the design named")
    void planStatesBothRegions() {
        OptimizationResult result = run(base("us-east-1").constraints(DesignConstraints.builder()
                .requiredMetros(List.of("DC", "TR")).build()).build());
        DeploymentPlan plan = DeploymentWizard.builder(fabric(), result).projectId("project-1").plan();

        PlannedConnection ashburn = connection(plan, "DC");
        PlannedConnection toronto = connection(plan, "TR");
        assertEquals("us-east-1", ashburn.getZSideSellerRegion());
        assertNull(ashburn.getZSideRequestedRegion(), "the named region is the one used: nothing to add");
        assertEquals("ca-central-1", toronto.getZSideSellerRegion());
        assertEquals("us-east-1", toronto.getZSideRequestedRegion());
        assertTrue(plan.toMarkdown().contains("ca-central-1; the design names us-east-1"), plan.toMarkdown());
    }

    @Test
    @DisplayName("the plan's design total equals the optimizer's estimate for the same design")
    void planAndOptimizerReconcile() {
        OptimizationResult result = run(base("us-east-1").constraints(DesignConstraints.builder()
                .requiredMetros(List.of("DC", "TR")).build()).build());
        DeploymentPlan plan = DeploymentWizard.builder(fabric(), result).projectId("project-1")
                .rateCard(ReferenceRateCard.standard()).plan();

        java.math.BigDecimal estimate = result.getRecommended().cost().monthlyTotal().orElseThrow();
        // The estimate lists the backbone between the two Cloud Routers, as the plan does.
        assertTrue(result.getRecommended().cost().lines().stream()
                .anyMatch(l -> l.label().startsWith("Fabric backbone") && l.label().endsWith("DC to TR")));
        // The plan lists what it does not create (the provider's own fees), as the estimate does.
        assertEquals(2, plan.getPricing().getOutsidePlanLines().size());
        assertEquals(0, estimate.compareTo(plan.getPricing().getDesignMonthly()),
                estimate + " vs " + plan.getPricing().getDesignMonthly());
        assertEquals(0, plan.getPricing().getDesignMonthly().compareTo(
                plan.getPricing().getMonthlyTotal().add(plan.getPricing().getOutsidePlanMonthly())));
        assertTrue(plan.toMarkdown().contains("Design total monthly"));
    }

    @Test
    @DisplayName("a plan connects a cloud only in the metros where the placement uses it")
    void planConnectsOnlyWhatThePlacementUses() {
        FabricGateway both = OptimizerFixtures.gateway(List.of(
                OptimizerFixtures.metro("DC", "Ashburn", Region.AMER, 39.0438, -77.4874,
                        List.of(GeoScopeType.CONUS), OptimizerFixtures.link("TR", 14.0)),
                OptimizerFixtures.metro("TR", "Toronto", Region.AMER, 43.6532, -79.3832,
                        List.of(GeoScopeType.CANADA), OptimizerFixtures.link("DC", 14.0))),
                List.of(OptimizerFixtures.profile("sp-aws", "AWS Direct Connect", "DC:us-east-1", "TR:us-east-1"),
                        OptimizerFixtures.profile("sp-gcp", "Google Cloud Partner Interconnect", "DC:us-east4",
                                "TR:us-east4")));
        DesignSpec spec = DesignSpec.builder().name("legs")
                .site(SiteIntent.builder().label("Toronto office").metro("TR").build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
                .cloud(CloudIntent.builder().id("gcp").provider(CloudProviderType.GOOGLE_CLOUD).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(500).clouds(List.of("aws")).build())
                // Pinned, so it has no standby: gcp is used in DC only, though TR offers it too.
                .workload(WorkloadIntent.builder().label("db").bandwidthMbps(500).clouds(List.of("gcp"))
                        .pinnedMetro("DC").build())
                .constraints(DesignConstraints.builder().requiredMetros(List.of("DC", "TR")).build())
                .build();
        OptimizationResult result = MetroOptimizer.forSpec(both, spec).rateCard(ReferenceRateCard.standard()).run();
        DeploymentPlan plan = DeploymentWizard.builder(both, result).projectId("project-1")
                .rateCard(ReferenceRateCard.standard()).plan();

        List<String> connected = plan.getProviderConnections().stream()
                .map(c -> c.getASideMetro().code() + ":" + c.getZSideProviderLabel()).sorted().toList();
        assertEquals(List.of("DC:aws", "DC:gcp", "TR:aws"), connected, "no gcp connection in TR");
        assertEquals(0, result.getRecommended().cost().monthlyTotal().orElseThrow()
                .compareTo(plan.getPricing().getDesignMonthly()), "the plan and the estimate still agree");
    }

    private static PlannedConnection connection(DeploymentPlan plan, String metro) {
        return plan.getProviderConnections().stream()
                .filter(c -> c.getASideMetro().code().equals(metro)).findFirst().orElseThrow();
    }
}
