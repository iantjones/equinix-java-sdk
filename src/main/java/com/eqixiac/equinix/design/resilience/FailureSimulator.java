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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Simulates failure scenarios on one {@link DesignGraph}. Deterministic and side-effect free; build one
 * per graph and call {@link #simulate(FailureScenario)} for each scenario.
 *
 * <h2>Rules</h2>
 * <ol>
 *   <li>A failed node takes every edge touching it; a metro failure takes the metro's router and on-ramps.</li>
 *   <li>Each workload runs in its primary metro or, failing that, its standby metro: whichever surviving host
 *       reaches more of its clouds, then more site weight (the primary on a tie). It is down when neither survives.</li>
 *   <li>A site reaches a host over its surviving access circuits and the backbone (shortest round trip);
 *       a host reaches a cloud through its Cloud Router, over a cloud connection in its own metro or, when that
 *       is gone, across the backbone to another metro's connection to the same cloud. Paths never transit
 *       another site or another cloud.</li>
 *   <li>A site counts as served only within the workload's {@code maxLatencyMs} and, for its access hop, the
 *       spec's {@code maxLatencyMs}; an unknown latency is not held against it.</li>
 *   <li>Capacity: every sized workload's bandwidth is put on the connection its path uses; when a connection's
 *       demand exceeds its capacity each workload on it gets {@code capacity / demand}. A connection of unknown
 *       capacity carries everything (and the report says so). Spill-over onto a third connection is not modelled.</li>
 * </ol>
 */
public final class FailureSimulator {

    private static final double EPS = 1e-9;

    private final DesignGraph graph;
    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final Map<String, List<GraphEdge>> adjacency = new HashMap<>();
    private final List<GraphNode> sites;
    private ScenarioOutcome baseline;
    private Map<String, Map<String, Boolean>> baselineSiteServed;
    private Map<String, Set<String>> baselineClouds;

    /**
     * @param graph the graph to simulate
     */
    public FailureSimulator(DesignGraph graph) {
        this.graph = graph;
        for (GraphNode n : graph.nodes()) {
            nodes.put(n.id(), n);
            adjacency.put(n.id(), new ArrayList<>());
        }
        for (GraphEdge e : graph.edges()) {
            adjacency.get(e.from()).add(e);
            adjacency.get(e.to()).add(e);
        }
        List<GraphNode> s = new ArrayList<>();
        for (GraphNode n : graph.nodes(NodeKind.SITE)) {
            if (adjacency.get(n.id()).stream().anyMatch(e -> e.kind() == EdgeKind.SITE_ACCESS)) {
                s.add(n);
            }
        }
        this.sites = List.copyOf(s);
    }

    /** @return the graph */
    public DesignGraph graph() {
        return graph;
    }

    /** @return the outcome with nothing failed */
    public ScenarioOutcome baseline() {
        if (baseline == null) {
            Run run = run(FailureScenario.baseline());
            baselineSiteServed = run.siteServed;
            baselineClouds = run.cloudsReached;
            baseline = assemble(run, null);
        }
        return baseline;
    }

    /**
     * @param scenario a scenario
     * @return what still works, compared with the baseline
     */
    public ScenarioOutcome simulate(FailureScenario scenario) {
        if (scenario.kind() == ScenarioKind.BASELINE) {
            return baseline();
        }
        baseline();
        return assemble(run(scenario), baseline);
    }

    /**
     * @param scenarios scenarios
     * @return their outcomes, in the same order
     */
    public List<ScenarioOutcome> simulateAll(Collection<FailureScenario> scenarios) {
        List<ScenarioOutcome> out = new ArrayList<>();
        for (FailureScenario s : scenarios) {
            out.add(simulate(s));
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    private record Path(List<String> nodeIds, List<GraphEdge> edges, double latencyMs, boolean latencyKnown) {
        String render(Map<String, GraphNode> nodes) {
            List<String> hops = new ArrayList<>();
            String last = null;
            for (String id : nodeIds) {
                GraphNode n = nodes.get(id);
                String hop = switch (n.kind()) {
                    case METRO, CLOUD_ROUTER -> n.metro();
                    case CLOUD_ONRAMP -> "on-ramp " + n.metro();
                    case CLOUD -> n.cloudId();
                    default -> n.label();
                };
                if (!hop.equals(last)) {
                    hops.add(hop);
                }
                last = hop;
            }
            boolean toCloud = nodes.get(nodeIds.get(nodeIds.size() - 1)).kind() == NodeKind.CLOUD;
            return String.join(" > ", hops) + (latencyKnown ? String.format(Locale.ROOT, " (%.1f ms%s)", latencyMs,
                    toCloud ? " to the on-ramp" : "") : " (latency unknown)");
        }

        GraphEdge connection() {
            for (GraphEdge e : edges) {
                if (e.kind() == EdgeKind.CLOUD_CONNECTION) {
                    return e;
                }
            }
            return null;
        }

        Set<String> metros(Map<String, GraphNode> nodes) {
            Set<String> out = new TreeSet<>();
            for (String id : nodeIds) {
                String m = nodes.get(id).metro();
                if (m != null) {
                    out.add(m);
                }
            }
            return out;
        }
    }

    private final class Failure {
        final Set<String> deadNodes;
        final Set<String> deadEdges;

        Failure(FailureScenario s) {
            deadNodes = new HashSet<>(s.failedNodes());
            deadEdges = new HashSet<>(s.failedEdges());
        }

        boolean alive(String node) {
            return !deadNodes.contains(node);
        }

        boolean alive(GraphEdge e) {
            return !deadEdges.contains(e.id()) && alive(e.from()) && alive(e.to());
        }
    }

    /** Dijkstra by round trip (an unknown edge latency counts 0 and marks the path's latency unknown). */
    private Path shortest(Failure f, String from, String to, Predicate<GraphNode> nodeOk, Set<EdgeKind> kinds) {
        if (!f.alive(from) || !f.alive(to)) {
            return null;
        }
        Map<String, Double> dist = new HashMap<>();
        Map<String, GraphEdge> via = new HashMap<>();
        Set<String> done = new HashSet<>();
        dist.put(from, 0.0);
        while (true) {
            String best = null;
            for (Map.Entry<String, Double> e : dist.entrySet()) {
                if (!done.contains(e.getKey()) && (best == null || e.getValue() < dist.get(best)
                        || (e.getValue().equals(dist.get(best)) && e.getKey().compareTo(best) < 0))) {
                    best = e.getKey();
                }
            }
            if (best == null) {
                return null;
            }
            if (best.equals(to)) {
                break;
            }
            done.add(best);
            for (GraphEdge e : adjacency.get(best)) {
                if (!kinds.contains(e.kind()) || !f.alive(e)) {
                    continue;
                }
                String next = e.other(best);
                if (done.contains(next) || !(next.equals(to) || nodeOk.test(nodes.get(next)))) {
                    continue;
                }
                double d = dist.get(best) + (e.roundTripMs() == null ? 0 : e.roundTripMs());
                Double old = dist.get(next);
                if (old == null || d < old - EPS) {
                    dist.put(next, d);
                    via.put(next, e);
                }
            }
        }
        List<String> path = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        String cur = to;
        boolean known = true;
        while (!cur.equals(from)) {
            path.add(0, cur);
            GraphEdge e = via.get(cur);
            edges.add(0, e);
            if (e.roundTripMs() == null && e.kind() != EdgeKind.ROUTER_ATTACHMENT
                    && e.kind() != EdgeKind.CLOUD_CONNECTION && e.kind() != EdgeKind.PROVIDER_LINK) {
                known = false;
            }
            cur = e.other(cur);
        }
        path.add(0, from);
        return new Path(path, edges, dist.get(to), known);
    }

    private static final Set<EdgeKind> SITE_EDGES = Set.of(EdgeKind.SITE_ACCESS, EdgeKind.ROUTER_ATTACHMENT,
            EdgeKind.BACKBONE);
    private static final Set<EdgeKind> CLOUD_EDGES = Set.of(EdgeKind.ROUTER_ATTACHMENT, EdgeKind.BACKBONE,
            EdgeKind.CLOUD_CONNECTION, EdgeKind.PROVIDER_LINK);

    private Path sitePath(Failure f, GraphNode site, String host) {
        return shortest(f, site.id(), DesignGraphBuilder.metroId(host),
                n -> n.kind() == NodeKind.METRO || n.kind() == NodeKind.CLOUD_ROUTER, SITE_EDGES);
    }

    private Path cloudPath(Failure f, String host, String cloud) {
        return shortest(f, DesignGraphBuilder.metroId(host), DesignGraphBuilder.cloudId(cloud),
                n -> n.kind() == NodeKind.CLOUD_ROUTER
                        || (n.kind() == NodeKind.CLOUD_ONRAMP && cloud.equals(n.cloudId())), CLOUD_EDGES);
    }

    /** The access hop a site uses: its surviving access circuit with the shortest round trip. */
    private GraphEdge accessHop(Failure f, GraphNode site) {
        GraphEdge best = null;
        for (GraphEdge e : adjacency.get(site.id())) {
            if (e.kind() == EdgeKind.SITE_ACCESS && f.alive(e)) {
                double ms = e.roundTripMs() == null ? Double.MAX_VALUE : e.roundTripMs();
                double bestMs = best == null ? 0 : best.roundTripMs() == null ? Double.MAX_VALUE : best.roundTripMs();
                if (best == null || ms < bestMs) {
                    best = e;
                }
            }
        }
        return best;
    }

    private static final class HostEval {
        String host;
        int cloudsReached;
        double siteWeight;
        double totalWeight;
        Map<String, Path> sitePaths = new LinkedHashMap<>();
        Map<String, Boolean> siteServed = new LinkedHashMap<>();
        Map<String, Path> cloudPaths = new LinkedHashMap<>();
        List<String> breaches = new ArrayList<>();
        Double worst;
    }

    private HostEval evaluateHost(Failure f, PlacementView p, String host) {
        HostEval h = new HostEval();
        h.host = host;
        for (GraphNode site : sites) {
            double w = site.weight() == null ? 0 : site.weight();
            h.totalWeight += w;
            Path path = sitePath(f, site, host);
            h.sitePaths.put(site.label(), path);
            boolean served = path != null;
            if (path != null && path.latencyKnown()) {
                h.worst = h.worst == null ? path.latencyMs() : Math.max(h.worst, path.latencyMs());
                if (p.maxLatencyMs() != null && path.latencyMs() > p.maxLatencyMs() + EPS) {
                    served = false;
                    h.breaches.add(String.format(Locale.ROOT, "workload '%s': site '%s' is %.1f ms from %s, over "
                            + "its %.1f ms limit", p.workload(), site.label(), path.latencyMs(), host, p.maxLatencyMs()));
                }
            }
            if (path != null && graph.siteMaxLatencyMs() != null) {
                GraphEdge hop = accessHop(f, site);
                if (hop != null && hop.roundTripMs() != null && hop.roundTripMs() > graph.siteMaxLatencyMs() + EPS) {
                    served = false;
                }
            }
            h.siteServed.put(site.label(), served);
            if (served) {
                h.siteWeight += w;
            }
        }
        for (String cloud : p.clouds()) {
            Path path = nodes.containsKey(DesignGraphBuilder.cloudId(cloud)) ? cloudPath(f, host, cloud) : null;
            h.cloudPaths.put(cloud, path);
            if (path != null) {
                h.cloudsReached++;
            }
        }
        return h;
    }

    private final class Run {
        final FailureScenario scenario;
        final Map<String, HostEval> chosen = new LinkedHashMap<>();
        final Map<String, Map<String, Boolean>> siteServed = new LinkedHashMap<>();
        final Map<String, Set<String>> cloudsReached = new LinkedHashMap<>();
        final Map<String, Double> edgeDemand = new HashMap<>();
        final Set<String> disconnected = new LinkedHashSet<>();
        final List<String> siteBreaches = new ArrayList<>();
        final Failure failure;

        Run(FailureScenario scenario) {
            this.scenario = scenario;
            this.failure = new Failure(scenario);
        }
    }

    private Run run(FailureScenario scenario) {
        Run r = new Run(scenario);
        Failure f = r.failure;
        for (GraphNode site : sites) {
            GraphEdge hop = accessHop(f, site);
            if (hop == null) {
                r.disconnected.add(site.label());
            } else if (graph.siteMaxLatencyMs() != null && hop.roundTripMs() != null
                    && hop.roundTripMs() > graph.siteMaxLatencyMs() + EPS) {
                r.siteBreaches.add(String.format(Locale.ROOT, "site '%s': nearest surviving metro %s is %.1f ms away, "
                        + "over the spec's %.1f ms limit", site.label(), nodes.get(hop.other(site.id())).metro(),
                        hop.roundTripMs(), graph.siteMaxLatencyMs()));
            }
        }
        for (PlacementView p : graph.placements()) {
            if (p.primary() == null) {
                continue;
            }
            HostEval best = null;
            for (String host : new String[]{p.primary(), p.standby()}) {
                if (host == null || !f.alive(DesignGraphBuilder.metroId(host))) {
                    continue;
                }
                HostEval h = evaluateHost(f, p, host);
                if (best == null || h.cloudsReached > best.cloudsReached
                        || (h.cloudsReached == best.cloudsReached && h.siteWeight > best.siteWeight + EPS)) {
                    best = h;
                }
            }
            r.chosen.put(p.workload(), best);
            Map<String, Boolean> served = new LinkedHashMap<>();
            Set<String> clouds = new LinkedHashSet<>();
            if (best != null) {
                served.putAll(best.siteServed);
                for (Map.Entry<String, Path> c : best.cloudPaths.entrySet()) {
                    if (c.getValue() != null) {
                        clouds.add(c.getKey());
                        GraphEdge conn = c.getValue().connection();
                        if (conn != null && p.bandwidthMbps() != null) {
                            r.edgeDemand.merge(conn.id(), p.bandwidthMbps().doubleValue(), Double::sum);
                        }
                    }
                }
            }
            r.siteServed.put(p.workload(), served);
            r.cloudsReached.put(p.workload(), clouds);
        }
        return r;
    }

    private ScenarioOutcome assemble(Run r, ScenarioOutcome base) {
        Map<String, Double> fraction = new HashMap<>();
        for (Map.Entry<String, Double> d : r.edgeDemand.entrySet()) {
            Double cap = graph.edge(d.getKey()).map(GraphEdge::capacityMbps).orElse(null);
            fraction.put(d.getKey(), cap == null || d.getValue() <= 0 ? 1.0 : Math.min(1.0, cap / d.getValue()));
        }
        List<WorkloadOutcome> workloads = new ArrayList<>();
        List<String> breaks = new ArrayList<>(r.siteBreaches);
        Set<String> zoneViolations = new LinkedHashSet<>();
        List<String> affected = new ArrayList<>();
        List<String> down = new ArrayList<>();
        Set<String> requiredLost = new TreeSet<>();
        Set<String> sitesAffected = new TreeSet<>();
        double demand = 0;
        double served = 0;
        double indexSum = 0;
        int placed = 0;
        Map<String, WorkloadOutcome> baseByLabel = new HashMap<>();
        if (base != null) {
            for (WorkloadOutcome w : base.workloads()) {
                baseByLabel.put(w.workload(), w);
            }
        }
        boolean pathChanged = false;
        for (PlacementView p : graph.placements()) {
            if (p.primary() == null) {
                continue;
            }
            placed++;
            HostEval h = r.chosen.get(p.workload());
            List<String> issues = new ArrayList<>();
            Map<String, String> sitePaths = new LinkedHashMap<>();
            Map<String, String> cloudPaths = new LinkedHashMap<>();
            double siteShare = 0;
            double capShare = 0;
            Set<String> baseClouds = baselineClouds == null ? Set.of() : baselineClouds.getOrDefault(p.workload(), Set.of());
            Map<String, Boolean> baseSites = baselineSiteServed == null ? Map.of()
                    : baselineSiteServed.getOrDefault(p.workload(), Map.of());
            if (h == null) {
                issues.add("no surviving host metro (primary " + p.primary()
                        + (p.standby() == null ? ", no standby" : ", standby " + p.standby()) + ")");
                for (String c : p.clouds()) {
                    cloudPaths.put(c, "unreachable");
                }
            } else {
                siteShare = h.totalWeight <= 0 ? 1.0 : h.siteWeight / h.totalWeight;
                for (Map.Entry<String, Path> s : h.sitePaths.entrySet()) {
                    sitePaths.put(s.getKey(), s.getValue() == null ? "unreachable" : s.getValue().render(nodes));
                    if (s.getValue() != null) {
                        for (String m : s.getValue().metros(nodes)) {
                            checkZone(zoneViolations, p.workload(), m, "site '" + s.getKey() + "' path");
                        }
                    } else {
                        issues.add("site '" + s.getKey() + "' cannot reach it");
                    }
                }
                checkZone(zoneViolations, p.workload(), h.host, "host");
                breaks.addAll(h.breaches);
                capShare = 1.0;
                for (String c : p.clouds()) {
                    Path path = h.cloudPaths.get(c);
                    if (path == null) {
                        cloudPaths.put(c, "unreachable");
                        issues.add("cloud '" + c + "' unreachable");
                        capShare = 0;
                        continue;
                    }
                    GraphEdge conn = path.connection();
                    double share = conn == null ? 1.0 : fraction.getOrDefault(conn.id(), 1.0);
                    capShare = Math.min(capShare, share);
                    cloudPaths.put(c, (conn == null ? "" : "via " + conn.id() + ": ") + path.render(nodes)
                            + (share < 1 - EPS ? String.format(Locale.ROOT, " at %.0f%% capacity", 100 * share) : ""));
                    if (share < 1 - EPS) {
                        issues.add(String.format(Locale.ROOT, "cloud '%s' at %.0f%% of its bandwidth", c, 100 * share));
                    }
                    for (String m : path.metros(nodes)) {
                        checkZone(zoneViolations, p.workload(), m, "cloud '" + c + "' path");
                    }
                }
            }
            for (String c : p.clouds()) {
                if (p.bandwidthMbps() != null && (base == null || baseClouds.contains(c))) {
                    demand += p.bandwidthMbps();
                    if (h != null && h.cloudPaths.get(c) != null) {
                        GraphEdge conn = h.cloudPaths.get(c).connection();
                        served += p.bandwidthMbps() * (conn == null ? 1.0 : fraction.getOrDefault(conn.id(), 1.0));
                    }
                }
                if (base != null && baseClouds.contains(c) && graph.requiredClouds().contains(c)
                        && (h == null || h.cloudPaths.get(c) == null)) {
                    requiredLost.add(c);
                }
            }
            double index = h == null ? 0 : siteShare * capShare;
            indexSum += index;
            boolean breached = h != null && !h.breaches.isEmpty();
            boolean nothingReaches = h == null || (p.clouds().isEmpty()
                    ? h.sitePaths.values().stream().allMatch(java.util.Objects::isNull)
                    : h.cloudsReached == 0);
            WorkloadStatus status = index <= EPS && nothingReaches ? WorkloadStatus.DOWN
                    : index >= 1 - EPS && !breached ? WorkloadStatus.FULL : WorkloadStatus.DEGRADED;
            WorkloadOutcome outcome = new WorkloadOutcome(p.workload(), status, h == null ? null : h.host,
                    h != null && !h.host.equals(p.primary()), siteShare, capShare, index, sitePaths, cloudPaths,
                    h == null ? null : h.worst, issues);
            workloads.add(outcome);
            WorkloadOutcome was = baseByLabel.get(p.workload());
            if (was != null) {
                if (outcome.serviceIndex() < was.serviceIndex() - EPS || outcome.status().ordinal() > was.status().ordinal()
                        || (breached && was.status() == WorkloadStatus.FULL)) {
                    affected.add(p.workload());
                }
                if (status == WorkloadStatus.DOWN && was.status() != WorkloadStatus.DOWN) {
                    down.add(p.workload());
                }
                if (!outcome.sitePaths().equals(was.sitePaths()) || !outcome.cloudPaths().equals(was.cloudPaths())
                        || !java.util.Objects.equals(outcome.activeMetro(), was.activeMetro())) {
                    pathChanged = true;
                }
                Map<String, Boolean> now = r.siteServed.getOrDefault(p.workload(), Map.of());
                for (Map.Entry<String, Boolean> s : baseSites.entrySet()) {
                    if (s.getValue() && !now.getOrDefault(s.getKey(), false)) {
                        sitesAffected.add(s.getKey());
                    }
                }
            }
        }
        double siteAffectedWeight = 0;
        double siteDisconnectedWeight = 0;
        for (GraphNode site : sites) {
            double w = site.weight() == null ? 0 : site.weight();
            if (r.disconnected.contains(site.label())) {
                siteDisconnectedWeight += w;
                sitesAffected.add(site.label());
            }
            if (sitesAffected.contains(site.label())) {
                siteAffectedWeight += w;
            }
        }
        double index = placed == 0 ? (sites.isEmpty() ? 1.0 : 1.0 - siteDisconnectedWeight) : indexSum / placed;
        double relative;
        if (base == null) {
            relative = 1.0;
        } else if (base.serviceIndex() <= EPS) {
            relative = 1.0;
        } else {
            relative = Math.max(0, Math.min(1.0, index / base.serviceIndex()));
        }
        Verdict verdict;
        if (base == null) {
            verdict = Verdict.UNAFFECTED;
        } else if (!down.isEmpty() || siteDisconnectedWeight > EPS || !requiredLost.isEmpty()) {
            verdict = Verdict.OUTAGE;
        } else if (!affected.isEmpty() || relative < 1 - EPS || served < demandBase(base) - EPS
                || newBreaches(breaks, base)) {
            verdict = Verdict.DEGRADED;
        } else if (pathChanged) {
            verdict = Verdict.SURVIVES;
        } else {
            verdict = Verdict.UNAFFECTED;
        }
        Map<String, Double> load = new java.util.TreeMap<>(r.edgeDemand);
        return new ScenarioOutcome(r.scenario, verdict, index, relative, siteAffectedWeight, siteDisconnectedWeight,
                affected, down, new ArrayList<>(requiredLost), demand, served, Math.max(0, demand - served), breaks,
                new ArrayList<>(zoneViolations), workloads, load);
    }

    private static double demandBase(ScenarioOutcome base) {
        return base.servedMbps();
    }

    private static boolean newBreaches(List<String> breaks, ScenarioOutcome base) {
        for (String b : breaks) {
            if (!base.constraintBreaks().contains(b)) {
                return true;
            }
        }
        return false;
    }

    private void checkZone(Set<String> violations, String workload, String metro, String where) {
        if (graph.allowedZones().isEmpty() || metro == null) {
            return;
        }
        GraphNode m = nodes.get(DesignGraphBuilder.metroId(metro));
        Set<String> zones = m == null ? Set.of() : m.geoZones();
        for (String z : graph.allowedZones()) {
            if (zones.contains(z)) {
                return;
            }
        }
        violations.add("workload '" + workload + "' " + where + " uses " + metro + " (zones " + zones
                + "), outside " + graph.allowedZones());
    }
}
