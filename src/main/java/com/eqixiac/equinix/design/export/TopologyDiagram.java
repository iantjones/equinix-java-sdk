package com.eqixiac.equinix.design.export;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.ConnectionBodies;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders deployment topologies as <a href="https://mermaid.js.org/">Mermaid</a>
 * {@code graph} text. Mermaid renders natively on GitHub, GitLab, and many docs
 * platforms, so the output is a portable, version-controllable diagram.
 *
 * <p>For a {@link DeploymentPlan}, each metro becomes a subgraph containing its
 * Cloud Router(s); provider connections are drawn as edges from a router to an
 * external provider node, and backbone links as edges between routers in different
 * metros. For an {@link OptimizationResult}, metros are nodes annotated with their
 * rank and score, optionally grouping the workloads placed in each metro.</p>
 *
 * <p><b>Beta.</b> A native multicloud link on the plan
 * ({@code DeploymentPlan.multicloudLinksOrEmpty()}) is drawn as a dashed bidirectional edge
 * ({@code <-.->}) between the two cloud nodes, never through a Cloud Router. Its label is
 * {@code native multicloud (outside Fabric)} followed by a second line with the bandwidth in
 * Mbps (the covering size, or the requested bandwidth when none covers it), the environment
 * status and the role. Each end attaches to the provider node already drawn for a connection
 * to the same cloud and region; when the plan has none (a {@code REPLACEMENT} link whose
 * connections were omitted, or a link matched on a different region) a cloud node labeled with
 * the provider name and region is added. An {@code UNAVAILABLE} entry is not drawn: no link can
 * be created for it. A plan without links produces the same diagram as before links existed.</p>
 *
 * <p>Node and edge labels are HTML-escaped ({@code &}, {@code <}, {@code >} and double quotes),
 * so metro, router, provider, region and workload names containing markup-significant
 * characters render literally instead of being interpreted by Mermaid's HTML label parser.</p>
 *
 * <p>This class is stateless and thread-safe.</p>
 */
public class TopologyDiagram {

    private static final String NL = "\n";

    /** The first line of a native multicloud edge label. */
    private static final String NATIVE_LINK_LABEL = "native multicloud (outside Fabric)";

    /** A drawn node that stands for one cloud region; {@code region} may be {@code null}. */
    private record CloudNode(CloudProviderType cloud, String region, String nodeId) {}

    /**
     * Renders the deployment plan as a Mermaid {@code graph LR} diagram.
     *
     * @param plan the deployment plan to diagram; must not be {@code null}
     * @return Mermaid source describing metros, Cloud Routers, provider connections
     *         and backbone links
     * @throws IllegalArgumentException if {@code plan} is {@code null}
     */
    public String toMermaid(DeploymentPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }

        StringBuilder mmd = new StringBuilder();
        mmd.append("graph LR").append(NL);

        // Group Cloud Routers by metro so each metro becomes a subgraph.
        Map<MetroId, List<PlannedCloudRouter>> routersByMetro = new LinkedHashMap<>();
        Map<String, String> routerNodeIds = new LinkedHashMap<>();
        if (plan.getCloudRouters() != null) {
            for (PlannedCloudRouter cr : plan.getCloudRouters()) {
                routersByMetro.computeIfAbsent(cr.getMetroId(), k -> new ArrayList<>()).add(cr);
            }
        }

        int idSeq = 0;
        for (Map.Entry<MetroId, List<PlannedCloudRouter>> entry : routersByMetro.entrySet()) {
            MetroId metro = entry.getKey();
            mmd.append("  subgraph metro_").append(safe(String.valueOf(metro)))
                    .append("[\"Metro: ").append(escape(String.valueOf(metro))).append("\"]").append(NL);
            for (PlannedCloudRouter cr : entry.getValue()) {
                String nodeId = "fcr" + (idSeq++);
                routerNodeIds.put(cr.getName(), nodeId);
                mmd.append("    ").append(nodeId)
                        .append("([\"").append(escape(cr.getName()));
                if (cr.getPackageCode() != null) {
                    mmd.append("<br/>").append(escape(cr.getPackageCode().name()));
                }
                mmd.append("\"])").append(NL);
            }
            mmd.append("  end").append(NL);
        }

        // Provider connections: router -> external provider node.
        Map<String, String> providerNodeIds = new LinkedHashMap<>();
        // The provider nodes that stand for a well-known cloud, so a native multicloud link can
        // attach to the node already drawn for the same cloud and region.
        List<CloudNode> cloudNodes = new ArrayList<>();
        if (plan.getProviderConnections() != null) {
            for (PlannedConnection conn : plan.getProviderConnections()) {
                String providerLabel = conn.getZSideProviderLabel() != null
                        ? conn.getZSideProviderLabel() : "Provider";
                String providerKey = providerLabel
                        + (conn.getZSideSellerRegion() != null ? "|" + conn.getZSideSellerRegion() : "");
                String providerId = providerNodeIds.get(providerKey);
                if (providerId == null) {
                    providerId = "prov" + (idSeq++);
                    providerNodeIds.put(providerKey, providerId);
                    StringBuilder pLabel = new StringBuilder(escape(providerLabel));
                    if (conn.getZSideSellerRegion() != null) {
                        pLabel.append("<br/>").append(escape(conn.getZSideSellerRegion()));
                    }
                    mmd.append("  ").append(providerId)
                            .append("[\"").append(pLabel).append("\"]").append(NL);
                    CloudProviderType cloud = conn.getZSideCloudType() != null
                            ? conn.getZSideCloudType()
                            : ConnectionBodies.resolveCloudType(conn.getZSideProviderLabel());
                    if (cloud != null && cloud != CloudProviderType.OTHER) {
                        cloudNodes.add(new CloudNode(cloud, conn.getZSideSellerRegion(), providerId));
                    }
                }

                String fromId = routerNodeIds.get(conn.getASideRouterName());
                if (fromId == null) {
                    fromId = "fcr" + (idSeq++);
                    routerNodeIds.put(conn.getASideRouterName(), fromId);
                    mmd.append("  ").append(fromId)
                            .append("([\"").append(escape(String.valueOf(conn.getASideRouterName())))
                            .append("\"])").append(NL);
                }
                mmd.append("  ").append(fromId).append(" -->|")
                        .append(escape(conn.getBandwidthMbps() + " Mbps"))
                        .append("| ").append(providerId).append(NL);
            }
        }

        // Backbone links: router <-> router across metros.
        if (plan.getBackboneLinks() != null) {
            for (PlannedBackboneLink link : plan.getBackboneLinks()) {
                PlannedConnection conn = link.getConnection();
                String aName = conn != null ? conn.getASideRouterName() : null;
                String zName = conn != null ? conn.getZSideRouterName() : null;
                String fromId = nodeFor(routerNodeIds, aName, link.getMetroA(), mmd);
                String toId = nodeFor(routerNodeIds, zName, link.getMetroZ(), mmd);
                if (fromId != null && toId != null) {
                    mmd.append("  ").append(fromId).append(" <-->|")
                            .append(escape(link.getBandwidthMbps() + " Mbps backbone"))
                            .append("| ").append(toId).append(NL);
                }
            }
        }

        // Native multicloud links (Beta): cloud <-> cloud, outside Fabric, so the edge joins the
        // two cloud nodes directly and is dashed to set it apart from Fabric connections.
        for (PlannedMulticloudInterconnect link : plan.multicloudLinksOrEmpty()) {
            if (link == null || link.getRole() == MulticloudLinkRole.UNAVAILABLE
                    || link.getProviderA() == null || link.getProviderZ() == null) {
                continue;
            }
            String aId = cloudNodeFor(cloudNodes, link.getProviderA(), link.getRegionA(), mmd);
            String zId = cloudNodeFor(cloudNodes, link.getProviderZ(), link.getRegionZ(), mmd);
            int mbps = link.getCoveringTierMbps() != null ? link.getCoveringTierMbps() : link.getRequestedMbps();
            StringBuilder detail = new StringBuilder().append(mbps).append(" Mbps");
            if (link.environmentStatus() != null) {
                detail.append(", ").append(link.environmentStatus());
            }
            if (link.getRole() != null) {
                detail.append(", ").append(link.getRole());
            }
            // Quoted label: the parentheses would otherwise end the edge text in Mermaid's grammar.
            mmd.append("  ").append(aId).append(" <-.->|\"")
                    .append(escape(NATIVE_LINK_LABEL)).append("<br/>").append(escape(detail.toString()))
                    .append("\"| ").append(zId).append(NL);
        }

        return mmd.toString();
    }

    /**
     * Renders an optimization result as a Mermaid {@code graph TD} diagram: each
     * recommended metro is a node annotated with its rank and composite score, and
     * any workloads placed in that metro (from the deployment topology) are shown as
     * child nodes connected to their metro.
     *
     * @param result the optimization result to diagram; must not be {@code null}
     * @return Mermaid source describing recommended metros and workload placements
     * @throws IllegalArgumentException if {@code result} is {@code null}
     */
    public String toMermaid(OptimizationResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }

        StringBuilder mmd = new StringBuilder();
        mmd.append("graph TD").append(NL);

        Map<MetroId, String> metroNodeIds = new LinkedHashMap<>();
        int idSeq = 0;

        if (result.getRecommendations() != null) {
            for (MetroRecommendation rec : result.getRecommendations()) {
                String nodeId = "metro" + (idSeq++);
                metroNodeIds.put(rec.getMetroId(), nodeId);

                StringBuilder label = new StringBuilder();
                label.append("#").append(rec.getRank()).append(" ");
                String name = rec.getMetroName() != null ? rec.getMetroName() : String.valueOf(rec.getMetroId());
                label.append(escape(name))
                        .append(" (").append(escape(String.valueOf(rec.getMetroId()))).append(")");
                if (rec.getScore() != null) {
                    label.append("<br/>score: ")
                            .append(String.format("%.1f", rec.getScore().getComposite())).append("/100");
                }
                mmd.append("  ").append(nodeId)
                        .append("[\"").append(label).append("\"]").append(NL);
            }
        }

        // Workload placements (from the deployment topology), grouped under their metro.
        if (result.getTopology() != null && result.getTopology().getPlacements() != null) {
            int wlSeq = 0;
            for (WorkloadPlacement wp : result.getTopology().getPlacements()) {
                String metroId = metroNodeIds.get(wp.getAssignedMetro());
                if (metroId == null) {
                    metroId = "metro" + (idSeq++);
                    metroNodeIds.put(wp.getAssignedMetro(), metroId);
                    mmd.append("  ").append(metroId)
                            .append("[\"").append(escape(String.valueOf(wp.getAssignedMetro())))
                            .append("\"]").append(NL);
                }
                String wlId = "wl" + (wlSeq++);
                mmd.append("  ").append(wlId)
                        .append("(\"").append(escape(wp.getWorkloadLabel())).append("\")").append(NL);
                mmd.append("  ").append(metroId).append(" --> ").append(wlId).append(NL);
            }
        }

        return mmd.toString();
    }

    private String nodeFor(Map<String, String> routerNodeIds, String routerName,
                           MetroId metroFallback, StringBuilder mmd) {
        if (routerName != null && routerNodeIds.containsKey(routerName)) {
            return routerNodeIds.get(routerName);
        }
        // The router was not declared in the plan's cloudRouters list; synthesize a node
        // so the backbone edge can still be drawn.
        String key = routerName != null ? routerName
                : (metroFallback != null ? "metro:" + metroFallback : null);
        if (key == null) {
            return null;
        }
        String existing = routerNodeIds.get(key);
        if (existing != null) {
            return existing;
        }
        int suffix = routerNodeIds.size();
        String nodeId = "fcrx" + suffix;
        routerNodeIds.put(key, nodeId);
        String label = routerName != null ? routerName : String.valueOf(metroFallback);
        mmd.append("  ").append(nodeId)
                .append("([\"").append(escape(label)).append("\"])").append(NL);
        return nodeId;
    }

    /**
     * The node a native multicloud link attaches to for one cloud region. Preference order: a
     * drawn node of the same cloud and the same region (compared trimmed, ignoring case); a drawn
     * node of the same cloud when either the node or the link carries no region; otherwise a new
     * node labeled with the provider name and the region, which is appended to {@code mmd} and
     * recorded in {@code cloudNodes} so a second link to the same cloud region reuses it.
     */
    private String cloudNodeFor(List<CloudNode> cloudNodes, CloudProviderType cloud, String region,
                                StringBuilder mmd) {
        String wanted = region == null || region.isBlank() ? null : region.trim();
        for (CloudNode node : cloudNodes) {
            if (node.cloud() == cloud && sameRegion(node.region(), wanted)) {
                return node.nodeId();
            }
        }
        for (CloudNode node : cloudNodes) {
            if (node.cloud() == cloud && (wanted == null || node.region() == null || node.region().isBlank())) {
                return node.nodeId();
            }
        }
        String nodeId = "cloud" + cloudNodes.size();
        cloudNodes.add(new CloudNode(cloud, wanted, nodeId));
        mmd.append("  ").append(nodeId).append("[\"").append(escape(cloud.getProviderName()));
        if (wanted != null) {
            mmd.append("<br/>").append(escape(wanted));
        }
        mmd.append("\"]").append(NL);
        return nodeId;
    }

    private boolean sameRegion(String drawn, String wanted) {
        String normalized = drawn == null || drawn.isBlank() ? null : drawn.trim();
        return normalized == null ? wanted == null : normalized.equalsIgnoreCase(wanted);
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        // Mermaid renders quoted node labels as HTML, so &, <, > and double-quotes in
        // user-supplied names must all be encoded as entities ('&' first, so the others'
        // entities are not double-escaped). Only the diagram's own <br/> separators —
        // appended outside escape() — remain live markup.
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String safe(String text) {
        if (text == null || text.isBlank()) {
            return "x";
        }
        return text.trim().replaceAll("[^A-Za-z0-9_]", "_");
    }
}
