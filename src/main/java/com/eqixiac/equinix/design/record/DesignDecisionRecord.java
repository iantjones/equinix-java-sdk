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

import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A Design Decision Record: what was decided for a {@link DesignSpec}, why, on what evidence, and what would
 * change the decision. Produced by {@link DesignDecisionRecorder}; immutable; round-trips through JSON
 * ({@link #toJson()}, {@link #fromJson(String)}); renders as an ADR-style Markdown document ({@link #toMarkdown()});
 * two records of the same design can be diffed ({@link #compare}).
 *
 * <p>Every section is a {@link RecordSection}: included, or skipped or failed with the reason. Figures keep their
 * provenance: each cost line says whether it is a live price, a reference figure or an estimate; availability is
 * computed from planning assumptions, not SLAs; latency counts say how many figures were measured.</p>
 *
 * @param schemaVersion   the record format version ({@value #SCHEMA_VERSION})
 * @param title           the record's title
 * @param status          the decision's status (for example {@code Proposed}, {@code Accepted})
 * @param generatedAt     when the record was produced
 * @param sdkVersion      the SDK version that produced it
 * @param specFingerprint the spec's {@code fingerprint()}
 * @param spec            the spec the record is about
 * @param context         1. context and requirements, restated from the spec
 * @param decision        2. the recommended portfolio, its placement and its objective values and scores
 * @param alternatives    3. the Pareto frontier and what each option trades away; what the constraints rejected
 * @param sensitivity     4. how far the weights can move before the decision changes
 * @param cost            5. priced lines, currency conversions, term and growth view, TCO and savings
 * @param resilience      6. failure simulation: N-1 / N-2, single points of failure, availability, remediation
 * @param risks           7. every engine's findings, de-duplicated, most severe first
 * @param provenance      8. assumptions, data notes and as-of dates
 * @param implementation  9. the deployment plan summary and how to reproduce the record
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DesignDecisionRecord(String schemaVersion, String title, String status, Instant generatedAt,
                                   String sdkVersion, String specFingerprint, DesignSpec spec,
                                   RecordSection<Context> context, RecordSection<Decision> decision,
                                   RecordSection<Alternatives> alternatives, RecordSection<Sensitivity> sensitivity,
                                   RecordSection<Cost> cost, RecordSection<Resilience> resilience,
                                   RecordSection<Risks> risks, RecordSection<Provenance> provenance,
                                   RecordSection<Implementation> implementation) {

    /** The record format version this release writes. */
    public static final String SCHEMA_VERSION = "1";

    /** Requires the identity fields. */
    public DesignDecisionRecord {
        if (spec == null || specFingerprint == null) {
            throw new IllegalArgumentException("A decision record needs its spec and the spec's fingerprint");
        }
        schemaVersion = schemaVersion == null ? SCHEMA_VERSION : schemaVersion;
    }

    /** @return the record as an ADR-style Markdown document */
    public String toMarkdown() {
        return DecisionRecordMarkdown.render(this);
    }

    /** @return the record as indented JSON (ISO-8601 instants) */
    public String toJson() {
        return RecordJson.write(this);
    }

    /**
     * Reads a record written by {@link #toJson()}.
     *
     * @param json the document
     * @return the record
     * @throws IllegalArgumentException when the document is not a readable record
     */
    public static DesignDecisionRecord fromJson(String json) {
        return RecordJson.read(json, DesignDecisionRecord.class);
    }

    /**
     * What changed between two records (spec, recommendation, cost, resilience, risks), for example the same
     * design re-run a quarter later.
     *
     * @param before the earlier record
     * @param after  the later record
     * @return the differences
     */
    public static DecisionDelta compare(DesignDecisionRecord before, DesignDecisionRecord after) {
        return DecisionDelta.between(before, after);
    }

    /**
     * The spec with the exchange rates this record used pinned in {@code commercial.exchangeRates}, so running it
     * again reproduces the cost figures. The spec itself when nothing was converted.
     *
     * @return the reproducible spec
     */
    public DesignSpec reproducibleSpec() {
        if (implementation == null || !implementation.isIncluded()
                || implementation.content().reproduction().pinnedRates().isEmpty()) {
            return spec;
        }
        Reproduction r = implementation.content().reproduction();
        CommercialTerms.ExchangeRates rates = new CommercialTerms.ExchangeRates(r.pinnedRates(), r.ratesAsOf(),
                r.ratesSource(), spec.commercial().exchangeRates() == null ? null
                : spec.commercial().exchangeRates().bandPercent());
        return spec.toBuilder().commercial(spec.commercial().toBuilder().exchangeRates(rates).build()).build();
    }

    /** @return the sections in document order, keyed by their heading */
    @JsonIgnore
    public Map<String, RecordSection<?>> sections() {
        Map<String, RecordSection<?>> all = new LinkedHashMap<>();
        all.put("Context", context);
        all.put("Decision", decision);
        all.put("Alternatives", alternatives);
        all.put("Sensitivity", sensitivity);
        all.put("Cost", cost);
        all.put("Resilience", resilience);
        all.put("Risks", risks);
        all.put("Provenance", provenance);
        all.put("Implementation", implementation);
        return all;
    }

    private static <T> List<T> list(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static <K, V> Map<K, V> map(Map<K, V> values) {
        return values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    // 1. Context

    /**
     * The requirements, restated from the spec in words.
     *
     * @param name          the spec name
     * @param description   the spec description, or {@code null}
     * @param sites         one line per site
     * @param workloads     one line per workload
     * @param clouds        one line per cloud
     * @param peers         one line per peer network
     * @param constraints   one line per stated constraint
     * @param weightProfile the named weight profile, or {@code null}
     * @param weights       each objective's share of the total weight, 0..1
     * @param commercial    one line per commercial term
     * @param specIssues    the spec's validation warnings
     * @param background    supplied context: the existing estate and the peering analysis, one line each
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Context(String name, String description, List<String> sites, List<String> workloads,
                          List<String> clouds, List<String> peers, List<String> constraints, String weightProfile,
                          Map<String, Double> weights, List<String> commercial, List<String> specIssues,
                          List<String> background) {
        /** Copies the collections. */
        public Context {
            sites = list(sites);
            workloads = list(workloads);
            clouds = list(clouds);
            peers = list(peers);
            constraints = list(constraints);
            weights = map(weights);
            commercial = list(commercial);
            specIssues = list(specIssues);
            background = list(background);
        }
    }

    // 2. Decision

    /**
     * The recommended design.
     *
     * @param portfolio      the portfolio key (for example {@code LD+NY})
     * @param metros         the metros, primary first
     * @param weightedScore  the weighted score, 0..100
     * @param paretoOptimal  whether no other feasible option is at least as good on every objective
     * @param objectives     each objective's value, score, weight and scale
     * @param placements     one line per workload: where it runs and where its standby is
     * @param constraints    every constraint check the recommendation passed, with what was verified
     * @param rationale      why this option, in a few sentences
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Decision(String portfolio, List<String> metros, double weightedScore, boolean paretoOptimal,
                           List<ObjectiveResult> objectives, List<String> placements, List<String> constraints,
                           String rationale) {
        /** Copies the collections. */
        public Decision {
            metros = list(metros);
            objectives = list(objectives);
            placements = list(placements);
            constraints = list(constraints);
        }
    }

    /**
     * One objective of the recommended design.
     *
     * @param id          the objective id
     * @param unit        the unit of {@code value}
     * @param value       the measured value; {@code null} when unknown
     * @param score       the 0..100 score; {@code null} when unknown
     * @param weight      the objective's share of the total weight, 0..1
     * @param scale       how the value was turned into the score
     * @param explanation how the value was reached, or why it is unknown
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ObjectiveResult(String id, String unit, Double value, Double score, Double weight, String scale,
                                  String explanation) {
    }

    // 3. Alternatives

    /**
     * The options considered and rejected.
     *
     * @param frontier    every Pareto-optimal option, best weighted score first, the recommendation included
     * @param evaluated   portfolios examined
     * @param feasible    portfolios that satisfied every constraint
     * @param exact       whether every portfolio in range was examined
     * @param constraints per constraint, whether it decided the answer and what would be chosen without it
     * @param notes       the search's own notes (rejection counts, narrowing)
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Alternatives(List<Alternative> frontier, long evaluated, long feasible, boolean exact,
                               List<ConstraintEffect> constraints, List<String> notes) {
        /** Copies the collections. */
        public Alternatives {
            frontier = list(frontier);
            constraints = list(constraints);
            notes = list(notes);
        }
    }

    /**
     * One option on the frontier.
     *
     * @param portfolio     the portfolio key
     * @param weightedScore the weighted score, 0..100
     * @param recommended   whether it is the recommendation
     * @param values        each known objective value, by objective id
     * @param monthlyCost   the expected monthly total, or {@code null} when it has none in one currency
     * @param currency      the currency of {@code monthlyCost}, or {@code null}
     * @param gains         where it is better than the recommendation, one phrase per objective
     * @param tradesAway    where it is worse than the recommendation, one phrase per objective
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Alternative(String portfolio, double weightedScore, boolean recommended, Map<String, Double> values,
                              BigDecimal monthlyCost, String currency, List<String> gains, List<String> tradesAway) {
        /** Copies the collections. */
        public Alternative {
            values = map(values);
            gains = list(gains);
            tradesAway = list(tradesAway);
        }
    }

    /**
     * What one constraint did to the search.
     *
     * @param constraint    the constraint id
     * @param binding       whether removing it changes the recommendation
     * @param rejectedAlone how many portfolios it alone rejected
     * @param withoutIt     the portfolio recommended without it, when binding
     * @param explanation   a sentence for the reader
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConstraintEffect(String constraint, boolean binding, int rejectedAlone, String withoutIt,
                                   String explanation) {
    }

    // 4. Sensitivity

    /**
     * How firm the decision is.
     *
     * @param weights the range of each weighted objective's share over which the recommendation holds
     * @param nearest the closest other frontier option, or {@code null} when there is none
     * @param notes   caveats
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Sensitivity(List<WeightRange> weights, NearestOption nearest, List<String> notes) {
        /** Copies the collections. */
        public Sensitivity {
            weights = list(weights);
            notes = list(notes);
        }
    }

    /**
     * The share of one objective's weight over which the recommendation holds.
     *
     * @param objective     the objective id
     * @param share         its current share, 0..1
     * @param low           the smallest share that keeps the recommendation
     * @param high          the largest share that keeps the recommendation
     * @param belowTakeover the option recommended below {@code low}, or {@code null}
     * @param aboveTakeover the option recommended above {@code high}, or {@code null}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WeightRange(String objective, double share, double low, double high, String belowTakeover,
                              String aboveTakeover) {
    }

    /**
     * The frontier option nearest the recommendation.
     *
     * @param portfolio its key
     * @param distance  Euclidean distance over the 0..100 scores
     * @param deltas    per objective, its value minus the recommendation's (in the objective's unit)
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record NearestOption(String portfolio, double distance, Map<String, Double> deltas) {
        /** Copies the map. */
        public NearestOption {
            deltas = map(deltas);
        }
    }

    // 5. Cost

    /**
     * What the recommended design costs, and where each figure came from.
     *
     * @param currency      the currency every line is in (the spec's commercial currency)
     * @param monthly       the expected monthly total, or {@code null} when the lines span currencies
     * @param monthlyLow    the low end of the monthly range, or {@code null}
     * @param monthlyHigh   the high end of the monthly range, or {@code null}
     * @param oneTime       the one-time total, or {@code null}
     * @param complete      whether every item was priced (an incomplete total understates the cost)
     * @param lines         the priced lines
     * @param unpriced      what could not be priced and why
     * @param ratesUsed     every exchange rate a line was converted at
     * @param growth        the term and growth view, or {@code null} (see {@code growthNote})
     * @param growthNote    why there is no growth view, or {@code null}
     * @param value         the TCO and savings view, or {@code null} (see {@code valueNote})
     * @param valueNote     why there is no TCO and savings view, or {@code null}
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Cost(String currency, BigDecimal monthly, BigDecimal monthlyLow, BigDecimal monthlyHigh,
                       BigDecimal oneTime, boolean complete, List<CostItem> lines, List<String> unpriced,
                       List<FxRate> ratesUsed, Growth growth, String growthNote, Value value, String valueNote) {
        /** Copies the collections. */
        public Cost {
            lines = list(lines);
            unpriced = list(unpriced);
            ratesUsed = list(ratesUsed);
        }
    }

    /**
     * One priced line.
     *
     * @param label            what is paid for
     * @param metro            the metro code, or {@code null} for a design-wide item
     * @param monthly          the expected monthly amount
     * @param monthlyLow       the low end of the plausible range
     * @param monthlyHigh      the high end of the plausible range
     * @param oneTime          the one-time amount
     * @param currency         the currency of the amounts
     * @param source           {@code live}, {@code reference}, {@code estimate}, {@code contract},
     *                         {@code provider API} or {@code composite}
     * @param note             provenance detail
     * @param originalMonthly  the amount before conversion, or {@code null} when not converted
     * @param originalCurrency the currency before conversion, or {@code null}
     * @param rate             the exchange rate applied, or {@code null}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CostItem(String label, String metro, BigDecimal monthly, BigDecimal monthlyLow, BigDecimal monthlyHigh,
                           BigDecimal oneTime, String currency, String source, String note, BigDecimal originalMonthly,
                           String originalCurrency, BigDecimal rate) {
    }

    /**
     * An exchange rate a line was converted at.
     *
     * @param from        the currency converted from
     * @param to          the currency converted to
     * @param rate        units of {@code to} per one {@code from}
     * @param kind        {@code PINNED}, {@code SUPPLIED}, {@code LIVE} or {@code REFERENCE}
     * @param source      the rate's source
     * @param asOf        the rate's date
     * @param bandPercent the widening applied to converted ranges, either side
     */
    public record FxRate(String from, String to, BigDecimal rate, String kind, String source, String asOf,
                         double bandPercent) {
    }

    /**
     * The design priced month by month under the spec's growth, with a term recommendation per item.
     *
     * @param annualGrowth  the growth rate used (fraction)
     * @param horizonMonths the horizon
     * @param currency      the currency of the amounts
     * @param onDemandTotal the on-demand total over the horizon, or {@code null} when a month is unpriced
     * @param items         one line per item
     * @param unpriced      what could not be priced
     * @param assumptions   the modelling assumptions
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Growth(double annualGrowth, int horizonMonths, String currency, BigDecimal onDemandTotal,
                         List<GrowthItem> items, List<String> unpriced, List<String> assumptions) {
        /** Copies the collections. */
        public Growth {
            items = list(items);
            unpriced = list(unpriced);
            assumptions = list(assumptions);
        }
    }

    /**
     * One item's growth and term decision.
     *
     * @param label           what is paid for
     * @param upgradeMonth    the first month the tier changes, or {@code null}
     * @param upgradeNote     the upgrade in words, or {@code null}
     * @param recommendedTerm the cheapest term over the horizon, or {@code null} when none priced
     * @param recommendation  why
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GrowthItem(String label, Integer upgradeMonth, String upgradeNote, String recommendedTerm,
                             String recommendation) {
    }

    /**
     * The TCO and egress-savings view of the design's main cloud.
     *
     * @param cloud       the cloud compared
     * @param savings     the private-path egress saving, or {@code null} when it could not be computed
     * @param tco         the archetype comparison, or {@code null} when it could not be computed
     * @param notes       why a part is missing, and caveats
     * @param assumptions what was assumed where the spec was silent
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Value(String cloud, Savings savings, Tco tco, List<String> notes, List<String> assumptions) {
        /** Copies the collections. */
        public Value {
            notes = list(notes);
            assumptions = list(assumptions);
        }
    }

    /**
     * Egress savings of the private path.
     *
     * @param monthlyEgressGb   the egress volume compared
     * @param netMonthlySavings the net monthly saving, or {@code null} when not priced
     * @param annualNetSavings  the annual net saving, or {@code null}
     * @param paybackMonths     months to recover one-time costs, or {@code null}
     * @param currency          the currency of the amounts
     * @param complete          whether every component was priced
     * @param unpriced          what could not be priced
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Savings(BigDecimal monthlyEgressGb, BigDecimal netMonthlySavings, BigDecimal annualNetSavings,
                          BigDecimal paybackMonths, String currency, boolean complete, List<String> unpriced) {
        /** Copies the list. */
        public Savings {
            unpriced = list(unpriced);
        }
    }

    /**
     * The TCO archetype comparison.
     *
     * @param recommended             the cheapest archetype over the term
     * @param baseline                the archetype compared against
     * @param monthlySavingsVsBaseline the monthly saving of the recommended archetype, or {@code null}
     * @param currency                the currency of the amounts
     * @param term                    the term compared over
     * @param note                    the comparison's own recommendation note
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Tco(String recommended, String baseline, BigDecimal monthlySavingsVsBaseline, String currency,
                      String term, String note) {
    }

    // 6. Resilience

    /**
     * The failure simulation of the recommended design.
     *
     * @param portfolio          the portfolio analysed
     * @param n1                 the N-1 verdict (single-element failures)
     * @param n2                 the N-2 verdict (pairs of failures)
     * @param singlePoints       single elements whose loss causes an outage
     * @param correlatedRisks    redundant elements sharing a failure domain
     * @param availability       estimated availability per workload
     * @param availabilityBasis  the source of the per-element figures: planning assumptions, not SLAs
     * @param elementAvailability the per-element-type availability used
     * @param remediations       concrete changes, with whether each was re-simulated
     * @param mermaid            the design graph as a Mermaid flowchart
     * @param notes              caveats
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Resilience(String portfolio, Verdict n1, Verdict n2, List<String> singlePoints,
                             List<String> correlatedRisks, List<Availability> availability, String availabilityBasis,
                             Map<String, Double> elementAvailability, List<String> remediations, String mermaid,
                             List<String> notes) {
        /** Copies the collections. */
        public Resilience {
            singlePoints = list(singlePoints);
            correlatedRisks = list(correlatedRisks);
            availability = list(availability);
            elementAvailability = elementAvailability == null ? Map.of()
                    : Collections.unmodifiableMap(new TreeMap<>(elementAvailability));
            remediations = list(remediations);
            notes = list(notes);
        }
    }

    /**
     * An N-1 or N-2 verdict.
     *
     * @param level     {@code PASS}, {@code DEGRADED}, {@code FAIL} or {@code NOT_APPLICABLE}
     * @param scenarios scenarios covered
     * @param survived  scenarios survived at full service
     * @param degraded  scenarios that degraded service
     * @param outages   scenarios that caused an outage
     * @param statement the verdict in words
     */
    public record Verdict(String level, int scenarios, int survived, int degraded, int outages, String statement) {
    }

    /**
     * One workload's estimated availability, under planning assumptions.
     *
     * @param workload               the workload
     * @param percent                end-to-end availability, percent
     * @param downtimeMinutesPerYear the implied downtime
     * @param formula                the series/parallel composition used
     */
    public record Availability(String workload, double percent, double downtimeMinutesPerYear, String formula) {
    }

    // 7. Risks

    /**
     * Every finding, de-duplicated, most severe first.
     *
     * @param items the findings
     */
    public record Risks(List<Risk> items) {
        /** Copies the list. */
        public Risks {
            items = list(items);
        }
    }

    /**
     * One finding.
     *
     * @param severity       how serious
     * @param source         the engine that raised it ({@code optimizer}, {@code resilience}, {@code cost},
     *                       {@code plan}, {@code estate}, {@code peering}, {@code spec}, {@code record})
     * @param category       the engine's category
     * @param statement      the finding
     * @param recommendation what to do, or {@code null}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Risk(RiskLevel severity, String source, String category, String statement, String recommendation) {
    }

    /** Finding severity, most severe first. */
    public enum RiskLevel {
        /** Blocks the design. */
        CRITICAL,
        /** Must be addressed before implementation. */
        HIGH,
        /** Should be addressed or accepted explicitly. */
        MEDIUM,
        /** Worth knowing. */
        LOW,
        /** Informational. */
        INFO
    }

    // 8. Provenance

    /**
     * What the record rests on.
     *
     * @param assumptions        every assumption an engine reported, de-duplicated
     * @param dataNotes          how complete the catalogue reads were
     * @param measuredLatencies  site-to-metro round trips of the recommendation that Fabric measured
     * @param estimatedLatencies site-to-metro round trips that were estimated (distance with the route factor, or
     *                           the intra-metro assumption)
     * @param unknownLatencies   site-to-metro round trips that are unknown
     * @param catalogueAsOf      when the catalogue facts were gathered, or {@code null}
     * @param asOf               the as-of dates of reference data and rates, one line each
     * @param latencyCalibration the route factor used for estimates and what it was fitted from, or {@code null}
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Provenance(List<Assumption> assumptions, List<String> dataNotes, int measuredLatencies,
                             int estimatedLatencies, int unknownLatencies, Instant catalogueAsOf, List<String> asOf,
                             String latencyCalibration) {
        /** Copies the collections. */
        public Provenance {
            assumptions = list(assumptions);
            dataNotes = list(dataNotes);
            asOf = list(asOf);
        }
    }

    /**
     * One assumption.
     *
     * @param source the engine that made it
     * @param text   the assumption
     */
    public record Assumption(String source, String text) {
    }

    // 9. Implementation

    /**
     * How to build and reproduce the design.
     *
     * @param plan         the deployment plan summary, or {@code null} (see {@code planNote})
     * @param planNote     why there is no plan summary, or a caveat about the plan, or {@code null}
     * @param reproduction what pins this record's figures
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Implementation(PlanSummary plan, String planNote, Reproduction reproduction) {
    }

    /**
     * A deployment plan in brief.
     *
     * @param planId            the plan's content id
     * @param deploymentName    the deployment name
     * @param steps             plan steps
     * @param executableSteps   steps the SDK will execute
     * @param routers           Cloud Routers
     * @param connections       provider connections
     * @param backboneLinks     backbone links
     * @param monthlyTotal      the plan's monthly total, or {@code null} when it spans currencies
     * @param setupTotal        the one-time total, or {@code null}
     * @param currency          the currency of the totals, or {@code null}
     * @param pricingSource     the dominant price source
     * @param valid             whether the plan passed validation
     * @param validationErrors  what failed validation
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record PlanSummary(String planId, String deploymentName, int steps, int executableSteps, int routers,
                              int connections, int backboneLinks, BigDecimal monthlyTotal, BigDecimal setupTotal,
                              String currency, String pricingSource, boolean valid, List<String> validationErrors) {
        /** Copies the list. */
        public PlanSummary {
            validationErrors = list(validationErrors);
        }
    }

    /**
     * What pins the record's figures.
     *
     * @param specFingerprint       the spec's fingerprint
     * @param pinnedSpecFingerprint the fingerprint of the spec with the used exchange rates pinned
     *                              ({@link DesignDecisionRecord#reproducibleSpec()}); equal to
     *                              {@code specFingerprint} when nothing was converted
     * @param pinnedRates           the exchange rates used, units of the spec currency per one unit of each key
     * @param ratesAsOf             the rates' dates, or {@code null}
     * @param ratesSource           the rates' sources, or {@code null}
     * @param sdkVersion            the SDK version that produced the record
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Reproduction(String specFingerprint, String pinnedSpecFingerprint, Map<String, BigDecimal> pinnedRates,
                               String ratesAsOf, String ratesSource, String sdkVersion) {
        /** Sorts the rates. */
        public Reproduction {
            pinnedRates = pinnedRates == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(pinnedRates));
        }
    }
}
