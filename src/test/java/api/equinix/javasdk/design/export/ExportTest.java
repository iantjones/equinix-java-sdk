package api.equinix.javasdk.design.export;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.design.optimizer.model.DeploymentTopology;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.MetroScore;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.WorkloadPlacement;
import api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology;
import api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose;
import api.equinix.javasdk.design.optimizer.wizard.model.BandwidthAllocation;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedBackboneLink;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedCloudRouter;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedConnection;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedRoutingProtocol;
import api.equinix.javasdk.design.optimizer.wizard.model.PlanPricing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the IaC export and topology-diagram generators:
 * {@link TerraformExporter} and {@link TopologyDiagram}.
 */
@DisplayName("IaC Export + Topology Diagrams")
class ExportTest {

    private DeploymentPlan plan;

    @BeforeEach
    void buildPlan() {
        List<PlannedCloudRouter> routers = Arrays.asList(
                PlannedCloudRouter.builder()
                        .metroId(MetroId.of(MetroCode.DC)).name("FCR-DC").packageCode("STANDARD")
                        .accountNumber(272010L).projectId("proj-uuid-123")
                        .notificationEmail("noc@example.com")
                        .build(),
                PlannedCloudRouter.builder()
                        .metroId(MetroId.of(MetroCode.DA)).name("FCR-DA").packageCode("STANDARD")
                        .build()
        );

        Map<String, Integer> perWorkload = new LinkedHashMap<>();
        perWorkload.put("ML Training", 800);
        perWorkload.put("Backup", 200);
        BandwidthAllocation allocation = BandwidthAllocation.builder()
                .totalMbps(1000).perWorkload(perWorkload).reasoning("sum").build();

        List<PlannedConnection> providerConnections = Collections.singletonList(
                PlannedConnection.builder()
                        .name("FCR-DC-to-AWS").connectionType(ConnectionType.EVPL_VC)
                        .purpose(ConnectionPurpose.PROVIDER).bandwidthMbps(1000)
                        .bandwidthAllocation(allocation)
                        .aSideMetro(MetroId.of(MetroCode.DC)).aSideRouterName("FCR-DC")
                        .zSideServiceProfileUuid("sp-uuid-aws")
                        .zSideProviderLabel("AWS Direct Connect")
                        .zSideSellerRegion("us-east-1")
                        .build()
        );

        PlannedConnection bbConn = PlannedConnection.builder()
                .name("Backbone-DC-DA").connectionType(ConnectionType.EVPL_VC)
                .purpose(ConnectionPurpose.BACKBONE).bandwidthMbps(10000)
                .aSideMetro(MetroId.of(MetroCode.DC)).aSideRouterName("FCR-DC")
                .zSideMetro(MetroId.of(MetroCode.DA)).zSideRouterName("FCR-DA")
                .build();

        List<PlannedBackboneLink> backbones = Collections.singletonList(
                PlannedBackboneLink.builder()
                        .metroA(MetroId.of(MetroCode.DC)).metroZ(MetroId.of(MetroCode.DA))
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
                .sourceOptimization(buildOptResult())
                .cloudRouters(routers)
                .providerConnections(providerConnections)
                .backboneLinks(backbones)
                .routingProtocols(protocols)
                .pricing(pricing)
                .valid(true)
                .validationErrors(Collections.emptyList())
                .build();
    }

    @Nested
    @DisplayName("TerraformExporter")
    class TerraformExporterTests {

        private final TerraformExporter exporter = new TerraformExporter();

        @Test
        @DisplayName("Header and provider block are emitted")
        void header() {
            String hcl = exporter.export(plan);
            assertTrue(hcl.contains("# Equinix Fabric deployment"), "header comment");
            assertTrue(hcl.contains("required_providers"), "terraform block");
            assertTrue(hcl.contains("source = \"equinix/equinix\""), "provider source");
        }

        @Test
        @DisplayName("Cloud routers emit equinix_fabric_cloud_router resources")
        void cloudRouters() {
            String hcl = exporter.export(plan);
            assertTrue(hcl.contains("resource \"equinix_fabric_cloud_router\" \"fcr_FCR-DC\""),
                    "DC router resource block");
            assertTrue(hcl.contains("resource \"equinix_fabric_cloud_router\" \"fcr_FCR-DA\""),
                    "DA router resource block");
            assertTrue(hcl.contains("name = \"FCR-DC\""), "router name attr");
            assertTrue(hcl.contains("metro_code = \"DC\""), "metro code attr");
            assertTrue(hcl.contains("code = \"STANDARD\""), "package code attr");
            // Optional fields present on the first router only.
            assertTrue(hcl.contains("account_number = 272010"), "account number raw attr");
            assertTrue(hcl.contains("project_id = \"proj-uuid-123\""), "project id attr");
            assertTrue(hcl.contains("noc@example.com"), "notification email");
        }

        @Test
        @DisplayName("Provider connection emits equinix_fabric_connection with SP z_side")
        void providerConnection() {
            String hcl = exporter.export(plan);
            assertTrue(hcl.contains("resource \"equinix_fabric_connection\" \"conn_FCR-DC-to-AWS\""),
                    "provider connection resource block");
            assertTrue(hcl.contains("type = \"EVPL_VC\""), "connection type");
            assertTrue(hcl.contains("bandwidth = 1000"), "bandwidth raw attr");
            assertTrue(hcl.contains("uuid = \"sp-uuid-aws\""), "service profile uuid");
            assertTrue(hcl.contains("seller_region = \"us-east-1\""), "seller region");
            // A-side references the DC router resource by expression.
            assertTrue(hcl.contains("equinix_fabric_cloud_router.fcr_FCR-DC.id"),
                    "a-side router reference");
        }

        @Test
        @DisplayName("Backbone link emits equinix_fabric_connection between two routers")
        void backboneLink() {
            String hcl = exporter.export(plan);
            assertTrue(hcl.contains("resource \"equinix_fabric_connection\" \"backbone_Backbone-DC-DA\""),
                    "backbone connection resource block");
            assertTrue(hcl.contains("name = \"Backbone-DC-DA\""), "backbone name");
            assertTrue(hcl.contains("bandwidth = 10000"), "backbone bandwidth");
            // Both ends reference cloud router resources.
            assertTrue(hcl.contains("equinix_fabric_cloud_router.fcr_FCR-DA.id"),
                    "z-side router reference");
        }

        @Test
        @DisplayName("Routing protocols emit equinix_fabric_routing_protocol resources")
        void routingProtocols() {
            String hcl = exporter.export(plan);
            assertTrue(hcl.contains("resource \"equinix_fabric_routing_protocol\" \"rp_FCR-DC-to-AWS-direct\""),
                    "direct routing protocol block");
            assertTrue(hcl.contains("resource \"equinix_fabric_routing_protocol\" \"rp_FCR-DC-to-AWS-bgp\""),
                    "bgp routing protocol block");
            assertTrue(hcl.contains("type = \"DIRECT\""), "direct type");
            assertTrue(hcl.contains("type = \"BGP\""), "bgp type");
            assertTrue(hcl.contains("equinix_iface_ip = \"10.100.0.1/30\""), "direct iface ip");
            assertTrue(hcl.contains("customer_asn = 65100"), "customer asn");
            assertTrue(hcl.contains("customer_peer_ip = \"10.100.0.2/30\""), "bgp customer peer");
            assertTrue(hcl.contains("equinix_peer_ip = \"10.100.0.1/30\""), "bgp equinix peer");
            assertTrue(hcl.contains("bfd {"), "bfd block");
            assertTrue(hcl.contains("interval = \"300\""), "bfd interval");
            // Routing protocol bound to its parent connection.
            assertTrue(hcl.contains("connection_uuid = equinix_fabric_connection.conn_FCR-DC-to-AWS.id"),
                    "routing protocol parent connection reference");
        }

        @Test
        @DisplayName("Only resource types present in the plan are emitted")
        void onlyPresentResources() {
            DeploymentPlan routersOnly = DeploymentPlan.builder()
                    .sourceOptimization(buildOptResult())
                    .cloudRouters(Collections.singletonList(
                            PlannedCloudRouter.builder()
                                    .metroId(MetroId.of(MetroCode.DC)).name("FCR-DC").packageCode("STANDARD").build()))
                    .providerConnections(Collections.emptyList())
                    .backboneLinks(Collections.emptyList())
                    .routingProtocols(Collections.emptyList())
                    .valid(true).validationErrors(Collections.emptyList())
                    .build();

            String hcl = exporter.export(routersOnly);
            assertTrue(hcl.contains("equinix_fabric_cloud_router"), "router emitted");
            assertFalse(hcl.contains("equinix_fabric_connection"), "no connection resources");
            assertFalse(hcl.contains("equinix_fabric_routing_protocol"), "no routing protocol resources");
        }

        @Test
        @DisplayName("Null plan is rejected")
        void nullPlan() {
            assertThrows(IllegalArgumentException.class, () -> exporter.export(null));
        }
    }

    @Nested
    @DisplayName("TopologyDiagram - DeploymentPlan")
    class PlanDiagramTests {

        private final TopologyDiagram diagram = new TopologyDiagram();

        @Test
        @DisplayName("Metros become subgraphs containing their cloud routers")
        void metrosAndRouters() {
            String mmd = diagram.toMermaid(plan);
            assertTrue(mmd.startsWith("graph LR"), "graph header");
            assertTrue(mmd.contains("subgraph metro_DC[\"Metro: DC\"]"), "DC subgraph");
            assertTrue(mmd.contains("subgraph metro_DA[\"Metro: DA\"]"), "DA subgraph");
            assertTrue(mmd.contains("FCR-DC"), "DC router node");
            assertTrue(mmd.contains("FCR-DA"), "DA router node");
            assertTrue(mmd.contains("end"), "subgraph terminator");
        }

        @Test
        @DisplayName("Provider connection is an edge to a provider node")
        void providerEdge() {
            String mmd = diagram.toMermaid(plan);
            assertTrue(mmd.contains("AWS Direct Connect"), "provider node label");
            assertTrue(mmd.contains("-->|1000 Mbps|"), "provider edge with bandwidth");
        }

        @Test
        @DisplayName("Backbone link is a bidirectional edge between routers")
        void backboneEdge() {
            String mmd = diagram.toMermaid(plan);
            assertTrue(mmd.contains("<-->|10000 Mbps backbone|"), "backbone bidirectional edge");
        }

        @Test
        @DisplayName("Null plan is rejected")
        void nullPlan() {
            assertThrows(IllegalArgumentException.class, () -> diagram.toMermaid((DeploymentPlan) null));
        }
    }

    @Nested
    @DisplayName("TopologyDiagram - OptimizationResult")
    class ResultDiagramTests {

        private final TopologyDiagram diagram = new TopologyDiagram();

        @Test
        @DisplayName("Recommended metros become annotated nodes")
        void recommendedMetros() {
            String mmd = diagram.toMermaid(buildOptResult());
            assertTrue(mmd.startsWith("graph TD"), "graph header");
            assertTrue(mmd.contains("#1 Ashburn (DC)"), "primary metro node");
            assertTrue(mmd.contains("#2 Dallas (DA)"), "secondary metro node");
            assertTrue(mmd.contains("score: 95.0/100"), "primary score annotation");
        }

        @Test
        @DisplayName("Workload placements link to their metro")
        void workloadPlacements() {
            String mmd = diagram.toMermaid(buildOptResult());
            assertTrue(mmd.contains("ML Training"), "workload node");
            assertTrue(mmd.contains("-->"), "metro-to-workload edge");
        }

        @Test
        @DisplayName("Null result is rejected")
        void nullResult() {
            assertThrows(IllegalArgumentException.class, () -> diagram.toMermaid((OptimizationResult) null));
        }
    }

    @Test
    @DisplayName("Generators never throw on a fully populated plan")
    void smoke() {
        assertDoesNotThrow(() -> {
            new TerraformExporter().export(plan);
            new TopologyDiagram().toMermaid(plan);
            new TopologyDiagram().toMermaid(buildOptResult());
        });
    }

    // ---- Helpers ----

    private static OptimizationResult buildOptResult() {
        MetroScore score1 = new MetroScore(95.0, Collections.emptyList());
        MetroScore score2 = new MetroScore(82.0, Collections.emptyList());

        DeploymentTopology topology = new DeploymentTopology(Arrays.asList(
                new WorkloadPlacement("ML Training", MetroId.of(MetroCode.DC), "GPU availability"),
                new WorkloadPlacement("DR Backup", MetroId.of(MetroCode.DA), "geographic diversity")
        ));

        return OptimizationResult.builder()
                .recommendations(Arrays.asList(
                        MetroRecommendation.builder()
                                .rank(1).metroId(MetroId.of(MetroCode.DC)).metroName("Ashburn")
                                .score(score1).reasons(Collections.singletonList("Primary metro"))
                                .build(),
                        MetroRecommendation.builder()
                                .rank(2).metroId(MetroId.of(MetroCode.DA)).metroName("Dallas")
                                .score(score2).reasons(Collections.singletonList("Secondary metro"))
                                .build()))
                .topology(topology)
                .computedAt(Instant.now())
                .computeTimeMs(200)
                .build();
    }
}
