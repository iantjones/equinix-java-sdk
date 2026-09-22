package com.eqixiac.equinix.design.export;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.GatewayPackageCode;
import com.eqixiac.equinix.fabric.enums.RedundancyPriority;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.design.optimizer.model.DeploymentTopology;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.MetroScore;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BackboneTopology;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.ConnectionPurpose;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.BandwidthAllocation;
import com.eqixiac.equinix.design.optimizer.wizard.model.ConnectionInputRequirement;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlanPricing;
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

    @Nested
    @DisplayName("TerraformExporter - native multicloud links")
    class MulticloudTerraformTests {

        private final TerraformExporter exporter = new TerraformExporter();

        @Test
        @DisplayName("A link is a delimited block in which every line is a comment")
        void commentedBlock() {
            String hcl = exporter.export(withLinks(awsGoogleLink(MulticloudLinkRole.ALTERNATIVE,
                    CloudToCloudStrategy.COMPARE)));
            String block = linkBlock(hcl, "aws-gcp-DC");
            for (String line : block.split("\n")) {
                assertTrue(line.startsWith("#"), "every line of the block is a comment: " + line);
            }
            assertTrue(block.startsWith(
                    "# ---- BEGIN native multicloud link: aws-gcp-DC (not a Terraform resource) ----"),
                    "the opening delimiter says the block is not a resource");

            String text = unwrapped(block);
            assertTrue(text.contains("Providers: Amazon Web Services us-east-1 <-> Google Cloud Platform us-east4"),
                    "provider pair and regions");
            assertTrue(text.contains(
                    "Bandwidth: 10000 Mbps (requested 8000 Mbps, rounded up to the smallest listed size)"),
                    "covering size and the requested bandwidth, both in Mbps");
            // Status, as-of date, sizes and id are the bundled catalog entry's, copied from provider docs.
            assertTrue(text.contains("Environment: GA, observed 2026-09-21; listed sizes "
                    + "[1000, 5000, 10000, 100000] Mbps; catalog entry aws-us-east-1--gcp-us-east4"),
                    "environment status with its observation date");
            assertTrue(text.contains(
                    "Source: https://docs.aws.amazon.com/interconnect/latest/userguide/region-availability.html"),
                    "the environment's source URL");
            assertTrue(text.contains("Role: ALTERNATIVE (strategy COMPARE). The configuration does not depend "
                    + "on this link. The Equinix connections for the flow stay in it: FCR-DC-to-AWS, FCR-DC-to-GCP."),
                    "role, strategy and the Equinix connections kept");
            assertTrue(text.contains("Workloads: ML Training"), "workloads that imply the flow");
        }

        @Test
        @DisplayName("The block carries the create-then-accept procedure with the AWS and Google Cloud sides")
        void createThenAcceptGoogle() {
            String block = linkBlock(exporter.export(withLinks(awsGoogleLink(
                    MulticloudLinkRole.ALTERNATIVE, CloudToCloudStrategy.COMPARE))), "aws-gcp-DC");
            String text = unwrapped(block);

            assertTrue(text.contains("1. Create: on one provider, request the link and enter the account "
                    + "identifier of the other (accepting) provider. Record that identifier in "
                    + "var.aws-gcp-DC_destination_account_id."), "step 1 names the far-side account id variable");
            assertTrue(text.contains("2. The creating provider returns an activation key."), "step 2");
            assertTrue(text.contains("is not an input of this configuration"),
                    "the activation key is never a Terraform input");
            assertTrue(text.contains("3. Accept: on the other provider, submit the activation key."), "step 3");

            assertTrue(text.contains("AWS Direct Connect console > AWS Interconnect > "
                    + "\"Create new multicloud Interconnect\" to create, or \"Accept multicloud Interconnect\""),
                    "AWS console path for create and accept");
            assertTrue(text.contains("The attach point is a Direct Connect gateway."), "AWS attach point");
            assertTrue(text.contains("AWS asks for the Google Cloud project ID"), "AWS-side far-side identifier");
            assertTrue(block.contains("#     gcloud network-connectivity transports create NAME --region=us-east4 "
                    + "--network=NETWORK --activation-key=KEY\n"),
                    "the gcloud accept command is one unwrapped line carrying the plan's Google Cloud region");
            assertTrue(block.contains("--bandwidth=10g --remote-account-id=AWS_ACCOUNT_ID "
                    + "--remote-profile=REMOTE_PROFILE\n"),
                    "the gcloud create-first command carries the covering size as a documented --bandwidth value");
            assertTrue(block.contains(
                    "https://docs.aws.amazon.com/interconnect/latest/userguide/getting-started-multicloud.html"),
                    "AWS procedure source");
            assertTrue(block.contains(
                    "https://docs.cloud.google.com/sdk/gcloud/reference/network-connectivity/transports/create"),
                    "gcloud command source");
            assertFalse(text.contains("Service key"), "no OCI field on a Google Cloud link");

            assertTrue(text.contains("Steps recorded on the plan: 1. AWS displays an activation key. "
                    + "2. Google Cloud: create the transport with that key."),
                    "the plan's own recipe lines are rendered verbatim and numbered");
        }

        @Test
        @DisplayName("An Oracle Cloud link names the FastConnect Service key field and no gcloud command")
        void createThenAcceptOracle() {
            String block = linkBlock(exporter.export(withLinks(awsOracleLink())), "aws-oci-DC");
            String text = unwrapped(block);
            assertTrue(text.contains("Providers: Amazon Web Services us-east-1 <-> "
                    + "Oracle Cloud Infrastructure us-ashburn-1"), "provider pair and regions");
            assertTrue(text.contains("Bandwidth: 500 Mbps"), "500 Mbps is a listed Oracle size");
            assertFalse(text.contains("rounded up"), "an exact size is not reported as rounded up");
            assertTrue(text.contains("FastConnect > \"Create FastConnect\" > \"FastConnect interconnect\""),
                    "OCI console path");
            assertTrue(text.contains("enter the AWS activation key in the \"Service key\" field"),
                    "the AWS activation key is the OCI service key");
            assertTrue(text.contains("AWS asks for the OCI tenancy OCID (format ocid1.tenancy.oc1..<unique_ID>)"),
                    "AWS-side far-side identifier");
            assertTrue(block.contains("https://docs.oracle.com/en-us/iaas/Content/multicloud/interconnect-aws.htm"),
                    "OCI procedure source");
            assertFalse(block.contains("gcloud"), "no Google Cloud command on an Oracle Cloud link");
        }

        @Test
        @DisplayName("A preview Azure link states its status and that no Azure-side procedure was verified")
        void previewAzure() {
            String block = linkBlock(exporter.export(withLinks(awsAzurePreviewLink())), "aws-azure-DC");
            String text = unwrapped(block);
            assertTrue(text.contains("Environment: PREVIEW, observed 2026-09-21"), "preview status is stated");
            assertTrue(text.contains("No Azure-side procedure was verified as of 2026-09-21."),
                    "an unverified procedure is labeled, not invented");
            assertFalse(block.contains("gcloud"), "no Google Cloud command");
            assertFalse(text.contains("Service key"), "no OCI field");
        }

        @Test
        @DisplayName("Generated HCL keeps balanced braces with links of every role")
        void balancedBracesWithLinks() {
            for (DeploymentPlan p : plansWithLinks()) {
                String hcl = exporter.export(p);
                long open = hcl.chars().filter(c -> c == '{').count();
                long close = hcl.chars().filter(c -> c == '}').count();
                assertEquals(open, close, "every opened block is closed");
            }
        }

        @Test
        @DisplayName("No cloud-provider resource or provider block is ever emitted")
        void noCloudProviderResources() {
            for (DeploymentPlan p : plansWithLinks()) {
                String hcl = exporter.export(p);
                for (String forbidden : List.of("resource \"aws_", "resource \"google_", "resource \"oci_",
                        "resource \"azurerm_", "provider \"aws\"", "provider \"google\"", "provider \"oci\"",
                        "provider \"azurerm\"", "data \"aws_", "data \"google_")) {
                    assertFalse(hcl.contains(forbidden), "never emitted, not even in a comment: " + forbidden);
                }
                for (String line : hcl.split("\n")) {
                    if (line.startsWith("resource ")) {
                        assertTrue(line.startsWith("resource \"equinix_fabric_"),
                                "every resource is an Equinix Fabric resource: " + line);
                    }
                }
            }
        }

        @Test
        @DisplayName("Links add no resource block: the block count equals totalResourceCount()")
        void resourceCountUnaffected() {
            DeploymentPlan linked = withLinks(
                    awsGoogleLink(MulticloudLinkRole.ALTERNATIVE, CloudToCloudStrategy.COMPARE), awsOracleLink());
            assertEquals(6, plan.totalResourceCount(), "2 routers + 1 connection + 1 backbone + 2 protocols");
            assertEquals(plan.totalResourceCount(), linked.totalResourceCount(),
                    "the plan's resource count ignores links");
            assertEquals(plan.totalResourceCount(), resourceBlocks(exporter.export(plan)),
                    "one resource block per planned resource");
            assertEquals(linked.totalResourceCount(), resourceBlocks(exporter.export(linked)),
                    "links contribute no resource block");
        }

        @Test
        @DisplayName("An ALTERNATIVE link declares an optional string variable for the far-side account id")
        void destinationAccountVariableOptional() {
            String hcl = exporter.export(withLinks(awsGoogleLink(MulticloudLinkRole.ALTERNATIVE,
                    CloudToCloudStrategy.COMPARE)));
            String variable = variableBlock(hcl, "aws-gcp-DC_destination_account_id");
            assertTrue(variable.contains("type = string"), "variable type");
            assertTrue(variable.contains("default = null"),
                    "the configuration does not depend on the link, so it must plan without a value");
            assertFalse(variable.contains("sensitive"), "an account identifier is not marked sensitive");
            assertTrue(variable.contains("the Google Cloud project ID when AWS creates the link, or the 12-digit "
                    + "AWS account ID when Google Cloud creates it (gcloud flag --remote-account-id)"),
                    "the description names the identifier for either direction");
            assertTrue(variable.contains("Optional: the plan does not depend on this link."), "optional is stated");
            assertTrue(hcl.indexOf("variable \"aws-gcp-DC_destination_account_id\"")
                    > hcl.indexOf("# ---- END native multicloud link: aws-gcp-DC ----"),
                    "the variable follows its link's block");
        }

        @Test
        @DisplayName("A REPLACEMENT link's variable has no default, so Terraform requires the value")
        void destinationAccountVariableRequired() {
            String variable = variableBlock(exporter.export(replacementPlan()), "aws-gcp-DC_destination_account_id");
            assertTrue(variable.contains("type = string"), "variable type");
            assertFalse(variable.contains("default"), "no default on a link the plan depends on");
            assertTrue(variable.contains("Required: the plan omits Equinix connections and depends on this link."),
                    "required is stated");
        }

        @Test
        @DisplayName("Links with the same name get distinct variable names")
        void uniqueVariableNames() {
            PlannedMulticloudInterconnect link = awsGoogleLink(MulticloudLinkRole.ALTERNATIVE,
                    CloudToCloudStrategy.COMPARE);
            String hcl = exporter.export(withLinks(link, link));
            assertTrue(hcl.contains("variable \"aws-gcp-DC_destination_account_id\" {"), "first variable");
            assertTrue(hcl.contains("variable \"aws-gcp-DC_destination_account_id_2\" {"), "de-duplicated variable");
        }

        @Test
        @DisplayName("The header states the link count and that no resource is emitted for links")
        void headerMentionsLinks() {
            String header = unwrapped(header(exporter.export(withLinks(awsGoogleLink(
                    MulticloudLinkRole.ALTERNATIVE, CloudToCloudStrategy.COMPARE)))));
            assertTrue(header.contains("Native multicloud links (Beta): 1."), "link count");
            assertTrue(header.contains("This file holds no resource for them"), "no resource is emitted");
            assertTrue(header.contains("The resource counts above exclude them."), "counts exclude links");
            assertFalse(header.contains("REPLACEMENT:"), "an ALTERNATIVE link replaces nothing");
        }

        @Test
        @DisplayName("The header names the Equinix connections a REPLACEMENT link stands in for")
        void headerStatesReplacement() {
            String hcl = exporter.export(replacementPlan());
            String header = unwrapped(header(hcl));
            assertTrue(header.contains("REPLACEMENT: native link aws-gcp-DC (Amazon Web Services us-east-1 <-> "
                    + "Google Cloud Platform us-east4) carries its flow in place of the Equinix connection(s) "
                    + "FCR-DC-to-AWS, FCR-DC-to-GCP."), "the replaced connections are named");
            assertTrue(header.contains("Those connections and their routing protocols are not in this "
                    + "configuration."), "the omission is stated");
            assertTrue(header.contains("The flow has no path until the link is created"),
                    "the dependency on the link is stated");
            assertFalse(hcl.contains("resource \"equinix_fabric_connection\" \"conn_"),
                    "the replaced provider connections are absent from the configuration");
            assertTrue(unwrapped(linkBlock(hcl, "aws-gcp-DC")).contains(
                    "Role: REPLACEMENT (strategy NATIVE_WHEN_AVAILABLE). This configuration omits the Equinix "
                    + "connection(s) FCR-DC-to-AWS, FCR-DC-to-GCP and their routing protocols."),
                    "the block repeats the replacement");
        }

        @Test
        @DisplayName("An UNAVAILABLE entry is reported in the header and the block and gets no variable")
        void unavailableEntry() {
            String hcl = exporter.export(withLinks(unavailableLink()));
            assertTrue(unwrapped(header(hcl)).contains("UNAVAILABLE: a native link was required for "
                    + "Amazon Web Services eu-west-3 <-> Google Cloud Platform europe-west9 (aws-gcp-PA)"),
                    "header reports the entry");
            String text = unwrapped(linkBlock(hcl, "aws-gcp-PA"));
            assertTrue(text.contains("Environment: none in the plan's catalog for this region pair"),
                    "no environment");
            assertTrue(text.contains("Bandwidth: 2000 Mbps requested; no environment to size it against"),
                    "requested bandwidth only");
            assertTrue(text.contains("No procedure:"), "nothing to create");
            assertFalse(hcl.contains("destination_account_id"), "no variable for a link that cannot be created");
        }

        @Test
        @DisplayName("A plan without links produces no multicloud output")
        void noLinksNoOutput() {
            String hcl = exporter.export(plan);
            assertFalse(hcl.toLowerCase().contains("multicloud"), "no multicloud text");
            assertFalse(hcl.contains("destination_account_id"), "no link variable");
            assertEquals(hcl, exporter.export(plan.toBuilder().multicloudLinks(Collections.emptyList()).build()),
                    "an empty list and an absent list render identically");
        }

        @Test
        @DisplayName("Line breaks and shell text in plan-supplied fields cannot leave the comment block")
        void planTextCannotEscapeComments() {
            PlannedMulticloudInterconnect hostile = awsGoogleLink(MulticloudLinkRole.ALTERNATIVE,
                    CloudToCloudStrategy.COMPARE).toBuilder()
                    .name("evil\nvariable \"injected\" {")
                    .regionZ("us-east4; curl example.invalid")
                    .workloadLabels(List.of("w1\r\noutput \"leak\" {"))
                    .createThenAcceptRecipe(List.of("step\n}\nlocals {"))
                    .build();
            String hcl = exporter.export(withLinks(hostile));
            String block = linkBlock(hcl, "evil variable");
            for (String line : block.split("\n")) {
                assertTrue(line.startsWith("#"), "every line of the block is a comment: " + line);
            }
            for (String line : hcl.split("\n")) {
                assertFalse(line.startsWith("variable \"injected\""), "no injected variable block");
                assertFalse(line.startsWith("output "), "no injected output block");
                assertFalse(line.startsWith("locals "), "no injected locals block");
            }
            assertTrue(block.contains("--region=REGION "),
                    "a region that is not a plain identifier is replaced by a placeholder in the command line");
            assertFalse(block.contains("--region=us-east4;"), "shell text never reaches the command line");
            assertTrue(hcl.contains("variable \"evil_variable_injected_destination_account_id\" {"),
                    "the variable name is sanitized to a Terraform identifier");
        }
    }

    @Nested
    @DisplayName("TopologyDiagram - native multicloud links")
    class MulticloudDiagramTests {

        private final TopologyDiagram diagram = new TopologyDiagram();

        @Test
        @DisplayName("A link is a dashed edge between the provider nodes already drawn for the two clouds")
        void dashedEdgeBetweenProviderNodes() {
            DeploymentPlan p = plan.toBuilder()
                    .providerConnections(Arrays.asList(awsConnection(), googleConnection()))
                    .multicloudLinks(List.of(awsGoogleLink(MulticloudLinkRole.ALTERNATIVE,
                            CloudToCloudStrategy.COMPARE)))
                    .build();
            String mmd = diagram.toMermaid(p);
            String awsNode = nodeIdOf(mmd, "AWS Direct Connect<br/>us-east-1");
            String googleNode = nodeIdOf(mmd, "Google Cloud Partner Interconnect Zone 1<br/>us-east4");
            assertTrue(mmd.contains("  " + awsNode + " <-.->|\"native multicloud (outside Fabric)"
                    + "<br/>10000 Mbps, GA, ALTERNATIVE\"| " + googleNode + "\n"),
                    "dashed bidirectional edge, quoted label, joining the two cloud nodes:\n" + mmd);
            assertFalse(mmd.contains("Amazon Web Services"), "no extra cloud node when a provider node exists");
            assertTrue(mmd.contains("-->|1000 Mbps| " + awsNode), "the Fabric connection edge is still drawn");
        }

        @Test
        @DisplayName("A REPLACEMENT link whose connections were omitted gets its own cloud nodes")
        void replacementAddsCloudNodes() {
            String mmd = diagram.toMermaid(replacementPlan());
            String awsNode = nodeIdOf(mmd, "Amazon Web Services<br/>us-east-1");
            String googleNode = nodeIdOf(mmd, "Google Cloud Platform<br/>us-east4");
            assertTrue(mmd.contains("  " + awsNode + " <-.->|\"native multicloud (outside Fabric)"
                    + "<br/>10000 Mbps, GA, REPLACEMENT\"| " + googleNode + "\n"),
                    "the edge joins the added cloud nodes and states the role:\n" + mmd);
            assertFalse(mmd.contains("-->|1000 Mbps|"), "no Fabric provider edge remains");
        }

        @Test
        @DisplayName("A link matched on another region of the same cloud does not reuse that cloud's node")
        void otherRegionGetsOwnNode() {
            PlannedMulticloudInterconnect west = awsGoogleLink(MulticloudLinkRole.ALTERNATIVE,
                    CloudToCloudStrategy.COMPARE).toBuilder()
                    .regionA("us-west-2").regionZ("us-west1")
                    .environment(catalogEnvironment(CloudProviderType.AWS, "us-west-2",
                            CloudProviderType.GOOGLE_CLOUD, "us-west1"))
                    .build();
            String mmd = diagram.toMermaid(withLinks(west));
            String awsEast = nodeIdOf(mmd, "AWS Direct Connect<br/>us-east-1");
            String awsWest = nodeIdOf(mmd, "Amazon Web Services<br/>us-west-2");
            assertFalse(awsEast.equals(awsWest), "us-west-2 is a separate node from the us-east-1 provider node");
            assertTrue(mmd.contains("  " + awsWest + " <-.->|"), "the edge starts at the us-west-2 node");
        }

        @Test
        @DisplayName("Link labels use the existing escaping")
        void labelEscaping() {
            PlannedMulticloudInterconnect spiky = PlannedMulticloudInterconnect.builder()
                    .name("spiky")
                    .providerA(CloudProviderType.AWS).regionA("us-<east>&\"1\"")
                    .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ("us-east4")
                    .role(MulticloudLinkRole.ALTERNATIVE).strategy(CloudToCloudStrategy.COMPARE)
                    .requestedMbps(1000)
                    .build();
            String mmd = diagram.toMermaid(replacementPlan().toBuilder()
                    .multicloudLinks(List.of(spiky)).build());
            assertTrue(mmd.contains("Amazon Web Services<br/>us-&lt;east&gt;&amp;&quot;1&quot;"),
                    "angle brackets, ampersand and quotes in a region are entity-encoded");
            assertFalse(mmd.contains("<east>"), "raw markup never reaches the label");
            assertTrue(mmd.contains("<-.->|\"native multicloud (outside Fabric)<br/>1000 Mbps, ALTERNATIVE\"|"),
                    "with no environment the label carries the requested bandwidth and no status");
        }

        @Test
        @DisplayName("An UNAVAILABLE entry is not drawn")
        void unavailableNotDrawn() {
            String mmd = diagram.toMermaid(withLinks(unavailableLink()));
            assertFalse(mmd.contains("<-.->"), "no edge for a link that cannot be created");
            assertFalse(mmd.contains("native multicloud"), "no label either");
            assertEquals(diagram.toMermaid(plan), mmd, "the diagram equals the one without the entry");
        }

        @Test
        @DisplayName("A plan without links has no dashed edge")
        void noLinksNoDashedEdge() {
            String mmd = diagram.toMermaid(plan);
            assertFalse(mmd.contains("-.-"), "no dashed edge");
            assertFalse(mmd.contains("native multicloud"), "no native link label");
        }
    }

    @Test
    @DisplayName("Generators never throw on a fully populated plan")
    void smoke() {
        assertDoesNotThrow(() -> {
            new TerraformExporter().export(plan);
            new TopologyDiagram().toMermaid(plan);
            new TopologyDiagram().toMermaid(buildOptResult());
            for (DeploymentPlan linked : plansWithLinks()) {
                new TerraformExporter().export(linked);
                new TopologyDiagram().toMermaid(linked);
            }
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

    /** A Google Cloud provider connection whose label is a Fabric service profile name for the product. */
    private static PlannedConnection googleConnection() {
        return PlannedConnection.builder()
                .name("FCR-DC-to-GCP").connectionType(ConnectionType.IP_VC)
                .purpose(ConnectionPurpose.PROVIDER).bandwidthMbps(1000)
                .aSideMetro(MetroId.of(MetroCode.DC)).aSideRouterName("FCR-DC")
                .zSideServiceProfileUuid("sp-uuid-gcp")
                .zSideProviderLabel("Google Cloud Partner Interconnect Zone 1")
                .zSideSellerRegion("us-east4")
                .build();
    }

    /**
     * The bundled catalog's entry for a region pair. The link fixtures take their environment
     * (status, sizes, as-of date, source URLs) from the catalog copied from provider documentation,
     * so the rendered facts asserted below are observed ones.
     */
    private static MulticloudEnvironment catalogEnvironment(CloudProviderType a, String aRegion,
                                                            CloudProviderType z, String zRegion) {
        return MulticloudEnvironmentCatalog.standard().find(a, aRegion, z, zRegion)
                .orElseThrow(() -> new AssertionError("the bundled catalog lists " + a + " " + aRegion
                        + " <-> " + z + " " + zRegion));
    }

    /**
     * AWS us-east-1 to Google Cloud us-east4 (GA in the bundled catalog) for an 8000 Mbps flow, which
     * the catalog's sizes cover at 10000 Mbps. Under {@code REPLACEMENT} both Equinix connections of
     * the flow are recorded as omitted; otherwise both are recorded as kept.
     */
    private static PlannedMulticloudInterconnect awsGoogleLink(MulticloudLinkRole role,
                                                                CloudToCloudStrategy strategy) {
        boolean replacement = role == MulticloudLinkRole.REPLACEMENT;
        List<String> legs = List.of("FCR-DC-to-AWS", "FCR-DC-to-GCP");
        return PlannedMulticloudInterconnect.builder()
                .name("aws-gcp-DC")
                .providerA(CloudProviderType.AWS).regionA("us-east-1")
                .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ("us-east4")
                .environment(catalogEnvironment(CloudProviderType.AWS, "us-east-1",
                        CloudProviderType.GOOGLE_CLOUD, "us-east4"))
                .role(role).strategy(strategy)
                .metro(MetroId.of(MetroCode.DC))
                .requestedMbps(8000).coveringTierMbps(10000)
                .workloadLabels(List.of("ML Training"))
                .equinixConnectionNames(replacement ? List.of() : legs)
                .replacedConnectionNames(replacement ? legs : List.of())
                .createThenAcceptRecipe(List.of("AWS displays an activation key.",
                        "Google Cloud: create the transport with that key."))
                .build();
    }

    /** AWS us-east-1 to OCI us-ashburn-1 (GA in the bundled catalog) at 500 Mbps, a size Oracle lists. */
    private static PlannedMulticloudInterconnect awsOracleLink() {
        return PlannedMulticloudInterconnect.builder()
                .name("aws-oci-DC")
                .providerA(CloudProviderType.AWS).regionA("us-east-1")
                .providerZ(CloudProviderType.ORACLE_CLOUD).regionZ("us-ashburn-1")
                .environment(catalogEnvironment(CloudProviderType.AWS, "us-east-1",
                        CloudProviderType.ORACLE_CLOUD, "us-ashburn-1"))
                .role(MulticloudLinkRole.ALTERNATIVE).strategy(CloudToCloudStrategy.COMPARE)
                .metro(MetroId.of(MetroCode.DC))
                .requestedMbps(500).coveringTierMbps(500)
                .workloadLabels(List.of("Backup"))
                .build();
    }

    /** AWS us-east-1 to Azure eastus, which the bundled catalog records as PREVIEW at 1000 Mbps only. */
    private static PlannedMulticloudInterconnect awsAzurePreviewLink() {
        return PlannedMulticloudInterconnect.builder()
                .name("aws-azure-DC")
                .providerA(CloudProviderType.AWS).regionA("us-east-1")
                .providerZ(CloudProviderType.AZURE).regionZ("eastus")
                .environment(catalogEnvironment(CloudProviderType.AWS, "us-east-1",
                        CloudProviderType.AZURE, "eastus"))
                .role(MulticloudLinkRole.ALTERNATIVE).strategy(CloudToCloudStrategy.COMPARE)
                .metro(MetroId.of(MetroCode.DC))
                .requestedMbps(1000).coveringTierMbps(1000)
                .build();
    }

    /**
     * A {@code NATIVE_ONLY} flow for a region pair the bundled catalog does not list (AWS eu-west-3
     * to Google Cloud europe-west9), as the wizard records it: no environment, no covering size.
     */
    private static PlannedMulticloudInterconnect unavailableLink() {
        assertTrue(MulticloudEnvironmentCatalog.standard().find(CloudProviderType.AWS, "eu-west-3",
                CloudProviderType.GOOGLE_CLOUD, "europe-west9").isEmpty(),
                "fixture premise: the bundled catalog has no entry for this pair");
        return PlannedMulticloudInterconnect.builder()
                .name("aws-gcp-PA")
                .providerA(CloudProviderType.AWS).regionA("eu-west-3")
                .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ("europe-west9")
                .role(MulticloudLinkRole.UNAVAILABLE).strategy(CloudToCloudStrategy.NATIVE_ONLY)
                .metro(MetroId.of(MetroCode.PA))
                .requestedMbps(2000)
                .build();
    }

    /** The fully populated plan with the given native links attached; its Equinix resources are unchanged. */
    private DeploymentPlan withLinks(PlannedMulticloudInterconnect... links) {
        return plan.toBuilder().multicloudLinks(Arrays.asList(links)).build();
    }

    /**
     * The plan as {@code NATIVE_WHEN_AVAILABLE} leaves it when the replacement rule applies: the
     * provider connections of the flow and their routing protocols are gone, the routers and the
     * backbone remain, and the native link is a {@code REPLACEMENT}.
     */
    private DeploymentPlan replacementPlan() {
        return plan.toBuilder()
                .providerConnections(Collections.emptyList())
                .routingProtocols(Collections.emptyList())
                .multicloudLinks(List.of(awsGoogleLink(MulticloudLinkRole.REPLACEMENT,
                        CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE)))
                .build();
    }

    /** Plans covering every role and every provider pair the exporter has text for. */
    private List<DeploymentPlan> plansWithLinks() {
        PlannedMulticloudInterconnect alternative = awsGoogleLink(MulticloudLinkRole.ALTERNATIVE,
                CloudToCloudStrategy.COMPARE);
        return List.of(
                withLinks(alternative),
                replacementPlan(),
                withLinks(unavailableLink()),
                withLinks(awsOracleLink(), awsAzurePreviewLink()),
                withLinks(alternative, awsOracleLink(), awsAzurePreviewLink(), unavailableLink()));
    }

    /** The comment block of one link, from its BEGIN delimiter through its END delimiter line. */
    private static String linkBlock(String hcl, String linkName) {
        int start = hcl.indexOf("# ---- BEGIN native multicloud link: " + linkName);
        assertTrue(start >= 0, "link block present: " + linkName);
        int endMarker = hcl.indexOf("# ---- END native multicloud link: " + linkName, start);
        assertTrue(endMarker > start, "link block closed: " + linkName);
        int end = hcl.indexOf('\n', endMarker);
        return end >= 0 ? hcl.substring(start, end) : hcl.substring(start);
    }

    /**
     * Comment text with the {@code #} prefixes, indentation and line wrapping removed, so an
     * assertion on a sentence does not depend on where the exporter wrapped it.
     */
    private static String unwrapped(String comments) {
        StringBuilder text = new StringBuilder();
        for (String line : comments.split("\n")) {
            String content = line.replaceFirst("^#+", "").trim();
            if (!content.isEmpty()) {
                text.append(content).append(' ');
            }
        }
        return text.toString().replaceAll("\\s+", " ").trim();
    }

    /** The header comment: everything before the {@code terraform} block. */
    private static String header(String hcl) {
        int end = hcl.indexOf("terraform {");
        assertTrue(end > 0, "terraform block present");
        return hcl.substring(0, end);
    }

    /** One {@code variable} block, from its declaration through its closing brace. */
    private static String variableBlock(String hcl, String name) {
        int start = hcl.indexOf("variable \"" + name + "\" {");
        assertTrue(start >= 0, "variable declared: " + name);
        int end = hcl.indexOf("\n}\n", start);
        assertTrue(end > start, "variable block closed: " + name);
        return hcl.substring(start, end + 3);
    }

    /** The number of {@code resource} blocks in the document. */
    private static long resourceBlocks(String hcl) {
        return Arrays.stream(hcl.split("\n")).filter(line -> line.startsWith("resource \"")).count();
    }

    /** The Mermaid node id of the node whose quoted label is exactly {@code label}. */
    private static String nodeIdOf(String mmd, String label) {
        for (String line : mmd.split("\n")) {
            int at = line.indexOf("[\"" + label + "\"]");
            if (at > 0) {
                return line.substring(0, at).trim();
            }
        }
        throw new AssertionError("no node labeled '" + label + "' in:\n" + mmd);
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
