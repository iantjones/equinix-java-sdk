package api.equinix.javasdk.design.export;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.enums.RedundancyPriority;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.design.optimizer.model.DeploymentTopology;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.MetroScore;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.WorkloadPlacement;
import api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology;
import api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose;
import api.equinix.javasdk.design.optimizer.wizard.model.BandwidthAllocation;
import api.equinix.javasdk.design.optimizer.wizard.model.ConnectionInputRequirement;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
                        .metroId(MetroId.of(MetroCode.DC)).name("FCR-DC").packageCode(GatewayPackageCode.STANDARD)
                        .accountNumber(272010L).projectId("proj-uuid-123")
                        // Two recipients: EVERY configured address must reach the emails list.
                        .notificationEmails(List.of("noc@example.com", "neteng@example.com"))
                        .build(),
                PlannedCloudRouter.builder()
                        .metroId(MetroId.of(MetroCode.DA)).name("FCR-DA").packageCode(GatewayPackageCode.STANDARD)
                        .build()
        );

        Map<String, Integer> perWorkload = new LinkedHashMap<>();
        perWorkload.put("ML Training", 800);
        perWorkload.put("Backup", 200);
        BandwidthAllocation allocation = BandwidthAllocation.builder()
                .totalMbps(1000).perWorkload(perWorkload).reasoning("sum").build();

        List<PlannedConnection> providerConnections = Collections.singletonList(
                awsConnection().toBuilder()
                        .bandwidthAllocation(allocation)
                        .build()
        );

        // IP_VC, not EVPL_VC: a backbone link is Cloud Router to Cloud Router, and Fabric accepts an
        // FCR-originated connection only as IP_VC (FCR A-side => IP_VC fix).
        PlannedConnection bbConn = PlannedConnection.builder()
                .name("Backbone-DC-DA").connectionType(ConnectionType.IP_VC)
                .purpose(ConnectionPurpose.BACKBONE).bandwidthMbps(10000)
                .aSideMetro(MetroId.of(MetroCode.DC)).aSideRouterName("FCR-DC")
                .zSideMetro(MetroId.of(MetroCode.DA)).zSideRouterName("FCR-DA")
                .notificationEmails(List.of("fabric-ops@example.com"))
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
        @DisplayName("Every configured notification email is rendered, not just the first")
        void allNotificationEmailsRendered() {
            String hcl = exporter.export(plan);
            String router = resourceBlock(hcl, "resource \"equinix_fabric_cloud_router\" \"fcr_FCR-DC\"");
            assertTrue(router.contains(
                    "emails = [\"noc@example.com\", \"neteng@example.com\"]"),
                    "the router's notifications block carries EVERY configured recipient");

            // A connection with several recipients renders them all too (same writeNotifications shape).
            DeploymentPlan p = planWithProviderConnection(awsConnection().toBuilder()
                    .notificationEmails(List.of("noc@example.com", "cloud-team@example.com"))
                    .build());
            String conn = resourceBlock(exporter.export(p),
                    "resource \"equinix_fabric_connection\" \"conn_FCR-DC-to-AWS\"");
            assertTrue(conn.contains(
                    "emails = [\"noc@example.com\", \"cloud-team@example.com\"]"),
                    "the connection's notifications block carries every recipient");
        }

        @Test
        @DisplayName("Provider connection emits equinix_fabric_connection with SP z_side")
        void providerConnection() {
            String hcl = exporter.export(plan);
            assertTrue(hcl.contains("resource \"equinix_fabric_connection\" \"conn_FCR-DC-to-AWS\""),
                    "provider connection resource block");
            // IP_VC alongside the CLOUD_ROUTER access point — the FCR A-side => IP_VC fix: the old
            // EVPL_VC expectation cemented a type Fabric rejects for an FCR-originated connection.
            assertTrue(hcl.contains("type = \"IP_VC\""), "connection type");
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
        @DisplayName("Backbone link emits the mandatory notifications block")
        void backboneNotifications() {
            String hcl = exporter.export(plan);
            String backbone = resourceBlock(hcl,
                    "resource \"equinix_fabric_connection\" \"backbone_Backbone-DC-DA\"");
            assertTrue(backbone.contains("notifications {"), "backbone notifications block");
            assertTrue(backbone.contains("type = \"ALL\""), "backbone notification type");
            assertTrue(backbone.contains("emails = [\"fabric-ops@example.com\"]"),
                    "backbone notification email");
        }

        @Test
        @DisplayName("Connection-level redundancy is rendered as redundancy { group, priority }")
        void redundancyBlock() {
            String hcl = exporter.export(plan);
            String conn = resourceBlock(hcl,
                    "resource \"equinix_fabric_connection\" \"conn_FCR-DC-to-AWS\"");
            assertTrue(conn.contains("redundancy {"), "redundancy block");
            assertTrue(conn.contains("group = \"aws-redundant\""), "redundancy group");
            assertTrue(conn.contains("priority = \"PRIMARY\""), "redundancy priority");
        }

        @Test
        @DisplayName("Redundancy priority defaults to PRIMARY when a group is set without a role")
        void redundancyDefaultPriority() {
            DeploymentPlan p = planWithProviderConnection(
                    awsConnection().toBuilder().redundancyPriority(null).build());
            String conn = resourceBlock(exporter.export(p),
                    "resource \"equinix_fabric_connection\" \"conn_FCR-DC-to-AWS\"");
            assertTrue(conn.contains("redundancy {"), "redundancy block");
            assertTrue(conn.contains("priority = \"PRIMARY\""), "defaulted priority");
        }

        @Test
        @DisplayName("Provider z_side carries location { metro_code } for the provider edge metro")
        void zSideLocation() {
            String hcl = exporter.export(plan);
            String conn = resourceBlock(hcl,
                    "resource \"equinix_fabric_connection\" \"conn_FCR-DC-to-AWS\"");
            assertTrue(conn.contains("location {"), "z_side location block");
            assertTrue(conn.contains("metro_code = \"DC\""), "provider edge metro code");
        }

        @Test
        @DisplayName("Cloud connections declare sensitive auth-key variables and reference them on the z_side")
        void authenticationKeyVariables() {
            String hcl = exporter.export(plan);
            int varStart = hcl.indexOf("variable \"FCR-DC-to-AWS_auth_key\"");
            assertTrue(varStart >= 0, "auth key variable declared");
            String variable = hcl.substring(varStart, hcl.indexOf("}", varStart));
            assertTrue(variable.contains("type = string"), "variable type");
            assertTrue(variable.contains("sensitive = true"), "auth key variable is sensitive");
            assertTrue(variable.contains("AWS Account ID (12-digit)"),
                    "description names the provider's key label");

            String conn = resourceBlock(hcl,
                    "resource \"equinix_fabric_connection\" \"conn_FCR-DC-to-AWS\"");
            assertTrue(conn.contains("authentication_key = var.FCR-DC-to-AWS_auth_key"),
                    "z_side references the auth key variable");
        }

        @Test
        @DisplayName("Cloud connections declare a VLAN variable and emit DOT1Q link_protocol on the z_side")
        void vlanVariable() {
            String hcl = exporter.export(plan);
            assertTrue(hcl.contains("variable \"FCR-DC-to-AWS_vlan\""), "vlan variable declared");
            String conn = resourceBlock(hcl,
                    "resource \"equinix_fabric_connection\" \"conn_FCR-DC-to-AWS\"");
            assertTrue(conn.contains("link_protocol {"), "link_protocol block");
            assertTrue(conn.contains("type = \"DOT1Q\""), "DOT1Q encapsulation");
            assertTrue(conn.contains("vlan_tag = var.FCR-DC-to-AWS_vlan"),
                    "z_side references the vlan variable");
        }

        @Test
        @DisplayName("A VLAN already on the plan is emitted literally with no variable")
        void literalVlan() {
            DeploymentPlan p = planWithProviderConnection(
                    awsConnection().toBuilder().zSideVlanTag(1234).build());
            String hcl = exporter.export(p);
            assertFalse(hcl.contains("variable \"FCR-DC-to-AWS_vlan\""), "no vlan variable");
            String conn = resourceBlock(hcl,
                    "resource \"equinix_fabric_connection\" \"conn_FCR-DC-to-AWS\"");
            assertTrue(conn.contains("vlan_tag = 1234"), "literal vlan tag");
        }

        @Test
        @DisplayName("A third-party (non-cloud) profile gets no auth-key variable or authentication_key")
        void thirdPartyProfileNoAuthKey() {
            DeploymentPlan p = planWithProviderConnection(
                    PlannedConnection.builder()
                            .name("FCR-DC-to-NSP").connectionType(ConnectionType.IP_VC)
                            .purpose(ConnectionPurpose.PROVIDER).bandwidthMbps(100)
                            .aSideMetro(MetroId.of(MetroCode.DC)).aSideRouterName("FCR-DC")
                            .zSideServiceProfileUuid("sp-uuid-nsp")
                            .zSideProviderLabel("Example Carrier Ethernet")
                            .build());
            String hcl = exporter.export(p);
            assertFalse(hcl.contains("_auth_key"), "no auth key variable for a third-party profile");
            assertFalse(hcl.contains("authentication_key"), "no authentication_key attribute");
        }

        @Test
        @DisplayName("A ConnectionInputRequirement on the plan overrides the derived key need")
        void requirementOverridesDerivedNeed() {
            DeploymentPlan p = planWithProviderConnection(awsConnection()).toBuilder()
                    .requiredInputs(Collections.singletonList(
                            ConnectionInputRequirement.builder()
                                    .connectionName("FCR-DC-to-AWS")
                                    .providerLabel("AWS Direct Connect")
                                    .cloudType(CloudProviderType.AWS)
                                    .authenticationKeyRequired(false)
                                    .vlanTagRequired(false)
                                    .build()))
                    .build();
            String hcl = exporter.export(p);
            assertFalse(hcl.contains("variable \"FCR-DC-to-AWS"),
                    "requirement saying no inputs suppresses the variables");
            assertFalse(hcl.contains("authentication_key"), "no authentication_key attribute");
        }

        @Test
        @DisplayName("BGP depends_on the DIRECT protocol for the same connection")
        void bgpDependsOnDirect() {
            String hcl = exporter.export(plan);
            String bgp = resourceBlock(hcl,
                    "resource \"equinix_fabric_routing_protocol\" \"rp_FCR-DC-to-AWS-bgp\"");
            assertTrue(bgp.contains(
                    "depends_on = [equinix_fabric_routing_protocol.rp_FCR-DC-to-AWS-direct]"),
                    "BGP resource depends on the DIRECT resource");
            String direct = resourceBlock(hcl,
                    "resource \"equinix_fabric_routing_protocol\" \"rp_FCR-DC-to-AWS-direct\"");
            assertFalse(direct.contains("depends_on"), "DIRECT resource carries no depends_on");
        }

        @Test
        @DisplayName("BGP listed before DIRECT in the plan still depends_on the DIRECT resource")
        void bgpBeforeDirectStillOrdered() {
            DeploymentPlan p = DeploymentPlan.builder()
                    .sourceOptimization(buildOptResult())
                    .cloudRouters(Collections.emptyList())
                    .providerConnections(Collections.emptyList())
                    .backboneLinks(Collections.emptyList())
                    .routingProtocols(Arrays.asList(
                            PlannedRoutingProtocol.builder()
                                    .name("FCR-DC-to-AWS-bgp").connectionName("FCR-DC-to-AWS")
                                    .type(RoutingProtocolType.BGP)
                                    .customerAsn(65100L)
                                    .customerPeerIpv4("10.100.0.2/30").equinixPeerIpv4("10.100.0.1/30")
                                    .build(),
                            PlannedRoutingProtocol.builder()
                                    .name("FCR-DC-to-AWS-direct").connectionName("FCR-DC-to-AWS")
                                    .type(RoutingProtocolType.DIRECT)
                                    .equinixIfaceIpv4("10.100.0.1/30")
                                    .build()))
                    .valid(true).validationErrors(Collections.emptyList())
                    .build();
            String bgp = resourceBlock(exporter.export(p),
                    "resource \"equinix_fabric_routing_protocol\" \"rp_FCR-DC-to-AWS-bgp\"");
            assertTrue(bgp.contains(
                    "depends_on = [equinix_fabric_routing_protocol.rp_FCR-DC-to-AWS-direct]"),
                    "ordering is by connection, not by list position");
        }

        @Test
        @DisplayName("Generated HCL has balanced braces")
        void balancedBraces() {
            String hcl = exporter.export(plan);
            long open = hcl.chars().filter(c -> c == '{').count();
            long close = hcl.chars().filter(c -> c == '}').count();
            assertEquals(open, close, "every opened block is closed");
        }

        @Test
        @DisplayName("Only resource types present in the plan are emitted")
        void onlyPresentResources() {
            DeploymentPlan routersOnly = DeploymentPlan.builder()
                    .sourceOptimization(buildOptResult())
                    .cloudRouters(Collections.singletonList(
                            PlannedCloudRouter.builder()
                                    .metroId(MetroId.of(MetroCode.DC)).name("FCR-DC").packageCode(GatewayPackageCode.STANDARD).build()))
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
        @DisplayName("Labels escape <, > and & alongside double quotes")
        void labelEscaping() {
            DeploymentPlan spiky = DeploymentPlan.builder()
                    .sourceOptimization(buildOptResult())
                    .cloudRouters(Collections.singletonList(
                            PlannedCloudRouter.builder()
                                    .metroId(MetroId.of(MetroCode.DC))
                                    .name("FCR <Core> & \"Main\"")
                                    .packageCode(GatewayPackageCode.STANDARD)
                                    .build()))
                    .providerConnections(Collections.emptyList())
                    .backboneLinks(Collections.emptyList())
                    .routingProtocols(Collections.emptyList())
                    .valid(true).validationErrors(Collections.emptyList())
                    .build();
            String mmd = diagram.toMermaid(spiky);
            assertTrue(mmd.contains("FCR &lt;Core&gt; &amp; &quot;Main&quot;"),
                    "angle brackets, ampersand and quotes are all entity-encoded");
            assertFalse(mmd.contains("<Core>"), "raw markup never reaches the label");
            assertTrue(mmd.contains("<br/>STANDARD"),
                    "the diagram's own <br/> separators stay live");
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

    /**
     * The canonical AWS provider connection used across the exporter tests: a cloud VC whose
     * provider label resolves to {@link CloudProviderType#AWS} (driving the auth-key/VLAN
     * variable emission) and which carries connection-level redundancy.
     */
    private static PlannedConnection awsConnection() {
        // IP_VC, not EVPL_VC: the A-side is a Cloud Router, which Fabric accepts only as IP_VC
        // (FCR A-side => IP_VC fix) — the exported HCL must carry the type Fabric will accept.
        return PlannedConnection.builder()
                .name("FCR-DC-to-AWS").connectionType(ConnectionType.IP_VC)
                .purpose(ConnectionPurpose.PROVIDER).bandwidthMbps(1000)
                .aSideMetro(MetroId.of(MetroCode.DC)).aSideRouterName("FCR-DC")
                .zSideServiceProfileUuid("sp-uuid-aws")
                .zSideProviderLabel("AWS Direct Connect")
                .zSideSellerRegion("us-east-1")
                .zSideRedundancyGroup("aws-redundant")
                .redundancyPriority(RedundancyPriority.PRIMARY)
                .build();
    }

    /** A minimal valid plan carrying one Cloud Router plus the given provider connection. */
    private static DeploymentPlan planWithProviderConnection(PlannedConnection conn) {
        return DeploymentPlan.builder()
                .sourceOptimization(buildOptResult())
                .cloudRouters(Collections.singletonList(
                        PlannedCloudRouter.builder()
                                .metroId(MetroId.of(MetroCode.DC)).name("FCR-DC")
                                .packageCode(GatewayPackageCode.STANDARD)
                                .build()))
                .providerConnections(Collections.singletonList(conn))
                .backboneLinks(Collections.emptyList())
                .routingProtocols(Collections.emptyList())
                .valid(true).validationErrors(Collections.emptyList())
                .build();
    }

    /**
     * Slices one resource block out of the generated HCL — from the given resource header to
     * the next {@code resource} declaration (or the end of the document) — so assertions can
     * verify an attribute landed in a specific resource rather than anywhere in the file.
     */
    private static String resourceBlock(String hcl, String header) {
        int start = hcl.indexOf(header);
        assertTrue(start >= 0, "resource block present: " + header);
        int end = hcl.indexOf("\nresource ", start + header.length());
        return end >= 0 ? hcl.substring(start, end) : hcl.substring(start);
    }

    private static OptimizationResult buildOptResult() {
        MetroScore score1 = new MetroScore(95.0, Collections.emptyList());
        MetroScore score2 = new MetroScore(82.0, Collections.emptyList());

        DeploymentTopology topology = new DeploymentTopology(Arrays.asList(
                WorkloadPlacement.builder()
                        .workloadLabel("ML Training")
                        .assignedMetro(MetroId.of(MetroCode.DC))
                        .reasoning("GPU availability")
                        .build(),
                WorkloadPlacement.builder()
                        .workloadLabel("DR Backup")
                        .assignedMetro(MetroId.of(MetroCode.DA))
                        .reasoning("geographic diversity")
                        .build()
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
