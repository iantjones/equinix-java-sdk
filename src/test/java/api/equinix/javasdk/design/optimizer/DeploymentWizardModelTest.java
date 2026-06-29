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

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.MetroScore;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology;
import api.equinix.javasdk.design.optimizer.wizard.enums.BandwidthStrategy;
import api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose;
import api.equinix.javasdk.design.optimizer.wizard.model.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Deployment Wizard models: DeploymentPlan, PlannedCloudRouter,
 * PlannedConnection, PlannedBackboneLink, DeploymentOutcome, and related value objects.
 */
@DisplayName("Deployment Wizard Models")
class DeploymentWizardModelTest {

    @Nested
    @DisplayName("PlannedCloudRouter")
    class PlannedCloudRouterTests {

        @Test
        @DisplayName("Should build with all fields")
        void fullBuild() {
            PlannedCloudRouter cr = PlannedCloudRouter.builder()
                    .metroCode(MetroCode.DC)
                    .name("FCR-DC")
                    .packageCode("STANDARD")
                    .accountNumber(272010L)
                    .projectId("proj-uuid-123")
                    .notificationEmail("noc@example.com")
                    .build();

            assertEquals(MetroCode.DC, cr.getMetroCode());
            assertEquals("FCR-DC", cr.getName());
            assertEquals("STANDARD", cr.getPackageCode());
            assertEquals(272010L, cr.getAccountNumber());
            assertEquals("proj-uuid-123", cr.getProjectId());
        }

        @Test
        @DisplayName("Optional fields should be null when not set")
        void optionalFieldsNull() {
            PlannedCloudRouter cr = PlannedCloudRouter.builder()
                    .metroCode(MetroCode.DA)
                    .name("FCR-DA")
                    .packageCode("STANDARD")
                    .build();

            assertNull(cr.getAccountNumber());
            assertNull(cr.getProjectId());
            assertNull(cr.getNotificationEmail());
        }
    }

    @Nested
    @DisplayName("PlannedConnection")
    class PlannedConnectionTests {

        @Test
        @DisplayName("Provider connection should identify correctly")
        void providerConnection() {
            PlannedConnection conn = PlannedConnection.builder()
                    .name("FCR-DC-to-AWS")
                    .connectionType(ConnectionType.EVPL_VC)
                    .purpose(ConnectionPurpose.PROVIDER)
                    .bandwidthMbps(1000)
                    .aSideMetro(MetroCode.DC)
                    .aSideRouterName("FCR-DC")
                    .zSideServiceProfileUuid("sp-uuid-aws")
                    .zSideProviderLabel("AWS Direct Connect")
                    .zSideSellerRegion("us-east-1")
                    .build();

            assertTrue(conn.isProviderConnection());
            assertFalse(conn.isBackboneLink());
            assertEquals(MetroCode.DC, conn.getASideMetro());
            assertEquals("us-east-1", conn.getZSideSellerRegion());
        }

        @Test
        @DisplayName("Backbone connection should identify correctly")
        void backboneConnection() {
            PlannedConnection conn = PlannedConnection.builder()
                    .name("Backbone-DC-DA")
                    .connectionType(ConnectionType.EVPL_VC)
                    .purpose(ConnectionPurpose.BACKBONE)
                    .bandwidthMbps(10000)
                    .aSideMetro(MetroCode.DC)
                    .aSideRouterName("FCR-DC")
                    .zSideMetro(MetroCode.DA)
                    .zSideRouterName("FCR-DA")
                    .build();

            assertFalse(conn.isProviderConnection());
            assertTrue(conn.isBackboneLink());
        }

        @Test
        @DisplayName("Bandwidth allocation should carry per-workload breakdown")
        void bandwidthAllocation() {
            Map<String, Integer> perWorkload = new LinkedHashMap<>();
            perWorkload.put("ML Training", 8000);
            perWorkload.put("DR Backup", 2000);

            BandwidthAllocation allocation = BandwidthAllocation.builder()
                    .totalMbps(10000)
                    .perWorkload(perWorkload)
                    .reasoning("Sum of dependent workloads: ML Training (8000) + DR Backup (2000)")
                    .build();

            PlannedConnection conn = PlannedConnection.builder()
                    .name("FCR-DC-to-AWS")
                    .connectionType(ConnectionType.EVPL_VC)
                    .purpose(ConnectionPurpose.PROVIDER)
                    .bandwidthMbps(10000)
                    .bandwidthAllocation(allocation)
                    .aSideMetro(MetroCode.DC)
                    .aSideRouterName("FCR-DC")
                    .build();

            assertNotNull(conn.getBandwidthAllocation());
            assertEquals(10000, conn.getBandwidthAllocation().getTotalMbps());
            assertEquals(2, conn.getBandwidthAllocation().getPerWorkload().size());
            assertEquals(8000, conn.getBandwidthAllocation().getPerWorkload().get("ML Training"));
        }
    }

    @Nested
    @DisplayName("PlannedBackboneLink")
    class PlannedBackboneLinkTests {

        @Test
        @DisplayName("Full mesh link should carry topology type")
        void fullMeshLink() {
            PlannedConnection conn = PlannedConnection.builder()
                    .name("Backbone-DC-DA")
                    .connectionType(ConnectionType.EVPL_VC)
                    .purpose(ConnectionPurpose.BACKBONE)
                    .bandwidthMbps(10000)
                    .aSideMetro(MetroCode.DC)
                    .aSideRouterName("FCR-DC")
                    .zSideMetro(MetroCode.DA)
                    .zSideRouterName("FCR-DA")
                    .build();

            PlannedBackboneLink link = PlannedBackboneLink.builder()
                    .metroA(MetroCode.DC)
                    .metroZ(MetroCode.DA)
                    .name("Backbone-DC-DA")
                    .bandwidthMbps(10000)
                    .topology(BackboneTopology.FULL_MESH)
                    .connection(conn)
                    .build();

            assertEquals(MetroCode.DC, link.getMetroA());
            assertEquals(MetroCode.DA, link.getMetroZ());
            assertEquals(BackboneTopology.FULL_MESH, link.getTopology());
            assertNotNull(link.getConnection());
        }
    }

    @Nested
    @DisplayName("PlanPricing")
    class PlanPricingTests {

        @Test
        @DisplayName("Should aggregate costs correctly")
        void costAggregation() {
            PlanPricing pricing = PlanPricing.builder()
                    .routerMonthlyCost(new BigDecimal("500.00"))
                    .providerConnectionMonthlyCost(new BigDecimal("1500.00"))
                    .backboneMonthlyCost(new BigDecimal("800.00"))
                    .monthlyTotal(new BigDecimal("2800.00"))
                    .setupTotal(new BigDecimal("0.00"))
                    .currency("USD")
                    .perConnectionCost(new LinkedHashMap<>())
                    .build();

            assertEquals(new BigDecimal("2800.00"), pricing.getMonthlyTotal());
            assertEquals("USD", pricing.getCurrency());
        }

        @Test
        @DisplayName("Disclaimer should have default value")
        void defaultDisclaimer() {
            PlanPricing pricing = PlanPricing.builder()
                    .monthlyTotal(BigDecimal.ZERO)
                    .build();

            assertNotNull(pricing.getDisclaimer());
            assertTrue(pricing.getDisclaimer().contains("Estimates"));
        }
    }

    @Nested
    @DisplayName("DeploymentPlan")
    class DeploymentPlanTests {

        private DeploymentPlan plan;

        @BeforeEach
        void build() {
            OptimizationResult optResult = buildMinimalOptResult();

            List<PlannedCloudRouter> routers = Arrays.asList(
                    PlannedCloudRouter.builder().metroCode(MetroCode.DC).name("FCR-DC").packageCode("STANDARD").build(),
                    PlannedCloudRouter.builder().metroCode(MetroCode.DA).name("FCR-DA").packageCode("STANDARD").build()
            );

            List<PlannedConnection> connections = Arrays.asList(
                    PlannedConnection.builder()
                            .name("FCR-DC-to-AWS").connectionType(ConnectionType.EVPL_VC)
                            .purpose(ConnectionPurpose.PROVIDER).bandwidthMbps(1000)
                            .aSideMetro(MetroCode.DC).aSideRouterName("FCR-DC")
                            .zSideProviderLabel("AWS").build()
            );

            PlannedConnection bbConn = PlannedConnection.builder()
                    .name("Backbone-DC-DA").connectionType(ConnectionType.EVPL_VC)
                    .purpose(ConnectionPurpose.BACKBONE).bandwidthMbps(10000)
                    .aSideMetro(MetroCode.DC).aSideRouterName("FCR-DC")
                    .zSideMetro(MetroCode.DA).zSideRouterName("FCR-DA")
                    .build();

            List<PlannedBackboneLink> backbones = Collections.singletonList(
                    PlannedBackboneLink.builder()
                            .metroA(MetroCode.DC).metroZ(MetroCode.DA)
                            .name("Backbone-DC-DA").bandwidthMbps(10000)
                            .topology(BackboneTopology.FULL_MESH)
                            .connection(bbConn).build()
            );

            List<PlannedRoutingProtocol> protocols = Arrays.asList(
                    PlannedRoutingProtocol.builder()
                            .name("FCR-DC-to-AWS-direct").connectionName("FCR-DC-to-AWS")
                            .type(RoutingProtocolType.DIRECT)
                            .equinixIfaceIpv4("10.100.0.1/30")
                            .build(),
                    PlannedRoutingProtocol.builder()
                            .name("FCR-DC-to-AWS-bgp").connectionName("FCR-DC-to-AWS")
                            .type(RoutingProtocolType.BGP)
                            .customerAsn(65100L)
                            .customerPeerIpv4("10.100.0.2/30").equinixPeerIpv4("10.100.0.1/30")
                            .bfdEnabled(true).bfdInterval(300)
                            .build()
            );

            PlanPricing pricing = PlanPricing.builder()
                    .routerMonthlyCost(new BigDecimal("500.00"))
                    .providerConnectionMonthlyCost(new BigDecimal("300.00"))
                    .backboneMonthlyCost(new BigDecimal("800.00"))
                    .monthlyTotal(new BigDecimal("1600.00"))
                    .setupTotal(BigDecimal.ZERO)
                    .build();

            plan = DeploymentPlan.builder()
                    .sourceOptimization(optResult)
                    .cloudRouters(routers)
                    .providerConnections(connections)
                    .backboneLinks(backbones)
                    .routingProtocols(protocols)
                    .pricing(pricing)
                    .valid(true)
                    .validationErrors(Collections.emptyList())
                    .build();
        }

        @Test
        @DisplayName("totalResourceCount should sum all resource types")
        void totalResourceCount() {
            // 2 routers + 1 provider conn + 1 backbone + 2 routing protocols = 6
            assertEquals(6, plan.totalResourceCount());
        }

        @Test
        @DisplayName("toSummary should produce concise text")
        void toSummary() {
            String summary = plan.toSummary();
            assertNotNull(summary);
            assertTrue(summary.contains("2 Cloud Router(s)"));
            assertTrue(summary.contains("1 provider connection(s)"));
            assertTrue(summary.contains("1 backbone link(s)"));
            assertTrue(summary.contains("2 routing protocol(s)"));
            assertTrue(summary.contains("Total resources: 6"));
            assertTrue(summary.contains("$1600.00"));
        }

        @Test
        @DisplayName("toMarkdown should produce structured report")
        void toMarkdown() {
            String md = plan.toMarkdown();
            assertNotNull(md);
            assertTrue(md.contains("# Deployment Plan"));
            assertTrue(md.contains("## Cloud Routers"));
            assertTrue(md.contains("FCR-DC"));
            assertTrue(md.contains("FCR-DA"));
            assertTrue(md.contains("## Provider Connections"));
            assertTrue(md.contains("FCR-DC-to-AWS"));
            assertTrue(md.contains("## Backbone Links"));
            assertTrue(md.contains("Backbone-DC-DA"));
            assertTrue(md.contains("## Routing Protocols"));
            assertTrue(md.contains("## Cost Estimate"));
            assertTrue(md.contains("## Execution"));
        }

        @Test
        @DisplayName("Invalid plan should show validation errors in summary")
        void invalidPlan() {
            DeploymentPlan invalid = DeploymentPlan.builder()
                    .sourceOptimization(buildMinimalOptResult())
                    .cloudRouters(Collections.emptyList())
                    .providerConnections(Collections.emptyList())
                    .backboneLinks(Collections.emptyList())
                    .routingProtocols(Collections.emptyList())
                    .valid(false)
                    .validationErrors(Arrays.asList("Missing metro DC", "No providers"))
                    .build();

            String summary = invalid.toSummary();
            assertTrue(summary.contains("VALIDATION ERRORS: 2"));
        }
    }

    @Nested
    @DisplayName("DeploymentOutcome")
    class DeploymentOutcomeTests {

        @Test
        @DisplayName("Successful outcome should report correctly")
        void successful() {
            DeploymentPlan plan = DeploymentPlan.builder()
                    .sourceOptimization(buildMinimalOptResult())
                    .cloudRouters(Collections.singletonList(
                            PlannedCloudRouter.builder().metroCode(MetroCode.DC).name("FCR-DC").packageCode("STANDARD").build()))
                    .providerConnections(Collections.emptyList())
                    .backboneLinks(Collections.emptyList())
                    .routingProtocols(Collections.emptyList())
                    .valid(true)
                    .validationErrors(Collections.emptyList())
                    .build();

            DeploymentOutcome outcome = DeploymentOutcome.builder()
                    .plan(plan)
                    .resources(Collections.singletonList(
                            ProvisionedResource.builder()
                                    .resourceType("CloudRouter")
                                    .name("FCR-DC")
                                    .uuid("uuid-123")
                                    .metroCode(MetroCode.DC)
                                    .status("PROVISIONED")
                                    .build()))
                    .fullySuccessful(true)
                    .errors(Collections.emptyList())
                    .executionTimeMs(1500)
                    .build();

            assertTrue(outcome.isFullySuccessful());
            assertEquals(1, outcome.getResources().size());
            assertTrue(outcome.getErrors().isEmpty());

            String summary = outcome.toSummary();
            assertTrue(summary.contains("SUCCEEDED"));
            assertTrue(summary.contains("1/1"));
        }

        @Test
        @DisplayName("Partial failure outcome should report errors")
        void partialFailure() {
            DeploymentPlan plan = DeploymentPlan.builder()
                    .sourceOptimization(buildMinimalOptResult())
                    .cloudRouters(Arrays.asList(
                            PlannedCloudRouter.builder().metroCode(MetroCode.DC).name("FCR-DC").packageCode("STANDARD").build(),
                            PlannedCloudRouter.builder().metroCode(MetroCode.DA).name("FCR-DA").packageCode("STANDARD").build()))
                    .providerConnections(Collections.emptyList())
                    .backboneLinks(Collections.emptyList())
                    .routingProtocols(Collections.emptyList())
                    .valid(true)
                    .validationErrors(Collections.emptyList())
                    .build();

            DeploymentOutcome outcome = DeploymentOutcome.builder()
                    .plan(plan)
                    .resources(Collections.singletonList(
                            ProvisionedResource.builder()
                                    .resourceType("CloudRouter").name("FCR-DC")
                                    .uuid("uuid-123").metroCode(MetroCode.DC).status("PROVISIONED").build()))
                    .fullySuccessful(false)
                    .errors(Collections.singletonList(
                            ProvisioningError.builder()
                                    .resourceType("CloudRouter").resourceName("FCR-DA")
                                    .reason("Account limit exceeded").recoverable(true).build()))
                    .executionTimeMs(3000)
                    .build();

            assertFalse(outcome.isFullySuccessful());
            assertEquals(1, outcome.getResources().size());
            assertEquals(1, outcome.getErrors().size());

            String summary = outcome.toSummary();
            assertTrue(summary.contains("COMPLETED WITH ERRORS"));
            assertTrue(summary.contains("1/2"));
            assertTrue(summary.contains("1 error(s)"));

            String md = outcome.toMarkdown();
            assertTrue(md.contains("# Deployment Outcome"));
            assertTrue(md.contains("## Errors"));
            assertTrue(md.contains("Account limit exceeded"));
        }
    }

    @Nested
    @DisplayName("BandwidthAllocation")
    class BandwidthAllocationTests {

        @Test
        @DisplayName("Per-workload breakdown should sum to total")
        void perWorkloadSum() {
            Map<String, Integer> perWorkload = new LinkedHashMap<>();
            perWorkload.put("ML Training", 8000);
            perWorkload.put("Backup", 2000);

            BandwidthAllocation ba = BandwidthAllocation.builder()
                    .totalMbps(10000)
                    .perWorkload(perWorkload)
                    .reasoning("Sum of ML Training + Backup")
                    .build();

            int sum = ba.getPerWorkload().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(ba.getTotalMbps(), sum);
        }
    }

    @Nested
    @DisplayName("Backbone Topology enums")
    class TopologyEnumTests {

        @Test
        @DisplayName("Full mesh link count formula: N*(N-1)/2")
        void fullMeshCount() {
            int metros = 3;
            int links = metros * (metros - 1) / 2;
            assertEquals(3, links);
        }

        @Test
        @DisplayName("Hub-spoke link count formula: N-1")
        void hubSpokeCount() {
            int metros = 3;
            int links = metros - 1;
            assertEquals(2, links);
        }

        @Test
        @DisplayName("Ring link count formula: N")
        void ringCount() {
            int metros = 3;
            int links = metros;
            assertEquals(3, links);
        }

        @Test
        @DisplayName("All topology values should exist")
        void allValues() {
            assertEquals(3, BackboneTopology.values().length);
        }

        @Test
        @DisplayName("All bandwidth strategies should exist")
        void bandwidthStrategies() {
            assertEquals(3, BandwidthStrategy.values().length);
            assertNotNull(BandwidthStrategy.PER_WORKLOAD);
            assertNotNull(BandwidthStrategy.AGGREGATED);
            assertNotNull(BandwidthStrategy.CUSTOM);
        }

        @Test
        @DisplayName("All connection purposes should exist")
        void connectionPurposes() {
            assertNotNull(ConnectionPurpose.PROVIDER);
            assertNotNull(ConnectionPurpose.BACKBONE);
        }
    }

    // ---- Helpers ----

    private static OptimizationResult buildMinimalOptResult() {
        MetroScore score1 = new MetroScore(95.0, Collections.emptyList());
        MetroScore score2 = new MetroScore(82.0, Collections.emptyList());

        return OptimizationResult.builder()
                .recommendations(Arrays.asList(
                        MetroRecommendation.builder()
                                .rank(1).metroCode(MetroCode.DC).metroName("Ashburn")
                                .score(score1).reasons(Collections.singletonList("Primary metro"))
                                .build(),
                        MetroRecommendation.builder()
                                .rank(2).metroCode(MetroCode.DA).metroName("Dallas")
                                .score(score2).reasons(Collections.singletonList("Secondary metro"))
                                .build()))
                .computedAt(Instant.now())
                .computeTimeMs(200)
                .build();
    }
}
