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
import com.eqixiac.equinix.design.connectivity.ConnectivityPlan;
import com.eqixiac.equinix.design.connectivity.ConnectivityPlan.Access;
import com.eqixiac.equinix.design.connectivity.ConnectivityPlan.Reach;
import com.eqixiac.equinix.design.connectivity.ConnectivityPlanner;
import com.eqixiac.equinix.design.connectivity.ProductAvailability;
import com.eqixiac.equinix.design.optimizer.enums.SiteRole;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.peering.model.PeeringIntelligenceResult;
import com.eqixiac.equinix.design.peering.model.UnifiedConnectivityView;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.PeerIntent;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.enums.GeoScopeType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The reach and access rules of {@link ConnectivityPlanner}, on a two-metro design (Ashburn, London). */
class ConnectivityPlannerTest {

    private static final DesignSpec SPEC = DesignSpec.builder().name("connectivity")
            .site(SiteIntent.builder().label("Ashburn cage").metro("DC").role(SiteRole.DATA_CENTER).build())
            .site(SiteIntent.builder().label("London office").metro("LD").build())
            .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
            .workload(WorkloadIntent.builder().label("api").bandwidthMbps(500).clouds(List.of("aws")).build())
            .peer(PeerIntent.builder().asn(13335).label("cdn").build())
            .peer(PeerIntent.builder().asn(64500).label("partner").build())
            .peer(PeerIntent.builder().asn(64501).build())
            .constraints(DesignConstraints.builder().requiredMetros(List.of("DC", "LD")).build())
            .build();

    private static OptimizationResult optimized() {
        FabricGateway fabric = OptimizerFixtures.gateway(List.of(
                OptimizerFixtures.metro("DC", "Ashburn", Region.AMER, 39.0438, -77.4874,
                        List.of(GeoScopeType.CONUS), OptimizerFixtures.link("LD", 73.4)),
                OptimizerFixtures.metro("LD", "London", Region.EMEA, 51.5074, -0.1278, List.of(GeoScopeType.UK),
                        OptimizerFixtures.link("DC", 73.4))),
                List.of(OptimizerFixtures.profile("sp-aws", "AWS Direct Connect", "DC:us-east-1", "LD:eu-west-2")));
        return MetroOptimizer.forSpec(fabric, SPEC).rateCard(ReferenceRateCard.standard()).run();
    }

    private static UnifiedConnectivityView view(long asn, String metro, boolean ix, boolean fabric) {
        return UnifiedConnectivityView.builder().asn(asn).metroConnectivity(List.of(
                UnifiedConnectivityView.MetroConnectivity.builder().metro(MetroId.of(metro)).hasIxPeering(ix)
                        .hasFabric(fabric).build())).build();
    }

    private static ConnectivityPlan.ReachDecision reach(ConnectivityPlan plan, String destinationStart) {
        return plan.reach().stream().filter(d -> d.destination().startsWith(destinationStart)).findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("clouds over Fabric, a peer at an IX, a peer with a service profile, a peer nowhere, and internet")
    void reachRules() {
        PeeringIntelligenceResult peering = PeeringIntelligenceResult.builder().unifiedViews(Map.of(
                13335L, view(13335, "LD", true, true),
                64500L, view(64500, "DC", false, true),
                64501L, view(64501, "SV", true, true))).build();
        ConnectivityPlan plan = ConnectivityPlanner.forSpec(SPEC).optimization(optimized()).peering(peering)
                .availability(ProductAvailability.of(Set.of("ld"), Set.of("DC"))).plan();

        assertEquals(Reach.FABRIC_CLOUD_ROUTER, reach(plan, "aws").reach());
        assertEquals(List.of("DC", "LD"), reach(plan, "aws").metros());
        assertEquals(Reach.INTERNET_EXCHANGE, reach(plan, "cdn").reach(), "IX wins over a service profile");
        assertEquals(List.of("LD"), reach(plan, "cdn").metros());
        assertEquals(Reach.FABRIC_PRIVATE, reach(plan, "partner").reach());
        assertEquals(Reach.INTERNET_ACCESS, reach(plan, "AS64501").reach(), "present only outside the design");
        assertEquals(List.of("DC"), reach(plan, "internet").metros());
        assertEquals(SPEC.fingerprint(), plan.specFingerprint());
    }

    @Test
    @DisplayName("ports where the spec has a data center, Network Edge where it is offered, else the router alone")
    void accessRules() {
        ConnectivityPlan plan = ConnectivityPlanner.forSpec(SPEC).optimization(optimized())
                .availability(ProductAvailability.of(Set.of("LD"), Set.of())).plan();

        assertEquals(Access.PHYSICAL_PORTS, plan.access().get(0).access());
        assertEquals(5, plan.access().get(0).orders().size());
        assertEquals(Access.NETWORK_EDGE, plan.access().get(1).access());

        ConnectivityPlan unknown = ConnectivityPlanner.forSpec(SPEC).optimization(optimized()).plan();
        assertEquals(Access.CLOUD_ROUTER_ONLY, unknown.access().get(1).access());
        assertTrue(unknown.access().get(1).reason().contains("availability is unknown"));
    }

    @Test
    @DisplayName("without peering or availability data the plan says undecided and why; it never guesses")
    void undecidedWithoutData() {
        ConnectivityPlan plan = ConnectivityPlanner.forSpec(SPEC).optimization(optimized()).plan();

        assertEquals(Reach.UNDECIDED, reach(plan, "cdn").reach());
        assertEquals(Reach.UNDECIDED, reach(plan, "internet").reach());
        assertTrue(plan.notes().stream().anyMatch(n -> n.contains("No Peering Intelligence result")));
        assertTrue(plan.toMarkdown().contains("Nothing is ordered"));
        assertThrows(IllegalStateException.class, () -> ConnectivityPlanner.forSpec(SPEC).plan());
    }
}
