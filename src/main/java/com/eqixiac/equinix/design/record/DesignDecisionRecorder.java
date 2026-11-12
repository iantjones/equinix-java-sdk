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


package com.eqixiac.equinix.design.record;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.design.cost.GrowthPlan;
import com.eqixiac.equinix.design.cost.GrowthPlanner;
import com.eqixiac.equinix.design.estate.EstateFinding;
import com.eqixiac.equinix.design.estate.EstateReport;
import com.eqixiac.equinix.design.optimizer.MetroOptimizer;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.context.FabricCatalogLoader;
import com.eqixiac.equinix.design.optimizer.model.LatencyEntry;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.RiskFinding;
import com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.peering.model.PeeringIntelligenceResult;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.plan.PlanStep;
import com.eqixiac.equinix.design.plan.PlanSteps;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Alternative;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Alternatives;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Assumption;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Availability;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.ConstraintEffect;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Context;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Cost;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.CostItem;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Decision;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.FxRate;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Growth;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.GrowthItem;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Implementation;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.NearestOption;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.ObjectiveResult;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.PlanSummary;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Provenance;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Reproduction;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Risk;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.RiskLevel;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Risks;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Savings;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Sensitivity;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Tco;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Value;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Verdict;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.WeightRange;
import com.eqixiac.equinix.design.resilience.ResilienceAnalyzer;
import com.eqixiac.equinix.design.resilience.ResilienceFindings;
import com.eqixiac.equinix.design.resilience.ResilienceOptions;
import com.eqixiac.equinix.design.resilience.ResilienceReport;
import com.eqixiac.equinix.design.resilience.WorkloadAvailability;
import com.eqixiac.equinix.design.solve.SensitivityReport;
import com.eqixiac.equinix.design.solve.WeightProfiles;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.PeerIntent;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.SpecIssue;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.CurrencyConverter;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.design.value.tco.TcoCalculator;
import com.eqixiac.equinix.design.value.tco.TcoComparison;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Produces a {@link DesignDecisionRecord}: runs the engines for a spec (Metro Optimizer, then resilience analysis,
 * then the growth and value models where the spec gives enough input, then a deployment plan) and records their
 * results section by section.
 *
 * <pre>{@code
 * DesignDecisionRecord adr = DesignDecisionRecorder.forSpec(fabric, spec)
 *     .optimization(alreadyComputed)          // optional: reuse a result for this spec
 *     .record();
 * }</pre>
 *
 * <p>Read-only: it never creates, changes or deletes anything. With {@link #forSpec} the catalogue is read once
 * and shared by every engine, and one rate card ({@code RateCard.standardChain(fabric)} unless
 * {@link #rateCard(RateCard)} is given) prices the optimisation, the term view, the value view and the plan, so a
 * price is looked up once. {@link #forContext} runs over facts already held, with no network unless the rate card
 * reaches it.</p>
 *
 * <p>A supplied result computed for another spec (a different {@code fingerprint()}) is rejected. A section whose
 * engine fails, or whose inputs are absent, is recorded as failed or skipped with the reason; the record is always
 * produced.</p>
 */
public final class DesignDecisionRecorder {

    /** The status a new record carries unless {@link #status(String)} says otherwise. */
    public static final String DEFAULT_STATUS = "Proposed";

    private final FabricGateway fabric;
    private final DesignSpec spec;
    private CatalogDesignContext context;
    private RateCard rateCard;
    private RateCard resolvedCard;
    private CurrencyConverter currencyConverter;
    private CurrencyConverter defaultRates;
    private OptimizationResult optimization;
    private ResilienceReport resilience;
    private DeploymentPlan plan;
    private EstateReport estate;
    private PeeringIntelligenceResult peering;
    private ResilienceOptions resilienceOptions = ResilienceOptions.defaults();
    private boolean buildPlan = true;
    private Clock clock = Clock.systemUTC();
    private String status = DEFAULT_STATUS;
    private String title;
    private String sdkVersion;

    private DesignDecisionRecorder(FabricGateway fabric, CatalogDesignContext context, DesignSpec spec) {
        this.fabric = fabric;
        this.context = context;
        this.spec = spec;
    }

    /**
     * Records a spec against live Fabric data (read-only: catalogue reads and price searches).
     *
     * @param fabric the Fabric API
     * @param spec   the design
     * @return a recorder to configure and {@link #record()}
     */
    public static DesignDecisionRecorder forSpec(FabricGateway fabric, DesignSpec spec) {
        if (fabric == null || spec == null) {
            throw new IllegalArgumentException("forSpec needs a Fabric gateway and a spec");
        }
        return new DesignDecisionRecorder(fabric, null, spec);
    }

    /**
     * Records over facts already gathered (offline what-ifs, tests, a cached catalogue). Prices come from the
     * context's cost model, or from {@link #rateCard(RateCard)}; the term, value and plan views need a rate card.
     *
     * @param context the facts; its spec is the design
     * @return a recorder to configure and {@link #record()}
     */
    public static DesignDecisionRecorder forContext(CatalogDesignContext context) {
        if (context == null) {
            throw new IllegalArgumentException("forContext needs a context");
        }
        return new DesignDecisionRecorder(null, context, context.spec());
    }

    /**
     * @param card the rate card every engine prices with; default {@code RateCard.standardChain(fabric)} for
     *             {@link #forSpec}, none for {@link #forContext} (the context's cost model prices the optimisation)
     * @return this recorder
     */
    public DesignDecisionRecorder rateCard(RateCard card) {
        this.rateCard = card;
        this.resolvedCard = null;
        return this;
    }

    /**
     * @param converter exchange rates ranked below rates pinned in the spec and above the default sources
     * @return this recorder
     */
    public DesignDecisionRecorder currencyConverter(CurrencyConverter converter) {
        this.currencyConverter = converter;
        return this;
    }

    /**
     * @param defaults replaces the default rate sources (live ECB rates, then the bundled table), for example
     *                 {@code CurrencyConverters.bundledOnly()} offline
     * @return this recorder
     */
    public DesignDecisionRecorder defaultRates(CurrencyConverter defaults) {
        this.defaultRates = defaults;
        return this;
    }

    /**
     * Uses an optimisation already computed for this spec instead of running the optimiser.
     *
     * @param result the result
     * @return this recorder
     * @throws IllegalArgumentException when the result was computed for another spec
     */
    public DesignDecisionRecorder optimization(OptimizationResult result) {
        this.optimization = requireSameSpec("optimisation result", result == null ? null : result.getSpecFingerprint(),
                result, false);
        return this;
    }

    /**
     * Uses a resilience analysis already computed for this spec.
     *
     * @param report the report
     * @return this recorder
     * @throws IllegalArgumentException when the report was computed for another spec
     */
    public DesignDecisionRecorder resilience(ResilienceReport report) {
        this.resilience = requireSameSpec("resilience report", report == null ? null : report.specFingerprint(),
                report, false);
        return this;
    }

    /**
     * Uses a deployment plan instead of building one. A plan that carries no spec fingerprint is accepted, and the
     * record says so.
     *
     * @param deploymentPlan the plan
     * @return this recorder
     * @throws IllegalArgumentException when the plan was built for another spec
     */
    public DesignDecisionRecorder plan(DeploymentPlan deploymentPlan) {
        this.plan = requireSameSpec("deployment plan", deploymentPlan == null ? null
                : deploymentPlan.getSpecFingerprint(), deploymentPlan, true);
        return this;
    }

    /**
     * Adds an analysis of the existing estate: its findings join the record's risks and its headline joins the
     * context. An estate report is not derived from a spec, so it carries no fingerprint to check.
     *
     * @param report the estate report
     * @return this recorder
     */
    public DesignDecisionRecorder estate(EstateReport report) {
        this.estate = report;
        return this;
    }

    /**
     * Adds a peering analysis: its presence is drawn into the resilience graph and its summary joins the context.
     * A result that carries no spec fingerprint is accepted, and the record says so.
     *
     * @param result the peering analysis
     * @return this recorder
     * @throws IllegalArgumentException when the analysis was run for another spec
     */
    public DesignDecisionRecorder peering(PeeringIntelligenceResult result) {
        this.peering = requireSameSpec("peering analysis", result == null ? null : result.getSpecFingerprint(), result,
                true);
        return this;
    }

    /**
     * @param options graph and simulation options for the resilience analysis
     * @return this recorder
     */
    public DesignDecisionRecorder resilienceOptions(ResilienceOptions options) {
        this.resilienceOptions = options == null ? ResilienceOptions.defaults() : options;
        return this;
    }

    /**
     * @param build whether to build a deployment plan from the recommendation when none is supplied (default
     *              {@code true}; the plan is built offline, without deployment inputs or live validation)
     * @return this recorder
     */
    public DesignDecisionRecorder buildPlan(boolean build) {
        this.buildPlan = build;
        return this;
    }

    /**
     * @param recordStatus the decision status written in the record (default {@value #DEFAULT_STATUS})
     * @return this recorder
     */
    public DesignDecisionRecorder status(String recordStatus) {
        this.status = recordStatus == null || recordStatus.isBlank() ? DEFAULT_STATUS : recordStatus;
        return this;
    }

    /**
     * @param recordTitle the record title (default {@code Metro design for <spec name>})
     * @return this recorder
     */
    public DesignDecisionRecorder title(String recordTitle) {
        this.title = recordTitle;
        return this;
    }

    /**
     * @param recordClock the clock for {@code generatedAt} (fix it for reproducible documents)
     * @return this recorder
     */
    public DesignDecisionRecorder clock(Clock recordClock) {
        this.clock = recordClock == null ? Clock.systemUTC() : recordClock;
        return this;
    }

    /**
     * Runs the engines and assembles the record.
     *
     * @return the record; never {@code null}
     * @throws IllegalArgumentException when the spec has errors ({@link DesignSpec#validate()})
     */
    public DesignDecisionRecord record() {
        spec.requireValid();
        Instant generatedAt = clock.instant();
        String version = sdkVersion();
        List<String> failures = new ArrayList<>();
        Map<String, Double> weights = WeightProfiles.resolve(spec.objectives());

        RecordSection<Context> contextSection = section("Context", failures, this::requirements);

        OptimizationResult opt = optimization;
        String optFailure = null;
        if (opt == null) {
            try {
                opt = runOptimizer();
            } catch (RuntimeException e) {
                optFailure = "the Metro Optimizer failed: " + describe(e);
                failures.add("Decision: " + optFailure);
            }
        }
        final OptimizationResult result = opt;
        final String noResult = optFailure;
        RecordSection<Decision> decision = result == null ? RecordSection.failed(noResult)
                : section("Decision", failures, () -> decision(result, weights));
        RecordSection<DesignDecisionRecord.Alternatives> alternatives = result == null
                ? RecordSection.failed(noResult) : section("Alternatives", failures, () -> alternatives(result));
        RecordSection<Sensitivity> sensitivity = result == null ? RecordSection.failed(noResult)
                : section("Sensitivity", failures, () -> sensitivity(result));

        List<Assumption> assumptions = new ArrayList<>();
        RecordSection<Cost> cost = result == null ? RecordSection.failed(noResult)
                : section("Cost", failures, () -> cost(result, assumptions));

        ResilienceReport report = resilience;
        String resilienceFailure = null;
        if (report == null && result != null && result.getRecommended() != null) {
            try {
                report = ResilienceAnalyzer.forContext(catalog()).optimization(result).options(resilienceOptions)
                        .peering(peering).run();
            } catch (RuntimeException e) {
                resilienceFailure = "the resilience analysis failed: " + describe(e);
                failures.add("Resilience: " + resilienceFailure);
            }
        }
        final ResilienceReport analysed = report;
        RecordSection<DesignDecisionRecord.Resilience> resilienceSection;
        if (analysed != null) {
            resilienceSection = section("Resilience", failures, () -> resilience(analysed, result));
        } else if (resilienceFailure != null) {
            resilienceSection = RecordSection.failed(resilienceFailure);
        } else if (result == null) {
            resilienceSection = RecordSection.skipped("no recommended design to analyse: " + noResult);
        } else {
            resilienceSection = RecordSection.skipped("no recommended design to analyse: nothing is feasible");
        }

        RecordSection<Implementation> implementation = section("Implementation", failures,
                () -> implementation(result, version));

        RecordSection<Risks> risks = section("Risks", failures,
                () -> risks(result, analysed, implementation, failures));
        RecordSection<Provenance> provenance = section("Provenance", failures,
                () -> provenance(result, analysed, assumptions, generatedAt));

        return new DesignDecisionRecord(DesignDecisionRecord.SCHEMA_VERSION,
                title != null && !title.isBlank() ? title : "Metro design for " + spec.name(), status, generatedAt,
                version, spec.fingerprint(), spec, contextSection, decision, alternatives, sensitivity, cost,
                resilienceSection, risks, provenance, implementation);
    }

    // engines

    private CatalogDesignContext catalog() {
        if (context == null) {
            context = FabricCatalogLoader.load(fabric, spec, null);
        }
        return context;
    }

    /** The one card shared by every engine of this record, or {@code null} offline without a card. */
    private RateCard card() {
        if (resolvedCard == null) {
            resolvedCard = rateCard != null ? rateCard : fabric != null ? RateCard.standardChain(fabric) : null;
        }
        return resolvedCard;
    }

    private OptimizationResult runOptimizer() {
        MetroOptimizer optimizer = MetroOptimizer.forContext(catalog());
        if (card() != null) {
            optimizer.rateCard(card());
        }
        return optimizer.currencyConverter(currencyConverter).defaultRates(defaultRates).run();
    }

    // 1. context

    private Context requirements() {
        List<String> sites = new ArrayList<>();
        for (SiteIntent site : spec.sites()) {
            List<String> parts = new ArrayList<>();
            if (site.metro() != null) {
                parts.add("metro " + site.metro());
            }
            if (site.hasCoordinates()) {
                parts.add(String.format(Locale.ROOT, "%.4f, %.4f", site.latitude(), site.longitude()));
            }
            if (site.role() != null) {
                parts.add(site.role().name().toLowerCase(Locale.ROOT).replace('_', ' '));
            }
            if (site.headcount() != null) {
                parts.add(site.headcount() + " people");
            }
            if (site.weight() != null) {
                parts.add("weight " + num(site.weight()));
            }
            sites.add(site.label() + ": " + (parts.isEmpty() ? "no location" : String.join(", ", parts)));
        }
        List<String> workloads = new ArrayList<>();
        for (WorkloadIntent w : spec.workloads()) {
            List<String> parts = new ArrayList<>();
            parts.add(w.bandwidthMbps() == null ? "unsized" : w.bandwidthMbps() + " Mbps");
            if (!w.clouds().isEmpty()) {
                parts.add("reaches " + String.join(", ", w.clouds()));
            }
            if (w.maxLatencyMs() != null) {
                parts.add("round trip at most " + num(w.maxLatencyMs()) + " ms from every site");
            }
            if (w.pinnedMetro() != null) {
                parts.add("pinned to " + w.pinnedMetro());
            }
            if (w.type() != null) {
                parts.add("type " + w.type());
            }
            workloads.add(w.label() + ": " + String.join("; ", parts));
        }
        List<String> clouds = new ArrayList<>();
        for (CloudIntent c : spec.clouds()) {
            List<String> parts = new ArrayList<>();
            parts.add(c.provider() != null ? c.provider().name() : "profile " + (c.serviceProfileName() != null
                    ? c.serviceProfileName() : c.serviceProfileUuid()));
            if (c.region() != null) {
                parts.add("region " + c.region());
            }
            parts.add(Boolean.TRUE.equals(c.required()) ? "required" : "preferred");
            if (c.monthlyEgressGb() != null) {
                parts.add(String.format(Locale.ROOT, "%,.0f GB/month out", c.monthlyEgressGb()));
            }
            if (c.monthlyIngressGb() != null) {
                parts.add(String.format(Locale.ROOT, "%,.0f GB/month in", c.monthlyIngressGb()));
            }
            clouds.add(c.id() + ": " + String.join(", ", parts));
        }
        List<String> peers = new ArrayList<>();
        for (PeerIntent p : spec.peers()) {
            peers.add("AS" + p.asn() + (p.label() == null ? "" : " (" + p.label() + ")"));
        }
        List<String> background = new ArrayList<>();
        if (estate != null) {
            StringBuilder b = new StringBuilder("Existing estate (" + estate.scope().describe() + ", inventory as of "
                    + estate.inventoryAsOf() + "): " + estate.findings().size() + " finding(s)");
            estate.addressableMonthlySaving().forEach((ccy, t) -> b.append(", addressable saving ").append(ccy)
                    .append(' ').append(money(t.monthly())).append("/month"));
            background.add(b.toString());
        }
        if (peering != null) {
            background.add("Peering analysis: " + peering.toSummary()
                    + (peering.getSpecFingerprint() == null ? " (the analysis carries no spec fingerprint)" : ""));
        }
        List<String> issues = new ArrayList<>();
        for (SpecIssue issue : spec.validate()) {
            issues.add(issue.path() + ": " + issue.message());
        }
        return new Context(spec.name(), spec.description(), sites, workloads, clouds, peers, constraints(),
                spec.objectives().profile(), WeightProfiles.resolve(spec.objectives()), commercial(), issues,
                background);
    }

    private List<String> constraints() {
        DesignConstraints c = spec.constraints();
        List<String> out = new ArrayList<>();
        if (!c.requiredMetros().isEmpty()) {
            out.add("Must include metros " + String.join(", ", c.requiredMetros()));
        }
        if (!c.excludedMetros().isEmpty()) {
            out.add("Must not use metros " + String.join(", ", c.excludedMetros()));
        }
        if (!c.requiredRegions().isEmpty()) {
            out.add("Must have a metro in each of " + joinNames(c.requiredRegions()));
        }
        if (!c.excludedRegions().isEmpty()) {
            out.add("Must not use regions " + joinNames(c.excludedRegions()));
        }
        if (!c.complianceZones().isEmpty()) {
            out.add("Must stay within compliance zones " + joinNames(c.complianceZones()));
        }
        if (!c.geoZones().isEmpty()) {
            out.add("Traffic must stay inside geo zones " + String.join(", ", c.geoZones()));
        }
        if (c.minimumRedundancy() != null) {
            out.add("Redundancy at least " + c.minimumRedundancy());
        }
        if (c.maxLatencyMs() != null) {
            out.add("Round trip from every site to its serving metro at most " + num(c.maxLatencyMs()) + " ms");
        }
        if (c.minMetros() != null || c.maxMetros() != null) {
            out.add("Metros per design: " + (c.minMetros() == null ? "1" : c.minMetros()) + " to "
                    + (c.maxMetros() == null ? MetroOptimizer.DEFAULT_MAX_METROS + " (default)" : c.maxMetros()));
        }
        if (c.minSurvivalPct() != null) {
            out.add("At least " + num(c.minSurvivalPct()) + "% of service must survive the worst single failure");
        }
        if (spec.commercial().budgetMaxMonthly() != null) {
            out.add("Monthly cost at most " + spec.commercial().currencyOrDefault() + " "
                    + money(spec.commercial().budgetMaxMonthly()));
        }
        return out;
    }

    private List<String> commercial() {
        CommercialTerms t = spec.commercial();
        List<String> out = new ArrayList<>();
        out.add("Currency " + t.currencyOrDefault() + (t.currency() == null ? " (default)" : ""));
        if (t.budgetMinMonthly() != null) {
            out.add("Expected spend at least " + t.currencyOrDefault() + " " + money(t.budgetMinMonthly()) + "/month");
        }
        out.add(t.term() == null ? "Term not set: priced on demand (1 month)" : "Term " + t.term());
        out.add(t.annualGrowthRate() == null ? "Traffic growth not set (0% assumed)"
                : "Traffic growth " + num(t.annualGrowthRate() * 100) + "%/year");
        out.add("Horizon " + t.horizonMonthsOrDefault() + " months" + (t.horizonMonths() == null ? " (default)" : ""));
        if (t.exchangeRates() != null) {
            List<String> rates = new ArrayList<>();
            t.exchangeRates().rates().forEach((ccy, r) -> rates.add(ccy + " " + r.stripTrailingZeros().toPlainString()));
            out.add("Pinned exchange rates (" + t.currencyOrDefault() + " per unit): " + String.join(", ", rates)
                    + ", as of " + t.exchangeRates().asOf() + ", source " + t.exchangeRates().source());
        }
        return out;
    }

    // 2. decision

    private Decision decision(OptimizationResult result, Map<String, Double> weights) {
        DesignOption rec = result.getRecommended();
        if (rec == null) {
            throw new Skip("no design satisfies every constraint: " + String.join(" ", result.getSolverNotes()));
        }
        List<String> metros = new ArrayList<>();
        if (result.getRecommendations() != null) {
            for (MetroRecommendation m : result.getRecommendations()) {
                metros.add(m.getMetroId().code() + (m.getMetroName() == null || m.getMetroName().equals(
                        m.getMetroId().code()) ? "" : " (" + m.getMetroName() + ")"));
            }
        }
        if (metros.isEmpty()) {
            rec.portfolio().metros().forEach(m -> metros.add(m.code()));
        }
        List<ObjectiveResult> objectives = new ArrayList<>();
        for (Map.Entry<String, ObjectiveValue> e : rec.values().entrySet()) {
            ObjectiveValue v = e.getValue();
            objectives.add(new ObjectiveResult(e.getKey(), unit(result, e.getKey()), v.known() ? round(v.value()) : null,
                    rec.scores().containsKey(e.getKey()) ? round(rec.scores().get(e.getKey())) : null,
                    round(weights.getOrDefault(e.getKey(), 0.0)), scale(result, e.getKey()), v.explanation()));
        }
        List<String> placements = new ArrayList<>();
        if (result.getPlacement() != null) {
            for (PlacementPlan.Placement p : result.getPlacement().placements()) {
                StringBuilder b = new StringBuilder(p.workload()).append(": ");
                if (p.reasoning() != null && !p.reasoning().isBlank()) {
                    b.append(p.reasoning().strip());
                } else {
                    b.append(p.primary() == null ? "not placed" : "runs in " + p.primary().code());
                    if (p.standby() != null) {
                        b.append(", standby in ").append(p.standby().code());
                    }
                    if (!p.clouds().isEmpty()) {
                        b.append(", reaches ").append(String.join(", ", p.clouds()));
                    }
                }
                if (p.issues() != null && !p.issues().isEmpty()) {
                    b.append("; not honoured: ").append(String.join("; ", p.issues()));
                }
                placements.add(b.toString());
            }
        }
        List<String> constraints = new ArrayList<>();
        for (ConstraintVerdict v : rec.verdicts()) {
            constraints.add(v.constraintId() + ": " + v.reason());
        }
        return new Decision(rec.portfolio().key(), metros, round(rec.weightedScore()), rec.paretoOptimal(), objectives,
                placements, constraints, rationale(result, rec, weights));
    }

    private String rationale(OptimizationResult result, DesignOption rec, Map<String, Double> weights) {
        StringBuilder b = new StringBuilder();
        long feasible = result.getSolveStatistics() == null ? 0 : result.getSolveStatistics().feasible();
        b.append(String.format(Locale.ROOT, "%s has the highest weighted score (%.1f of 100) of the %,d feasible "
                + "design(s)", rec.portfolio().key(), rec.weightedScore(), feasible));
        if (result.getSolveStatistics() != null) {
            b.append(String.format(Locale.ROOT, " out of %,d examined", result.getSolveStatistics().evaluated()));
        }
        b.append(rec.paretoOptimal() ? "; no feasible design is at least as good on every objective."
                : "; it is not Pareto-optimal (a dominated option won only a tie).");
        List<String> top = new ArrayList<>();
        weights.entrySet().stream().filter(w -> w.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(w -> top.add(w.getKey() + " " + pct(w.getValue())));
        if (!top.isEmpty()) {
            b.append(" Weights: ").append(String.join(", ", top)).append('.');
        }
        SensitivityReport s = result.getSensitivity();
        if (s != null && s.nearest() != null) {
            DesignOption near = result.getFrontier().stream()
                    .filter(o -> o.portfolio().key().equals(s.nearest().portfolio())).findFirst().orElse(null);
            b.append(" The closest alternative is ").append(s.nearest().portfolio());
            if (near != null) {
                b.append(String.format(Locale.ROOT, " (%.1f)", near.weightedScore()));
            }
            b.append('.');
        }
        return b.toString();
    }

    // 3. alternatives

    private Alternatives alternatives(OptimizationResult result) {
        DesignOption rec = result.getRecommended();
        List<Alternative> frontier = new ArrayList<>();
        for (DesignOption o : result.getFrontier() == null ? List.<DesignOption>of() : result.getFrontier()) {
            Map<String, Double> values = new LinkedHashMap<>();
            o.values().forEach((id, v) -> {
                if (v.known()) {
                    values.put(id, round(v.value()));
                }
            });
            List<String> gains = new ArrayList<>();
            List<String> away = new ArrayList<>();
            boolean isRec = rec != null && rec.portfolio().equals(o.portfolio());
            if (rec != null && !isRec) {
                for (Map.Entry<String, ObjectiveValue> e : o.values().entrySet()) {
                    ObjectiveValue mine = e.getValue();
                    ObjectiveValue theirs = rec.values().get(e.getKey());
                    Double myScore = o.scores().get(e.getKey());
                    Double recScore = rec.scores().get(e.getKey());
                    if (mine == null || theirs == null || !mine.known() || !theirs.known() || myScore == null
                            || recScore == null || Math.abs(mine.value() - theirs.value()) < 1e-9) {
                        continue;
                    }
                    String phrase = e.getKey() + " " + value(result, e.getKey(), mine.value()) + " vs "
                            + value(result, e.getKey(), theirs.value());
                    if (myScore > recScore + 1e-9) {
                        gains.add(phrase);
                    } else if (myScore < recScore - 1e-9) {
                        away.add(phrase);
                    }
                }
            }
            PortfolioCost c = o.cost();
            frontier.add(new Alternative(o.portfolio().key(), round(o.weightedScore()), isRec, values,
                    c == null ? null : c.monthlyTotal().orElse(null), c == null ? null : c.currency().orElse(null),
                    gains, away));
        }
        List<ConstraintEffect> constraints = new ArrayList<>();
        if (result.getSensitivity() != null) {
            for (SensitivityReport.BindingConstraint b : result.getSensitivity().constraints()) {
                constraints.add(new ConstraintEffect(b.constraint(), b.binding(), b.rejectedAlone(), b.alternative(),
                        b.explanation()));
            }
        }
        var stats = result.getSolveStatistics();
        return new Alternatives(frontier, stats == null ? 0 : stats.evaluated(), stats == null ? 0 : stats.feasible(),
                stats != null && stats.exact(), constraints, result.getSolverNotes());
    }

    // 4. sensitivity

    private Sensitivity sensitivity(OptimizationResult result) {
        SensitivityReport s = result.getSensitivity();
        if (s == null) {
            throw new Skip("no recommended design, so there is nothing whose stability to test");
        }
        List<WeightRange> ranges = new ArrayList<>();
        for (SensitivityReport.WeightRange w : s.weightRanges()) {
            ranges.add(new WeightRange(w.objective(), round(w.weight()), round(w.low()), round(w.high()),
                    w.belowTakeover(), w.aboveTakeover()));
        }
        NearestOption nearest = null;
        if (s.nearest() != null) {
            Map<String, Double> deltas = new LinkedHashMap<>();
            s.nearest().deltas().forEach((k, v) -> deltas.put(k, round(v)));
            nearest = new NearestOption(s.nearest().portfolio(), round(s.nearest().distance()), deltas);
        }
        return new Sensitivity(ranges, nearest, s.notes());
    }

    // 5. cost

    private Cost cost(OptimizationResult result, List<Assumption> assumptions) {
        DesignOption rec = result.getRecommended();
        if (rec == null) {
            throw new Skip("no recommended design to price");
        }
        PortfolioCost pc = rec.cost();
        if (pc == null) {
            throw new Skip("the run had no cost model, so nothing was priced (give a rate card for an offline run)");
        }
        List<CostItem> lines = new ArrayList<>();
        Map<String, FxRate> rates = new TreeMap<>();
        for (CostLine l : pc.lines()) {
            CostLine.Conversion cv = l.conversion();
            lines.add(new CostItem(l.label(), l.metro() == null ? null : l.metro().code(), l.monthly(), l.monthlyLow(),
                    l.monthlyHigh(), l.oneTime(), l.currency(), source(l.source()), l.note(),
                    cv == null ? null : cv.originalMonthly(), cv == null ? null : cv.originalCurrency(),
                    cv == null ? null : cv.rate()));
            if (cv != null) {
                FxRate r = new FxRate(cv.originalCurrency(), l.currency(), cv.rate(), cv.kind().name(), cv.source(),
                        cv.asOf(), cv.bandPercent());
                rates.putIfAbsent(r.from() + ">" + r.to() + "|" + r.rate().toPlainString() + "|" + r.source(), r);
            }
        }
        Growth growth = null;
        String growthNote = null;
        try {
            growth = growth(rec, assumptions);
        } catch (Skip s) {
            growthNote = s.getMessage();
        } catch (RuntimeException e) {
            growthNote = "the growth planner failed: " + describe(e);
        }
        Value value = null;
        String valueNote = null;
        try {
            value = value(rec, assumptions);
        } catch (Skip s) {
            valueNote = s.getMessage();
        } catch (RuntimeException e) {
            valueNote = "the value models failed: " + describe(e);
        }
        return new Cost(pc.currency().orElse(spec.commercial().currencyOrDefault()), pc.monthlyTotal().orElse(null),
                pc.monthlyLow().orElse(null), pc.monthlyHigh().orElse(null), pc.oneTimeTotal().orElse(null),
                pc.isComplete(), lines, pc.unpriced(), new ArrayList<>(rates.values()), growth, growthNote, value,
                valueNote);
    }

    private Growth growth(DesignOption rec, List<Assumption> assumptions) {
        if (card() == null) {
            throw new Skip("no rate card to price terms with (an offline record needs rateCard(...))");
        }
        GrowthPlan g = GrowthPlanner.forSpec(spec, rec.portfolio(), catalog(), card(), ReferenceRateCard.standard(),
                currencyConverter).plan();
        List<GrowthItem> items = new ArrayList<>();
        for (GrowthPlan.ItemPlan item : g.items()) {
            items.add(new GrowthItem(item.label(), item.upgradeMonth(), item.upgradeNote(),
                    item.recommended() == null ? null : item.recommended().name(), item.recommendation()));
        }
        for (String a : g.assumptions()) {
            assumptions.add(new Assumption("growth", a));
        }
        return new Growth(g.annualGrowth(), g.horizonMonths(), g.currency(), g.onDemandTotal(), items, g.unpriced(),
                g.assumptions());
    }

    private Value value(DesignOption rec, List<Assumption> assumptions) {
        CloudIntent cloud = null;
        for (CloudIntent c : spec.clouds()) {
            if (c.provider() != null && c.monthlyEgressGb() != null && c.monthlyEgressGb() > 0) {
                cloud = c;
                break;
            }
        }
        if (cloud == null) {
            throw new Skip("no cloud states monthlyEgressGb, so there is no egress to compare (the TCO and savings "
                    + "models price a cloud's egress)");
        }
        if (card() == null) {
            throw new Skip("no rate card to price the comparison with (an offline record needs rateCard(...))");
        }
        List<String> notes = new ArrayList<>();
        List<String> valueAssumptions = new ArrayList<>();
        Savings savings = null;
        try {
            SavingsEstimate s = SavingsCalculator.forSpec(fabric, spec, cloud.id(), rec.portfolio()).rateCard(card())
                    .currencyConverter(currencyConverter).defaultRates(defaultRates).calculate();
            savings = new Savings(s.getMonthlyEgressGb(), s.getNetMonthlySavings(), s.getAnnualNetSavings(),
                    s.getPaybackMonths(), s.getCurrency(), s.isComplete(), s.getUnpriced());
            valueAssumptions.addAll(s.getAssumptions());
        } catch (RuntimeException e) {
            notes.add("Savings could not be computed: " + describe(e));
        }
        Tco tco = null;
        try {
            TcoComparison t = TcoCalculator.forSpec(fabric, spec, cloud.id(), rec.portfolio()).rateCard(card())
                    .currencyConverter(currencyConverter).defaultRates(defaultRates).compare();
            tco = new Tco(t.getRecommended() == null ? null : t.getRecommended().name(),
                    t.getBaseline() == null ? null : t.getBaseline().name(), t.getMonthlySavingsVsBaseline(),
                    t.getCurrency(), t.getTerm() == null ? null : t.getTerm().name(), t.getRecommendationNote());
            for (String a : t.getAssumptions()) {
                if (!valueAssumptions.contains(a)) {
                    valueAssumptions.add(a);
                }
            }
        } catch (RuntimeException e) {
            notes.add("TCO could not be computed: " + describe(e));
        }
        if (savings == null && tco == null) {
            throw new IllegalStateException(String.join(" ", notes));
        }
        for (String a : valueAssumptions) {
            assumptions.add(new Assumption("value", a));
        }
        return new Value(cloud.id(), savings, tco, notes, valueAssumptions);
    }

    // 6. resilience

    private DesignDecisionRecord.Resilience resilience(ResilienceReport r, OptimizationResult result) {
        List<String> spofs = new ArrayList<>();
        for (ResilienceFindings.SinglePointOfFailure s : r.singlePointsOfFailure()) {
            spofs.add(s.description() + ": " + s.effect());
        }
        List<String> correlated = new ArrayList<>();
        for (ResilienceFindings.CorrelatedRisk c : r.correlatedRisks()) {
            correlated.add(c.description() + " (" + c.severity() + ")");
        }
        List<Availability> availability = new ArrayList<>();
        for (WorkloadAvailability w : r.availability()) {
            availability.add(new Availability(w.workload(), round(w.percent(), 4), round(w.downtimeMinutesPerYear(), 1),
                    w.formula()));
        }
        Map<String, Double> elements = new TreeMap<>();
        if (r.availabilityAssumptions() != null) {
            r.availabilityAssumptions().availability().forEach((k, v) -> elements.put(k.name(), v));
        }
        List<String> remediations = new ArrayList<>();
        for (ResilienceFindings.Remediation m : r.remediations()) {
            remediations.add(m.action() + (m.rationale() == null || m.rationale().isBlank()
                    ? (m.verified() ? " (re-simulated)" : " (not re-simulated)") : " (" + m.rationale().strip() + ")"));
        }
        List<String> notes = new ArrayList<>(r.notes());
        if (result != null && result.getRecommended() != null
                && !result.getRecommended().portfolio().key().equals(r.portfolio())) {
            notes.add(0, "This analysis is of " + r.portfolio() + ", not of the recommended "
                    + result.getRecommended().portfolio().key() + ".");
        }
        return new DesignDecisionRecord.Resilience(r.portfolio(), verdict(r.n1()), verdict(r.n2()), spofs, correlated,
                availability, r.availabilityAssumptions() == null ? null : r.availabilityAssumptions().source(),
                elements, remediations, r.graph() == null ? null : r.graph().toMermaid(), notes);
    }

    private static Verdict verdict(ResilienceFindings.NVerdict v) {
        return v == null ? null : new Verdict(v.level().name(), v.scenarios(), v.survived(), v.degraded(), v.outages(),
                v.statement());
    }

    // 9. implementation

    private Implementation implementation(OptimizationResult result, String version) {
        PlanSummary summary = null;
        String note = null;
        DeploymentPlan p = plan;
        boolean built = false;
        if (p == null && result != null && result.getRecommended() != null && buildPlan) {
            try {
                p = DeploymentWizard.builder(null, result).designSpec(spec)
                        .rateCard(card() != null ? card() : ReferenceRateCard.standard())
                        .currencyConverter(currencyConverter).defaultRates(defaultRates).plan();
                built = true;
            } catch (RuntimeException e) {
                note = "The deployment plan could not be built: " + describe(e);
            }
        }
        if (p != null) {
            List<PlanStep> steps = PlanSteps.of(p);
            int executable = (int) steps.stream().filter(PlanStep::executable).count();
            var pricing = p.getPricing();
            summary = new PlanSummary(p.getPlanId(), p.effectiveDeploymentName(), steps.size(), executable,
                    size(p.getCloudRouters()), size(p.getProviderConnections()), size(p.getBackboneLinks()),
                    pricing == null ? null : pricing.getMonthlyTotal(), pricing == null ? null : pricing.getSetupTotal(),
                    pricing == null ? null : pricing.getCurrency(),
                    pricing == null || pricing.getSource() == null ? null : source(pricing.getSource()),
                    p.isValid(), p.getValidationErrors());
            if (built) {
                note = "Built by the recorder from the recommendation, priced with " + (card() != null
                        ? card().cardName() : ReferenceRateCard.standard().cardName()) + ", without deployment "
                        + "inputs (project, account, notification addresses) and without live validation; build it "
                        + "with DeploymentWizard and run dryRun() before executing.";
            } else if (p.getSpecFingerprint() == null) {
                note = "The supplied plan carries no spec fingerprint; it is recorded as given.";
            }
        } else if (note == null) {
            note = !buildPlan ? "No plan was supplied and building one was not requested."
                    : result == null || result.getRecommended() == null ? "No recommended design to plan."
                    : "No plan.";
        }
        DesignSpec pinned = result == null ? spec : result.specWithUsedRatesPinned();
        CommercialTerms.ExchangeRates rates = pinned.commercial().exchangeRates();
        Reproduction reproduction = new Reproduction(spec.fingerprint(), pinned.fingerprint(),
                rates == null ? Map.of() : rates.rates(), rates == null ? null : rates.asOf(),
                rates == null ? null : rates.source(), version);
        return new Implementation(summary, note, reproduction);
    }

    // 7. risks

    private Risks risks(OptimizationResult result, ResilienceReport report, RecordSection<Implementation> impl,
                        List<String> failures) {
        Map<String, Risk> byKey = new LinkedHashMap<>();
        if (result != null && result.getRiskAssessment() != null && result.getRiskAssessment().getFindings() != null) {
            for (RiskFinding f : result.getRiskAssessment().getFindings()) {
                if ("HEALTHY".equals(f.getCategory())) {
                    continue;
                }
                add(byKey, new Risk(level(f.getSeverity() == null ? null : f.getSeverity().name()), "optimizer",
                        f.getCategory(), f.getDescription(), f.getRecommendation()));
            }
        }
        if (result != null && result.getRecommended() != null && result.getRecommended().cost() != null
                && !result.getRecommended().cost().isComplete()) {
            List<String> unpriced = result.getRecommended().cost().unpriced();
            add(byKey, new Risk(RiskLevel.MEDIUM, "cost", "COST_UNPRICED", unpriced.size() + " item(s) could not be "
                    + "priced, so the monthly total understates the cost: " + String.join("; ", unpriced),
                    "Price the items with a rate card that covers them"));
        }
        if (report != null) {
            if (report.n1() != null && report.n1().level() == ResilienceFindings.Level.FAIL) {
                add(byKey, new Risk(RiskLevel.HIGH, "resilience", "N1_FAIL", report.n1().statement(),
                        "Apply the remediations in the resilience section"));
            } else if (report.n1() != null && report.n1().level() == ResilienceFindings.Level.DEGRADED) {
                add(byKey, new Risk(RiskLevel.MEDIUM, "resilience", "N1_DEGRADED", report.n1().statement(), null));
            }
            if (report.n2() != null && report.n2().level() == ResilienceFindings.Level.FAIL) {
                add(byKey, new Risk(RiskLevel.LOW, "resilience", "N2_FAIL", report.n2().statement(), null));
            }
            for (ResilienceFindings.SinglePointOfFailure s : report.singlePointsOfFailure()) {
                add(byKey, new Risk(RiskLevel.HIGH, "resilience", "SINGLE_POINT_OF_FAILURE",
                        s.description() + ": " + s.effect(), null));
            }
            for (ResilienceFindings.CorrelatedRisk c : report.correlatedRisks()) {
                add(byKey, new Risk(c.severity() == ResilienceFindings.Severity.HIGH ? RiskLevel.HIGH : RiskLevel.MEDIUM,
                        "resilience", "CORRELATED_RISK", c.description(), null));
            }
        }
        if (plan != null && !plan.isValid() && plan.getValidationErrors() != null) {
            for (String e : plan.getValidationErrors()) {
                add(byKey, new Risk(RiskLevel.HIGH, "plan", "PLAN_INVALID", e, "Fix the plan before executing it"));
            }
        }
        if (impl.isIncluded() && impl.content().plan() != null && plan == null
                && impl.content().plan().currency() == null) {
            add(byKey, new Risk(RiskLevel.MEDIUM, "plan", "PLAN_MIXED_CURRENCY", "The plan's prices span currencies "
                    + "that no rate source converts, so it has no single total", "Pin the missing exchange rates in "
                    + "commercial.exchangeRates"));
        }
        if (estate != null) {
            for (EstateFinding f : estate.findings()) {
                add(byKey, new Risk(level(f.severity().name()), "estate", f.category().name(), f.title(), f.action()));
            }
        }
        if (peering != null) {
            for (String w : peering.warnings()) {
                add(byKey, new Risk(RiskLevel.LOW, "peering", "PEERING_WARNING", w, null));
            }
        }
        for (SpecIssue issue : spec.validate()) {
            add(byKey, new Risk(RiskLevel.LOW, "spec", "SPEC_WARNING", issue.path() + ": " + issue.message(), null));
        }
        for (String f : failures) {
            add(byKey, new Risk(RiskLevel.MEDIUM, "record", "SECTION_FAILED", f,
                    "Re-run once the cause is fixed; the rest of the record stands"));
        }
        List<Risk> items = new ArrayList<>(byKey.values());
        items.sort(Comparator.comparing(Risk::severity).thenComparing(Risk::source).thenComparing(Risk::category)
                .thenComparing(Risk::statement));
        return new Risks(items);
    }

    private static void add(Map<String, Risk> byKey, Risk risk) {
        String key = risk.category() + "|" + risk.statement().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        Risk existing = byKey.get(key);
        if (existing == null || risk.severity().compareTo(existing.severity()) < 0) {
            byKey.put(key, risk);
        }
    }

    private static RiskLevel level(String name) {
        if (name == null) {
            return RiskLevel.INFO;
        }
        try {
            return RiskLevel.valueOf(name);
        } catch (IllegalArgumentException e) {
            return RiskLevel.INFO;
        }
    }

    // 8. provenance

    private Provenance provenance(OptimizationResult result, ResilienceReport report, List<Assumption> extra,
                                  Instant generatedAt) {
        Map<String, Assumption> assumptions = new LinkedHashMap<>();
        if (result != null && result.getExplanation() != null && result.getExplanation().getAssumptions() != null) {
            for (String a : result.getExplanation().getAssumptions()) {
                assumptions.putIfAbsent(a, new Assumption("optimizer", a));
            }
        }
        if (report != null) {
            for (String a : report.assumptions()) {
                assumptions.putIfAbsent(a, new Assumption("resilience", a));
            }
            if (report.availabilityAssumptions() != null) {
                String a = "Availability figures: " + report.availabilityAssumptions().source();
                assumptions.putIfAbsent(a, new Assumption("resilience", a));
            }
        }
        for (Assumption a : extra) {
            assumptions.putIfAbsent(a.text(), a);
        }
        List<String> dataNotes = new ArrayList<>();
        int measured = 0;
        int estimated = 0;
        int unknown = 0;
        Instant catalogueAsOf = null;
        String calibration = null;
        Set<String> asOf = new LinkedHashSet<>();
        if (result != null) {
            if (result.getDataProvenance() != null) {
                dataNotes.addAll(result.getDataProvenance());
            }
            if (result.getLatencyMatrix() != null && result.getLatencyMatrix().getMatrix() != null) {
                for (List<LatencyEntry> row : result.getLatencyMatrix().getMatrix()) {
                    for (LatencyEntry e : row) {
                        if (Double.isNaN(e.getLatencyMs())) {
                            unknown++;
                        } else if (e.isEstimated()) {
                            estimated++;
                        } else {
                            measured++;
                        }
                    }
                }
            }
            catalogueAsOf = result.getDataAsOf();
            if (catalogueAsOf != null) {
                asOf.add("Fabric catalogue (metros, latencies, service profiles) read " + catalogueAsOf);
            }
            if (result.getLatencyCalibration() != null && result.getLatencyCalibration().overall() != null) {
                var fit = result.getLatencyCalibration().overall();
                calibration = String.format(Locale.ROOT, "Unmeasured round trips are the fibre floor x %.2f (%s, "
                        + "fitted from %,d measured pairs, interquartile range %.2f to %.2f)", fit.factor(),
                        fit.scope(), fit.pairs(), fit.low(), fit.high());
            }
            DesignOption rec = result.getRecommended();
            if (rec != null && rec.cost() != null) {
                boolean reference = false;
                for (CostLine l : rec.cost().lines()) {
                    if (l.source() == PriceSource.REFERENCE || l.source() == PriceSource.ESTIMATE) {
                        reference = true;
                    }
                    if (l.conversion() != null) {
                        asOf.add("Exchange rate " + l.conversion().originalCurrency() + " to " + l.currency() + ": "
                                + l.conversion().source() + ", as of " + l.conversion().asOf());
                    }
                }
                if (reference) {
                    asOf.add("Reference rate card as of " + ReferenceRateCard.standard().asOf());
                }
            }
        }
        if (report != null && report.dataAsOf() != null) {
            asOf.add("Resilience analysis facts gathered " + report.dataAsOf());
        }
        if (peering != null && peering.getDataSources() != null) {
            for (String d : peering.getDataSources()) {
                dataNotes.add("Peering: " + d);
            }
        }
        if (estate != null) {
            dataNotes.add("Estate inventory: " + estate.inventorySource() + ", as of " + estate.inventoryAsOf());
            asOf.add("Estate inventory as of " + estate.inventoryAsOf());
        }
        asOf.add("Record generated " + generatedAt);
        return new Provenance(new ArrayList<>(assumptions.values()), dataNotes, measured, estimated, unknown,
                catalogueAsOf, new ArrayList<>(asOf), calibration);
    }

    // helpers

    /** Signals a section skipped for want of input, with the reason. */
    private static final class Skip extends RuntimeException {
        Skip(String reason) {
            super(reason, null, false, false);
        }
    }

    private static <T> RecordSection<T> section(String name, List<String> failures, Supplier<T> body) {
        try {
            return RecordSection.included(body.get());
        } catch (Skip s) {
            return RecordSection.skipped(s.getMessage());
        } catch (RuntimeException e) {
            String reason = describe(e);
            failures.add(name + ": " + reason);
            return RecordSection.failed(reason);
        }
    }

    private <T> T requireSameSpec(String what, String fingerprint, T value, boolean allowMissing) {
        if (value == null) {
            return null;
        }
        if (fingerprint == null) {
            if (allowMissing) {
                return value;
            }
            throw new IllegalArgumentException("The " + what + " carries no spec fingerprint, so it cannot be tied "
                    + "to spec '" + spec.name() + "'");
        }
        if (!fingerprint.equals(spec.fingerprint())) {
            throw new IllegalArgumentException("The " + what + " was computed for another spec (fingerprint "
                    + fingerprint + "; this spec is " + spec.fingerprint() + ")");
        }
        return value;
    }

    private static String describe(Throwable e) {
        String message = e.getMessage();
        return e.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private String sdkVersion() {
        if (sdkVersion == null) {
            Package p = DesignDecisionRecorder.class.getPackage();
            String v = p == null ? null : p.getImplementationVersion();
            sdkVersion = v == null || v.isBlank() ? "development build (no jar manifest version)" : v;
        }
        return sdkVersion;
    }

    private static String unit(OptimizationResult result, String id) {
        return result.getObjectiveUnits() == null ? null : result.getObjectiveUnits().get(id);
    }

    private static String scale(OptimizationResult result, String id) {
        return result.getObjectiveScales() == null ? null : result.getObjectiveScales().get(id);
    }

    private String value(OptimizationResult result, String id, double v) {
        return DecisionRecordMarkdown.value(unit(result, id), spec.commercial().currencyOrDefault(), v);
    }

    static String source(PriceSource source) {
        if (source == null) {
            return "unknown";
        }
        return switch (source) {
            case EQUINIX_LIVE -> "live";
            case REFERENCE -> "reference";
            case ESTIMATE -> "estimate";
            case CUSTOM -> "contract";
            case PROVIDER_API -> "provider API";
            case COMPOSITE -> "composite";
        };
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static String joinNames(List<?> values) {
        List<String> names = new ArrayList<>();
        for (Object v : values) {
            names.add(String.valueOf(v));
        }
        return String.join(", ", names);
    }

    private static Double round(double v) {
        return round(v, 2);
    }

    private static Double round(double v, int places) {
        if (!Double.isFinite(v)) {
            return null;
        }
        return BigDecimal.valueOf(v).setScale(places, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private static String num(double v) {
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }

    private static String pct(double share) {
        return String.format(Locale.ROOT, "%.0f%%", share * 100);
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "n/a" : String.format(Locale.ROOT, "%,.2f", amount);
    }
}
