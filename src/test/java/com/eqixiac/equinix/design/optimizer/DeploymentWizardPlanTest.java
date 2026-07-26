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
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.DeploymentTopology;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.MetroScore;
import com.eqixiac.equinix.design.optimizer.model.OptimizationRequest;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import com.eqixiac.equinix.design.optimizer.model.WorkloadSpec;
import com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BackboneTopology;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BandwidthStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.ConnectionPurpose;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.GatewayPackageCode;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
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
 * a small hand-built {@link OptimizationResult} (three metros, two providers, three workloads) into the
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

    // Named MetroId constants, for readability and for asserting against.
    //
    // DeploymentTopology.forMetro() matches on VALUE, not reference identity (MetroId is a value type
    // whose factories allocate per call and never intern; DeploymentTopologyTest pins that contract).
    // Sharing an instance between a recommendation and its placements is therefore NOT required — and
    // it is deliberately avoided in the fixtures below, which build their placements with
    // distinctButEqual(...). An earlier revision of this file instructed maintainers to share the
    // instances "because forMetro matches on reference identity"; that was the very misunderstanding
    // that shipped `==` into forMetro and made the wizard silently fall back to its 1000 Mbps default
    // against live data, where the recommendation's id and the placement's id are never the same
    // object. Keep the fixtures using non-identical ids so a return to identity matching fails here.
    private static final MetroId DC = MetroId.of(MetroCode.DC);
    private static final MetroId DA = MetroId.of(MetroCode.DA);
    private static final MetroId SV = MetroId.of(MetroCode.SV);

    /**
     * A fresh {@link MetroId} equal to — but deliberately not the same object as — the given one, so
     * the wizard has to resolve placements by value the way it must against live data. The
     * assertions guard the premise: if {@code MetroId.of} ever starts interning, this fails loudly
     * rather than letting the coverage below quietly become vacuous.
     */
    private static MetroId distinctButEqual(MetroId metro) {
        MetroId copy = MetroId.of(metro.code());
        assertNotSame(metro, copy, "MetroId.of must not intern, or these fixtures prove nothing");
        assertEquals(metro, copy);
        return copy;
    }

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
                // IP_VC (also the new default): every planned connection has a Cloud Router A-side,
                // which Fabric accepts only as IP_VC (FCR A-side => IP_VC fix).
                .providerConnectionType(ConnectionType.IP_VC)
                .backboneBandwidthMbps(10_000)
                .backboneTopology(BackboneTopology.FULL_MESH)
                .bandwidthStrategy(strategy)
                .customerAsn(65100L)
                .withBFD(true, 300)
                .notifications("noc@example.com")
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
        assertTrue(routers.stream().allMatch(r -> r.getPackageCode() == GatewayPackageCode.STANDARD));
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
        assertEquals(ConnectionType.IP_VC, aws.getConnectionType());
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
                                        ProviderAvailability.builder()
                                                .providerLabel("AWS")
                                                .available(true)
                                                .sellerRegions(List.of("us-east-1"))
                                                .serviceProfileUuid("sp-aws")
                                                .build()))
                                .assignedWorkloads(List.of(WorkloadPlacement.builder()
                                        .workloadLabel("ML Training")
                                        .assignedMetro(distinctButEqual(DC))
                                        .reasoning("primary")
                                        .build()))
                                .build()))
                .topology(new DeploymentTopology(List.of(WorkloadPlacement.builder()
                        .workloadLabel("ML Training")
                        .assignedMetro(distinctButEqual(DC))
                        .reasoning("primary")
                        .build())))
                .computedAt(Instant.now())
                .computeTimeMs(10)
                .build();

        DeploymentPlan plan = planFor(result, BandwidthStrategy.PER_WORKLOAD);

        assertEquals(1, plan.getCloudRouters().size());
        assertEquals(1, plan.getProviderConnections().size());
        assertTrue(plan.getBackboneLinks().isEmpty(), "no inter-metro links with a single metro");
    }

    // ── customBandwidthMap / CUSTOM bandwidth strategy ──

    @Test
    @DisplayName("customBandwidthMap() overrides per-connection bandwidth by '{metro}-{provider}' key and forces CUSTOM strategy")
    void customBandwidthMapOverridesConnectionBandwidth() {
        FabricGateway fabric = mock(FabricGateway.class);

        // Keys are "{metroId}-{providerLabel}". DC-AWS is mapped; DA-AZURE is deliberately NOT,
        // to exercise the documented fallback for unmapped connections.
        DeploymentPlan plan = DeploymentWizard.builder(fabric, threeMetroResult())
                .routerPackage("STANDARD")
                .routerNamePrefix("FCR")
                .customBandwidthMap(Map.of("DC-AWS", 7000))
                .notifications("noc@example.com")
                .rateCard(emptyRateCard())
                .plan();

        PlannedConnection aws = named(plan.getProviderConnections(), "FCR-DC-to-aws");
        assertEquals(7000, aws.getBandwidthMbps(),
                "the mapped DC-AWS override must replace the 10000 Mbps workload-derived size");
        assertNotNull(aws.getBandwidthAllocation());
        assertEquals(7000, aws.getBandwidthAllocation().getTotalMbps());
        assertEquals(Map.of("custom", 7000), aws.getBandwidthAllocation().getPerWorkload());
        assertTrue(aws.getBandwidthAllocation().getReasoning().contains("Custom bandwidth map"),
                aws.getBandwidthAllocation().getReasoning());

        PlannedConnection azure = named(plan.getProviderConnections(), "FCR-DA-to-azure");
        assertEquals(4000, azure.getBandwidthMbps(),
                "an unmapped (metro, provider) under CUSTOM falls back to the documented per-workload "
                        + "aggregation (the 4000 Mbps Analytics workload at DA) — never a fabricated 1000 Mbps");
    }

    @Test
    @DisplayName("the customBandwidthMap javadoc contract: an unmatched key is sized by per-workload aggregation, and says so")
    void customBandwidthMapUnmatchedKeyFallsBackToPerWorkloadAggregation() {
        FabricGateway fabric = mock(FabricGateway.class);

        // A map whose only key matches NOTHING in the plan: every connection takes the documented
        // fallback — the normal per-workload aggregation — exactly as Builder#customBandwidthMap
        // promises ("sized by the normal per-workload aggregation instead of a fabricated default").
        DeploymentPlan plan = DeploymentWizard.builder(fabric, threeMetroResult())
                .customBandwidthMap(Map.of("SG-GCP", 2000))
                .notifications("noc@example.com")
                .rateCard(emptyRateCard())
                .plan();

        PlannedConnection aws = named(plan.getProviderConnections(), "FCR-DC-to-aws");
        assertEquals(10_000, aws.getBandwidthMbps(),
                "8000 (ML Training) + 2000 (DR Backup) — the per-workload aggregation, not 1000");
        assertEquals(Map.of("ML Training", 8000, "DR Backup", 2000),
                aws.getBandwidthAllocation().getPerWorkload(),
                "the fallback carries the real per-workload breakdown, not a fake 'custom' entry");
        assertTrue(aws.getBandwidthAllocation().getReasoning().contains("no bandwidth-map key matched")
                        && aws.getBandwidthAllocation().getReasoning().contains("per-workload"),
                "the reasoning states the fallback plainly: " + aws.getBandwidthAllocation().getReasoning());

        PlannedConnection azure = named(plan.getProviderConnections(), "FCR-DA-to-azure");
        assertEquals(4000, azure.getBandwidthMbps(), "the AZURE-dependent Analytics workload at DA");
    }

    @Test
    @DisplayName("customBandwidthMap() rejects a null map and null values with a message naming the key")
    void customBandwidthMapRejectsNulls() {
        FabricGateway fabric = mock(FabricGateway.class);

        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult()).customBandwidthMap(null));

        Map<String, Integer> withNullValue = new java.util.HashMap<>();
        withNullValue.put("DC-AWS", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult()).customBandwidthMap(withNullValue));
        assertTrue(ex.getMessage().contains("DC-AWS"),
                "the rejection names the offending key: " + ex.getMessage());
    }

    @Test
    @DisplayName("customBandwidthMap() implicitly switches the bandwidth strategy to CUSTOM")
    void customBandwidthMapForcesCustomStrategy() {
        FabricGateway fabric = mock(FabricGateway.class);

        // bandwidthStrategy(PER_WORKLOAD) is set BEFORE customBandwidthMap; the map must win.
        DeploymentPlan plan = DeploymentWizard.builder(fabric, threeMetroResult())
                .bandwidthStrategy(BandwidthStrategy.PER_WORKLOAD)
                .customBandwidthMap(Map.of("DC-AWS", 2500))
                .notifications("noc@example.com")
                .rateCard(emptyRateCard())
                .plan();

        assertEquals(2500, named(plan.getProviderConnections(), "FCR-DC-to-aws").getBandwidthMbps(),
                "the custom map applies even when PER_WORKLOAD was previously selected");
    }

    // ── connection types: FCR A-side => IP_VC ──

    @Test
    @DisplayName("connection types default to IP_VC — the only type Fabric accepts on a Cloud Router A-side")
    void connectionTypesDefaultToIpVc() {
        FabricGateway fabric = mock(FabricGateway.class);
        DeploymentPlan plan = DeploymentWizard.builder(fabric, threeMetroResult())
                .notifications("noc@example.com")
                .rateCard(emptyRateCard())
                .plan();

        assertTrue(plan.getProviderConnections().stream()
                        .allMatch(c -> c.getConnectionType() == ConnectionType.IP_VC),
                "provider connections default to IP_VC");
        assertTrue(plan.getBackboneLinks().stream()
                        .allMatch(l -> l.getConnection().getConnectionType() == ConnectionType.IP_VC),
                "backbone links default to IP_VC");
        assertTrue(plan.isValid(), () -> plan.getValidationErrors().toString());
    }

    @Test
    @DisplayName("an explicitly configured EVPL_VC is a Layer-1 validation error naming the FCR A-side => IP_VC rule")
    void explicitEvplVcIsALayer1Error() {
        FabricGateway fabric = mock(FabricGateway.class);
        DeploymentPlan plan = DeploymentWizard.builder(fabric, threeMetroResult())
                .providerConnectionType(ConnectionType.EVPL_VC)
                .backboneConnectionType(ConnectionType.EVPL_VC)
                .notifications("noc@example.com")
                .rateCard(emptyRateCard())
                .plan();

        assertFalse(plan.isValid(), "an FCR A-side with a port-based type is a hard plan error");
        assertTrue(plan.getValidationErrors().stream()
                        .anyMatch(e -> e.contains("FCR A-side => IP_VC") && e.contains("EVPL_VC")),
                () -> "expected the named rule in the errors: " + plan.getValidationErrors());
    }

    // ── RING topology ──

    @Test
    @DisplayName("a 2-metro RING degenerates to ONE link (like FULL_MESH), never a doubly-billed (0,1)+(1,0) pair")
    void twoMetroRingDegeneratesToSingleLink() {
        FabricGateway fabric = mock(FabricGateway.class);
        DeploymentPlan plan = DeploymentWizard.builder(fabric, twoMetroResultOf())
                .backboneTopology(BackboneTopology.RING)
                .notifications("noc@example.com")
                .rateCard(emptyRateCard())
                .plan();

        assertEquals(1, plan.getBackboneLinks().size(),
                "two metros have exactly one distinct pair — a second link would double the bill: "
                        + plan.getBackboneLinks());
        PlannedBackboneLink only = plan.getBackboneLinks().get(0);
        assertEquals(DC, only.getMetroA());
        assertEquals(DA, only.getMetroZ());

        // Three metros remain a genuine ring: A-B, B-C, C-A.
        DeploymentPlan three = DeploymentWizard.builder(fabric, threeMetroResult())
                .backboneTopology(BackboneTopology.RING)
                .notifications("noc@example.com")
                .rateCard(emptyRateCard())
                .plan();
        assertEquals(3, three.getBackboneLinks().size(), "a 3-metro ring keeps its 3 links");
    }

    // ── notifications: every recipient reaches every planned resource ──

    @Test
    @DisplayName("notifications(a, b) stamps the FULL recipient list onto every router, provider connection and backbone link")
    void allNotificationEmailsReachEveryPlannedResource() {
        FabricGateway fabric = mock(FabricGateway.class);
        List<String> expected = List.of("noc@example.com", "neteng@example.com");
        DeploymentPlan plan = DeploymentWizard.builder(fabric, threeMetroResult())
                .notifications("noc@example.com", "neteng@example.com")
                .rateCard(emptyRateCard())
                .plan();

        assertTrue(plan.getCloudRouters().stream()
                        .allMatch(r -> expected.equals(r.getNotificationEmails())),
                "every Cloud Router carries both recipients: " + plan.getCloudRouters());
        assertTrue(plan.getProviderConnections().stream()
                        .allMatch(c -> expected.equals(c.getNotificationEmails())),
                "every provider connection carries both recipients");
        assertTrue(plan.getBackboneLinks().stream()
                        .allMatch(l -> expected.equals(l.getConnection().getNotificationEmails())),
                "every backbone link's connection carries both recipients");
    }

    @Test
    @DisplayName("notifications() rejects null/blank addresses")
    void notificationsRejectBlankAddresses() {
        FabricGateway fabric = mock(FabricGateway.class);
        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult()).notifications("  "));
        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult()).notifications((String) null));
    }

    // ── subnet allocation: plan-scoped, never static ──

    @Test
    @DisplayName("concurrent plan() calls never corrupt each other's /30 allocation (allocation is plan-scoped, not static)")
    void concurrentPlansGetIndependentSubnetSequences() throws Exception {
        // With the old STATIC counter, interleaved generatePlan calls consumed each other's indexes,
        // so at least one plan came out with gaps/duplicates relative to the canonical sequence.
        OptimizationResult result = threeMetroResult();
        List<String> expected = planFor(result, BandwidthStrategy.PER_WORKLOAD).getRoutingProtocols().stream()
                .filter(p -> p.getType() == RoutingProtocolType.BGP)
                .map(PlannedRoutingProtocol::getEquinixPeerIpv4)
                .toList();
        assertEquals(List.of("10.100.0.1/30", "10.100.1.1/30", "10.100.2.1/30",
                        "10.100.3.1/30", "10.100.4.1/30"), expected,
                "the canonical single-threaded sequence (5 connections)");

        int threads = 8;
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(threads);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        try {
            List<java.util.concurrent.Future<List<String>>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    barrier.await();
                    return planFor(result, BandwidthStrategy.PER_WORKLOAD).getRoutingProtocols().stream()
                            .filter(p -> p.getType() == RoutingProtocolType.BGP)
                            .map(PlannedRoutingProtocol::getEquinixPeerIpv4)
                            .toList();
                }));
            }
            for (java.util.concurrent.Future<List<String>> future : futures) {
                assertEquals(expected, future.get(30, java.util.concurrent.TimeUnit.SECONDS),
                        "every concurrently-generated plan gets the full, uncorrupted sequence");
            }
        }
        finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("subnetBase() moves the allocation base so plans landing in one project can use disjoint ranges")
    void subnetBaseLeverMovesAllocation() {
        FabricGateway fabric = mock(FabricGateway.class);
        DeploymentPlan plan = DeploymentWizard.builder(fabric, threeMetroResult())
                .subnetBase("10.200.0.0")
                .notifications("noc@example.com")
                .rateCard(emptyRateCard())
                .plan();

        assertTrue(plan.getRoutingProtocols().stream()
                        .filter(p -> p.getType() == RoutingProtocolType.BGP)
                        .allMatch(p -> p.getEquinixPeerIpv4().startsWith("10.200.")),
                "all peering subnets come from the configured base: " + plan.getRoutingProtocols());

        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult()).subnetBase("not-an-ip"));
        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult()).subnetBase("10.999.0.0"));
    }

    // ── mixed-currency pricing: per-category reconciliation, no fabricated sums, no hardcoded '$' ──

    @Test
    @DisplayName("a mixed-currency plan reconciles per CATEGORY: single-currency categories keep figure+currency, mixed ones are nulled with per-currency subtotals")
    void mixedCurrencyPricingReconcilesPerCategory() {
        // Routers price in USD everywhere; connections price in EUR at DA and USD elsewhere. The
        // provider-connection category (DC=USD, DA=EUR) and the backbone category (links priced at
        // metro-A: DC, DC, DA) are therefore MIXED, while the router category stays single-currency.
        FabricGateway fabric = mock(FabricGateway.class);
        RateCard perMetroCurrencyCard = new RateCard() {
            @Override
            public Optional<com.eqixiac.equinix.design.value.ratecard.PriceQuote> connection(
                    ConnectionType type, int bandwidthMbps, MetroCode metro,
                    com.eqixiac.equinix.design.value.ratecard.Term term) {
                boolean eur = metro == MetroCode.DA;
                return Optional.of(com.eqixiac.equinix.design.value.ratecard.PriceQuote.of(
                        java.math.BigDecimal.valueOf(eur ? 400 : 500), java.math.BigDecimal.ZERO,
                        java.util.Currency.getInstance(eur ? "EUR" : "USD"),
                        com.eqixiac.equinix.design.value.ratecard.PriceSource.EQUINIX_LIVE));
            }

            @Override
            public Optional<com.eqixiac.equinix.design.value.ratecard.PriceQuote> cloudRouter(
                    String packageCode, MetroCode metro,
                    com.eqixiac.equinix.design.value.ratecard.Term term) {
                return Optional.of(com.eqixiac.equinix.design.value.ratecard.PriceQuote.of(
                        java.math.BigDecimal.valueOf(300), java.math.BigDecimal.ZERO,
                        java.util.Currency.getInstance("USD"),
                        com.eqixiac.equinix.design.value.ratecard.PriceSource.EQUINIX_LIVE));
            }

            @Override
            public com.eqixiac.equinix.design.value.ratecard.PriceSource source() {
                return com.eqixiac.equinix.design.value.ratecard.PriceSource.EQUINIX_LIVE;
            }
        };

        DeploymentPlan plan = DeploymentWizard.builder(fabric, threeMetroResult())
                .notifications("noc@example.com")
                .rateCard(perMetroCurrencyCard)
                .plan();
        var pricing = plan.getPricing();

        // No fabricated plan-wide total or currency.
        assertNull(pricing.getMonthlyTotal(), "no single total across USD and EUR");
        assertNull(pricing.getCurrency());

        // Router category: single-currency — figure kept WITH its currency.
        assertEquals(0, java.math.BigDecimal.valueOf(900).compareTo(pricing.getRouterMonthlyCost()),
                "3 routers x USD 300");
        assertEquals("USD", pricing.getRouterCurrency());

        // Provider category: DC=USD 500 + DA=EUR 400 — a raw sum of 900 would be a fabricated
        // cross-currency figure, so the single figure is nulled and the subtotals carry the truth.
        assertNull(pricing.getProviderConnectionMonthlyCost(),
                "a mixed category must not report a raw cross-currency sum");
        assertEquals(0, java.math.BigDecimal.valueOf(500)
                .compareTo(pricing.getProviderConnectionMonthlyByCurrency().get("USD")));
        assertEquals(0, java.math.BigDecimal.valueOf(400)
                .compareTo(pricing.getProviderConnectionMonthlyByCurrency().get("EUR")));

        // Backbone category (metro-A pricing: DC, DC, DA) is mixed too.
        assertNull(pricing.getBackboneMonthlyCost());

        // Rendering: actual currency codes/symbols, never a hardcoded '$' beside a mixed figure.
        String md = plan.toMarkdown();
        assertTrue(md.contains("| Cloud Routers | $900 |"),
                "the single-currency router figure renders with its real (USD) symbol: " + md);
        assertTrue(md.contains("| Provider Connections | ")
                        && md.contains("USD 500") && md.contains("EUR 400"),
                "the mixed provider category renders per-currency subtotals: " + md);
        assertFalse(md.contains("| Provider Connections | $"),
                "no hardcoded '$' next to a mixed category: " + md);
        assertTrue(md.contains("multiple currencies"), "the total row calls out the mix: " + md);

        String summary = plan.toSummary();
        assertTrue(summary.contains("spans multiple currencies")
                        && summary.contains("USD") && summary.contains("EUR"),
                "the summary names the per-currency subtotals instead of a fabricated total: " + summary);
    }

    // ── routerPackage validation (plan-time, never execute-time) ──

    @Test
    @DisplayName("routerPackage(String) is lenient: lowercase and padded codes resolve to the enum at plan time")
    void routerPackageLenientResolution() {
        FabricGateway fabric = mock(FabricGateway.class);

        DeploymentPlan lowercase = DeploymentWizard.builder(fabric, threeMetroResult())
                .routerPackage("standard")
                .rateCard(emptyRateCard())
                .plan();
        assertTrue(lowercase.getCloudRouters().stream()
                        .allMatch(r -> r.getPackageCode() == GatewayPackageCode.STANDARD),
                "lowercase 'standard' must resolve to GatewayPackageCode.STANDARD at plan time");

        DeploymentPlan padded = DeploymentWizard.builder(fabric, threeMetroResult())
                .routerPackage("  Premium ")
                .rateCard(emptyRateCard())
                .plan();
        assertTrue(padded.getCloudRouters().stream()
                .allMatch(r -> r.getPackageCode() == GatewayPackageCode.PREMIUM));
    }

    @Test
    @DisplayName("a garbage package code fails fast at plan time with a message naming the valid codes")
    void routerPackageGarbageFailsFast() {
        FabricGateway fabric = mock(FabricGateway.class);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult()).routerPackage("TURBO"));
        assertTrue(ex.getMessage().contains("TURBO"), "the message names the offending code");
        assertTrue(ex.getMessage().contains("LAB, BASIC, STANDARD, ADVANCED, PREMIUM"),
                "the message lists every valid code (and never offers UNKNOWN): " + ex.getMessage());

        // UNKNOWN is a deserialization placeholder, never a deployable tier.
        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult()).routerPackage("UNKNOWN"));
        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult()).routerPackage("unknown"));
        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult()).routerPackage((String) null));
    }

    @Test
    @DisplayName("the typed routerPackage(GatewayPackageCode) overload accepts real tiers and rejects null/UNKNOWN")
    void routerPackageTypedOverload() {
        FabricGateway fabric = mock(FabricGateway.class);

        DeploymentPlan plan = DeploymentWizard.builder(fabric, threeMetroResult())
                .routerPackage(GatewayPackageCode.ADVANCED)
                .rateCard(emptyRateCard())
                .plan();
        assertTrue(plan.getCloudRouters().stream()
                .allMatch(r -> r.getPackageCode() == GatewayPackageCode.ADVANCED));

        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult())
                        .routerPackage(GatewayPackageCode.UNKNOWN));
        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(fabric, threeMetroResult())
                        .routerPackage((GatewayPackageCode) null));
    }

    // ── Helpers ──

    private static PlannedConnection named(List<PlannedConnection> conns, String name) {
        return conns.stream().filter(c -> c.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("no connection named " + name + " in " + conns));
    }

    /**
     * A three-metro optimization: DC (AWS, two AWS-dependent workloads), DA (AZURE, one AZURE-dependent
     * workload), SV (no available providers). The topology places each workload at its metro using a
     * {@link #distinctButEqual} id, never the instance the recommendation carries, so the wizard's
     * {@code forMetro} lookup has to match on value — the live shape, where
     * {@code MetroRecommendation.getMetroId()} and the placement's id are separate objects.
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
                WorkloadPlacement.builder()
                        .workloadLabel("ML Training")
                        .assignedMetro(distinctButEqual(DC))
                        .reasoning("AWS at DC")
                        .build(),
                WorkloadPlacement.builder()
                        .workloadLabel("DR Backup")
                        .assignedMetro(distinctButEqual(DC))
                        .reasoning("AWS at DC")
                        .build(),
                WorkloadPlacement.builder()
                        .workloadLabel("Analytics")
                        .assignedMetro(distinctButEqual(DA))
                        .reasoning("AZURE at DA")
                        .build()));

        MetroRecommendation dc = MetroRecommendation.builder()
                .rank(1).metroId(DC).metroName("Ashburn").score(score).reasons(List.of("Primary"))
                .availableProviders(List.of(
                        ProviderAvailability.builder()
                                .providerLabel("AWS")
                                .available(true)
                                .sellerRegions(List.of("us-east-1"))
                                .serviceProfileUuid("sp-aws")
                                .build()))
                .build();
        MetroRecommendation da = MetroRecommendation.builder()
                .rank(2).metroId(DA).metroName("Dallas").score(score).reasons(List.of("Secondary"))
                .availableProviders(List.of(
                        ProviderAvailability.builder()
                                .providerLabel("AZURE")
                                .available(true)
                                .sellerRegions(List.of("eastus"))
                                .serviceProfileUuid("sp-azure")
                                .build()))
                .build();
        MetroRecommendation sv = MetroRecommendation.builder()
                .rank(3).metroId(SV).metroName("Silicon Valley").score(score).reasons(List.of("Tertiary"))
                .availableProviders(List.of(
                        // Present but not available -> filtered out, so SV gets no provider connection.
                        ProviderAvailability.builder()
                                .providerLabel("GCP")
                                .available(false)
                                .sellerRegions(List.of("us-west1"))
                                .serviceProfileUuid("sp-gcp")
                                .build()))
                .build();

        return OptimizationResult.builder()
                .request(request)
                .recommendations(List.of(dc, da, sv))
                .topology(topology)
                .computedAt(Instant.now())
                .computeTimeMs(42)
                .build();
    }

    private static com.eqixiac.equinix.design.optimizer.model.ProviderRequirement providerReq(String label) {
        return com.eqixiac.equinix.design.optimizer.model.ProviderRequirement.builder().label(label).build();
    }

    /** A minimal two-metro result (DC, DA — no providers) for topology-shape tests. */
    private static OptimizationResult twoMetroResultOf() {
        MetroScore score = new MetroScore(90.0, Collections.emptyList());
        return OptimizationResult.builder()
                .recommendations(List.of(
                        MetroRecommendation.builder()
                                .rank(1).metroId(DC).metroName("Ashburn").score(score)
                                .reasons(List.of("Primary")).availableProviders(Collections.emptyList())
                                .build(),
                        MetroRecommendation.builder()
                                .rank(2).metroId(DA).metroName("Dallas").score(score)
                                .reasons(List.of("Secondary")).availableProviders(Collections.emptyList())
                                .build()))
                .computedAt(Instant.now())
                .computeTimeMs(1)
                .build();
    }
}
