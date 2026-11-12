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

import com.eqixiac.equinix.core.enums.Region;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Enumerates the failure scenarios of a {@link DesignGraph}, deterministically.
 *
 * <ul>
 *   <li><b>Single failures</b> (exhaustive): every metro, Cloud Router, on-ramp, cloud connection, site access
 *       circuit and backbone link; then the shared-fate events: every cloud provider region (clouds grouped
 *       by provider and region; a cloud with no region is its own group) and every Equinix region the
 *       portfolio touches.</li>
 *   <li><b>Pairs</b> (bounded): every unordered pair of single-element failures, skipping a pair in which one
 *       failure already contains the other (a router and its own metro), in a fixed order, up to a limit.</li>
 * </ul>
 */
public final class ScenarioCatalog {

    private ScenarioCatalog() {
    }

    /**
     * The pairwise scenarios and how many exist.
     *
     * @param scenarios the pairs simulated, in order
     * @param total     how many distinct pairs exist
     */
    public record Pairs(List<FailureScenario> scenarios, int total) {
        /** Makes the list immutable. */
        public Pairs {
            scenarios = List.copyOf(scenarios);
        }

        /** @return {@code true} when every pair is in {@link #scenarios()} */
        public boolean exhaustive() {
            return scenarios.size() == total;
        }
    }

    /**
     * @param graph a design graph
     * @return every single failure: element failures first (metros, routers, on-ramps, connections), then
     *         cloud regions, then Equinix regions
     */
    public static List<FailureScenario> singles(DesignGraph graph) {
        List<FailureScenario> list = new ArrayList<>();
        for (GraphNode metro : graph.nodes(NodeKind.METRO)) {
            List<String> failed = new ArrayList<>();
            for (GraphNode n : graph.nodes()) {
                if (metro.metro().equals(n.metro())) {
                    failed.add(n.id());
                }
            }
            list.add(new FailureScenario(metro.id(), ScenarioKind.METRO, "loss of metro " + metro.metro(), failed,
                    null, null));
        }
        for (GraphNode router : graph.nodes(NodeKind.CLOUD_ROUTER)) {
            list.add(new FailureScenario(router.id(), ScenarioKind.CLOUD_ROUTER, "loss of the Cloud Router in "
                    + router.metro(), List.of(router.id()), null, null));
        }
        for (GraphNode onramp : graph.nodes(NodeKind.CLOUD_ONRAMP)) {
            list.add(new FailureScenario(onramp.id(), ScenarioKind.CLOUD_ONRAMP, "loss of the " + onramp.cloudId()
                    + " on-ramp in " + onramp.metro(), List.of(onramp.id()), null, null));
        }
        for (EdgeKind kind : List.of(EdgeKind.CLOUD_CONNECTION, EdgeKind.SITE_ACCESS, EdgeKind.BACKBONE)) {
            for (GraphEdge e : graph.edges(kind)) {
                list.add(new FailureScenario(e.id(), ScenarioKind.CONNECTION, describe(graph, e), null,
                        List.of(e.id()), null));
            }
        }
        Map<String, List<String>> regions = new TreeMap<>();
        Map<String, String> regionLabels = new LinkedHashMap<>();
        Set<String> stated = new HashSet<>();
        for (GraphNode cloud : graph.nodes(NodeKind.CLOUD)) {
            String key = cloud.providerRegion() != null ? cloud.providerRegion() : cloud.cloudId();
            regions.computeIfAbsent(key, k -> new ArrayList<>()).add(cloud.id());
            regionLabels.put(key, key);
            if (cloud.providerRegion() != null) {
                stated.add(key);
            }
        }
        for (Map.Entry<String, List<String>> r : regions.entrySet()) {
            list.add(new FailureScenario("cloud-region:" + r.getKey().replace(' ', '/'), ScenarioKind.CLOUD_REGION,
                    (stated.contains(r.getKey()) ? "outage of cloud region " + regionLabels.get(r.getKey())
                            : "outage of the provider region serving " + r.getKey() + " (region not stated in the spec)")
                            + " (clouds " + String.join(", ",
                    r.getValue().stream().map(id -> id.substring("cloud:".length())).toList()) + ")",
                    r.getValue(), null, null));
        }
        Map<Region, List<String>> equinix = new TreeMap<>();
        for (GraphNode n : graph.nodes()) {
            if (n.region() != null && n.metro() != null) {
                equinix.computeIfAbsent(n.region(), k -> new ArrayList<>()).add(n.id());
            }
        }
        for (Map.Entry<Region, List<String>> r : equinix.entrySet()) {
            list.add(new FailureScenario("equinix-region:" + r.getKey().name(), ScenarioKind.EQUINIX_REGION,
                    "loss of the entire Equinix " + r.getKey().name() + " region", r.getValue(), null, null));
        }
        return List.copyOf(list);
    }

    /**
     * @param graph a design graph
     * @param max   the most pairs to return
     * @return the pairwise scenarios over the single-element failures
     */
    public static Pairs pairs(DesignGraph graph, int max) {
        List<FailureScenario> elements = singles(graph).stream().filter(s -> s.kind().isSingleElement()).toList();
        List<Set<String>> effects = new ArrayList<>();
        for (FailureScenario s : elements) {
            effects.add(effect(graph, s));
        }
        List<FailureScenario> pairs = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < elements.size(); i++) {
            for (int j = i + 1; j < elements.size(); j++) {
                if (effects.get(i).containsAll(effects.get(j)) || effects.get(j).containsAll(effects.get(i))) {
                    continue;
                }
                total++;
                if (pairs.size() < max) {
                    FailureScenario a = elements.get(i);
                    FailureScenario b = elements.get(j);
                    Set<String> nodes = new java.util.LinkedHashSet<>(a.failedNodes());
                    nodes.addAll(b.failedNodes());
                    Set<String> edges = new java.util.LinkedHashSet<>(a.failedEdges());
                    edges.addAll(b.failedEdges());
                    pairs.add(new FailureScenario("pair:" + a.id() + "+" + b.id(), ScenarioKind.PAIR,
                            a.description() + " AND " + b.description(), new ArrayList<>(nodes),
                            new ArrayList<>(edges), List.of(a.id(), b.id())));
                }
            }
        }
        return new Pairs(pairs, total);
    }

    /** Every node and edge a scenario takes away, including edges touching its failed nodes. */
    static Set<String> effect(DesignGraph graph, FailureScenario s) {
        Set<String> out = new HashSet<>(s.failedNodes());
        out.addAll(s.failedEdges());
        for (GraphEdge e : graph.edges()) {
            if (s.failedNodes().contains(e.from()) || s.failedNodes().contains(e.to())) {
                out.add(e.id());
            }
        }
        return out;
    }

    private static String describe(DesignGraph graph, GraphEdge e) {
        return switch (e.kind()) {
            case CLOUD_CONNECTION -> "loss of the " + (e.role() == EdgeRole.STANDBY ? "standby " : "") + "connection "
                    + e.id().substring("conn:".length());
            case SITE_ACCESS -> "loss of the access circuit " + e.id().substring("access:".length());
            case BACKBONE -> "loss of the backbone link " + e.id().substring("backbone:".length());
            default -> "loss of " + e.id();
        };
    }
}
