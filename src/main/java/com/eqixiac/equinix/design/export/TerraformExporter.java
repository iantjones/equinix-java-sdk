package com.eqixiac.equinix.design.export;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.enums.RedundancyPriority;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.ConnectionBodies;
import com.eqixiac.equinix.design.optimizer.wizard.model.ConnectionInputRequirement;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * <h2>Native multicloud links</h2>
 * <p><b>Beta.</b> A {@link PlannedMulticloudInterconnect} on the plan
 * ({@code DeploymentPlan.multicloudLinksOrEmpty()}) is a direct link between two cloud
 * providers that uses no Equinix resource. The Equinix Terraform provider has no resource
 * type for it, and this exporter emits no cloud-provider resource ({@code aws_*},
 * {@code google_*}, {@code oci_*}, {@code azurerm_*}) and no cloud-provider {@code provider}
 * block. Per link the output holds:</p>
 * <table>
 *   <caption>Output per native multicloud link</caption>
 *   <tr><th>Output</th><th>Content</th></tr>
 *   <tr><td>Comment block, delimited by {@code BEGIN native multicloud link} and
 *       {@code END native multicloud link} lines; every line starts with {@code #}</td>
 *       <td>Both providers and regions; the bandwidth the link would be ordered at and the
 *       requested bandwidth, in Mbps; the catalog environment's status, as-of date and source
 *       URLs; the role and strategy; the create-then-accept procedure (create on one provider
 *       with the other provider's account identifier, receive the activation key, accept on the
 *       other provider) with the AWS console path, the {@code gcloud network-connectivity
 *       transports create} command line, or the OCI FastConnect "Service key" field for the
 *       providers of the link; the steps recorded on the plan, verbatim</td></tr>
 *   <tr><td>{@code variable "<sanitized name>_destination_account_id"}</td>
 *       <td>{@code type = string}, with a description naming the identifier to record. For a
 *       {@code REPLACEMENT} link the variable has no default, so Terraform requires a value.
 *       For any other role it has {@code default = null}, so a configuration that does not
 *       depend on the link needs no value. No resource references the variable. Not emitted for
 *       an {@code UNAVAILABLE} entry.</td></tr>
 * </table>
 * <p>Links add no {@code resource} block: the number of {@code resource} blocks equals
 * {@code DeploymentPlan.totalResourceCount()} with or without links. The header comment states
 * the number of links and, for each {@code REPLACEMENT} link, the names of the Equinix
 * connections this configuration omits because of it. The activation key is never an input of
 * the configuration. Provider procedures are copies of provider documentation retrieved
 * 2026-09-21 and go stale; each is printed with its source URL. Provider-side procedures are
 * printed for two pairs only: AWS with Google Cloud, and AWS with Oracle Cloud. For AWS with
 * Azure (preview) and for every other pair the block states that no procedure was verified.
 * The output for a plan without links contains none of the above.</p>
 *
 * <p>Plan-supplied text written into a comment has its line breaks replaced by spaces, so a
 * name or recipe line cannot end the comment and start an HCL construct.</p>
 *
 * <p>This class is stateless and thread-safe; a single instance may be reused.</p>
 */
public class TerraformExporter {

    private static final String INDENT = "  ";

    /** Maximum width, in characters, of a wrapped comment line. A single longer word (a URL) is not split. */
    private static final int COMMENT_WIDTH = 100;

    private static final String AWS_MULTICLOUD_GETTING_STARTED =
            "https://docs.aws.amazon.com/interconnect/latest/userguide/getting-started-multicloud.html";

    private static final String GCLOUD_TRANSPORTS_CREATE =
            "https://docs.cloud.google.com/sdk/gcloud/reference/network-connectivity/transports/create";

    private static final String OCI_INTERCONNECT_FOR_AWS =
            "https://docs.oracle.com/en-us/iaas/Content/multicloud/interconnect-aws.htm";

    /** The date the three provider pages above were read. */
    private static final String PROVIDER_DOCS_RETRIEVED = "2026-09-21";

    /**
     * The values {@code gcloud network-connectivity transports create --bandwidth} accepts
     * (source: the {@code GCLOUD_TRANSPORTS_CREATE} page, retrieved 2026-09-21).
     */
    private static final Set<String> GCLOUD_TRANSPORT_BANDWIDTHS = Set.of(
            "50m", "100m", "200m", "300m", "400m", "500m",
            "1g", "2g", "5g", "10g", "20g", "50g", "100g");

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
     * <p><b>Beta.</b> Native multicloud links on the plan are written last, as comment blocks
     * plus one {@code <name>_destination_account_id} variable each; see the class documentation.
     * A {@code REPLACEMENT} link's variable has no default and therefore also needs a value.</p>
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
        // Every variable name declared in the document, so a native link's variable cannot
        // collide with a connection's.
        List<String> usedVariableNames = new ArrayList<>();

        writeInputVariables(hcl, plan, authKeyVariables, vlanVariables, usedVariableNames);
        writeCloudRouters(hcl, plan, routerLabels);
        writeProviderConnections(hcl, plan, routerLabels, connectionLabels, authKeyVariables, vlanVariables);
        writeBackboneLinks(hcl, plan, routerLabels, connectionLabels);
        writeRoutingProtocols(hcl, plan, connectionLabels);
        writeMulticloudLinks(hcl, plan, usedVariableNames);

        return hcl.toString();
    }

    private void writeHeader(StringBuilder hcl, DeploymentPlan plan) {
        hcl.append("# ----------------------------------------------------------------------------\n");
        hcl.append("# Equinix Fabric deployment - generated by the Equinix Java SDK\n");
        hcl.append("# Deployment Wizard (com.eqixiac.equinix.design.export.TerraformExporter).\n");
        hcl.append("#\n");
        hcl.append("# ").append(plan.toSummary().replace("\n", "\n# ")).append("\n");
        writeMulticloudHeaderNotes(hcl, plan);
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
                                     Map<String, String> vlanVariables,
                                     List<String> usedNames) {
        List<PlannedConnection> connections = plan.getProviderConnections();
        if (connections == null || connections.isEmpty()) {
            return;
        }

        StringBuilder vars = new StringBuilder();
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

    // ---- Native multicloud links (Beta) ----

    /**
     * Adds the header lines about native multicloud links: their count, the statement that the
     * file holds no resource for them, and one line per {@code REPLACEMENT} entry (naming the
     * Equinix connections the configuration omits) and per {@code UNAVAILABLE} entry. Emits
     * nothing when the plan has no link.
     */
    private void writeMulticloudHeaderNotes(StringBuilder hcl, DeploymentPlan plan) {
        List<PlannedMulticloudInterconnect> links = multicloudLinks(plan);
        if (links.isEmpty()) {
            return;
        }
        hcl.append("#\n");
        commentWrapped(hcl, "# ", "# ", "Native multicloud links (Beta): " + links.size()
                + ". Each is a direct link between two cloud providers that uses no Equinix resource. "
                + "This file holds no resource for them: the Equinix Terraform provider has no such "
                + "resource type, and no cloud-provider resource is emitted. Each link appears at the end "
                + "of this file as a comment block plus one input variable (none for an UNAVAILABLE entry). "
                + "The resource counts above exclude them.");
        for (PlannedMulticloudInterconnect link : links) {
            if (link.getRole() == MulticloudLinkRole.REPLACEMENT) {
                commentWrapped(hcl, "# ", "#   ", "REPLACEMENT: native link " + displayName(link) + " ("
                        + pairLabel(link) + ") carries its flow in place of " + replacedLabel(link)
                        + ". Those connections and their routing protocols are not in this configuration. "
                        + "The flow has no path until the link is created with the two cloud providers.");
            }
            else if (link.getRole() == MulticloudLinkRole.UNAVAILABLE) {
                commentWrapped(hcl, "# ", "#   ", "UNAVAILABLE: a native link was required for "
                        + pairLabel(link) + " (" + displayName(link) + ") and the plan's catalog has no "
                        + "usable environment for it. The plan is invalid.");
            }
        }
    }

    /**
     * Writes one delimited comment block per native multicloud link, followed by the link's
     * {@code <name>_destination_account_id} variable. No {@code resource} or {@code provider}
     * block is written. Emits nothing when the plan has no link.
     */
    private void writeMulticloudLinks(StringBuilder hcl, DeploymentPlan plan, List<String> usedVariableNames) {
        List<PlannedMulticloudInterconnect> links = multicloudLinks(plan);
        if (links.isEmpty()) {
            return;
        }

        hcl.append("# === Native Multicloud Links (Beta; comments and input variables only) ===\n");
        commentWrapped(hcl, "# ", "# ", "A native multicloud link is a direct connection between two cloud "
                + "providers. It uses no Equinix resource, and the customer creates it with the two providers, "
                + "outside Terraform. Nothing in this section is a resource, and `terraform apply` creates "
                + "nothing for a link. Provider procedures were read on " + PROVIDER_DOCS_RETRIEVED
                + " and go stale; check each source URL before use.");
        hcl.append("\n");

        for (PlannedMulticloudInterconnect link : links) {
            String name = displayName(link);
            String variable = link.getRole() == MulticloudLinkRole.UNAVAILABLE
                    ? null
                    : uniqueName(usedVariableNames, variableBase(link) + "_destination_account_id");

            hcl.append("# ---- BEGIN native multicloud link: ").append(name)
                    .append(" (not a Terraform resource) ----\n");
            field(hcl, "Providers", pairLabel(link));
            field(hcl, "Bandwidth", bandwidthLabel(link));
            field(hcl, "Environment", environmentLabel(link));
            MulticloudEnvironment environment = link.getEnvironment();
            if (environment != null) {
                for (String url : environment.getSourceUrls()) {
                    field(hcl, "Source", url);
                }
                if (environment.getNote() != null && !environment.getNote().isBlank()) {
                    field(hcl, "Note", environment.getNote());
                }
            }
            field(hcl, "Role", roleLabel(link));
            if (link.getWorkloadLabels() != null && !link.getWorkloadLabels().isEmpty()) {
                field(hcl, "Workloads", String.join(", ", link.getWorkloadLabels()));
            }
            writeCreateThenAccept(hcl, link, variable);
            hcl.append("# ---- END native multicloud link: ").append(name).append(" ----\n");

            if (variable != null) {
                hcl.append("\n");
                hcl.append("variable \"").append(variable).append("\" {\n");
                hcl.append(rawAttr(1, "type", "string"));
                if (link.getRole() != MulticloudLinkRole.REPLACEMENT) {
                    // The configuration does not depend on the link, so it must plan without a value.
                    hcl.append(rawAttr(1, "default", "null"));
                }
                hcl.append(attr(1, "description", destinationAccountDescription(link)));
                hcl.append("}\n");
            }
            hcl.append("\n");
        }
    }

    /**
     * Writes the create-then-accept procedure of one link as comment lines: the three generic
     * steps, the provider-side console path or command line for each provider of a pair that
     * includes AWS, and the steps recorded on the plan. {@code variable} is the link's
     * destination-account-id variable name, or {@code null} for an {@code UNAVAILABLE} entry,
     * for which only a statement that there is nothing to create is written.
     */
    private void writeCreateThenAccept(StringBuilder hcl, PlannedMulticloudInterconnect link, String variable) {
        hcl.append("#\n");
        if (variable == null) {
            commentWrapped(hcl, "# ", "# ", "No procedure: the plan's catalog has no usable environment for "
                    + "this pair, so there is no link to create and no variable is emitted.");
            return;
        }
        hcl.append("# Create-then-accept procedure (performed by the customer with the two providers):\n");
        commentWrapped(hcl, "#   1. ", "#      ", "Create: on one provider, request the link and enter the "
                + "account identifier of the other (accepting) provider. Record that identifier in var."
                + variable + ".");
        commentWrapped(hcl, "#   2. ", "#      ", "The creating provider returns an activation key. The key "
                + "passes between the two providers' consoles or CLIs and is not an input of this configuration.");
        commentWrapped(hcl, "#   3. ", "#      ", "Accept: on the other provider, submit the activation key.");

        CloudProviderType a = link.getProviderA();
        CloudProviderType z = link.getProviderZ();
        boolean includesAws = a == CloudProviderType.AWS || z == CloudProviderType.AWS;
        CloudProviderType peer = a == CloudProviderType.AWS ? z : a;
        boolean documentedPeer = peer == CloudProviderType.GOOGLE_CLOUD || peer == CloudProviderType.ORACLE_CLOUD;

        if (includesAws && documentedPeer) {
            hcl.append("# Provider-side reference (retrieved ").append(PROVIDER_DOCS_RETRIEVED).append("):\n");
            String peerIdentifier = peer == CloudProviderType.GOOGLE_CLOUD
                    ? "the Google Cloud project ID"
                    : "the OCI tenancy OCID (format ocid1.tenancy.oc1..<unique_ID>)";
            commentWrapped(hcl, "#   AWS: ", "#     ", "AWS Direct Connect console > AWS Interconnect > "
                    + "\"Create new multicloud Interconnect\" to create, or \"Accept multicloud Interconnect\" "
                    + "to accept a key issued by the other provider. The attach point is a Direct Connect "
                    + "gateway. On create, AWS asks for " + peerIdentifier + ".");
            hcl.append("#     ").append(AWS_MULTICLOUD_GETTING_STARTED).append("\n");
            if (peer == CloudProviderType.GOOGLE_CLOUD) {
                String region = shellSafeOr(regionOf(link, CloudProviderType.GOOGLE_CLOUD), "REGION");
                hcl.append("#   Google Cloud, accept a key issued by AWS:\n");
                hcl.append("#     gcloud network-connectivity transports create NAME --region=").append(region)
                        .append(" --network=NETWORK --activation-key=KEY\n");
                hcl.append("#   Google Cloud, create first (Google Cloud issues the key; "
                        + "--remote-account-id is the AWS account ID):\n");
                hcl.append("#     gcloud network-connectivity transports create NAME --region=").append(region)
                        .append(" --network=NETWORK --bandwidth=").append(gcloudBandwidth(link.getCoveringTierMbps()))
                        .append(" --remote-account-id=AWS_ACCOUNT_ID --remote-profile=REMOTE_PROFILE\n");
                hcl.append("#     ").append(GCLOUD_TRANSPORTS_CREATE).append("\n");
            }
            else {
                commentWrapped(hcl, "#   OCI: ", "#     ", "FastConnect > \"Create FastConnect\" > \"FastConnect "
                        + "interconnect\" (an interconnect virtual circuit attached to a dynamic routing "
                        + "gateway). With \"Configured in AWS first\", enter the AWS activation key in the "
                        + "\"Service key\" field. With \"Configure in OCI first\", OCI asks for the AWS account "
                        + "ID and issues the key, which AWS then accepts.");
                hcl.append("#     ").append(OCI_INTERCONNECT_FOR_AWS).append("\n");
            }
        }
        else if (includesAws && peer == CloudProviderType.AZURE) {
            commentWrapped(hcl, "# ", "#   ", "Provider-side reference: none for Microsoft Azure. AWS lists "
                    + "Azure as a preview provider and states that activation with a provider in public "
                    + "preview can require the CLI. No Azure-side procedure was verified as of "
                    + PROVIDER_DOCS_RETRIEVED + ".");
            hcl.append("#   ").append(AWS_MULTICLOUD_GETTING_STARTED).append("\n");
        }
        else {
            commentWrapped(hcl, "# ", "#   ", "Provider-side reference: none. No provider procedure was "
                    + "verified for this pair; follow the two providers' documentation.");
        }

        List<String> recorded = link.getCreateThenAcceptRecipe();
        if (recorded != null && !recorded.isEmpty()) {
            hcl.append("# Steps recorded on the plan:\n");
            int step = 1;
            for (String line : recorded) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                commentWrapped(hcl, "#   " + step + ". ", "#      ", line);
                step++;
            }
        }
    }

    /** The plan's native links with {@code null} elements removed; never {@code null}. */
    private List<PlannedMulticloudInterconnect> multicloudLinks(DeploymentPlan plan) {
        List<PlannedMulticloudInterconnect> links = new ArrayList<>();
        for (PlannedMulticloudInterconnect link : plan.multicloudLinksOrEmpty()) {
            if (link != null) {
                links.add(link);
            }
        }
        return links;
    }

    /** The link's name on one line, or {@code unnamed} when the plan gives none. */
    private String displayName(PlannedMulticloudInterconnect link) {
        String name = singleLine(link.getName());
        return name.isEmpty() ? "unnamed" : name;
    }

    /** The sanitized stem of the link's variable name. */
    private String variableBase(PlannedMulticloudInterconnect link) {
        return link.getName() == null || link.getName().isBlank()
                ? "multicloud_link"
                : sanitizeLabel(link.getName());
    }

    private String pairLabel(PlannedMulticloudInterconnect link) {
        return sideLabel(link.getProviderA(), link.getRegionA()) + " <-> "
                + sideLabel(link.getProviderZ(), link.getRegionZ());
    }

    private String sideLabel(CloudProviderType provider, String region) {
        String name = provider == null ? "unspecified provider" : provider.getProviderName();
        return region == null || region.isBlank()
                ? name + " (no region on the plan)"
                : name + " " + singleLine(region);
    }

    private String bandwidthLabel(PlannedMulticloudInterconnect link) {
        if (link.getCoveringTierMbps() == null) {
            return link.getRequestedMbps() + " Mbps requested; "
                    + (link.getEnvironment() == null
                            ? "no environment to size it against"
                            : "the environment lists no size that covers it");
        }
        return link.getCoveringTierMbps() + " Mbps"
                + (link.isRoundedUp()
                        ? " (requested " + link.getRequestedMbps() + " Mbps, rounded up to the smallest listed size)"
                        : "");
    }

    private String environmentLabel(PlannedMulticloudInterconnect link) {
        MulticloudEnvironment environment = link.getEnvironment();
        if (environment == null) {
            return "none in the plan's catalog for this region pair";
        }
        return environment.getStatus()
                + (environment.getAsOf() != null ? ", observed " + environment.getAsOf() : ", no observation date")
                + "; listed sizes " + environment.sizesMbps()
                + "; catalog entry " + environment.getEnvironment().getEnvironmentId()
                + " (an id local to the catalog, not a provider API value)";
    }

    private String roleLabel(PlannedMulticloudInterconnect link) {
        String strategy = link.getStrategy() != null ? " (strategy " + link.getStrategy() + ")" : "";
        if (link.getRole() == null) {
            return "not stated on the plan" + strategy;
        }
        switch (link.getRole()) {
            case REPLACEMENT:
                return "REPLACEMENT" + strategy + ". This configuration omits " + replacedLabel(link)
                        + " and their routing protocols. The flow has no path until this link is created.";
            case UNAVAILABLE:
                return "UNAVAILABLE" + strategy + ". A native link was required and the plan's catalog has "
                        + "no usable environment. The plan is invalid.";
            default:
                List<String> kept = link.getEquinixConnectionNames();
                return "ALTERNATIVE" + strategy + ". The configuration does not depend on this link."
                        + (kept == null || kept.isEmpty()
                                ? ""
                                : " The Equinix connections for the flow stay in it: " + String.join(", ", kept) + ".");
        }
    }

    private String replacedLabel(PlannedMulticloudInterconnect link) {
        List<String> replaced = link.getReplacedConnectionNames();
        return replaced == null || replaced.isEmpty()
                ? "Equinix connections the plan does not name"
                : "the Equinix connection(s) " + String.join(", ", replaced);
    }

    /** The {@code description} of a link's destination-account-id variable. */
    private String destinationAccountDescription(PlannedMulticloudInterconnect link) {
        CloudProviderType a = link.getProviderA();
        CloudProviderType z = link.getProviderZ();
        boolean includesAws = a == CloudProviderType.AWS || z == CloudProviderType.AWS;
        CloudProviderType peer = a == CloudProviderType.AWS ? z : a;
        String identifier;
        if (includesAws && peer == CloudProviderType.GOOGLE_CLOUD) {
            identifier = "the Google Cloud project ID when AWS creates the link, or the 12-digit AWS account ID "
                    + "when Google Cloud creates it (gcloud flag --remote-account-id)";
        }
        else if (includesAws && peer == CloudProviderType.ORACLE_CLOUD) {
            identifier = "the OCI tenancy OCID (ocid1.tenancy.oc1..<unique_ID>) when AWS creates the link, or the "
                    + "12-digit AWS account ID when OCI creates it";
        }
        else {
            identifier = "the account, project or tenancy identifier the creating provider asks for";
        }
        return "Account identifier on the accepting provider of native multicloud link " + displayName(link)
                + " (" + pairLabel(link) + "), entered on the creating provider when the link is requested: "
                + identifier + ". No resource in this configuration reads it; it is declared so the value is "
                + "kept with the stack."
                + (link.getRole() == MulticloudLinkRole.REPLACEMENT
                        ? " Required: the plan omits Equinix connections and depends on this link."
                        : " Optional: the plan does not depend on this link.");
    }

    /** The link's region for the given provider, or {@code null} when the provider is not on the link. */
    private String regionOf(PlannedMulticloudInterconnect link, CloudProviderType provider) {
        if (link.getProviderA() == provider) {
            return link.getRegionA();
        }
        return link.getProviderZ() == provider ? link.getRegionZ() : null;
    }

    /**
     * {@code value} when it consists of letters, digits, dots, underscores and dashes only,
     * otherwise {@code placeholder}. Keeps plan-supplied text out of a command line a reader
     * may copy into a shell.
     */
    private String shellSafeOr(String value, String placeholder) {
        return value != null && value.trim().matches("[A-Za-z0-9._-]+") ? value.trim() : placeholder;
    }

    /**
     * The {@code --bandwidth} value for a size in Mbps ({@code 10000} gives {@code 10g},
     * {@code 500} gives {@code 500m}), or the placeholder {@code BANDWIDTH} when the size is
     * {@code null} or not a value the command documents.
     */
    private String gcloudBandwidth(Integer mbps) {
        if (mbps == null || mbps <= 0) {
            return "BANDWIDTH";
        }
        String value = mbps % 1000 == 0 ? (mbps / 1000) + "g" : mbps + "m";
        return GCLOUD_TRANSPORT_BANDWIDTHS.contains(value) ? value : "BANDWIDTH";
    }

    /** Writes {@code "# Label:       value"}, wrapped, with continuation lines aligned under the value. */
    private void field(StringBuilder hcl, String label, String value) {
        commentWrapped(hcl, "# " + String.format("%-13s", label + ":"), "# " + " ".repeat(13), value);
    }

    /**
     * Appends {@code text} as {@code #} comment lines wrapped at {@code COMMENT_WIDTH} characters.
     * The first line starts with {@code firstPrefix} and the rest with {@code continuationPrefix};
     * both must start with {@code #}. Line breaks in {@code text} become spaces, so the text
     * cannot end the comment. A word longer than the width is written unsplit on its own line.
     */
    private void commentWrapped(StringBuilder hcl, String firstPrefix, String continuationPrefix, String text) {
        String[] words = singleLine(text).split(" +");
        StringBuilder line = new StringBuilder(firstPrefix);
        boolean lineHasWord = false;
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (lineHasWord && line.length() + 1 + word.length() > COMMENT_WIDTH) {
                hcl.append(stripTrailing(line)).append("\n");
                line = new StringBuilder(continuationPrefix);
                lineHasWord = false;
            }
            if (lineHasWord) {
                line.append(' ');
            }
            line.append(word);
            lineHasWord = true;
        }
        hcl.append(stripTrailing(line)).append("\n");
    }

    private String stripTrailing(StringBuilder line) {
        return line.toString().stripTrailing();
    }

    /** {@code text} with every run of line terminators replaced by one space; {@code ""} for {@code null}. */
    private static String singleLine(String text) {
        return text == null ? "" : text.replaceAll("[\\r\\n\\u000B\\u000C\\u0085\\u2028\\u2029]+", " ").trim();
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
        // One line only: plan-supplied text (a provider label, a router name) must not end the comment.
        return indent(depth) + "# " + singleLine(text) + "\n";
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
