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

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.design.optimizer.MetroOptimizer;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.context.FabricCatalogLoader;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.peering.model.PeeringIntelligenceResult;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.placement.WorkloadPlacer;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.CorrelatedRisk;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.Level;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.NVerdict;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.PairSummary;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.RankedScenario;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.Remediation;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.Severity;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.SinglePointOfFailure;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.value.ratecard.RateCard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Analyses how a design survives failures: builds its {@link DesignGraph}, simulates every single failure
 * (exhaustively) and pairs of failures (bounded), ranks the blast radius, finds single points of failure
 * and correlated risks, estimates availability and proposes remediation, re-simulating it where it can.
 *
 * <pre>{@code
 * // Live (read-only): load the Fabric catalogue, run the optimiser, analyse its recommendation
 * ResilienceReport report = ResilienceAnalyzer.forSpec(fabric, spec).run();
 * System.out.println(report.toMarkdown());
 *
 * // An explicit portfolio, or an optimiser result you already have
 * ResilienceAnalyzer.forSpec(fabric, spec).portfolio(MetroPortfolio.ofCodes("DC", "NY")).run();
 * ResilienceAnalyzer.forSpec(fabric, spec).optimization(result).run();
 *
 * // Offline, over facts you already hold (tests, cached catalogue, what-ifs)
 * ResilienceAnalyzer.forContext(catalogContext).run();
 * }</pre>
 *
 * <p>Read-only: it never creates, changes or deletes anything. Without an explicit portfolio, the
 * portfolio is the Metro Optimizer's recommendation for the spec; with a {@link CatalogDesignContext}
 * this needs no network, otherwise the optimiser prices with {@code RateCard.standardChain(fabric)}
 * unless a {@link #rateCard(RateCard)} is given.</p>
 */
public final class ResilienceAnalyzer {

    private final FabricGateway fabric;
    private final DesignContext offlineContext;
    private final DesignSpec spec;
    private MetroPortfolio portfolio;
    private OptimizationResult optimization;
    private RateCard rateCard;
    private ResilienceOptions options = ResilienceOptions.defaults();
    private PeeringIntelligenceResult peering;

    private ResilienceAnalyzer(FabricGateway fabric, DesignContext context, DesignSpec spec) {
        this.fabric = fabric;
        this.offlineContext = context;
        this.spec = spec;
    }

    /**
     * Analyses a spec against live Fabric data (read-only).
     *
     * @param fabric the Fabric API
     * @param spec   the design
     * @return an analysis to configure and {@link #run()}
     */
    public static ResilienceAnalyzer forSpec(FabricGateway fabric, DesignSpec spec) {
        if (fabric == null || spec == null) {
            throw new IllegalArgumentException("forSpec needs a Fabric gateway and a spec");
        }
        return new ResilienceAnalyzer(fabric, null, spec);
    }

    /**
     * Analyses over facts already gathered (no network unless the optimiser has to price and the context
     * has no cost model).
     *
     * @param context the facts; its spec is the design
     * @return an analysis to configure and {@link #run()}
     */
    public static ResilienceAnalyzer forContext(DesignContext context) {
        if (context == null) {
            throw new IllegalArgumentException("forContext needs a context");
        }
        return new ResilienceAnalyzer(null, context, context.spec());
    }

    /**
     * @param metros the portfolio to analyse instead of the optimiser's recommendation
     * @return this analysis
     */
    public ResilienceAnalyzer portfolio(MetroPortfolio metros) {
        this.portfolio = metros;
        return this;
    }

    /**
     * @param result an optimiser result whose recommended portfolio is analysed (it must be for the same spec)
     * @return this analysis
     */
    public ResilienceAnalyzer optimization(OptimizationResult result) {
        this.optimization = result;
        return this;
    }

    /**
     * @param card the rate card the optimiser prices with when it has to run
     * @return this analysis
     */
    public ResilienceAnalyzer rateCard(RateCard card) {
        this.rateCard = card;
        return this;
    }

    /**
     * @param resilienceOptions graph and simulation options
     * @return this analysis
     */
    public ResilienceAnalyzer options(ResilienceOptions resilienceOptions) {
        this.options = resilienceOptions == null ? ResilienceOptions.defaults() : resilienceOptions;
        return this;
    }

    /**
     * @param result a peering analysis whose presence is drawn into the graph as peer nodes
     * @return this analysis
     */
    public ResilienceAnalyzer peering(PeeringIntelligenceResult result) {
        this.peering = result;
        return this;
    }

    /**
     * Runs the analysis.
     *
     * @return the report
     * @throws IllegalArgumentException when the spec has errors, the optimisation result belongs to another
     *                                  spec, or nothing is feasible to analyse
     */
    public ResilienceReport run() {
        spec.requireValid();
        Map<String, Long> timings = new LinkedHashMap<>();
        long t = System.currentTimeMillis();
        DesignContext context = offlineContext;
        if (context == null) {
            context = FabricCatalogLoader.load(fabric, spec, null);
            timings.put("catalogue load", System.currentTimeMillis() - t);
            t = System.currentTimeMillis();
        }
        MetroPortfolio metros = portfolio;
        String source = "given";
        List<String> notes = new ArrayList<>();
        if (metros == null && optimization != null) {
            if (!Objects.equals(optimization.getSpecFingerprint(), spec.fingerprint())) {
                throw new IllegalArgumentException("The optimisation result is for another spec (fingerprint "
                        + optimization.getSpecFingerprint() + ", this spec " + spec.fingerprint() + ")");
            }
            metros = recommended(optimization);
            source = "the given optimisation result's recommendation";
        }
        if (metros == null) {
            if (!(context instanceof CatalogDesignContext catalog)) {
                throw new IllegalArgumentException("Give a portfolio: the optimiser needs a CatalogDesignContext");
            }
            MetroOptimizer optimizer = MetroOptimizer.forContext(catalog);
            if (rateCard != null) {
                optimizer.rateCard(rateCard);
            } else if (fabric != null && catalog.costModel() == null) {
                optimizer.rateCard(RateCard.standardChain(fabric));
            }
            OptimizationResult result = optimizer.run();
            metros = recommended(result);
            source = "the Metro Optimizer's recommendation";
            timings.put("optimise", System.currentTimeMillis() - t);
            t = System.currentTimeMillis();
        }
        PlacementPlan plan = WorkloadPlacer.place(metros, context);
        List<PeerPresence> peers = DesignGraphBuilder.peers(peering, spec);
        if (peering != null && peering.getSpecFingerprint() != null
                && !peering.getSpecFingerprint().equals(spec.fingerprint())) {
            notes.add("The peering analysis was run for another spec (fingerprint "
                    + peering.getSpecFingerprint() + "); its presence is drawn as given.");
        }
        ResilienceReport report = analyze(context, metros, plan, options, peers, source, notes, timings, t);
        return report;
    }

    /**
     * The analysis itself, pure: no I/O, deterministic apart from {@code timingsMs}.
     *
     * @param context   the facts (its spec is the design)
     * @param metros    the portfolio
     * @param options   options; {@code null} for the defaults
     * @return the report
     */
    public static ResilienceReport analyze(DesignContext context, MetroPortfolio metros, ResilienceOptions options) {
        return analyze(context, metros, WorkloadPlacer.place(metros, context),
                options == null ? ResilienceOptions.defaults() : options, List.of(), "given", new ArrayList<>(),
                new LinkedHashMap<>(), System.currentTimeMillis());
    }

    private static ResilienceReport analyze(DesignContext context, MetroPortfolio metros, PlacementPlan plan,
                                            ResilienceOptions options, List<PeerPresence> peers, String source,
                                            List<String> notes, Map<String, Long> timings, long started) {
        long t = started;
        DesignGraph graph = DesignGraphBuilder.build(context, metros, plan, options, peers);
        timings.put("graph", System.currentTimeMillis() - t);
        t = System.currentTimeMillis();

        FailureSimulator sim = new FailureSimulator(graph);
        ScenarioOutcome baseline = sim.baseline();
        List<ScenarioOutcome> singles = sim.simulateAll(ScenarioCatalog.singles(graph));
        timings.put("single failures (" + singles.size() + ")", System.currentTimeMillis() - t);
        t = System.currentTimeMillis();

        ScenarioCatalog.Pairs pairSet = ScenarioCatalog.pairs(graph, options.maxPairScenarios());
        List<ScenarioOutcome> pairOutcomes = sim.simulateAll(pairSet.scenarios());
        timings.put("pairs (" + pairOutcomes.size() + ")", System.currentTimeMillis() - t);
        t = System.currentTimeMillis();

        List<ScenarioOutcome> elementSingles = singles.stream().filter(o -> o.scenario().kind().isSingleElement())
                .toList();
        NVerdict n1 = verdict("N-1", elementSingles, elementSingles.size(), true);
        NVerdict n2 = verdict("N-2", pairOutcomes, pairSet.total(), pairSet.exhaustive());
        List<RankedScenario> blast = rank(singles, Integer.MAX_VALUE);
        PairSummary pairs = new PairSummary(pairSet.total(), pairOutcomes.size(), pairSet.exhaustive(),
                rank(pairOutcomes, 10));

        List<SinglePointOfFailure> spofs = new ArrayList<>();
        for (ScenarioOutcome o : elementSingles) {
            if (o.verdict() == Verdict.OUTAGE) {
                spofs.add(new SinglePointOfFailure(o.scenario().id(), o.scenario().kind(), o.scenario().description(),
                        effect(o)));
            }
        }
        List<CorrelatedRisk> risks = correlatedRisks(graph);
        List<Remediation> remediations = new RemediationAdvisor(graph, context, metros, options, baseline)
                .advise(singles);
        timings.put("remediation", System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        List<WorkloadAvailability> availability = AvailabilityModel.estimate(graph, options.availability());
        timings.put("availability", System.currentTimeMillis() - t);

        List<String> assumptions = new ArrayList<>(graph.assumptions());
        assumptions.add("All latencies are round-trip ms: Fabric measurements where published, calibrated estimates "
                + "otherwise; an unknown latency is never held against a path.");
        assumptions.add("Failures are of whole elements; partial degradation (packet loss, brown-outs) is not modelled.");
        assumptions.add("N-1 counts single-element failures (metro, Cloud Router, on-ramp, connection); cloud-region "
                + "and Equinix-region losses are shared-fate events, reported and ranked but not counted in N-1.");
        assumptions.add("Availability figures compose assumed per-element availabilities assuming independent failures "
                + "(" + options.availability().source() + ").");
        for (PlacementPlan.Placement p : plan.placements()) {
            for (String issue : p.issues()) {
                notes.add("Placement of '" + p.workload() + "': " + issue);
            }
        }
        return new ResilienceReport(context.spec().fingerprint(), context.spec().name(), metros.key(), source,
                context.asOf(), graph, baseline, singles, pairs, blast, spofs, risks, n1, n2, remediations,
                availability, options.availability(), assumptions, notes, timings);
    }

    private static MetroPortfolio recommended(OptimizationResult result) {
        if (result.getRecommended() == null) {
            throw new IllegalArgumentException("Nothing to analyse: the optimiser found no feasible design ("
                    + String.join("; ", result.getSolverNotes() == null ? List.of() : result.getSolverNotes()) + ")");
        }
        return result.getRecommended().portfolio();
    }

    private static String effect(ScenarioOutcome o) {
        List<String> parts = new ArrayList<>();
        if (!o.workloadsDown().isEmpty()) {
            parts.add("workloads down: " + String.join(", ", o.workloadsDown()));
        }
        if (!o.requiredCloudsAffected().isEmpty()) {
            parts.add("required clouds lost: " + String.join(", ", o.requiredCloudsAffected()));
        }
        if (o.siteWeightDisconnected() > 0) {
            parts.add(String.format(Locale.ROOT, "%.0f%% of site weight cut off", 100 * o.siteWeightDisconnected()));
        }
        return String.join("; ", parts);
    }

    static NVerdict verdict(String name, List<ScenarioOutcome> outcomes, int total, boolean exhaustive) {
        int survived = 0;
        int degraded = 0;
        int outages = 0;
        for (ScenarioOutcome o : outcomes) {
            switch (o.verdict()) {
                case OUTAGE -> outages++;
                case DEGRADED -> degraded++;
                default -> survived++;
            }
        }
        if (outcomes.isEmpty()) {
            return new NVerdict(Level.NOT_APPLICABLE, 0, 0, 0, 0, name + ": nothing to simulate.");
        }
        Level level = outages > 0 ? Level.FAIL : degraded > 0 ? Level.DEGRADED : Level.PASS;
        String coverage = exhaustive ? "all " + outcomes.size() : outcomes.size() + " of " + total
                + " (bounded; the rest were not simulated)";
        String text = switch (level) {
            case PASS -> name + " passes: every one of " + coverage + " scenarios survives at full service.";
            case DEGRADED -> name + " passes degraded: no outage across " + coverage + " scenarios, but " + degraded
                    + " reduce service.";
            default -> name + " fails: " + outages + " of " + coverage + " scenarios cause an outage"
                    + (degraded > 0 ? " and " + degraded + " degrade service" : "") + ".";
        };
        return new NVerdict(level, outcomes.size(), survived, degraded, outages, text);
    }

    static List<RankedScenario> rank(List<ScenarioOutcome> outcomes, int limit) {
        List<ScenarioOutcome> sorted = new ArrayList<>(outcomes);
        sorted.sort(Comparator.comparingDouble(ScenarioOutcome::impact).reversed()
                .thenComparing(Comparator.comparingDouble(ScenarioOutcome::siteWeightAffected).reversed())
                .thenComparing(Comparator.comparingDouble(ScenarioOutcome::shortfallMbps).reversed())
                .thenComparing(Comparator.comparing((ScenarioOutcome o) -> o.verdict().ordinal()).reversed())
                .thenComparing(o -> o.scenario().id()));
        List<RankedScenario> ranked = new ArrayList<>();
        for (ScenarioOutcome o : sorted) {
            if (ranked.size() >= limit) {
                break;
            }
            ranked.add(new RankedScenario(ranked.size() + 1, o.scenario().id(), o.scenario().description(),
                    o.scenario().kind(), o.verdict(), o.impact(), o.siteWeightAffected(), o.shortfallMbps(),
                    o.workloadsDown()));
        }
        return ranked;
    }

    /**
     * Redundant elements that share a failure domain: a workload's primary and standby metros in one Equinix
     * region; a cloud's connections all in one metro or one region; a site's access circuits all in one
     * region; clouds the design treats as separate that share one provider region.
     */
    static List<CorrelatedRisk> correlatedRisks(DesignGraph graph) {
        List<CorrelatedRisk> risks = new ArrayList<>();
        for (PlacementView p : graph.placements()) {
            if (p.primary() == null || p.standby() == null) {
                continue;
            }
            var a = graph.node(DesignGraphBuilder.metroId(p.primary())).orElse(null);
            var b = graph.node(DesignGraphBuilder.metroId(p.standby())).orElse(null);
            if (a != null && b != null && a.region() != null && a.region() == b.region()) {
                risks.add(new CorrelatedRisk(List.of(a.id(), b.id()), "Equinix region " + a.region(), Severity.MEDIUM,
                        "equinix-region:" + a.region().name(), "workload '" + p.workload() + "': primary "
                        + p.primary() + " and standby " + p.standby() + " are both in " + a.region()));
            }
        }
        Map<String, List<GraphEdge>> byCloud = new TreeMap<>();
        for (GraphEdge e : graph.edges(EdgeKind.CLOUD_CONNECTION)) {
            String cloud = graph.node(e.to()).map(GraphNode::cloudId).orElse(null);
            if (cloud != null) {
                byCloud.computeIfAbsent(cloud, k -> new ArrayList<>()).add(e);
            }
        }
        for (Map.Entry<String, List<GraphEdge>> c : byCloud.entrySet()) {
            List<GraphEdge> conns = c.getValue();
            if (conns.size() < 2) {
                continue;
            }
            List<String> ids = conns.stream().map(GraphEdge::id).toList();
            var metros = conns.stream().map(e -> graph.node(e.to()).map(GraphNode::metro).orElse("?")).distinct().toList();
            var regions = conns.stream().map(e -> graph.node(e.to()).map(GraphNode::region).orElse(null)).distinct()
                    .toList();
            if (metros.size() == 1) {
                risks.add(new CorrelatedRisk(ids, "metro " + metros.get(0), Severity.HIGH, "metro:" + metros.get(0),
                        "every connection to " + c.getKey() + " is in " + metros.get(0)));
            } else if (regions.size() == 1 && regions.get(0) != null) {
                risks.add(new CorrelatedRisk(ids, "Equinix region " + regions.get(0), Severity.MEDIUM,
                        "equinix-region:" + regions.get(0).name(), "every connection to " + c.getKey() + " ("
                        + String.join(", ", metros) + ") is in " + regions.get(0)));
            }
        }
        for (GraphNode site : graph.nodes(NodeKind.SITE)) {
            List<GraphEdge> access = graph.edges(EdgeKind.SITE_ACCESS).stream()
                    .filter(e -> e.from().equals(site.id())).toList();
            if (access.size() < 2) {
                continue;
            }
            var regions = access.stream().map(e -> graph.node(e.to()).map(GraphNode::region).orElse(null)).distinct()
                    .toList();
            if (regions.size() == 1 && regions.get(0) != null) {
                risks.add(new CorrelatedRisk(access.stream().map(GraphEdge::id).toList(),
                        "Equinix region " + regions.get(0), Severity.MEDIUM, "equinix-region:" + regions.get(0).name(),
                        "site '" + site.label() + "': both access circuits land in " + regions.get(0)));
            }
        }
        Map<String, List<String>> byProviderRegion = new TreeMap<>();
        for (GraphNode cloud : graph.nodes(NodeKind.CLOUD)) {
            if (cloud.providerRegion() != null) {
                byProviderRegion.computeIfAbsent(cloud.providerRegion(), k -> new ArrayList<>()).add(cloud.id());
            }
        }
        for (Map.Entry<String, List<String>> r : byProviderRegion.entrySet()) {
            if (r.getValue().size() > 1) {
                risks.add(new CorrelatedRisk(r.getValue(), "cloud region " + r.getKey(), Severity.MEDIUM,
                        "cloud-region:" + r.getKey().replace(' ', '/'), "clouds " + r.getValue() + " are separate in "
                        + "the spec but share the provider region " + r.getKey()));
            }
        }
        return risks;
    }
}
