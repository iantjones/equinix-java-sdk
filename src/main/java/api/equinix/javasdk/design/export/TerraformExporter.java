package api.equinix.javasdk.design.export;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.enums.RedundancyPriority;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.design.optimizer.wizard.model.ConnectionBodies;
import api.equinix.javasdk.design.optimizer.wizard.model.ConnectionInputRequirement;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedBackboneLink;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedCloudRouter;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedConnection;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedRoutingProtocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a {@link DeploymentPlan} produced by the Deployment Wizard into
 * Infrastructure-as-Code (HCL) for the
 * <a href="https://registry.terraform.io/providers/equinix/equinix/latest">Equinix Terraform provider</a>.
 *
 * <p>The exporter only emits the resource types the plan actually contains and keeps
 * attributes faithful to the fields the plan provides; it does not invent provider
 * attributes that cannot be sourced from the plan. The generated HCL is intended as a
 * readable, version-controllable starting point for {@code terraform plan} / {@code apply}.</p>
 *
 * <p>Resource mapping:</p>
 * <ul>
 *     <li>{@link PlannedCloudRouter} &rarr; {@code resource "equinix_fabric_cloud_router"}</li>
 *     <li>{@link PlannedConnection} (provider connections and backbone links)
 *         &rarr; {@code resource "equinix_fabric_connection"}</li>
 *     <li>{@link PlannedRoutingProtocol} &rarr; {@code resource "equinix_fabric_routing_protocol"}</li>
 * </ul>
 *
 * <p>Customer inputs the plan deliberately leaves unresolved (the cloud authorization
 * key, and the DOT1Q VLAN tag when not yet known) become {@code variable} blocks —
 * the authorization key marked {@code sensitive} — referenced from the connection's
 * Z-side, so the generated configuration is complete without ever fabricating or
 * inlining a secret. A BGP routing protocol on a connection that also carries a DIRECT
 * protocol is emitted with {@code depends_on} on the DIRECT resource, because Fabric
 * requires DIRECT to exist before BGP on the same connection. The Fabric-mandated
 * {@code notifications} block is emitted on every Cloud Router and connection —
 * backbone links included — with every recipient the plan carries, and a planned
 * connection's redundancy group/priority becomes a connection-level
 * {@code redundancy} block.</p>
 *
 * <p><strong>Not emitted:</strong> Equinix provider credentials (supply them via the
 * standard provider configuration), and any physical colocation the wider design may
 * assume — the Equinix Terraform provider has no cabinet or cross-connect resources,
 * so those cannot be expressed in HCL at all.</p>
 *
 * <p>This class is stateless and thread-safe; a single instance may be reused.</p>
 */
public class TerraformExporter {

    private static final String INDENT = "  ";

    /**
     * Exports the given deployment plan as Equinix Terraform provider HCL.
     *
     * <p>The output is a complete document but not immediately appliable: before
     * {@code terraform apply}, the caller must configure the Equinix provider's credentials and
     * supply a value for each generated input {@code variable} — the per-connection cloud
     * authorization key (sensitive) and DOT1Q VLAN tag, exactly the customer inputs
     * {@code DeploymentPlan.getRequiredInputs()} enumerates. The generated header comment
     * repeats this.</p>
     *
     * @param plan the deployment plan to export; must not be {@code null}
     * @return an HCL document. Resources are emitted in dependency
     *         order (cloud routers, then connections, then routing protocols), and
     *         later resources reference earlier ones via Terraform resource expressions.
     * @throws IllegalArgumentException if {@code plan} is {@code null}
     */
    public String export(DeploymentPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }

        StringBuilder hcl = new StringBuilder();
        writeHeader(hcl, plan);

        // Map of planned cloud-router name -> Terraform resource label, so connections
        // can reference the routers they sit on via interpolation.
        Map<String, String> routerLabels = new LinkedHashMap<>();
        // Map of planned connection name -> Terraform resource label, so routing
        // protocols can reference their parent connection.
        Map<String, String> connectionLabels = new LinkedHashMap<>();
        // Maps of planned connection name -> declared variable name for the customer
        // inputs the plan leaves unresolved (auth key, VLAN tag).
        Map<String, String> authKeyVariables = new LinkedHashMap<>();
        Map<String, String> vlanVariables = new LinkedHashMap<>();

        writeInputVariables(hcl, plan, authKeyVariables, vlanVariables);
        writeCloudRouters(hcl, plan, routerLabels);
        writeProviderConnections(hcl, plan, routerLabels, connectionLabels, authKeyVariables, vlanVariables);
        writeBackboneLinks(hcl, plan, routerLabels, connectionLabels);
        writeRoutingProtocols(hcl, plan, connectionLabels);

        return hcl.toString();
    }

    private void writeHeader(StringBuilder hcl, DeploymentPlan plan) {
        hcl.append("# ----------------------------------------------------------------------------\n");
        hcl.append("# Equinix Fabric deployment - generated by the Equinix Java SDK\n");
        hcl.append("# Deployment Wizard (api.equinix.javasdk.design.export.TerraformExporter).\n");
        hcl.append("#\n");
        hcl.append("# ").append(plan.toSummary().replace("\n", "\n# ")).append("\n");
        hcl.append("#\n");
        hcl.append("# Review carefully before running `terraform apply`. Some provider attributes\n");
        hcl.append("# (e.g. credentials, account/project context) may need to be supplied via\n");
        hcl.append("# variables before this configuration is complete.\n");
        hcl.append("# ----------------------------------------------------------------------------\n\n");

        hcl.append("terraform {\n");
        hcl.append(INDENT).append("required_providers {\n");
        hcl.append(INDENT).append(INDENT).append("equinix = {\n");
        hcl.append(INDENT).append(INDENT).append(INDENT).append("source = \"equinix/equinix\"\n");
        hcl.append(INDENT).append(INDENT).append("}\n");
        hcl.append(INDENT).append("}\n");
        hcl.append("}\n\n");
    }

    /**
     * Declares one {@code variable} block per customer input a cloud provider connection still
     * needs at provisioning: the cloud-specific authorization key (marked {@code sensitive},
     * with a description naming the exact key to gather — e.g. "AWS Account ID (12-digit)"),
     * and the DOT1Q VLAN tag when the plan does not already carry one. The connection's Z-side
     * references these via {@code var.<name>}, so the export never fabricates or inlines a
     * secret. Emits nothing when no provider connection needs an input.
     */
    private void writeInputVariables(StringBuilder hcl, DeploymentPlan plan,
                                     Map<String, String> authKeyVariables,
                                     Map<String, String> vlanVariables) {
        List<PlannedConnection> connections = plan.getProviderConnections();
        if (connections == null || connections.isEmpty()) {
            return;
        }

        StringBuilder vars = new StringBuilder();
        List<String> usedNames = new ArrayList<>();
        for (PlannedConnection conn : connections) {
            ConnectionInputRequirement requirement = requirementFor(plan, conn);
            if (needsAuthenticationKey(requirement, conn)) {
                String varName = uniqueName(usedNames, sanitizeLabel(conn.getName()) + "_auth_key");
                authKeyVariables.put(conn.getName(), varName);
                vars.append("variable \"").append(varName).append("\" {\n");
                vars.append(rawAttr(1, "type", "string"));
                vars.append(rawAttr(1, "sensitive", "true"));
                vars.append(attr(1, "description",
                        authenticationKeyLabel(requirement, conn) + " for connection " + conn.getName()));
                vars.append("}\n\n");
            }
            if (conn.getZSideVlanTag() == null && needsVlanTag(requirement, conn)) {
                String varName = uniqueName(usedNames, sanitizeLabel(conn.getName()) + "_vlan");
                vlanVariables.put(conn.getName(), varName);
                vars.append("variable \"").append(varName).append("\" {\n");
                vars.append(rawAttr(1, "type", "number"));
                vars.append(attr(1, "description",
                        "DOT1Q VLAN tag for connection " + conn.getName()));
                vars.append("}\n\n");
            }
        }
        if (vars.length() == 0) {
            return;
        }

        hcl.append("# === Required Customer Inputs ===\n");
        hcl.append("# Cloud provider connections need customer-gathered authorization at\n");
        hcl.append("# provisioning; supply these via terraform.tfvars or -var (never commit\n");
        hcl.append("# secrets to version control).\n\n");
        hcl.append(vars);
    }

    private void writeCloudRouters(StringBuilder hcl, DeploymentPlan plan, Map<String, String> routerLabels) {
        List<PlannedCloudRouter> routers = plan.getCloudRouters();
        if (routers == null || routers.isEmpty()) {
            return;
        }

        hcl.append("# === Cloud Routers ===\n\n");
        for (PlannedCloudRouter cr : routers) {
            String label = uniqueLabel(routerLabels.values(), "fcr", cr.getName());
            routerLabels.put(cr.getName(), label);

            hcl.append("resource \"equinix_fabric_cloud_router\" \"").append(label).append("\" {\n");
            hcl.append(attr(1, "name", cr.getName()));
            hcl.append(attr(1, "type", "XF_ROUTER"));

            hcl.append(INDENT).append("package {\n");
            hcl.append(attr(2, "code", cr.getPackageCode() != null ? cr.getPackageCode().name() : null));
            hcl.append(INDENT).append("}\n");

            hcl.append(INDENT).append("location {\n");
            hcl.append(attr(2, "metro_code", String.valueOf(cr.getMetroId())));
            hcl.append(INDENT).append("}\n");

            if (cr.getAccountNumber() != null) {
                hcl.append(INDENT).append("account {\n");
                hcl.append(rawAttr(2, "account_number", String.valueOf(cr.getAccountNumber())));
                hcl.append(INDENT).append("}\n");
            }

            if (cr.getProjectId() != null) {
                hcl.append(INDENT).append("project {\n");
                hcl.append(attr(2, "project_id", cr.getProjectId()));
                hcl.append(INDENT).append("}\n");
            }

            writeNotifications(hcl, cr.getNotificationEmails());

            hcl.append("}\n\n");
        }
    }

    private void writeProviderConnections(StringBuilder hcl, DeploymentPlan plan,
                                          Map<String, String> routerLabels,
                                          Map<String, String> connectionLabels,
                                          Map<String, String> authKeyVariables,
                                          Map<String, String> vlanVariables) {
        List<PlannedConnection> connections = plan.getProviderConnections();
        if (connections == null || connections.isEmpty()) {
            return;
        }

        hcl.append("# === Provider Connections ===\n\n");
        for (PlannedConnection conn : connections) {
            String label = uniqueLabel(connectionLabels.values(), "conn", conn.getName());
            connectionLabels.put(conn.getName(), label);

            hcl.append("resource \"equinix_fabric_connection\" \"").append(label).append("\" {\n");
            hcl.append(attr(1, "name", conn.getName()));
            if (conn.getConnectionType() != null) {
                hcl.append(attr(1, "type", String.valueOf(conn.getConnectionType())));
            }
            hcl.append(rawAttr(1, "bandwidth", String.valueOf(conn.getBandwidthMbps())));

            writeNotifications(hcl, conn.getNotificationEmails());
            writeRedundancy(hcl, conn);

            // A-side: the Cloud Router this connection originates from.
            writeASideRouter(hcl, conn.getASideRouterName(), routerLabels);

            // Z-side: a service-profile redemption (provider connection).
            hcl.append(INDENT).append("z_side {\n");
            hcl.append(INDENT).append(INDENT).append("access_point {\n");
            hcl.append(attr(3, "type", "SP"));
            String authKeyVariable = authKeyVariables.get(conn.getName());
            if (authKeyVariable != null) {
                hcl.append(rawAttr(3, "authentication_key", "var." + authKeyVariable));
            }
            if (conn.getZSideServiceProfileUuid() != null) {
                hcl.append(INDENT).append(INDENT).append(INDENT).append("profile {\n");
                hcl.append(attr(4, "type", "L2_PROFILE"));
                hcl.append(attr(4, "uuid", conn.getZSideServiceProfileUuid()));
                hcl.append(INDENT).append(INDENT).append(INDENT).append("}\n");
            }
            else if (conn.getZSideProviderLabel() != null) {
                hcl.append(comment(3, "Service profile UUID not resolved for provider: "
                        + conn.getZSideProviderLabel()));
            }
            Integer vlanTag = conn.getZSideVlanTag();
            String vlanVariable = vlanVariables.get(conn.getName());
            if (vlanTag != null || vlanVariable != null) {
                hcl.append(INDENT).append(INDENT).append(INDENT).append("link_protocol {\n");
                hcl.append(attr(4, "type", "DOT1Q"));
                hcl.append(rawAttr(4, "vlan_tag",
                        vlanTag != null ? String.valueOf(vlanTag) : "var." + vlanVariable));
                hcl.append(INDENT).append(INDENT).append(INDENT).append("}\n");
            }
            // The provider edge metro the service profile is redeemed in.
            MetroId zSideMetro = conn.getZSideMetro() != null ? conn.getZSideMetro() : conn.getASideMetro();
            if (zSideMetro != null) {
                hcl.append(INDENT).append(INDENT).append(INDENT).append("location {\n");
                hcl.append(attr(4, "metro_code", String.valueOf(zSideMetro)));
                hcl.append(INDENT).append(INDENT).append(INDENT).append("}\n");
            }
            if (conn.getZSideSellerRegion() != null) {
                hcl.append(attr(3, "seller_region", conn.getZSideSellerRegion()));
            }
            hcl.append(INDENT).append(INDENT).append("}\n");
            hcl.append(INDENT).append("}\n");

            hcl.append("}\n\n");
        }
    }

    private void writeBackboneLinks(StringBuilder hcl, DeploymentPlan plan,
                                    Map<String, String> routerLabels,
                                    Map<String, String> connectionLabels) {
        List<PlannedBackboneLink> links = plan.getBackboneLinks();
        if (links == null || links.isEmpty()) {
            return;
        }

        hcl.append("# === Backbone Links (Cloud Router to Cloud Router) ===\n\n");
        for (PlannedBackboneLink link : links) {
            PlannedConnection conn = link.getConnection();
            String connName = conn != null ? conn.getName() : link.getName();
            String label = uniqueLabel(connectionLabels.values(), "backbone", connName);
            connectionLabels.put(connName, label);

            hcl.append("resource \"equinix_fabric_connection\" \"").append(label).append("\" {\n");
            hcl.append(attr(1, "name", link.getName()));
            if (conn != null && conn.getConnectionType() != null) {
                hcl.append(attr(1, "type", String.valueOf(conn.getConnectionType())));
            }
            hcl.append(rawAttr(1, "bandwidth", String.valueOf(link.getBandwidthMbps())));

            // Notifications are mandatory on a Fabric connection; a backbone link's planned
            // connection carries the same notification recipients as a provider connection.
            writeNotifications(hcl, conn != null ? conn.getNotificationEmails() : null);
            writeRedundancy(hcl, conn);

            // A-side router.
            String aRouterName = conn != null ? conn.getASideRouterName() : null;
            writeASideRouter(hcl, aRouterName, routerLabels);

            // Z-side router (the far end of the backbone link).
            String zRouterName = conn != null ? conn.getZSideRouterName() : null;
            hcl.append(INDENT).append("z_side {\n");
            hcl.append(INDENT).append(INDENT).append("access_point {\n");
            hcl.append(attr(3, "type", "CLOUD_ROUTER"));
            String zLabel = zRouterName != null ? routerLabels.get(zRouterName) : null;
            if (zLabel != null) {
                hcl.append(INDENT).append(INDENT).append(INDENT).append("router {\n");
                hcl.append(rawAttr(4, "uuid",
                        "equinix_fabric_cloud_router." + zLabel + ".id"));
                hcl.append(INDENT).append(INDENT).append(INDENT).append("}\n");
            }
            else if (zRouterName != null) {
                hcl.append(comment(3, "Z-side Cloud Router not found in plan: " + zRouterName));
            }
            hcl.append(INDENT).append(INDENT).append("}\n");
            hcl.append(INDENT).append("}\n");

            hcl.append("}\n\n");
        }
    }

    private void writeASideRouter(StringBuilder hcl, String routerName, Map<String, String> routerLabels) {
        hcl.append(INDENT).append("a_side {\n");
        hcl.append(INDENT).append(INDENT).append("access_point {\n");
        hcl.append(attr(3, "type", "CLOUD_ROUTER"));
        String label = routerName != null ? routerLabels.get(routerName) : null;
        if (label != null) {
            hcl.append(INDENT).append(INDENT).append(INDENT).append("router {\n");
            hcl.append(rawAttr(4, "uuid", "equinix_fabric_cloud_router." + label + ".id"));
            hcl.append(INDENT).append(INDENT).append(INDENT).append("}\n");
        }
        else if (routerName != null) {
            hcl.append(comment(3, "A-side Cloud Router not found in plan: " + routerName));
        }
        hcl.append(INDENT).append(INDENT).append("}\n");
        hcl.append(INDENT).append("}\n");
    }

    /**
     * Emits the {@code notifications} block Fabric mandates on Cloud Routers and connections,
     * carrying EVERY recipient the plan lists — {@code emails = ["a@x", "b@x"]} — never just the
     * first. No-op when the plan carries no email — the attribute cannot be fabricated.
     */
    private void writeNotifications(StringBuilder hcl, List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return;
        }
        StringBuilder list = new StringBuilder("[");
        boolean first = true;
        for (String email : emails) {
            if (email == null || email.isBlank()) {
                continue;
            }
            if (!first) {
                list.append(", ");
            }
            list.append(quote(email));
            first = false;
        }
        list.append("]");
        if (first) {
            return; // every entry was blank — nothing to emit
        }
        hcl.append(INDENT).append("notifications {\n");
        hcl.append(attr(2, "type", "ALL"));
        hcl.append(rawAttr(2, "emails", list.toString()));
        hcl.append(INDENT).append("}\n");
    }

    /**
     * Emits the connection-level {@code redundancy { group, priority }} block when the planned
     * connection names a redundancy group, mirroring how the wizard stamps it on the wire body.
     * A group with no explicit role defaults to {@code PRIMARY} (the same default the execution
     * path uses) rather than emitting an ambiguous group with no priority.
     */
    private void writeRedundancy(StringBuilder hcl, PlannedConnection conn) {
        if (conn == null) {
            return;
        }
        String group = conn.getZSideRedundancyGroup();
        if (group == null || group.isBlank()) {
            return;
        }
        RedundancyPriority priority = conn.getRedundancyPriority() != null
                ? conn.getRedundancyPriority()
                : RedundancyPriority.PRIMARY;
        hcl.append(INDENT).append("redundancy {\n");
        hcl.append(attr(2, "group", group));
        hcl.append(attr(2, "priority", priority.name()));
        hcl.append(INDENT).append("}\n");
    }

    private void writeRoutingProtocols(StringBuilder hcl, DeploymentPlan plan,
                                       Map<String, String> connectionLabels) {
        List<PlannedRoutingProtocol> protocols = plan.getRoutingProtocols();
        if (protocols == null || protocols.isEmpty()) {
            return;
        }

        // Pre-compute every protocol's resource label (in list order, so labels are stable),
        // and index the DIRECT protocol per connection: Fabric requires DIRECT to exist before
        // BGP on the same connection, so the BGP resource must depends_on its DIRECT sibling
        // regardless of the order the plan lists them in.
        List<String> labels = new ArrayList<>(protocols.size());
        Map<String, String> directLabelByConnection = new LinkedHashMap<>();
        for (PlannedRoutingProtocol rp : protocols) {
            String label = uniqueLabel(labels, "rp", rp.getName());
            labels.add(label);
            if (rp.getType() == RoutingProtocolType.DIRECT && rp.getConnectionName() != null) {
                directLabelByConnection.putIfAbsent(rp.getConnectionName(), label);
            }
        }

        hcl.append("# === Routing Protocols ===\n\n");
        for (int i = 0; i < protocols.size(); i++) {
            PlannedRoutingProtocol rp = protocols.get(i);
            String label = labels.get(i);

            hcl.append("resource \"equinix_fabric_routing_protocol\" \"").append(label).append("\" {\n");
            hcl.append(attr(1, "name", rp.getName()));
            if (rp.getType() != null) {
                hcl.append(attr(1, "type", String.valueOf(rp.getType())));
            }

            // Bind the protocol to its parent connection.
            String connLabel = connectionLabels.get(rp.getConnectionName());
            if (connLabel != null) {
                hcl.append(rawAttr(1, "connection_uuid",
                        "equinix_fabric_connection." + connLabel + ".id"));
            }
            else if (rp.getConnectionName() != null) {
                hcl.append(comment(1, "Parent connection not found in plan: " + rp.getConnectionName()));
            }

            if (rp.getType() == RoutingProtocolType.DIRECT) {
                writeDirectProtocol(hcl, rp);
            }
            else if (rp.getType() == RoutingProtocolType.BGP) {
                writeBgpProtocol(hcl, rp);
                String directLabel = rp.getConnectionName() != null
                        ? directLabelByConnection.get(rp.getConnectionName())
                        : null;
                if (directLabel != null && !directLabel.equals(label)) {
                    hcl.append(comment(1, "Fabric requires the DIRECT protocol to exist before BGP "
                            + "on the same connection."));
                    hcl.append(rawAttr(1, "depends_on",
                            "[equinix_fabric_routing_protocol." + directLabel + "]"));
                }
            }

            hcl.append("}\n\n");
        }
    }

    private void writeDirectProtocol(StringBuilder hcl, PlannedRoutingProtocol rp) {
        if (rp.getEquinixIfaceIpv4() != null) {
            hcl.append(INDENT).append("direct_ipv4 {\n");
            hcl.append(attr(2, "equinix_iface_ip", rp.getEquinixIfaceIpv4()));
            hcl.append(INDENT).append("}\n");
        }
    }

    private void writeBgpProtocol(StringBuilder hcl, PlannedRoutingProtocol rp) {
        if (rp.getCustomerAsn() != null) {
            hcl.append(rawAttr(1, "customer_asn", String.valueOf(rp.getCustomerAsn())));
        }
        if (rp.getCustomerPeerIpv4() != null || rp.getEquinixPeerIpv4() != null) {
            hcl.append(INDENT).append("bgp_ipv4 {\n");
            if (rp.getCustomerPeerIpv4() != null) {
                hcl.append(attr(2, "customer_peer_ip", rp.getCustomerPeerIpv4()));
            }
            if (rp.getEquinixPeerIpv4() != null) {
                hcl.append(attr(2, "equinix_peer_ip", rp.getEquinixPeerIpv4()));
            }
            hcl.append(rawAttr(2, "enabled", "true"));
            hcl.append(INDENT).append("}\n");
        }
        if (rp.isBfdEnabled()) {
            hcl.append(INDENT).append("bfd {\n");
            hcl.append(rawAttr(2, "enabled", "true"));
            hcl.append(attr(2, "interval", String.valueOf(rp.getBfdInterval())));
            hcl.append(INDENT).append("}\n");
        }
    }

    // ---- Customer-input resolution helpers ----

    /**
     * The plan's enumerated input requirement for this connection, when the plan carries one
     * (the wizard populates {@code requiredInputs} per provider connection). {@code null} for a
     * hand-built plan without requirements — the exporter then derives the need from the
     * connection's resolved cloud type.
     */
    private ConnectionInputRequirement requirementFor(DeploymentPlan plan, PlannedConnection conn) {
        if (plan.getRequiredInputs() == null || conn.getName() == null) {
            return null;
        }
        for (ConnectionInputRequirement requirement : plan.getRequiredInputs()) {
            if (conn.getName().equals(requirement.getConnectionName())) {
                return requirement;
            }
        }
        return null;
    }

    private boolean needsAuthenticationKey(ConnectionInputRequirement requirement, PlannedConnection conn) {
        if (requirement != null) {
            return requirement.isAuthenticationKeyRequired();
        }
        // Same derivation the execution path uses: every well-known cloud needs a key; a
        // third-party (OTHER) service profile needs none.
        return resolveCloudType(conn) != CloudProviderType.OTHER;
    }

    private boolean needsVlanTag(ConnectionInputRequirement requirement, PlannedConnection conn) {
        if (requirement != null) {
            return requirement.isVlanTagRequired();
        }
        // A cloud VC is DOT1Q-encapsulated and always needs a VLAN tag.
        return resolveCloudType(conn) != CloudProviderType.OTHER;
    }

    private CloudProviderType resolveCloudType(PlannedConnection conn) {
        return conn.getZSideCloudType() != null
                ? conn.getZSideCloudType()
                : ConnectionBodies.resolveCloudType(conn.getZSideProviderLabel());
    }

    private String authenticationKeyLabel(ConnectionInputRequirement requirement, PlannedConnection conn) {
        if (requirement != null && requirement.getAuthenticationKeyLabel() != null) {
            return requirement.getAuthenticationKeyLabel();
        }
        return ConnectionBodies.authenticationKeyLabel(resolveCloudType(conn));
    }

    // ---- HCL formatting helpers ----

    private String attr(int depth, String key, String value) {
        return indent(depth) + key + " = " + quote(value) + "\n";
    }

    private String rawAttr(int depth, String key, String value) {
        return indent(depth) + key + " = " + value + "\n";
    }

    private String comment(int depth, String text) {
        return indent(depth) + "# " + text + "\n";
    }

    private String indent(int depth) {
        return INDENT.repeat(depth);
    }

    private String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("${", "$${").replace("%{", "%%{")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    /**
     * Derives a valid, unique Terraform resource label from a display name. Terraform
     * labels must start with a letter/underscore and contain only letters, digits,
     * underscores and dashes; we sanitize and de-duplicate against already-used labels.
     */
    private String uniqueLabel(Iterable<String> existing, String prefix, String name) {
        String sanitized = sanitizeLabel(name);
        String base = prefix + "_" + sanitized;
        String candidate = base;
        int suffix = 2;
        while (contains(existing, candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    /**
     * De-duplicates a fully-formed name (no prefix) against the already-used names, then
     * records and returns it. Used for {@code variable} names, which carry a semantic suffix
     * ({@code _auth_key}, {@code _vlan}) rather than a resource-type prefix.
     */
    private String uniqueName(List<String> used, String base) {
        String candidate = base;
        int suffix = 2;
        while (used.contains(candidate)) {
            candidate = base + "_" + suffix++;
        }
        used.add(candidate);
        return candidate;
    }

    private boolean contains(Iterable<String> existing, String candidate) {
        for (String e : existing) {
            if (e.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String sanitizeLabel(String name) {
        if (name == null || name.isBlank()) {
            return "resource";
        }
        String cleaned = name.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        // Collapse runs of underscores and trim leading/trailing ones for readability.
        cleaned = cleaned.replaceAll("_{2,}", "_").replaceAll("^_+|_+$", "");
        if (cleaned.isEmpty()) {
            return "resource";
        }
        char first = cleaned.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            cleaned = "_" + cleaned;
        }
        return cleaned;
    }
}
