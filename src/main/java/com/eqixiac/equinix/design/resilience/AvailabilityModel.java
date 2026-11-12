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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Composes per-element availability assumptions into a figure per workload, in series and in parallel
 * over the graph's redundant paths. All elements are assumed to fail independently.
 *
 * <p>For a workload with hosts {@code H} (primary and, when it has one, standby) and clouds {@code C}:</p>
 * <pre>
 *   leg(h, c)    = A(connection) x A(on-ramp)                          when h has its own connection to c
 *                = A(backbone) x A(metro m) x A(router m) x A(connection) x A(on-ramp)
 *                                                                       otherwise, over the nearest metro m that has one
 *   branch(h)    = A(metro h) x A(router h) [if it needs a cloud] x  product over c of leg(h, c)
 *   core         = (1 - product over h of (1 - branch(h)))  x  product over distinct cloud regions of A(cloud region)
 *   access(site) = 1 - product over its access circuits of (1 - A(site access))
 *   end-to-end   = core x (site-weighted mean of access(site))
 * </pre>
 * <p>Approximations, stated in each result's notes: the hosts' branches are treated as independent (exact when
 * each host has its own connections; a branch that borrows another metro's connection shares elements with it,
 * which slightly overstates the result), and a site's access is composed apart from the metros it lands in
 * (their failure is counted in {@code core}).</p>
 */
public final class AvailabilityModel {

    private AvailabilityModel() {
    }

    /**
     * @param graph       the design graph
     * @param assumptions the per-element availabilities
     * @return one estimate per placed workload, in graph order
     */
    public static List<WorkloadAvailability> estimate(DesignGraph graph, AvailabilityAssumptions assumptions) {
        List<WorkloadAvailability> out = new ArrayList<>();
        double siteAccess = siteAccess(graph, assumptions);
        for (PlacementView p : graph.placements()) {
            if (p.primary() == null) {
                continue;
            }
            List<String> notes = new ArrayList<>();
            List<String> branches = new ArrayList<>();
            double allFail = 1.0;
            for (String host : new String[]{p.primary(), p.standby()}) {
                if (host == null) {
                    continue;
                }
                double branch = assumptions.of(ElementType.METRO);
                StringBuilder f = new StringBuilder("metro " + host);
                if (!p.clouds().isEmpty()) {
                    branch *= assumptions.of(ElementType.CLOUD_ROUTER);
                    f.append(" x router ").append(host);
                }
                for (String cloud : p.clouds()) {
                    Leg leg = leg(graph, assumptions, host, cloud);
                    branch *= leg.value;
                    f.append(" x ").append(leg.description);
                    if (leg.borrowed) {
                        notes.add("host " + host + " reaches " + cloud + " over another metro's connection: the "
                                + "branches share elements, so the figure is slightly optimistic");
                    }
                }
                allFail *= 1 - branch;
                branches.add("[" + f + "]");
            }
            double core = 1 - allFail;
            Set<String> regions = new LinkedHashSet<>();
            for (String cloud : p.clouds()) {
                Optional<GraphNode> node = graph.node(DesignGraphBuilder.cloudId(cloud));
                if (node.isPresent()) {
                    regions.add(node.get().providerRegion() != null ? node.get().providerRegion() : cloud);
                }
            }
            for (int i = 0; i < regions.size(); i++) {
                core *= assumptions.of(ElementType.CLOUD_REGION);
            }
            double endToEnd = core * siteAccess;
            String formula = (branches.size() > 1 ? "parallel(" + String.join(", ", branches) + ")"
                    : String.join("", branches))
                    + (regions.isEmpty() ? "" : " x cloud region(s) " + regions)
                    + (graph.nodes(NodeKind.SITE).isEmpty() ? "" : String.format(Locale.ROOT,
                    " x site access %.6f", siteAccess));
            out.add(new WorkloadAvailability(p.workload(), core, endToEnd, 100 * endToEnd,
                    (1 - endToEnd) * WorkloadAvailability.MINUTES_PER_YEAR, formula, notes));
        }
        return out;
    }

    private record Leg(double value, String description, boolean borrowed) {
    }

    private static Leg leg(DesignGraph graph, AvailabilityAssumptions a, String host, String cloud) {
        double local = a.of(ElementType.CLOUD_CONNECTION) * a.of(ElementType.CLOUD_ONRAMP);
        if (graph.edge(DesignGraphBuilder.connectionId(cloud, host)).isPresent()) {
            return new Leg(local, "conn " + cloud + "@" + host + " x on-ramp", false);
        }
        List<GraphEdge> options = new ArrayList<>();
        for (GraphEdge e : graph.edges(EdgeKind.BACKBONE)) {
            String other = e.other(DesignGraphBuilder.routerId(host));
            if (other != null && graph.edge(DesignGraphBuilder.connectionId(cloud,
                    other.substring("router:".length()))).isPresent()) {
                options.add(e);
            }
        }
        options.sort(Comparator.comparingDouble((GraphEdge e) -> e.roundTripMs() == null ? Double.MAX_VALUE
                : e.roundTripMs()).thenComparing(GraphEdge::id));
        if (options.isEmpty()) {
            return new Leg(0, "no path to " + cloud, false);
        }
        String m = options.get(0).other(DesignGraphBuilder.routerId(host)).substring("router:".length());
        double v = a.of(ElementType.BACKBONE_LINK) * a.of(ElementType.METRO) * a.of(ElementType.CLOUD_ROUTER) * local;
        return new Leg(v, "backbone to " + m + " x metro " + m + " x router " + m + " x conn " + cloud + "@" + m
                + " x on-ramp", true);
    }

    private static double siteAccess(DesignGraph graph, AvailabilityAssumptions a) {
        double weighted = 0;
        double total = 0;
        for (GraphNode site : graph.nodes(NodeKind.SITE)) {
            long circuits = graph.edges(EdgeKind.SITE_ACCESS).stream().filter(e -> e.from().equals(site.id())).count();
            if (circuits == 0) {
                continue;
            }
            double w = site.weight() == null ? 0 : site.weight();
            weighted += w * (1 - Math.pow(1 - a.of(ElementType.SITE_ACCESS), circuits));
            total += w;
        }
        return total <= 0 ? 1.0 : weighted / total;
    }
}
