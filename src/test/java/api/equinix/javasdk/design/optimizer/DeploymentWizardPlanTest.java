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

package api.equinix.javasdk.design.optimizer;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.model.DeploymentTopology;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.MetroScore;
import api.equinix.javasdk.design.optimizer.model.OptimizationRequest;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.ProviderAvailability;
import api.equinix.javasdk.design.optimizer.model.WorkloadPlacement;
import api.equinix.javasdk.design.optimizer.model.WorkloadSpec;
import api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard;
import api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology;
import api.equinix.javasdk.design.optimizer.wizard.enums.BandwidthStrategy;
import api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedBackboneLink;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedCloudRouter;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedConnection;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedRoutingProtocol;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the live wizard engine end-to-end through {@link DeploymentWizard.Builder#plan()}: it feeds
 * a small hand-built {@link OptimizationResult} (three metros, two providers, two workloads) into the
 * builder and asserts the generated {@link DeploymentPlan} — Cloud Routers per metro, provider
 * connections with bandwidth sized from the topology, full-mesh backbone links, and the DIRECT+BGP
 * routing-protocol pairs the engine derives for each connection.
 *
 * <p>Every other wizard test builds a {@code DeploymentPlan} POJO by hand; this one actually runs
 * {@code DeploymentWizardEngine.generatePlan(...)}. The {@link FabricGateway} is a bare Mockito stub
 * (the plan phase makes no HTTP calls), and pricing is pinned to a mocked {@link RateCard} that returns
 * {@link Optional#empty()} so the engine falls back to its deterministic built-in heuristic.</p>
 */
@DisplayName("DeploymentWizard.plan() — live engine")
class DeploymentWizardPlanTest {

    // Shared MetroId instances. DeploymentTopology.forMetro() matches on reference identity, and
    // MetroId.of(...) does not intern, so the recommendation and the placement MUST share the same
    // MetroId object for the PER_WORKLOAD bandwidth path to see the workloads placed at that metro.
    private static final MetroId DC = MetroId.of(MetroCode.DC);
    private static final MetroId DA = MetroId.of(MetroCode.DA);
    private static final MetroId SV = MetroId.of(MetroCode.SV);

    private static RateCard emptyRateCard() {
        RateCard card = mock(RateCard.class);
        lenient().when(card.connection(any(), anyInt(), any(), any())).thenReturn(Optional.empty());
        lenient().when(card.cloudRouter(anyString(), any(), any())).thenReturn(Optional.empty());
        return card;
    }

    private static DeploymentPlan planFor(OptimizationResult result, BandwidthStrategy strategy) {
        FabricGateway fabric = mock(FabricGateway.class);
        return DeploymentWizard.builder(fabric, result)
                .routerPackage("STANDARD")
                .routerNamePrefix("FCR")
                .providerConnectionType(ConnectionType.EVPL_VC)
                .backboneBandwidthMbps(10_000)
                .backboneTopology(BackboneTopology.FULL_MESH)
                .bandwidthStrategy(strategy)
                .customerAsn(65100L)
                .withBFD(true, 300)
                .rateCard(emptyRateCard())
                .plan();
    }

    @Test
    @DisplayName("generates a Cloud Router per metro, named {prefix}-{metro} with the configured package")
    void cloudRoutersPerMetro() {
        DeploymentPlan plan = planFor(threeMetroResult(), BandwidthStrategy.PER_WORKLOAD);

        List<PlannedCloudRouter> routers = plan.getCloudRouters();
        assertEquals(3, routers.size(), "one Cloud Router per recommended metro");

        assertEquals(List.of("FCR-DC", "FCR-DA", "FCR-SV"),
                routers.stream().map(PlannedCloudRouter::getName).toList());
        assertTrue(routers.stream().allMatch(r -> r.getPackageCode().equals("STANDARD")));
        assertEquals(DC, routers.get(0).getMetroId());
    }

    @Test
    @DisplayName("PER_WORKLOAD sizes each provider connection from the dependent workloads placed at its metro")
    void providerConnectionsSizedPerWorkload() {
        DeploymentPlan plan = planFor(threeMetroResult(), BandwidthStrategy.PER_WORKLOAD);

        List<PlannedConnection> conns = plan.getProviderConnections();
        // DC: AWS available + 2 AWS-dependent workloads (8000 + 2000); DA: AZURE available + 1 workload (4000);
        // SV has no available providers, so no provider connection.
        assertEquals(2, conns.size());

        PlannedConnection aws = named(conns, "FCR-DC-to-aws");
        assertEquals(ConnectionPurpose.PROVIDER, aws.getPurpose());
        assertEquals(ConnectionType.EVPL_VC, aws.getConnectionType());
        assertEquals(DC, aws.getASideMetro());
        assertEquals("FCR-DC", aws.getASideRouterName());
        assertEquals("AWS", aws.getZSideProviderLabel());
        assertEquals("sp-aws", aws.getZSideServiceProfileUuid());
        assertEquals("us-east-1", aws.getZSideSellerRegion());
        assertEquals(10_000, aws.getBandwidthMbps(), "8000 (ML Training) + 2000 (DR Backup)");
        assertNotNull(aws.getBandwidthAllocation());
        assertEquals(10_000, aws.getBandwidthAllocation().getTotalMbps());
        assertEquals(Map.of("ML Training", 8000, "DR Backup", 2000),
                aws.getBandwidthAllocation().getPerWorkload());

        PlannedConnection azure = named(conns, "FCR-DA-to-azure");
        assertEquals(DA, azure.getASideMetro());
        assertEquals(4000, azure.getBandwidthMbps());
    }

    @Test
    @DisplayName("FULL_MESH backbone links every metro pair, each carrying a BACKBONE connection")
    void fullMeshBackboneLinks() {
        DeploymentPlan plan = planFor(threeMetroResult(), BandwidthStrategy.PER_WORKLOAD);

        List<PlannedBackboneLink> links = plan.getBackboneLinks();
        // Full mesh over 3 metros = N*(N-1)/2 = 3 links.
        assertEquals(3, links.size());
        assertEquals(List.of("FCR-DC-to-DA", "FCR-DC-to-SV", "FCR-DA-to-SV"),
                links.stream().map(PlannedBackboneLink::getName).toList());

        PlannedBackboneLink first = links.get(0);
        assertEquals(DC, first.getMetroA());
        assertEquals(DA, first.getMetroZ());
        assertEquals(10_000, first.getBandwidthMbps());
        assertEquals(BackboneTopology.FULL_MESH, first.getTopology());

        PlannedConnection bbConn = first.getConnection();
        assertNotNull(bbConn);
        assertEquals(ConnectionPurpose.BACKBONE, bbConn.getPurpose());
        assertEquals("FCR-DC", bbConn.getASideRouterName());
        assertEquals("FCR-DA", bbConn.getZSideRouterName());
        assertEquals(DA, bbConn.getZSideMetro());
    }

    @Test
    @DisplayName("emits a DIRECT + BGP routing-protocol pair per connection, carrying the customer ASN and BFD")
    void routingProtocolPairPerConnection() {
        DeploymentPlan plan = planFor(threeMetroResult(), BandwidthStrategy.PER_WORKLOAD);

        // 2 provider connections + 3 backbone connections = 5 connections, each with a DIRECT+BGP pair.
        List<PlannedRoutingProtocol> protocols = plan.getRoutingProtocols();
        assertEquals(10, protocols.size());

        long direct = protocols.stream().filter(p -> p.getType() == RoutingProtocolType.DIRECT).count();
        long bgp = protocols.stream().filter(p -> p.getType() == RoutingProtocolType.BGP).count();
        assertEquals(5, direct);
        assertEquals(5, bgp);

        // The BGP protocol for the AWS connection carries the configured ASN, BFD, and a /30 peer pair.
        PlannedRoutingProtocol awsBgp = protocols.stream()
                .filter(p -> p.getType() == RoutingProtocolType.BGP && "FCR-DC-to-aws".equals(p.getConnectionName()))
                .findFirst().orElseThrow();
        assertEquals("FCR-DC-to-aws-BGP", awsBgp.getName());
        assertEquals(65100L, awsBgp.getCustomerAsn());
        assertTrue(awsBgp.isBfdEnabled());
        assertEquals(300, awsBgp.getBfdInterval());
        assertNotNull(awsBgp.getCustomerPeerIpv4());
        assertTrue(awsBgp.getCustomerPeerIpv4().endsWith(".2/30"));
        assertTrue(awsBgp.getEquinixPeerIpv4().endsWith(".1/30"));

        PlannedRoutingProtocol awsDirect = protocols.stream()
                .filter(p -> p.getType() == RoutingProtocolType.DIRECT && "FCR-DC-to-aws".equals(p.getConnectionName()))
                .findFirst().orElseThrow();
        assertEquals("FCR-DC-to-aws-DIRECT", awsDirect.getName());
        assertFalse(awsDirect.isBfdEnabled());
        assertNotNull(awsDirect.getEquinixIfaceIpv4());
    }

    @Test
    @DisplayName("the generated plan is valid, wires the source optimization back, and counts every resource")
    void planValidAndCounted() {
        OptimizationResult result = threeMetroResult();
        DeploymentPlan plan = planFor(result, BandwidthStrategy.PER_WORKLOAD);

        assertTrue(plan.isValid(), "a well-formed optimization should produce a valid plan");
        assertTrue(plan.getValidationErrors().isEmpty());
        assertSame(result, plan.getSourceOptimization());
        assertNotNull(plan.getPricing());

        // 3 routers + 2 provider conns + 3 backbone links + 10 routing protocols = 18.
        assertEquals(18, plan.totalResourceCount());
    }

    @Test
    @DisplayName("AGGREGATED bandwidth ignores per-provider dependency and sums all workloads at the metro")
    void aggregatedBandwidthStrategy() {
        DeploymentPlan plan = planFor(threeMetroResult(), BandwidthStrategy.AGGREGATED);

        // Under AGGREGATED, the DC-to-AWS connection still sums both DC workloads (8000 + 2000).
        PlannedConnection aws = named(plan.getProviderConnections(), "FCR-DC-to-aws");
        assertEquals(10_000, aws.getBandwidthMbps());
    }

    @Test
    @DisplayName("an empty optimization result yields an explicitly invalid, empty plan (no exception)")
    void emptyRecommendationsInvalidPlan() {
        OptimizationResult empty = OptimizationResult.builder()
                .recommendations(Collections.emptyList())
                .computedAt(Instant.now())
                .computeTimeMs(1)
                .build();

        DeploymentPlan plan = planFor(empty, BandwidthStrategy.PER_WORKLOAD);

        assertFalse(plan.isValid());
        assertFalse(plan.getValidationErrors().isEmpty());
        assertEquals(0, plan.totalResourceCount());
        assertTrue(plan.getCloudRouters().isEmpty());
        assertTrue(plan.getProviderConnections().isEmpty());
        assertTrue(plan.getBackboneLinks().isEmpty());
    }

    @Test
    @DisplayName("a single-metro result plans a router but no backbone links (needs >= 2 metros)")
    void singleMetroNoBackbone() {
        MetroScore score = new MetroScore(90.0, Collections.emptyList());
        WorkloadSpec ml = WorkloadSpec.builder().label("ML Training").bandwidthMbps(8000).build();

        OptimizationResult result = OptimizationResult.builder()
                .request(OptimizationRequest.builder().workloads(List.of(ml)).build())
                .recommendations(List.of(
                        MetroRecommendation.builder()
                                .rank(1).metroId(DC).metroName("Ashburn").score(score)
                                .reasons(List.of("Primary"))
                                .availableProviders(List.of(
                                        new ProviderAvailability("AWS", true, List.of("us-east-1"), "sp-aws")))
                                .assignedWorkloads(List.of(new WorkloadPlacement("ML Training", DC, "primary")))
                                .build()))
                .topology(new DeploymentTopology(List.of(new WorkloadPlacement("ML Training", DC, "primary"))))
                .computedAt(Instant.now())
                .computeTimeMs(10)
                .build();

        DeploymentPlan plan = planFor(result, BandwidthStrategy.PER_WORKLOAD);

        assertEquals(1, plan.getCloudRouters().size());
        assertEquals(1, plan.getProviderConnections().size());
        assertTrue(plan.getBackboneLinks().isEmpty(), "no inter-metro links with a single metro");
    }

    // ── Helpers ──

    private static PlannedConnection named(List<PlannedConnection> conns, String name) {
        return conns.stream().filter(c -> c.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("no connection named " + name + " in " + conns));
    }

    /**
     * A three-metro optimization: DC (AWS, two AWS-dependent workloads), DA (AZURE, one AZURE-dependent
     * workload), SV (no available providers). The topology places each workload at its metro, using the
     * shared MetroId instances so {@code forMetro} reference-matching resolves.
     */
    private static OptimizationResult threeMetroResult() {
        MetroScore score = new MetroScore(90.0, Collections.emptyList());

        WorkloadSpec mlTraining = WorkloadSpec.builder()
                .label("ML Training").bandwidthMbps(8000)
                .dependsOnProviders(List.of(providerReq("AWS"))).build();
        WorkloadSpec drBackup = WorkloadSpec.builder()
                .label("DR Backup").bandwidthMbps(2000)
                .dependsOnProviders(List.of(providerReq("AWS"))).build();
        WorkloadSpec analytics = WorkloadSpec.builder()
                .label("Analytics").bandwidthMbps(4000)
                .dependsOnProviders(List.of(providerReq("AZURE"))).build();

        OptimizationRequest request = OptimizationRequest.builder()
                .workloads(List.of(mlTraining, drBackup, analytics))
                .build();

        DeploymentTopology topology = new DeploymentTopology(List.of(
                new WorkloadPlacement("ML Training", DC, "AWS at DC"),
                new WorkloadPlacement("DR Backup", DC, "AWS at DC"),
                new WorkloadPlacement("Analytics", DA, "AZURE at DA")));

        MetroRecommendation dc = MetroRecommendation.builder()
                .rank(1).metroId(DC).metroName("Ashburn").score(score).reasons(List.of("Primary"))
                .availableProviders(List.of(
                        new ProviderAvailability("AWS", true, List.of("us-east-1"), "sp-aws")))
                .build();
        MetroRecommendation da = MetroRecommendation.builder()
                .rank(2).metroId(DA).metroName("Dallas").score(score).reasons(List.of("Secondary"))
                .availableProviders(List.of(
                        new ProviderAvailability("AZURE", true, List.of("eastus"), "sp-azure")))
                .build();
        MetroRecommendation sv = MetroRecommendation.builder()
                .rank(3).metroId(SV).metroName("Silicon Valley").score(score).reasons(List.of("Tertiary"))
                .availableProviders(List.of(
                        // Present but not available -> filtered out, so SV gets no provider connection.
                        new ProviderAvailability("GCP", false, List.of("us-west1"), "sp-gcp")))
                .build();

        return OptimizationResult.builder()
                .request(request)
                .recommendations(List.of(dc, da, sv))
                .topology(topology)
                .computedAt(Instant.now())
                .computeTimeMs(42)
                .build();
    }

    private static api.equinix.javasdk.design.optimizer.model.ProviderRequirement providerReq(String label) {
        return api.equinix.javasdk.design.optimizer.model.ProviderRequirement.builder().label(label).build();
    }
}
