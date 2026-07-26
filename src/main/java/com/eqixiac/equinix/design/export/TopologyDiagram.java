package com.eqixiac.equinix.design.export;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;

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
 * <p>Node labels are HTML-escaped ({@code &}, {@code <}, {@code >} and double quotes), so
 * metro, router, provider and workload names containing markup-significant characters render
 * literally instead of being interpreted by Mermaid's HTML label parser.</p>
 *
 * <p>This class is stateless and thread-safe.</p>
 */
public class TopologyDiagram {

    private static final String NL = "\n";

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
