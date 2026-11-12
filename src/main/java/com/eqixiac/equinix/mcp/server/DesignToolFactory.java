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

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.geo.RouteFactorCalibration;
import com.eqixiac.equinix.design.geo.SpeedOfLightLatency;
import com.eqixiac.equinix.design.optimizer.MetroOptimizer;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.RiskFinding;
import com.eqixiac.equinix.design.peering.PeeringIntelligence;
import com.eqixiac.equinix.design.peering.model.PeeringIntelligenceResult;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.resilience.ResilienceAnalyzer;
import com.eqixiac.equinix.design.resilience.ResilienceFindings;
import com.eqixiac.equinix.design.resilience.ResilienceOptions;
import com.eqixiac.equinix.design.resilience.ResilienceReport;
import com.eqixiac.equinix.design.resilience.ScenarioOutcome;
import com.eqixiac.equinix.design.resilience.WorkloadAvailability;
import com.eqixiac.equinix.design.solve.SensitivityReport;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.MetroRegistry;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.internetaccess.model.Ibx;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static com.eqixiac.equinix.mcp.server.Args.enumValue;
import static com.eqixiac.equinix.mcp.server.Args.optInt;
import static com.eqixiac.equinix.mcp.server.Args.optLong;
import static com.eqixiac.equinix.mcp.server.Args.requireString;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.Detail;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.capped;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.round1;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.round4;
import static com.eqixiac.equinix.mcp.server.Schemas.array;
import static com.eqixiac.equinix.mcp.server.Schemas.detail;
import static com.eqixiac.equinix.mcp.server.Schemas.integer;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.objectOrString;
import static com.eqixiac.equinix.mcp.server.Schemas.props;
import static com.eqixiac.equinix.mcp.server.Schemas.string;
import static com.eqixiac.equinix.mcp.server.Schemas.with;

/**
 * The design toolset's catalog and its engine tools. Every engine tool takes the same input, a
 * {@code DesignSpec} ({@link SpecInput}), and every result carries the spec fingerprint and a
 * {@code provenance} block, so an agent can run the whole loop with the spec as the shared currency:
 * {@code design_spec_template} -> {@code design_validate_spec} -> {@code design_optimize} ->
 * {@code design_analyze_resilience} / {@code design_growth_and_terms} / {@code design_estimate_tco} ->
 * {@code design_plan_deployment} -> {@code design_get_plan} / {@code design_plan_drift} /
 * {@code design_export_terraform}.
 *
 * <p>Built here: optimization, resilience, latency and peering. The spec tools are in
 * {@link SpecTools}, the value tools in {@link ValueTools}, the plan lifecycle in {@link PlanTools},
 * the cloud-to-cloud tools (Beta) in {@link MulticloudDesignTools} and the estate analysis (its own
 * toolset) in {@link EstateTools}. Every tool here is read-only.</p>
 */
final class DesignToolFactory {

    static final String[] CLOUD_VALUES = {
            "aws", "azure", "gcp", "google_cloud", "oci", "oracle_cloud", "ibm_cloud", "alibaba_cloud"};

    private static final Pattern METRO = Pattern.compile("[A-Z]{2}");

    private DesignToolFactory() {
    }

    /**
     * The {@code design} toolset, in the order an agent typically uses it.
     *
     * @return the registrations
     */
    static List<ToolRegistration> tools() {
        List<ToolRegistration> tools = new ArrayList<>(SpecTools.tools());
        tools.add(optimize());
        tools.add(analyzeResilience());
        tools.add(ValueTools.growthAndTerms());
        tools.add(ValueTools.estimateTco());
        tools.add(ValueTools.compareCloudEgress());
        tools.add(estimateLatency());
        tools.add(MulticloudDesignTools.compareCloudToCloud());
        tools.add(MulticloudDesignTools.listMulticloudEnvironments());
        tools.add(analyzePeering());
        tools.addAll(PlanTools.tools());
        tools.add(DecisionRecordTools.decisionRecord());
        return List.copyOf(tools);
    }

    // shared inputs

    /** The {@code portfolio} argument schema. */
    static Map<String, Object> portfolioSchema(String what) {
        return with(array("Metro codes of the design to " + what + ", e.g. [\"LD\", \"NY\"]. Omit to use the "
                + "recommendation of the stored design_optimize result for this spec (or, where stated, to run the "
                + "optimizer).", with(string("Two-letter Fabric metro code."), "pattern", "^[A-Z]{2}$")),
                "minItems", 1, "maxItems", 10);
    }

    /** The {@code spec_fingerprint} argument schema. */
    static Map<String, Object> fingerprintSchema() {
        return with(string("Instead of 'spec': the spec_fingerprint of a design_optimize result stored in this "
                + "server process (60 minutes). The stored result's spec is used."), "pattern", "^[0-9a-f]{64}$");
    }

    /**
     * The spec of a call: the {@code spec} argument, else the spec of the stored optimization named by
     * {@code spec_fingerprint}.
     *
     * @param args the arguments
     * @param ctx  the context
     * @return the spec and the stored optimization when one was named or exists for the spec
     */
    static SpecAndRun specOf(JsonNode args, ServerContext ctx) {
        if (args.hasNonNull(SpecInput.ARG)) {
            DesignSpec spec = SpecInput.require(args, ctx.objectMapper());
            DesignStore.Lookup<OptimizationResult> stored = ctx.designStore().optimization(spec.fingerprint());
            return new SpecAndRun(spec, stored.found() ? stored.entry().value() : null);
        }
        Optional<String> fingerprint = Args.optString(args, SpecInput.FINGERPRINT_ARG);
        if (fingerprint.isEmpty()) {
            throw new IllegalArgumentException("Give 'spec' (a DesignSpec object or JSON/YAML string) or "
                    + "'spec_fingerprint' (of a design_optimize result from this server process).");
        }
        DesignStore.Lookup<OptimizationResult> stored = ctx.designStore().optimization(fingerprint.get());
        if (!stored.found()) {
            throw new IllegalArgumentException(ctx.designStore().explainMissing(DesignStore.Kind.OPTIMIZATION,
                    fingerprint.get(), stored.status()));
        }
        return new SpecAndRun(stored.entry().value().getSpec(), stored.entry().value());
    }

    /**
     * A spec and, when available, its stored optimization.
     *
     * @param spec      the spec
     * @param optimized the stored optimization for this spec, or {@code null}
     */
    record SpecAndRun(DesignSpec spec, OptimizationResult optimized) {
    }

    /**
     * Reads the {@code portfolio} argument.
     *
     * @param args the arguments
     * @return the portfolio, or empty when absent
     */
    static Optional<MetroPortfolio> portfolio(JsonNode args) {
        List<String> codes = Args.stringList(args, "portfolio");
        if (codes.isEmpty()) {
            return Optional.empty();
        }
        List<String> upper = new ArrayList<>();
        for (String code : codes) {
            String c = code.trim().toUpperCase(Locale.ROOT);
            if (!METRO.matcher(c).matches()) {
                throw new IllegalArgumentException("'portfolio' entry '" + code + "' is not a metro code: use the "
                        + "two-letter Fabric code, e.g. LD, NY, DC.");
            }
            upper.add(c);
        }
        return Optional.of(MetroPortfolio.ofCodes(upper.toArray(new String[0])));
    }

    /**
     * Runs the optimizer for a spec under the call's pricing budget and cancellation, stores the result
     * and returns it.
     *
     * @param spec         the spec
     * @param ctx          the context
     * @param budget       the call's pricing budget (read on the calling thread)
     * @param guards       collects the guarded live Equinix pricing layer
     * @param cancellation the call's signal
     * @return the result and what the store evicted
     * @throws Exception when the run fails or is cancelled
     */
    static Optimized optimize(DesignSpec spec, ServerContext ctx, PricingBudget budget, List<TimeoutRateCard> guards,
                              CancellationSignal cancellation) throws Exception {
        FabricGateway fabric = ctx.fabric();
        MetroOptimizer optimizer = MetroOptimizer.forSpec(fabric, spec)
                .rateCard(TimeoutRateCard.equinixChain(fabric, budget, guards))
                .multicloudEnvironments(ctx.multicloudEnvironments());
        OptimizationResult result = EngineRunner.run("the optimization", cancellation, optimizer::run);
        DesignStore.Put put = ctx.designStore().putOptimization(result);
        return new Optimized(result, put.evicted());
    }

    /**
     * An optimization this call ran.
     *
     * @param result  the result
     * @param evicted store entries evicted to keep it
     */
    record Optimized(OptimizationResult result, List<String> evicted) {
    }

    /**
     * The recommended portfolio of an optimization.
     *
     * @param result the result
     * @return the portfolio, or empty when nothing was feasible
     */
    static Optional<MetroPortfolio> recommended(OptimizationResult result) {
        return result == null || result.getRecommended() == null ? Optional.empty()
                : Optional.of(result.getRecommended().portfolio());
    }

    static void reportEvicted(ObjectNode payload, List<String> evicted, DesignStore.Kind kind) {
        if (!evicted.isEmpty()) {
            ObjectNode e = payload.putObject("evicted_from_store");
            e.put("kind", kind.name().toLowerCase(Locale.ROOT));
            ArrayNode ids = e.putArray("ids");
            evicted.forEach(ids::add);
            e.put("note", "The store keeps at most its capacity per kind; these older entries can no longer be "
                    + "resolved by id.");
        }
    }

    // design_optimize

    private static ToolRegistration optimize() {
        return ToolRegistration.builder()
                .name("design_optimize")
                .title("Optimize metro placement for a DesignSpec")
                .description("Runs the Metro Optimizer on a DesignSpec: enumerates metro portfolios (sets of metros) "
                        + "within the spec's min/max metros, rejects those breaking a hard constraint (required/"
                        + "excluded metros and regions, compliance and geo zones, redundancy tier, ROUND-TRIP latency "
                        + "ceilings, required clouds, pinned workloads, single-currency budget), scores the rest on "
                        + "the spec's objectives (latency, cost, provider coverage, resilience, compliance) and "
                        + "returns the recommendation, the Pareto frontier, sensitivity (how far each weight can move "
                        + "before the answer changes, nearest alternative, binding constraints), findings, and the "
                        + "cost with every line's source (live Equinix price, reference figure or estimate, with "
                        + "low/high range) converted into commercial.currency (pinned, live ECB or bundled rates, each "
                        + "conversion listed). Returns the spec_fingerprint (later tools accept it instead of the "
                        + "spec; the result is kept in this server process for 60 minutes) and pinned_spec: the spec "
                        + "with the exchange rates it used pinned, so a re-run reproduces the figures. READ-ONLY; "
                        + "reads the live Fabric catalogue and prices. Takes tens of seconds; cancellable. Live "
                        + "pricing shares the call's EQUINIX_MCP_PRICING_TIMEOUT_MS budget: lookups past it fall back "
                        + "to reference figures and are listed under equinix_live_pricing. Unknown values are null, "
                        + "never 0.")
                .inputSchema(object(props(
                                SpecInput.ARG, objectOrString(SpecTools.SPEC_ARG_DESCRIPTION),
                                "detail", detail("summary", "standard adds sensitivity, cost lines, constraint "
                                        + "verdicts, workload placement, solver notes and data provenance.")),
                        SpecInput.ARG))
                .outputSchema(DesignPayloads.outputSchema("status, recommended design (portfolio, objectives, cost), "
                        + "frontier, findings, currency_conversions, pinned_spec, spec_fingerprint, provenance; more "
                        + "with detail=standard/full.", "status", "spec_fingerprint"))
                .toolset(Toolset.DESIGN)
                .handler((ToolHandler.Cancellable) DesignToolFactory::handleOptimize)
                .build();
    }

    private static ObjectNode handleOptimize(JsonNode args, ServerContext ctx, CancellationSignal cancellation)
            throws Exception {
        DesignSpec spec = SpecInput.require(args, ctx.objectMapper());
        Detail detail = Detail.of(args, Detail.SUMMARY);
        PricingBudget budget = ctx.pricingBudget();
        List<TimeoutRateCard> guards = new ArrayList<>();
        Optimized run = optimize(spec, ctx, budget, guards, cancellation);
        ObjectNode payload = optimizationPayload(run.result(), detail, ctx.objectMapper());
        reportEvicted(payload, run.evicted(), DesignStore.Kind.OPTIMIZATION);
        TimeoutRateCard.reportEquinix(payload, guards, budget);
        return payload;
    }

    static ObjectNode optimizationPayload(OptimizationResult result, Detail detail, ObjectMapper mapper) {
        ObjectNode payload = mapper.createObjectNode();
        DesignOption recommended = result.getRecommended();
        payload.put("status", recommended == null ? "no_feasible_design" : "recommended");
        payload.put("spec_fingerprint", result.getSpecFingerprint());
        payload.put("spec_name", result.getSpec() == null ? null : result.getSpec().name());
        payload.set("summary", UntrustedText.document(result.toSummary()));

        Map<String, MetroRecommendation> byCode = new java.util.HashMap<>();
        if (result.getRecommendations() != null) {
            result.getRecommendations().forEach(r -> byCode.put(String.valueOf(r.getMetroId()), r));
        }
        if (recommended != null) {
            ObjectNode rec = payload.putObject("recommended");
            option(rec, recommended, detail, byCode);
        }
        List<DesignOption> frontier = result.getFrontier() == null ? List.of() : result.getFrontier();
        int frontierCap = detail == Detail.SUMMARY ? 3 : detail.cap();
        capped(payload, "frontier", frontier, frontierCap, (array, option) -> {
            ObjectNode o = array.addObject();
            o.put("portfolio", option.portfolio().key());
            o.put("weighted_score", round1(option.weightedScore()));
            ObjectNode values = o.putObject("objectives");
            option.values().forEach((id, v) -> values.put(id, v.known() ? round1(v.value()) : null));
            ObjectNode c = DesignPayloads.cost(o, "cost", option.cost(), false, 5);
            c.remove("unpriced");
        });

        List<RiskFinding> findings = result.getRiskAssessment() == null
                || result.getRiskAssessment().getFindings() == null ? List.of()
                : result.getRiskAssessment().getFindings();
        capped(payload, "findings", findings, detail.cap(), (array, f) -> {
            ObjectNode n = array.addObject();
            n.put("severity", String.valueOf(f.getSeverity()));
            n.put("category", f.getCategory());
            UntrustedText.put(n, "description", f.getDescription());
            UntrustedText.put(n, "recommendation", f.getRecommendation());
        });

        List<PortfolioCost> costs = new ArrayList<>();
        if (recommended != null) {
            costs.add(recommended.cost());
        }
        frontier.forEach(o -> costs.add(o.cost()));
        DesignPayloads.conversions(payload, costs);
        if (result.getCostComparability() != null && result.getCostComparability().incomplete()) {
            payload.put("cost_comparability", result.getCostComparability().headline());
        }

        DesignSpec pinned = result.getSpec() == null ? null : result.specWithUsedRatesPinned();
        if (pinned != null && !pinned.fingerprint().equals(result.getSpecFingerprint())) {
            payload.set("pinned_spec", SpecInput.toNode(pinned, mapper));
            payload.put("pinned_spec_fingerprint", pinned.fingerprint());
            payload.put("pinned_spec_note", "The spec with every exchange rate this run used pinned in "
                    + "commercial.exchangeRates: optimizing it again reproduces these figures exactly. It has its own "
                    + "fingerprint.");
        } else {
            payload.putNull("pinned_spec");
            payload.put("pinned_spec_note", "No currency conversion was needed: the spec already reproduces these "
                    + "figures.");
        }

        if (result.getSolveStatistics() != null) {
            payload.set("solve_statistics", mapper.valueToTree(result.getSolveStatistics()));
        }

        if (detail.atLeast(Detail.STANDARD)) {
            sensitivity(payload, result.getSensitivity(), detail);
            List<ConstraintVerdict> verdicts = result.getConstraintVerdicts() == null ? List.of()
                    : result.getConstraintVerdicts();
            List<ConstraintVerdict> shown = detail == Detail.FULL ? verdicts
                    : verdicts.stream().filter(v -> !v.satisfied()).toList();
            ObjectNode cv = payload.putObject("constraint_verdicts");
            cv.put("checked", verdicts.size());
            cv.put("failed", verdicts.stream().filter(v -> !v.satisfied()).count());
            capped(cv, "verdicts", shown, detail.cap(), (array, v) -> {
                ObjectNode n = array.addObject();
                n.put("constraint", v.constraintId());
                n.put("satisfied", v.satisfied());
                UntrustedText.put(n, "reason", v.reason());
            });
            placement(payload, result.getPlacement(), detail);
            DesignPayloads.strings(payload, "solver_notes", result.getSolverNotes(), detail.cap(), true);
            DesignPayloads.strings(payload, "data_provenance", result.getDataProvenance(), detail.cap(), true);
            if (result.getLatencyCalibration() != null) {
                payload.put("latency_calibration", result.getLatencyCalibration().describe());
            }
        }
        if (detail == Detail.FULL) {
            payload.set("markdown", UntrustedText.document(result.toMarkdown()));
        }
        ArrayNode next = payload.putArray("next_steps");
        if (recommended != null) {
            next.add("design_analyze_resilience with spec_fingerprint " + result.getSpecFingerprint()
                    + " to see what breaks when an element fails");
            next.add("design_growth_and_terms / design_estimate_tco with the same spec_fingerprint");
            next.add("design_plan_deployment with spec_fingerprint (plans the recommended design; dry-run "
                    + "validation only)");
        } else {
            next.add("Relax the constraint named in findings (NO_FEASIBLE_DESIGN) and optimize again.");
        }

        Set<PriceSource> priceSources = new TreeSet<>();
        costs.forEach(c -> priceSources.addAll(DesignPayloads.sources(c)));
        List<String> sources = new ArrayList<>();
        sources.add("Fabric metro catalogue and service profiles (live, read-only)");
        priceSources.forEach(s -> sources.add("prices: " + s.name().toLowerCase(Locale.ROOT)));
        DesignPayloads.provenance(payload, priceSources.isEmpty() ? "live" : DesignPayloads.kindOf(priceSources),
                result.getDataAsOf() == null ? null : result.getDataAsOf().toString(), result.getSpecFingerprint(),
                sources);
        return payload;
    }

    private static void option(ObjectNode rec, DesignOption option, Detail detail,
                               Map<String, MetroRecommendation> byCode) {
        rec.put("portfolio", option.portfolio().key());
        ArrayNode metros = rec.putArray("metros");
        option.portfolio().metros().forEach(id -> {
            ObjectNode m = metros.addObject();
            m.put("code", id.code());
            MetroRecommendation r = byCode.get(id.code());
            if (r != null) {
                UntrustedText.put(m, "name", r.getMetroName());
                if (r.getRegion() != null) {
                    m.put("region", r.getRegion().name());
                }
            }
        });
        rec.put("weighted_score", round1(option.weightedScore()));
        rec.put("pareto_optimal", option.paretoOptimal());
        ObjectNode objectives = rec.putObject("objectives");
        for (Map.Entry<String, ObjectiveValue> e : option.values().entrySet()) {
            ObjectNode v = objectives.putObject(e.getKey());
            ObjectiveValue value = e.getValue();
            if (value.known()) {
                v.put("value", round1(value.value()));
            } else {
                v.putNull("value");
            }
            Double score = option.scores().get(e.getKey());
            if (score != null) {
                v.put("score", round1(score));
            }
            UntrustedText.put(v, "explanation", value.explanation());
        }
        DesignPayloads.cost(rec, "cost", option.cost(), detail.atLeast(Detail.STANDARD), detail.cap());
    }

    private static void sensitivity(ObjectNode payload, SensitivityReport report, Detail detail) {
        if (report == null) {
            return;
        }
        ObjectNode s = payload.putObject("sensitivity");
        capped(s, "weight_ranges", report.weightRanges(), detail.cap(), (array, r) -> {
            ObjectNode n = array.addObject();
            n.put("objective", r.objective());
            n.put("weight", round4(r.weight()));
            n.put("stable_from", round4(r.low()));
            n.put("stable_to", round4(r.high()));
            n.put("below_takes_over", r.belowTakeover());
            n.put("above_takes_over", r.aboveTakeover());
        });
        if (report.nearest() != null) {
            ObjectNode n = s.putObject("nearest_alternative");
            n.put("portfolio", report.nearest().portfolio());
            n.put("distance", round4(report.nearest().distance()));
            ObjectNode deltas = n.putObject("deltas");
            report.nearest().deltas().forEach((k, v) -> deltas.put(k, round4(v)));
        }
        capped(s, "binding_constraints", report.constraints(), detail.cap(), (array, b) -> {
            ObjectNode n = array.addObject();
            n.put("constraint", b.constraint());
            n.put("binding", b.binding());
            n.put("rejected_alone", b.rejectedAlone());
            n.put("alternative", b.alternative());
            UntrustedText.put(n, "explanation", b.explanation());
        });
        DesignPayloads.strings(s, "notes", report.notes(), detail.cap(), true);
    }

    private static void placement(ObjectNode payload, PlacementPlan plan, Detail detail) {
        if (plan == null) {
            return;
        }
        ObjectNode p = payload.putObject("placement");
        capped(p, "workloads", plan.placements(), detail.cap(), (array, w) -> {
            ObjectNode n = array.addObject();
            n.put("workload", w.workload());
            n.put("primary", w.primary() == null ? null : w.primary().code());
            n.put("standby", w.standby() == null ? null : w.standby().code());
            ArrayNode clouds = n.putArray("clouds");
            w.clouds().forEach(clouds::add);
            if (w.bandwidthMbps() != null) {
                n.put("bandwidth_mbps", w.bandwidthMbps());
            }
            UntrustedText.put(n, "reasoning", w.reasoning());
            ArrayNode issues = n.putArray("issues");
            w.issues().forEach(i -> UntrustedText.add(issues, i));
        });
        capped(p, "cloud_legs", plan.legs(), detail.cap(), (array, leg) -> {
            ObjectNode n = array.addObject();
            n.put("metro", leg.metro().code());
            n.put("cloud", leg.cloudId());
            n.put("sized_mbps", leg.sizedMbps());
        });
    }

    // design_analyze_resilience

    private static ToolRegistration analyzeResilience() {
        return ToolRegistration.builder()
                .name("design_analyze_resilience")
                .title("Simulate failures of a design (what breaks if X fails)")
                .description("Simulates the failure of every element of a design (each metro, cloud on-ramp, "
                        + "connection and Cloud Router, plus shared-fate events: a cloud region, an Equinix region) "
                        + "and bounded pairs of failures, and returns: the N-1 verdict (single element failures; "
                        + "shared-fate events are reported, not counted) and N-2 verdict, single points of failure, "
                        + "the blast-radius ranking (share of service lost, site weight affected, capacity "
                        + "shortfall in Mbps), correlated risks, remediations (each re-simulated where possible), "
                        + "estimated availability per workload under stated planning assumptions (not SLAs), and "
                        + "the design graph as Mermaid text. The design is 'portfolio' when given; else the stored "
                        + "design_optimize recommendation for the spec; else the optimizer runs first (and its result "
                        + "is stored). Pass 'failures' (scenario ids, or element names such as a metro code or cloud "
                        + "id) to get the full outcome of those scenarios: which workloads go down or fail over and "
                        + "the ROUND-TRIP latency of the surviving paths. READ-ONLY; reads the live Fabric catalogue. "
                        + "Cancellable.")
                .inputSchema(object(props(
                                SpecInput.ARG, objectOrString(SpecTools.SPEC_ARG_DESCRIPTION),
                                SpecInput.FINGERPRINT_ARG, fingerprintSchema(),
                                "portfolio", portfolioSchema("analyse"),
                                "failures", with(array("Scenarios to explain in full: a scenario id from the "
                                        + "result's scenarios list, or text matched against scenario descriptions and "
                                        + "failed elements (e.g. 'LD', 'aws-use1').", string("Scenario id or "
                                        + "element.")), "maxItems", 10),
                                "max_pair_scenarios", with(integer("Most failure pairs simulated for N-2 (a "
                                        + "deterministic subset above it; the result says whether N-2 was "
                                        + "exhaustive). Default 2000."), "minimum", 0, "maximum", 20000),
                                "detail", detail("summary", "standard adds every single-failure scenario id, "
                                        + "availability formulas, assumptions and notes."))))
                .outputSchema(DesignPayloads.outputSchema("portfolio and its source, n1/n2 verdicts, "
                        + "single_points_of_failure, blast_radius, correlated_risks, remediations, availability, "
                        + "graph_mermaid, scenario details for 'failures', spec_fingerprint, provenance.",
                        "portfolio", "n1", "spec_fingerprint"))
                .toolset(Toolset.DESIGN)
                .handler((ToolHandler.Cancellable) DesignToolFactory::handleResilience)
                .build();
    }

    private static ObjectNode handleResilience(JsonNode args, ServerContext ctx, CancellationSignal cancellation)
            throws Exception {
        SpecAndRun input = specOf(args, ctx);
        DesignSpec spec = input.spec();
        Detail detail = Detail.of(args, Detail.SUMMARY);
        PricingBudget budget = ctx.pricingBudget();
        List<TimeoutRateCard> guards = new ArrayList<>();
        ResilienceOptions options = ResilienceOptions.defaults();
        Optional<Integer> pairs = optInt(args, "max_pair_scenarios");
        if (pairs.isPresent()) {
            if (pairs.get() < 0) {
                throw new IllegalArgumentException("'max_pair_scenarios' must not be negative.");
            }
            options = options.withMaxPairScenarios(pairs.get());
        }
        ResilienceAnalyzer analyzer = ResilienceAnalyzer.forSpec(ctx.fabric(), spec).options(options);
        Optional<MetroPortfolio> given = portfolio(args);
        List<String> evicted = List.of();
        String optimizationSource = null;
        if (given.isPresent()) {
            analyzer.portfolio(given.get());
        } else {
            OptimizationResult result = input.optimized();
            optimizationSource = "stored design_optimize result";
            if (result == null) {
                Optimized run = optimize(spec, ctx, budget, guards, cancellation);
                result = run.result();
                evicted = run.evicted();
                optimizationSource = "design_optimize run by this call (now stored)";
            }
            if (result.getRecommended() == null) {
                throw new IllegalArgumentException("The optimizer found no feasible design for this spec, so there "
                        + "is nothing to analyse. Relax the constraints (see design_optimize findings) or give "
                        + "'portfolio'.");
            }
            analyzer.optimization(result);
        }
        ResilienceReport report = EngineRunner.run("the resilience simulation", cancellation, analyzer::run);

        ObjectNode payload = resiliencePayload(report, detail, Args.stringList(args, "failures"), ctx.objectMapper());
        if (optimizationSource != null) {
            payload.put("optimization_source", optimizationSource);
        }
        reportEvicted(payload, evicted, DesignStore.Kind.OPTIMIZATION);
        TimeoutRateCard.reportEquinix(payload, guards, budget);
        return payload;
    }

    static ObjectNode resiliencePayload(ResilienceReport report, Detail detail, List<String> failures,
                                        ObjectMapper mapper) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("spec_fingerprint", report.specFingerprint());
        payload.put("portfolio", report.portfolio());
        payload.put("portfolio_source", report.portfolioSource());
        payload.set("summary", UntrustedText.document(report.toSummary()));
        payload.set("n1", mapper.valueToTree(report.n1()));
        payload.set("n2", mapper.valueToTree(report.n2()));
        if (report.pairs() != null) {
            ObjectNode p = payload.putObject("pairs");
            p.put("total", report.pairs().total());
            p.put("evaluated", report.pairs().evaluated());
            p.put("exhaustive", report.pairs().exhaustive());
        }
        capped(payload, "single_points_of_failure", report.singlePointsOfFailure(), detail.cap(), (array, s) -> {
            ObjectNode n = array.addObject();
            n.put("scenario_id", s.scenarioId());
            n.put("kind", s.kind().name());
            UntrustedText.put(n, "fails", s.description());
            UntrustedText.put(n, "effect", s.effect());
        });
        capped(payload, "blast_radius", report.blastRadius(), detail.cap(), (array, r) -> ranked(array.addObject(), r));
        capped(payload, "correlated_risks", report.correlatedRisks(), detail.cap(), (array, r) -> {
            ObjectNode n = array.addObject();
            n.put("severity", r.severity().name());
            n.put("shared_domain", r.sharedDomain());
            ArrayNode el = n.putArray("elements");
            r.elements().forEach(el::add);
            n.put("scenario_id", r.scenarioId());
            UntrustedText.put(n, "description", r.description());
        });
        capped(payload, "remediations", report.remediations(), detail.cap(), (array, r) -> {
            ObjectNode n = array.addObject();
            UntrustedText.put(n, "action", r.action());
            ArrayNode addresses = n.putArray("addresses");
            r.addresses().forEach(addresses::add);
            UntrustedText.put(n, "rationale", r.rationale());
            n.put("verified_by_simulation", r.verified());
        });
        capped(payload, "availability", report.availability(), detail.cap(), (array, a) -> availability(array, a,
                detail));
        if (report.availabilityAssumptions() != null) {
            payload.put("availability_basis", "Planning assumptions, not SLAs: "
                    + report.availabilityAssumptions().source());
        }
        if (report.graph() != null) {
            payload.put("graph_mermaid", report.graph().toMermaid());
        }
        if (!failures.isEmpty()) {
            ArrayNode details = payload.putArray("failure_details");
            List<String> unmatched = new ArrayList<>();
            for (String wanted : failures) {
                List<ScenarioOutcome> matches = match(report, wanted);
                if (matches.isEmpty()) {
                    unmatched.add(wanted);
                }
                matches.stream().limit(3).forEach(o -> details.add(scenario(o, mapper)));
            }
            if (!unmatched.isEmpty()) {
                ArrayNode u = payload.putArray("failures_unmatched");
                unmatched.forEach(u::add);
                payload.put("failures_hint", "Use a scenario id from 'scenarios' (detail=standard) or an element "
                        + "named in the graph: a metro code, a cloud id, a connection or router id.");
            }
        }
        if (detail.atLeast(Detail.STANDARD)) {
            capped(payload, "scenarios", report.singleFailures(), detail == Detail.FULL ? 200 : 50, (array, o) -> {
                ObjectNode n = array.addObject();
                n.put("id", o.scenario().id());
                n.put("kind", o.scenario().kind().name());
                UntrustedText.put(n, "fails", o.scenario().description());
                n.put("verdict", o.verdict().name());
                n.put("impact", round4(o.impact()));
            });
            DesignPayloads.strings(payload, "assumptions", report.assumptions(), detail.cap(), true);
            DesignPayloads.strings(payload, "notes", report.notes(), detail.cap(), true);
        }
        if (detail == Detail.FULL) {
            payload.set("markdown", UntrustedText.document(report.toMarkdown()));
        }
        DesignPayloads.provenance(payload, "computed",
                report.dataAsOf() == null ? null : report.dataAsOf().toString(), report.specFingerprint(),
                List.of("Fabric metro catalogue and measured round-trip latency (live, read-only)",
                        "failure simulation over the design graph (this server)",
                        "availability: planning assumptions, not SLAs"));
        return payload;
    }

    private static void ranked(ObjectNode n, ResilienceFindings.RankedScenario r) {
        n.put("rank", r.rank());
        n.put("scenario_id", r.scenarioId());
        n.put("kind", r.kind().name());
        UntrustedText.put(n, "fails", r.description());
        n.put("verdict", r.verdict().name());
        n.put("service_lost", round4(r.impact()));
        n.put("site_weight_affected", round4(r.siteWeightAffected()));
        n.put("shortfall_mbps", round1(r.shortfallMbps()));
        ArrayNode down = n.putArray("workloads_down");
        r.workloadsDown().forEach(down::add);
    }

    private static void availability(ArrayNode array, WorkloadAvailability a, Detail detail) {
        ObjectNode n = array.addObject();
        n.put("workload", a.workload());
        n.put("percent", round4(a.percent()));
        n.put("downtime_minutes_per_year", round1(a.downtimeMinutesPerYear()));
        if (detail.atLeast(Detail.STANDARD)) {
            n.put("formula", a.formula());
        }
    }

    private static List<ScenarioOutcome> match(ResilienceReport report, String wanted) {
        String w = wanted.trim().toLowerCase(Locale.ROOT);
        Set<ScenarioOutcome> found = new LinkedHashSet<>();
        for (ScenarioOutcome o : report.singleFailures()) {
            if (o.scenario().id().equalsIgnoreCase(wanted.trim())) {
                return List.of(o);
            }
        }
        for (ScenarioOutcome o : report.singleFailures()) {
            boolean hit = o.scenario().description().toLowerCase(Locale.ROOT).contains(w)
                    || o.scenario().failedNodes().stream().anyMatch(n -> n.toLowerCase(Locale.ROOT).contains(w))
                    || o.scenario().failedEdges().stream().anyMatch(n -> n.toLowerCase(Locale.ROOT).contains(w));
            if (hit) {
                found.add(o);
            }
        }
        return new ArrayList<>(found);
    }

    private static ObjectNode scenario(ScenarioOutcome o, ObjectMapper mapper) {
        ObjectNode n = mapper.createObjectNode();
        n.put("scenario_id", o.scenario().id());
        n.put("kind", o.scenario().kind().name());
        UntrustedText.put(n, "fails", o.scenario().description());
        n.put("verdict", o.verdict().name());
        n.put("service_lost", round4(o.impact()));
        n.put("relative_survival", round4(o.relativeSurvival()));
        n.put("site_weight_affected", round4(o.siteWeightAffected()));
        n.put("site_weight_disconnected", round4(o.siteWeightDisconnected()));
        n.put("demand_mbps", round1(o.demandMbps()));
        n.put("served_mbps", round1(o.servedMbps()));
        n.put("shortfall_mbps", round1(o.shortfallMbps()));
        ArrayNode down = n.putArray("workloads_down");
        o.workloadsDown().forEach(down::add);
        ArrayNode clouds = n.putArray("required_clouds_lost");
        o.requiredCloudsAffected().forEach(clouds::add);
        ArrayNode breaks = n.putArray("latency_limits_broken");
        o.constraintBreaks().forEach(b -> UntrustedText.add(breaks, b));
        ArrayNode zones = n.putArray("geo_zone_violations");
        o.zoneViolations().forEach(z -> UntrustedText.add(zones, z));
        ArrayNode workloads = n.putArray("workloads");
        o.workloads().forEach(w -> {
            ObjectNode wn = workloads.addObject();
            wn.put("workload", w.workload());
            wn.put("status", w.status().name());
            wn.put("active_metro", w.activeMetro());
            wn.put("failed_over", w.failedOver());
            wn.put("service_index", round4(w.serviceIndex()));
            if (w.worstSiteLatencyMs() != null) {
                wn.put("worst_site_round_trip_ms", round1(w.worstSiteLatencyMs()));
            } else {
                wn.putNull("worst_site_round_trip_ms");
            }
            ObjectNode sitePaths = wn.putObject("site_paths");
            w.sitePaths().forEach((k, v) -> sitePaths.set(k, UntrustedText.node(v)));
            ObjectNode cloudPaths = wn.putObject("cloud_paths");
            w.cloudPaths().forEach((k, v) -> cloudPaths.set(k, UntrustedText.node(v)));
            ArrayNode issues = wn.putArray("issues");
            w.issues().forEach(i -> UntrustedText.add(issues, i));
        });
        return n;
    }

    // design_estimate_latency

    private static ToolRegistration estimateLatency() {
        return ToolRegistration.builder()
                .name("design_estimate_latency")
                .title("Round-trip latency between two Equinix metros or IBXs")
                .description("Returns the ROUND-TRIP latency in ms between two endpoints (metro codes such as 'DC', "
                        + "'LD', or IBX codes such as 'DC11'): measured_round_trip_ms is Fabric's published "
                        + "metro-to-metro average (connectedMetros.avgLatency, live) when the pair has one, else null; "
                        + "floor_round_trip_ms is the straight-fibre physics floor (great-circle distance at the speed "
                        + "of light in glass, refractive index 1.467) that no path beats; estimated_round_trip_ms is "
                        + "the measured figure, or the floor times the route factor (1.75, the default; a live "
                        + "calibration on 2026-11-11 fitted a median of 1.73 over 2,140 measured pairs) with "
                        + "basis='estimated'. READ-ONLY; reads the live metro catalogue.")
                .inputSchema(object(props(
                                "from", string("Origin: Equinix metro code (e.g. 'DC') or IBX code (e.g. 'DC11')."),
                                "to", string("Destination: metro code or IBX code.")),
                        "from", "to"))
                .outputSchema(DesignPayloads.outputSchema("Endpoints, distance_km, measured/floor/estimated "
                        + "round-trip ms, basis, caveats, provenance.", "estimated_round_trip_ms", "basis"))
                .toolset(Toolset.DESIGN)
                .handler(DesignToolFactory::handleEstimateLatency)
                .build();
    }

    private static ObjectNode handleEstimateLatency(JsonNode args, ServerContext ctx) {
        String fromCode = requireString(args, "from");
        String toCode = requireString(args, "to");
        MetroRegistry registry = ctx.metroRegistry();
        Endpoint from = resolveEndpoint(registry, fromCode, "from");
        Endpoint to = resolveEndpoint(registry, toCode, "to");

        SpeedOfLightLatency floor = SpeedOfLightLatency.roundTrip();
        double km;
        double floorMs;
        if (from.metro != null && to.metro != null) {
            km = SpeedOfLightLatency.distanceKm(from.metro, to.metro);
            floorMs = floor.millisBetween(from.metro, to.metro);
        } else if (from.ibx != null && to.ibx != null) {
            km = SpeedOfLightLatency.distanceKm(from.ibx, to.ibx);
            floorMs = floor.millisBetween(from.ibx, to.ibx);
        } else if (from.ibx != null) {
            km = SpeedOfLightLatency.distanceKm(from.ibx, to.metro);
            floorMs = floor.millisBetween(from.ibx, to.metro);
        } else {
            km = SpeedOfLightLatency.distanceKm(from.metro, to.ibx);
            floorMs = floor.millisBetween(from.metro, to.ibx);
        }
        Metro fromMetro = from.metro != null ? from.metro : registry.get(from.ibx.getMetroCode()).orElse(null);
        Metro toMetro = to.metro != null ? to.metro : registry.get(to.ibx.getMetroCode()).orElse(null);
        Double measured = measured(fromMetro, toMetro);

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.set("from", from.describe(ctx.objectMapper()));
        payload.set("to", to.describe(ctx.objectMapper()));
        payload.put("distance_km", round1(km));
        payload.put("floor_round_trip_ms", Math.round(floorMs * 100.0) / 100.0);
        if (measured != null) {
            payload.put("measured_round_trip_ms", measured);
            payload.put("estimated_round_trip_ms", measured);
            payload.put("basis", "measured");
            payload.put("route_factor_observed", Math.round(measured / floorMs * 100.0) / 100.0);
        } else {
            Region a = fromMetro == null ? null : fromMetro.getRegion();
            Region b = toMetro == null ? null : toMetro.getRegion();
            double estimate = RouteFactorCalibration.uncalibrated().estimateRoundTripMs(km, a, b);
            payload.putNull("measured_round_trip_ms");
            payload.put("estimated_round_trip_ms", Math.round(estimate * 100.0) / 100.0);
            payload.put("basis", "estimated");
            payload.put("route_factor", RouteFactorCalibration.DEFAULT_FACTOR);
        }
        ArrayNode caveats = payload.putArray("caveats");
        caveats.add("All figures are ROUND-TRIP milliseconds.");
        caveats.add("Measured figures are Fabric's metro-to-metro averages between metros, not between your "
                + "devices; IBX endpoints use their metro's figure.");
        caveats.add("The floor is a physics bound: real routes are longer (measured routes run about 1.5-2x it).");
        DesignPayloads.provenance(payload, measured != null ? "live" : "estimate", null, null,
                List.of("Fabric /fabric/v4/metros connectedMetros.avgLatency (live)", "great-circle geometry"));
        return payload;
    }

    private static Double measured(Metro from, Metro to) {
        if (from == null || to == null || from.getConnectedMetros() == null) {
            return null;
        }
        String target = to.metroId().code();
        for (ConnectedMetro c : from.getConnectedMetros()) {
            if (c != null && target.equalsIgnoreCase(c.getCodeValue()) && c.getAvgLatency() != null
                    && c.getAvgLatency() > 0) {
                return c.getAvgLatency();
            }
        }
        return null;
    }

    private static Endpoint resolveEndpoint(MetroRegistry registry, String code, String field) {
        Optional<Metro> metro = registry.get(code.toUpperCase(Locale.ROOT));
        if (metro.isPresent()) {
            return new Endpoint(metro.get(), null);
        }
        Optional<Ibx> ibx = registry.ibx(code.toUpperCase(Locale.ROOT));
        if (ibx.isPresent()) {
            return new Endpoint(null, ibx.get());
        }
        throw new IllegalArgumentException("'" + field + "' value '" + code + "' is neither a known metro code "
                + "nor a known IBX code. Metro codes are two letters (e.g. 'DC'); IBX codes are metro code + "
                + "number (e.g. 'DC11'). If IBX codes keep failing, use metro codes.");
    }

    private record Endpoint(Metro metro, Ibx ibx) {

        ObjectNode describe(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            if (metro != null) {
                node.put("kind", "metro");
                node.put("code", metro.metroId().code());
                UntrustedText.put(node, "name", metro.getName());
            } else {
                node.put("kind", "ibx");
                node.put("code", ibx.getIbxCode());
                node.put("metro", ibx.getMetroCode());
            }
            return node;
        }
    }

    // design_analyze_peering

    private static ToolRegistration analyzePeering() {
        return ToolRegistration.builder()
                .name("design_analyze_peering")
                .title("Peering intelligence for a DesignSpec's peers")
                .description("Runs Peering Intelligence for the spec's peers (peers[].asn): where each network is "
                        + "present (PeeringDB and the Equinix IX footprint), peering opportunities from customer_asn "
                        + "(empty without it), and a resiliency assessment across the spec's metros (sites[].metro "
                        + "plus constraints.requiredMetros; absent when there are none). The spec must declare at "
                        + "least one peer. Uses EQUINIX_PEERINGDB_KEY when configured; anonymous PeeringDB access is "
                        + "heavily rate-limited, so call this sparingly. READ-ONLY. Cancellable.")
                .inputSchema(object(props(
                                SpecInput.ARG, objectOrString(SpecTools.SPEC_ARG_DESCRIPTION),
                                SpecInput.FINGERPRINT_ARG, fingerprintSchema(),
                                "customer_asn", with(integer("Your own ASN: peering opportunities are computed from "
                                        + "your side."), "minimum", 1, "maximum", 4_294_967_295L),
                                "detail", detail("summary", "standard and full raise the list caps."))))
                .outputSchema(DesignPayloads.outputSchema("networks, peering_opportunities, resiliency, data_sources, "
                        + "spec_fingerprint, provenance.", "networks", "spec_fingerprint"))
                .toolset(Toolset.DESIGN)
                .handler((ToolHandler.Cancellable) DesignToolFactory::handleAnalyzePeering)
                .build();
    }

    private static ObjectNode handleAnalyzePeering(JsonNode args, ServerContext ctx, CancellationSignal cancellation)
            throws Exception {
        DesignSpec spec = specOf(args, ctx).spec();
        if (spec.peers().isEmpty()) {
            throw new IllegalArgumentException("The spec declares no peers: add peers: [{asn: 13335, label: "
                    + "Cloudflare}] (one entry per network to analyse).");
        }
        Detail detail = Detail.of(args, Detail.SUMMARY);
        PeeringIntelligence.Builder builder = PeeringIntelligence.forSpec(ctx.fabric(), spec,
                ctx.env(ServerContext.ENV_PEERINGDB_KEY).orElse(null));
        optLong(args, "customer_asn").ifPresent(builder::customerAsn);
        PeeringIntelligenceResult result = EngineRunner.run("the peering analysis", cancellation, builder::analyze);

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("spec_fingerprint", result.getSpecFingerprint() != null ? result.getSpecFingerprint()
                : spec.fingerprint());
        List<com.eqixiac.equinix.design.peering.model.NetworkPresence> networks = result.getNetworkPresences() == null
                ? List.of() : new ArrayList<>(result.getNetworkPresences().values());
        capped(payload, "networks", networks, detail.cap(), (array, np) -> {
            ObjectNode n = array.addObject();
            n.put("asn", np.getAsn());
            UntrustedText.put(n, "label", np.getLabel());
            UntrustedText.put(n, "peeringdb_name", np.getPeeringDbName());
            n.put("peering_policy", String.valueOf(np.getPeeringPolicy()));
            n.put("network_type", String.valueOf(np.getNetworkType()));
            UntrustedText.put(n, "traffic_volume", np.getTrafficVolume());
            UntrustedText.put(n, "traffic_ratio", np.getTrafficRatio());
            n.put("route_server_participant", np.isRouteServerParticipant());
            n.put("ipv6_capable", np.isIpv6Capable());
        });
        List<com.eqixiac.equinix.design.peering.model.PeeringOpportunity> opportunities =
                result.getPeeringOpportunities() == null ? List.of() : result.getPeeringOpportunities();
        capped(payload, "peering_opportunities", opportunities, detail == Detail.SUMMARY ? 10 : detail.cap(),
                (array, op) -> {
                    ObjectNode o = array.addObject();
                    o.put("customer_asn", op.getCustomerAsn());
                    o.put("target_asn", op.getTargetAsn());
                    UntrustedText.put(o, "target_label", op.getTargetLabel());
                    o.put("metro", String.valueOf(op.getMetro()));
                    UntrustedText.put(o, "ix_name", op.getIxName());
                    o.put("target_policy", String.valueOf(op.getTargetPolicy()));
                    o.put("target_uses_route_server", op.isTargetUsesRouteServer());
                });
        if (result.getResiliency() != null) {
            ObjectNode resiliency = payload.putObject("resiliency");
            resiliency.put("overall_score", round1(result.getResiliency().getOverallScore()));
            resiliency.put("overall_rating", result.getResiliency().getOverallRating());
            DesignPayloads.strings(resiliency, "findings", result.getResiliency().getFindings(), detail.cap(), true);
        }
        DesignPayloads.strings(payload, "warnings", result.warnings(), detail.cap(), true);
        List<String> sources = result.getDataSources() == null ? List.of() : result.getDataSources();
        DesignPayloads.strings(payload, "data_sources", sources, 20, false);
        DesignPayloads.provenance(payload, "live", result.getComputedAt() == null ? null
                        : result.getComputedAt().toString(), payload.get("spec_fingerprint").asText(),
                List.of("PeeringDB (live)", "Equinix IX footprint and Fabric service profiles (live, read-only)"));
        return payload;
    }

    // shared helpers

    /**
     * The Fabric gateway when this context can supply one, else empty (a test context that injects only a
     * design facade).
     *
     * @param ctx the context
     * @return the gateway, or empty
     */
    static Optional<FabricGateway> fabricIfAvailable(ServerContext ctx) {
        try {
            return Optional.ofNullable(ctx.fabric());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    static CloudProviderType cloudProvider(String raw, String field) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("gcp") || normalized.equals("google")) {
            return CloudProviderType.GOOGLE_CLOUD;
        }
        if (normalized.equals("oci") || normalized.equals("oracle")) {
            return CloudProviderType.ORACLE_CLOUD;
        }
        return enumValue(CloudProviderType.class, raw, field);
    }

    static MetroCode metroCode(String code) {
        try {
            return MetroCode.fromCode(code);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("'" + code + "' is not a known Equinix metro code. Use the "
                    + "two-letter code, e.g. 'DC' (Ashburn), 'LD' (London), 'SG' (Singapore).");
        }
    }
}
