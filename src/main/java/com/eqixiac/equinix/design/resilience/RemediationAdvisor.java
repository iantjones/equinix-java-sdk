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
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.Remediation;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;

/**
 * Turns the scenarios that hurt into concrete changes, each tied to the scenarios it addresses. Where the
 * change can be expressed on the graph (an extra connection, access circuit or backbone link, more
 * capacity, an extra metro) it is applied to a copy of the design and the addressed scenarios are
 * re-simulated: {@link Remediation#verified()} is {@code true} only when they then no longer cause an
 * outage or a degradation.
 */
final class RemediationAdvisor {

    private static final int METRO_CANDIDATES = 5;

    private final DesignGraph graph;
    private final DesignContext context;
    private final MetroPortfolio portfolio;
    private final ResilienceOptions options;
    private final ScenarioOutcome baseline;
    private final Map<String, Remediation> found = new LinkedHashMap<>();

    RemediationAdvisor(DesignGraph graph, DesignContext context, MetroPortfolio portfolio, ResilienceOptions options,
                       ScenarioOutcome baseline) {
        this.graph = graph;
        this.context = context;
        this.portfolio = portfolio;
        this.options = options;
        this.baseline = baseline;
    }

    List<Remediation> advise(List<ScenarioOutcome> singles) {
        for (ScenarioOutcome o : singles) {
            if (o.verdict() != Verdict.OUTAGE && o.verdict() != Verdict.DEGRADED) {
                continue;
            }
            FailureScenario s = o.scenario();
            boolean outage = o.verdict() == Verdict.OUTAGE;
            switch (s.kind()) {
                case CONNECTION -> connection(o);
                case CLOUD_ONRAMP, CLOUD_ROUTER, METRO, EQUINIX_REGION -> {
                    if (outage) {
                        topology(o);
                    }
                }
                case CLOUD_REGION -> add("Add a second region of the affected cloud as another cloud in the spec and "
                        + "connect to it: " + s.description() + " is outside anything the Equinix design can "
                        + "protect", s.id(), "a cloud-provider region outage takes every on-ramp to it", false);
                default -> {
                }
            }
            capacity(o);
            latency(o);
        }
        List<Remediation> list = new ArrayList<>(found.values());
        list.sort(Comparator.comparing((Remediation r) -> !r.verified())
                .thenComparing(r -> -r.addresses().size()).thenComparing(Remediation::action));
        return list;
    }

    // --- per scenario kind ----------------------------------------------------------------------

    private void connection(ScenarioOutcome o) {
        String id = o.scenario().failedEdges().get(0);
        GraphEdge edge = graph.edge(id).orElseThrow();
        switch (edge.kind()) {
            case CLOUD_CONNECTION -> {
                String[] cm = cloudMetro(id.substring("conn:".length()));
                if (!standbyElsewhere(o, cm[0], cm[1])) {
                    double cap = load(id);
                    GraphEdge twin = new GraphEdge(id + "#2", EdgeKind.CLOUD_CONNECTION, edge.from(), edge.to(),
                            EdgeRole.STANDBY, cap > 0 ? Double.valueOf(cap) : edge.capacityMbps(), null, null, edge.carries(),
                            "redundant twin");
                    tryEdit("Add a second, redundant " + cm[0] + " connection in " + cm[1] + " (a Fabric redundant "
                            + "pair on separate ports)" + sized(cap), o, g -> with(g, List.of(), List.of(twin)),
                            "a second connection on the same on-ramp survives the loss of one connection, not of the "
                                    + "on-ramp or the metro");
                }
            }
            case SITE_ACCESS -> {
                String site = edge.from();
                String label = site.substring("site:".length());
                Set<String> used = new LinkedHashSet<>();
                for (GraphEdge e : graph.edges(EdgeKind.SITE_ACCESS)) {
                    if (e.from().equals(site)) {
                        used.add(e.to().substring("metro:".length()));
                    }
                }
                Optional<MetroId> next = portfolio.metros().stream()
                        .filter(m -> !used.contains(m.code()))
                        .filter(m -> siteLatency(label, m).isPresent())
                        .min(Comparator.comparingDouble((MetroId m) -> siteLatency(label, m).getAsDouble())
                                .thenComparing(MetroId::code));
                if (next.isPresent()) {
                    String m = next.get().code();
                    double ms = siteLatency(label, next.get()).getAsDouble();
                    GraphEdge access = new GraphEdge("access:" + label + "@" + m, EdgeKind.SITE_ACCESS, site,
                            DesignGraphBuilder.metroId(m), EdgeRole.STANDBY, null, ms, "context", null, null);
                    tryEdit(String.format(Locale.ROOT, "Add a second access circuit for site '%s' into %s "
                            + "(%.1f ms)", label, m, ms), o, g -> with(g, List.of(), List.of(access)),
                            "the site is single-homed");
                } else {
                    tryEdit("Add a second, diverse access circuit for site '" + label + "' into "
                            + edge.to().substring("metro:".length()), o, g -> with(g, List.of(), List.of(
                            new GraphEdge(edge.id() + "#2", EdgeKind.SITE_ACCESS, edge.from(), edge.to(),
                                    EdgeRole.STANDBY, null, edge.roundTripMs(), edge.latencyBasis(), null, null))),
                            "no other portfolio metro has a known latency to the site");
                }
            }
            case BACKBONE -> tryEdit("Add a second, diverse inter-metro path for " + id.substring("backbone:".length()),
                    o, g -> with(g, List.of(), List.of(new GraphEdge(edge.id() + "#2", EdgeKind.BACKBONE, edge.from(),
                            edge.to(), null, null, edge.roundTripMs(), edge.latencyBasis(), null, null))),
                    "traffic between the two metros has no other path");
            default -> {
            }
        }
    }

    private void topology(ScenarioOutcome o) {
        switch (o.scenario().kind()) {
            case CLOUD_ONRAMP -> onramp(o);
            case CLOUD_ROUTER -> router(o);
            case METRO -> metro(o);
            case EQUINIX_REGION -> equinixRegion(o);
            default -> {
            }
        }
    }

    private void onramp(ScenarioOutcome o) {
        GraphNode node = graph.node(o.scenario().failedNodes().get(0)).orElseThrow();
        if (!standbyElsewhere(o, node.cloudId(), node.metro())) {
            addMetro(o, Set.of(node.cloudId()), null, "no other metro of the design reaches " + node.cloudId());
        }
    }

    private void router(ScenarioOutcome o) {
        GraphNode node = graph.node(o.scenario().failedNodes().get(0)).orElseThrow();
        add("Add a second Cloud Router in " + node.metro() + " and duplicate its connections on it, or give the "
                        + "affected workloads a standby metro", o.scenario().id(),
                "the router carries every cloud and inter-metro path of " + node.metro() + " (not re-simulated: "
                        + "a twin router changes the design shape)", false);
        if (portfolio.size() == 1) {
            addMetro(o, cloudsUsed(), null, "a one-metro design has no other router");
        }
    }

    private void metro(ScenarioOutcome o) {
        for (PlacementView p : graph.placements()) {
            if (o.workloadsDown().contains(p.workload()) && p.standby() == null && portfolio.size() > 1) {
                add("Give workload '" + p.workload() + "' a standby metro (it has none: pinned, or no second metro "
                        + "reaches all its clouds)", o.scenario().id(), "it cannot fail over", false);
            }
        }
        addMetro(o, cloudsUsed(), null, "the design has no metro to fail over to");
    }

    private void equinixRegion(ScenarioOutcome o) {
        Region lost = Region.valueOf(o.scenario().id().substring("equinix-region:".length()));
        addMetro(o, cloudsUsed(), lost, "every metro of the design is in " + lost);
    }

    private void capacity(ScenarioOutcome o) {
        List<GraphEdge> raised = new ArrayList<>();
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Double> load : o.connectionLoadMbps().entrySet()) {
            GraphEdge e = graph.edge(load.getKey()).orElse(null);
            if (e == null || e.capacityMbps() == null || load.getValue() <= e.capacityMbps() + 1e-9) {
                continue;
            }
            double need = load.getValue();
            parts.add(String.format(Locale.ROOT, "%s connection from %s to %s Mbps", e.id().substring("conn:".length()),
                    DesignGraph.fmt(e.capacityMbps()), DesignGraph.fmt(need)));
            raised.add(new GraphEdge(e.id(), e.kind(), e.from(), e.to(), e.role(), need, e.roundTripMs(),
                    e.latencyBasis(), e.carries(), e.note()));
        }
        if (raised.isEmpty()) {
            return;
        }
        List<String> ids = raised.stream().map(GraphEdge::id).toList();
        tryEdit("Raise the " + String.join(" and the ", parts),
                o, g -> with(g, ids, raised), "after " + o.scenario().description()
                        + " the surviving connections carry more than they are sized for");
    }

    private void latency(ScenarioOutcome o) {
        for (String b : o.constraintBreaks()) {
            if (!baseline.constraintBreaks().contains(b)) {
                add("Choose a standby metro within the latency limit: " + b, o.scenario().id(),
                        "the failover path breaks a stated limit (not re-simulated)", false);
            }
        }
    }

    // --- helpers --------------------------------------------------------------------------------

    /** Adds a standby connection to {@code cloud} in another portfolio metro that reaches it. */
    private boolean standbyElsewhere(ScenarioOutcome o, String cloud, String lostMetro) {
        for (MetroId m : portfolio.metros()) {
            if (m.code().equals(lostMetro) || !context.reachableClouds(m).contains(cloud)
                    || graph.edge(DesignGraphBuilder.connectionId(cloud, m.code())).isPresent()
                    || graph.node(DesignGraphBuilder.routerId(m.code())).isEmpty()) {
                continue;
            }
            double cap = load(DesignGraphBuilder.connectionId(cloud, lostMetro));
            String onramp = DesignGraphBuilder.onrampId(cloud, m.code());
            GraphNode ramp = new GraphNode(onramp, NodeKind.CLOUD_ONRAMP, cloud + " on-ramp " + m.code(), m.code(),
                    context.metro(m).map(MetroFacts::region).orElse(null), cloud, null, null);
            List<GraphEdge> add = List.of(
                    new GraphEdge(DesignGraphBuilder.connectionId(cloud, m.code()), EdgeKind.CLOUD_CONNECTION,
                            DesignGraphBuilder.routerId(m.code()), onramp, EdgeRole.STANDBY, cap > 0 ? Double.valueOf(cap) : null, null,
                            null, null, "remediation"),
                    new GraphEdge("provider:" + cloud + "@" + m.code(), EdgeKind.PROVIDER_LINK, onramp,
                            DesignGraphBuilder.cloudId(cloud), null, null, null, null, null, null));
            return tryEdit("Add a standby " + cloud + " connection in " + m.code() + sized(cap), o,
                    g -> withNodes(g, List.of(ramp), add), m.code() + " already reaches " + cloud);
        }
        return false;
    }

    private void addMetro(ScenarioOutcome o, Set<String> clouds, Region avoid, String why) {
        String lostMetro = o.scenario().kind() == ScenarioKind.METRO
                ? o.scenario().id().substring("metro:".length()) : portfolio.metros().get(0).code();
        List<MetroFacts> candidates = new ArrayList<>();
        for (MetroFacts m : context.metros()) {
            if (portfolio.contains(m.id()) || !context.reachableClouds(m.id()).containsAll(clouds)
                    || (avoid != null && avoid.equals(m.region())) || !allowed(m)) {
                continue;
            }
            candidates.add(m);
        }
        MetroId lost = MetroId.of(lostMetro);
        candidates.sort(Comparator.comparingDouble((MetroFacts m) -> {
            OptionalDouble ms = context.roundTripLatencyMs(lost, m.id());
            return ms.isPresent() ? ms.getAsDouble() : Double.MAX_VALUE;
        }).thenComparing(m -> m.id().code()));
        if (candidates.isEmpty()) {
            add("No metro outside the failure reaches " + (clouds.isEmpty() ? "the design" : String.join(", ", clouds))
                    + (graph.allowedZones().isEmpty() ? "" : " inside " + graph.allowedZones())
                    + ": " + o.scenario().description() + " cannot be removed by adding a metro", o.scenario().id(),
                    why, false);
            return;
        }
        for (MetroFacts c : candidates.subList(0, Math.min(METRO_CANDIDATES, candidates.size()))) {
            List<MetroId> metros = new ArrayList<>(portfolio.metros());
            metros.add(c.id());
            MetroPortfolio bigger = MetroPortfolio.of(metros);
            DesignGraph g = DesignGraphBuilder.build(context, bigger, options);
            if (survives(g, o.scenario().id())) {
                OptionalDouble ms = context.roundTripLatencyMs(lost, c.id());
                add("Add metro " + c.id().code() + (ms.isPresent() ? String.format(Locale.ROOT, " (%.1f ms from %s)",
                        ms.getAsDouble(), lostMetro) : "") + " to the design, reaching "
                        + (clouds.isEmpty() ? "the same workloads" : String.join(", ", clouds))
                        + overMax(bigger.size()), o.scenario().id(),
                        why + "; re-simulated on " + bigger.key(), true);
                return;
            }
        }
        add("Add a metro to fail over to (no candidate among the nearest " + METRO_CANDIDATES + " removed "
                + o.scenario().description() + " when re-simulated)", o.scenario().id(), why, false);
    }

    private String overMax(int size) {
        Integer max = context.spec().constraints().maxMetros();
        return max != null && size > max ? " (needs " + size + " metros: above the spec's maxMetros " + max + ")" : "";
    }

    private boolean allowed(MetroFacts m) {
        if (graph.allowedZones().isEmpty()) {
            return true;
        }
        for (String z : graph.allowedZones()) {
            if (m.geoZones().contains(z)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryEdit(String action, ScenarioOutcome o, Function<DesignGraph, DesignGraph> edit, String why) {
        boolean ok;
        try {
            ok = survives(edit.apply(graph), o.scenario().id());
        } catch (IllegalArgumentException e) {
            ok = false;
        }
        add(action, o.scenario().id(), why + (ok ? "; re-simulated: the scenario no longer hurts"
                : "; re-simulated: not sufficient on its own"), ok);
        return ok;
    }

    private static boolean survives(DesignGraph g, String scenarioId) {
        FailureSimulator sim = new FailureSimulator(g);
        for (FailureScenario s : ScenarioCatalog.singles(g)) {
            if (s.id().equals(scenarioId)) {
                Verdict v = sim.simulate(s).verdict();
                return v == Verdict.UNAFFECTED || v == Verdict.SURVIVES;
            }
        }
        return false;
    }

    private void add(String action, String scenarioId, String rationale, boolean verified) {
        Remediation old = found.get(action);
        if (old == null) {
            found.put(action, new Remediation(action, List.of(scenarioId), rationale, verified));
        } else if (!old.addresses().contains(scenarioId)) {
            List<String> ids = new ArrayList<>(old.addresses());
            ids.add(scenarioId);
            found.put(action, new Remediation(action, ids, old.rationale(), old.verified() && verified));
        }
    }

    private double load(String connectionId) {
        Double v = baseline.connectionLoadMbps().get(connectionId);
        return v == null ? 0 : v;
    }

    private static String sized(double mbps) {
        return mbps > 0 ? " sized for " + DesignGraph.fmt(mbps) + " Mbps" : "";
    }

    private Set<String> cloudsUsed() {
        Set<String> clouds = new LinkedHashSet<>();
        for (GraphNode n : graph.nodes(NodeKind.CLOUD)) {
            clouds.add(n.cloudId());
        }
        return clouds;
    }

    private OptionalDouble siteLatency(String label, MetroId m) {
        return context.spec().sites().stream().filter(s -> s.label().equals(label)).findFirst()
                .map(s -> context.roundTripLatencyMs(m, s)).orElse(OptionalDouble.empty());
    }

    private static String[] cloudMetro(String cloudAtMetro) {
        int at = cloudAtMetro.lastIndexOf('@');
        return new String[]{cloudAtMetro.substring(0, at), cloudAtMetro.substring(at + 1)};
    }

    static DesignGraph with(DesignGraph g, List<String> removeEdges, List<GraphEdge> addEdges) {
        return withNodes(g, List.of(), addEdges, removeEdges);
    }

    static DesignGraph withNodes(DesignGraph g, List<GraphNode> addNodes, List<GraphEdge> addEdges) {
        return withNodes(g, addNodes, addEdges, List.of());
    }

    private static DesignGraph withNodes(DesignGraph g, List<GraphNode> addNodes, List<GraphEdge> addEdges,
                                         List<String> removeEdges) {
        List<GraphNode> nodes = new ArrayList<>(g.nodes());
        nodes.addAll(addNodes);
        List<GraphEdge> edges = new ArrayList<>();
        for (GraphEdge e : g.edges()) {
            if (!removeEdges.contains(e.id())) {
                edges.add(e);
            }
        }
        edges.addAll(addEdges);
        return new DesignGraph(g.specFingerprint(), g.specName(), g.portfolio(), nodes, edges, g.placements(),
                g.requiredClouds(), g.allowedZones(), g.siteMaxLatencyMs(), g.assumptions());
    }
}
