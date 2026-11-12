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
package com.eqixiac.equinix.design.resilience;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * An explicit, immutable graph of one design: sites, metros, Cloud Routers, cloud on-ramps, clouds,
 * workloads and (optionally) peer networks, joined by access circuits, the inter-metro backbone,
 * primary and standby cloud connections, provider links and workload hosting, each with its capacity
 * and round-trip latency where known. Built by {@link DesignGraphBuilder}; simulated by
 * {@link FailureSimulator}. Jackson-serialisable; {@link #toMermaid()} draws it and
 * {@link #toTable()} lists it.
 *
 * @param specFingerprint  the {@code DesignSpec.fingerprint()} the graph was built from
 * @param specName         the spec's name
 * @param portfolio        the portfolio key (for example {@code DC+NY})
 * @param nodes            every node, in a fixed order (by kind, then id)
 * @param edges            every edge, in a fixed order (by kind, then id)
 * @param placements       every workload's placement, in spec order
 * @param requiredClouds   ids of the spec clouds marked required
 * @param allowedZones     the spec's {@code constraints.geoZones} (upper case); empty when sovereignty is not constrained
 * @param siteMaxLatencyMs the spec's {@code constraints.maxLatencyMs} (site to its access metro), or {@code null}
 * @param assumptions      how the graph was built where the spec is silent (stated for the reader)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DesignGraph(String specFingerprint, String specName, String portfolio, List<GraphNode> nodes,
                          List<GraphEdge> edges, List<PlacementView> placements, List<String> requiredClouds,
                          List<String> allowedZones,
                          Double siteMaxLatencyMs, List<String> assumptions) {

    /** Makes the lists immutable and rejects duplicate or dangling ids. */
    public DesignGraph {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
        placements = placements == null ? List.of() : List.copyOf(placements);
        requiredClouds = requiredClouds == null ? List.of() : List.copyOf(requiredClouds);
        allowedZones = allowedZones == null ? List.of() : List.copyOf(allowedZones);
        assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
        Map<String, GraphNode> ids = new HashMap<>();
        for (GraphNode n : nodes) {
            if (ids.put(n.id(), n) != null) {
                throw new IllegalArgumentException("Duplicate node id " + n.id());
            }
        }
        java.util.Set<String> edgeIds = new java.util.HashSet<>();
        for (GraphEdge e : edges) {
            if (!edgeIds.add(e.id())) {
                throw new IllegalArgumentException("Duplicate edge id " + e.id());
            }
            if (!ids.containsKey(e.from()) || !ids.containsKey(e.to())) {
                throw new IllegalArgumentException("Edge " + e.id() + " references an unknown node");
            }
        }
    }

    /**
     * @param id a node id
     * @return the node
     */
    public Optional<GraphNode> node(String id) {
        return nodes.stream().filter(n -> n.id().equals(id)).findFirst();
    }

    /**
     * @param id an edge id
     * @return the edge
     */
    public Optional<GraphEdge> edge(String id) {
        return edges.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    /**
     * @param kind a node kind
     * @return the nodes of that kind, in graph order
     */
    public List<GraphNode> nodes(NodeKind kind) {
        return nodes.stream().filter(n -> n.kind() == kind).toList();
    }

    /**
     * @param kind an edge kind
     * @return the edges of that kind, in graph order
     */
    public List<GraphEdge> edges(EdgeKind kind) {
        return edges.stream().filter(e -> e.kind() == kind).toList();
    }

    /**
     * Draws the graph as a Mermaid flowchart: one subgraph per metro (its metro, Cloud Router and
     * on-ramps), sites on the left, clouds on the right. Standby edges are dashed; edge labels carry
     * capacity and round-trip latency where known.
     *
     * @return Mermaid source (paste into a {@code mermaid} fenced block)
     */
    public String toMermaid() {
        Map<String, String> alias = new LinkedHashMap<>();
        for (GraphNode n : nodes) {
            alias.put(n.id(), "n" + alias.size());
        }
        StringBuilder sb = new StringBuilder("flowchart LR\n");
        Map<String, List<GraphNode>> byMetro = new LinkedHashMap<>();
        List<GraphNode> loose = new ArrayList<>();
        for (GraphNode n : nodes) {
            boolean inMetro = n.metro() != null && (n.kind() == NodeKind.METRO || n.kind() == NodeKind.CLOUD_ROUTER
                    || n.kind() == NodeKind.CLOUD_ONRAMP);
            if (inMetro) {
                byMetro.computeIfAbsent(n.metro(), k -> new ArrayList<>()).add(n);
            } else {
                loose.add(n);
            }
        }
        for (GraphNode n : loose) {
            sb.append("    ").append(shape(alias.get(n.id()), n)).append('\n');
        }
        for (Map.Entry<String, List<GraphNode>> metro : byMetro.entrySet()) {
            sb.append("    subgraph m_").append(metro.getKey()).append("[\"").append(metro.getKey()).append("\"]\n");
            for (GraphNode n : metro.getValue()) {
                sb.append("        ").append(shape(alias.get(n.id()), n)).append('\n');
            }
            sb.append("    end\n");
        }
        for (GraphEdge e : edges) {
            String label = edgeLabel(e);
            String arrow = e.role() == EdgeRole.STANDBY ? "-.-" : e.kind() == EdgeKind.PEERING ? "~~~" : "---";
            sb.append("    ").append(alias.get(e.from())).append(' ').append(arrow);
            if (!label.isEmpty() && e.kind() != EdgeKind.PEERING) {
                sb.append("|\"").append(label).append("\"|");
            }
            sb.append(' ').append(alias.get(e.to())).append('\n');
        }
        return sb.toString();
    }

    /**
     * @return a Markdown table of every edge: kind, endpoints, role, capacity and round trip with its basis
     */
    public String toTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("| Edge | Kind | From | To | Role | Capacity | Round trip |\n");
        sb.append("|---|---|---|---|---|---|---|\n");
        for (GraphEdge e : edges) {
            sb.append("| ").append(e.id()).append(" | ").append(e.kind().name().toLowerCase(Locale.ROOT))
                    .append(" | ").append(labelOf(e.from())).append(" | ").append(labelOf(e.to())).append(" | ")
                    .append(e.role() == null ? "" : e.role().name().toLowerCase(Locale.ROOT)).append(" | ")
                    .append(e.capacityMbps() == null ? "unknown" : fmt(e.capacityMbps()) + " Mbps").append(" | ")
                    .append(e.roundTripMs() == null ? "unknown"
                            : String.format(Locale.ROOT, "%.1f ms (%s)", e.roundTripMs(), e.latencyBasis()))
                    .append(" |\n");
        }
        return sb.toString();
    }

    /** @return the graph as indented JSON */
    @JsonIgnore
    public String toJson() {
        return ResilienceJson.write(this);
    }

    private String labelOf(String id) {
        return node(id).map(GraphNode::label).orElse(id);
    }

    private static String shape(String alias, GraphNode n) {
        String text = n.label().replace("\"", "'");
        return switch (n.kind()) {
            case SITE -> alias + "([\"" + text + "\"])";
            case METRO -> alias + "[\"" + text + "\"]";
            case CLOUD_ROUTER -> alias + "{{\"" + text + "\"}}";
            case CLOUD_ONRAMP -> alias + "[/\"" + text + "\"/]";
            case CLOUD -> alias + "((\"" + text + "\"))";
            case WORKLOAD -> alias + "[[\"" + text + "\"]]";
            case PEER -> alias + ">\"" + text + "\"]";
        };
    }

    private static String edgeLabel(GraphEdge e) {
        List<String> parts = new ArrayList<>();
        if (e.capacityMbps() != null) {
            parts.add(fmt(e.capacityMbps()) + " Mbps");
        }
        if (e.roundTripMs() != null && e.kind() != EdgeKind.ROUTER_ATTACHMENT) {
            parts.add(String.format(Locale.ROOT, "%.1f ms", e.roundTripMs()));
        }
        if (e.role() == EdgeRole.STANDBY) {
            parts.add("standby");
        }
        return String.join(" / ", parts);
    }

    static String fmt(double mbps) {
        return mbps == Math.rint(mbps) ? String.valueOf((long) mbps) : String.format(Locale.ROOT, "%.1f", mbps);
    }
}
