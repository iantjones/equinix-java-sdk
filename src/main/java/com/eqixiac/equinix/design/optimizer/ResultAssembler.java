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

package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.cost.CostComparability;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.context.LatencyFigure;
import com.eqixiac.equinix.design.optimizer.enums.RiskSeverity;
import com.eqixiac.equinix.design.optimizer.enums.ScoreCategory;
import com.eqixiac.equinix.design.optimizer.model.CostEstimate;
import com.eqixiac.equinix.design.optimizer.model.DeploymentTopology;
import com.eqixiac.equinix.design.optimizer.model.LatencyEntry;
import com.eqixiac.equinix.design.optimizer.model.LatencyMatrix;
import com.eqixiac.equinix.design.optimizer.model.MetroCostBreakdown;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.MetroScore;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.optimizer.model.OptimizationExplanation;
import com.eqixiac.equinix.design.optimizer.model.OptimizationRequest;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.optimizer.model.ProviderConnectivityMap;
import com.eqixiac.equinix.design.optimizer.model.ProviderRequirement;
import com.eqixiac.equinix.design.optimizer.model.RiskAssessment;
import com.eqixiac.equinix.design.optimizer.model.RiskFinding;
import com.eqixiac.equinix.design.optimizer.model.ScoreComponent;
import com.eqixiac.equinix.design.optimizer.model.SolveStatistics;
import com.eqixiac.equinix.design.optimizer.model.UserSite;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import com.eqixiac.equinix.design.optimizer.model.WorkloadProfile;
import com.eqixiac.equinix.design.optimizer.model.WorkloadSpec;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.placement.WorkloadPlacer;
import com.eqixiac.equinix.design.solve.SensitivityReport;
import com.eqixiac.equinix.design.solve.SolveRun;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;
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

/** Turns a solve into an {@link OptimizationResult}, including the views the Deployment Wizard reads. */
final class ResultAssembler {

    private ResultAssembler() {
    }

    static OptimizationResult assemble(CatalogDesignContext context, SolveRun run, SensitivityReport sensitivity,
                                       int minMetros, int maxMetros, RateCard rateCard, Term term,
                                       MulticloudEnvironmentCatalog environments, long startedMillis,
                                       CostComparability comparability, List<Objective> objectives) {
        DesignSpec spec = context.spec();
        DesignOption rec = run.solution().recommended();
        PlacementPlan plan = rec == null ? null : WorkloadPlacer.place(rec.portfolio(), context);
        List<MetroId> ordered = rec == null ? List.of() : orderMetros(rec, plan, context);
        OptimizationRequest request = request(spec, rateCard, term, environments);

        List<MetroRecommendation> recommendations = new ArrayList<>();
        Map<MetroId, List<ProviderAvailability>> providerMap = new LinkedHashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            MetroId metro = ordered.get(i);
            List<ProviderAvailability> providers = providers(metro, spec, context);
            providerMap.put(metro, providers);
            MetroFacts facts = context.metro(metro).orElse(null);
            Map<String, Double> latencies = new LinkedHashMap<>();
            for (SiteIntent site : spec.sites()) {
                LatencyFigure figure = context.latency(metro, site);
                latencies.put(site.label(), figure.roundTripMs());
            }
            recommendations.add(MetroRecommendation.builder()
                    .rank(i + 1).metroId(metro)
                    .metroName(facts == null ? metro.code() : facts.name())
                    .region(facts == null ? null : facts.region())
                    .coordinates(coordinates(facts))
                    .score(score(rec, run))
                    .reasons(reasons(metro, plan, context))
                    .availableProviders(providers)
                    .siteLatencies(latencies)
                    .estimatedCost(metroCost(metro, rec.cost()))
                    .assignedWorkloads(placementsIn(metro, plan, false))
                    .build());
        }
        List<WorkloadPlacement> placements = new ArrayList<>();
        if (plan != null) {
            for (MetroId metro : ordered) {
                placements.addAll(placementsIn(metro, plan, false));
            }
            for (MetroId metro : ordered) {
                placements.addAll(placementsIn(metro, plan, true));
            }
        }
        List<RiskFinding> findings = findings(context, rec, plan, run, environments);
        if (comparability != null && comparability.costWithdrawn()) {
            findings.add(0, finding(RiskSeverity.MEDIUM, "COST_COMPARISON_INCOMPLETE", comparability.headline()
                    + ". " + comparability.affected().size() + " of " + comparability.feasibleCount()
                    + " feasible design(s) are fully priced but partly in another currency (" + String.join(", ",
                    comparability.affected()) + "); cost was withdrawn from the ranking so they are not out-scored "
                    + "by designs priced in " + comparability.currency() + ".", "Use a cost model that converts "
                    + "currencies, or pin commercial.exchangeRates for " + String.join(", ",
                    comparability.missingRates()), null));
        }
        if (rec != null) {
            com.eqixiac.equinix.design.cost.CurrencyConverters.advisory(rec.cost()).ifPresent(a ->
                    findings.add(finding(RiskSeverity.INFO, "COST_CURRENCY_CONVERTED", a + ". Not a risk: pin the "
                            + "rates (OptimizationResult.specWithUsedRatesPinned()) to reproduce the run", null, null)));
        }
        if (context.costModel() instanceof com.eqixiac.equinix.design.cost.RateCardCostModel model) {
            for (String note : model.converter().notes()) {
                findings.add(finding(RiskSeverity.INFO, "FX_FALLBACK", note, null, null));
            }
            for (String note : model.notes()) {
                findings.add(finding(RiskSeverity.LOW, "PRICE_LOOKUP_FALLBACK", note, "Re-run when the live price "
                        + "search answers, or price with a rate card you hold", null));
            }
        }
        ObjectiveValue resilience = rec == null ? null : rec.values().get(Objective.RESILIENCE);
        RiskAssessment risk = new RiskAssessment(findings, overall(findings),
                resilience != null && resilience.known() ? resilience.value() : 0);

        List<String> assumptions = assumptions(context);
        OptimizationExplanation explanation = OptimizationExplanation.builder()
                .methodology(methodology(run, minMetros, maxMetros))
                .assumptions(assumptions)
                .dataFreshness("Catalogue facts gathered " + context.asOf() + ".")
                .humanReadable(human(rec, run))
                .build();

        return OptimizationResult.builder()
                .request(request)
                .recommendations(recommendations)
                .topology(new DeploymentTopology(placements))
                .providerMap(new ProviderConnectivityMap(providerMap))
                .latencyMatrix(matrix(ordered, spec, context))
                .riskAssessment(risk)
                .costEstimate(rec == null ? null : costEstimate(rec.cost(), spec))
                .explanation(explanation)
                .computedAt(Instant.now())
                .computeTimeMs(System.currentTimeMillis() - startedMillis)
                .spec(spec)
                .specFingerprint(spec.fingerprint())
                .recommended(rec)
                .frontier(run.solution().frontier())
                .constraintVerdicts(rec == null ? List.of() : rec.verdicts())
                .solverNotes(run.solution().notes())
                .solveStatistics(new SolveStatistics(run.candidates(), minMetros, maxMetros, run.solution().evaluated(),
                        run.solution().feasible(), run.solution().frontier().size(), run.solution().exact(),
                        run.elapsedMillis()))
                .sensitivity(sensitivity)
                .objectiveScales(scales(run))
                .objectiveUnits(units(objectives, run))
                .costComparability(comparability)
                .latencyCalibration(context.calibration())
                .placement(plan)
                .dataAsOf(context.asOf())
                .dataProvenance(context.dataNotes())
                .build();
    }

    private static Map<String, String> scales(SolveRun run) {
        Map<String, String> scales = new LinkedHashMap<>();
        for (int o = 0; o < run.objectiveIds().size(); o++) {
            scales.put(run.objectiveIds().get(o), run.scales()[o] == null ? null : run.scales()[o].describe());
        }
        return scales;
    }

    private static Map<String, String> units(List<Objective> objectives, SolveRun run) {
        Map<String, String> units = new LinkedHashMap<>();
        if (objectives != null) {
            for (Objective objective : objectives) {
                if (run.objectiveIds().contains(objective.id())) {
                    units.put(objective.id(), objective.unit());
                }
            }
        }
        return units;
    }

    /** Primary first: the metro hosting the most primary placements, then by site weight served, then code. */
    private static List<MetroId> orderMetros(DesignOption rec, PlacementPlan plan, CatalogDesignContext context) {
        Map<MetroId, Integer> primaries = new LinkedHashMap<>();
        Map<MetroId, Double> served = new LinkedHashMap<>();
        for (MetroId metro : rec.portfolio().metros()) {
            primaries.put(metro, 0);
            served.put(metro, 0.0);
        }
        for (PlacementPlan.Placement p : plan.placements()) {
            if (p.primary() != null) {
                primaries.merge(p.primary(), 1, Integer::sum);
            }
        }
        for (SiteIntent site : context.spec().sites()) {
            MetroId nearest = null;
            double best = Double.POSITIVE_INFINITY;
            for (MetroId metro : rec.portfolio().metros()) {
                var ms = context.roundTripLatencyMs(metro, site);
                if (ms.isPresent() && ms.getAsDouble() < best) {
                    best = ms.getAsDouble();
                    nearest = metro;
                }
            }
            if (nearest != null) {
                served.merge(nearest, context.siteWeight(site), Double::sum);
            }
        }
        List<MetroId> ordered = new ArrayList<>(rec.portfolio().metros());
        ordered.sort(Comparator.comparingInt((MetroId m) -> -primaries.get(m))
                .thenComparingDouble(m -> -served.get(m)).thenComparing(MetroId::code));
        return ordered;
    }

    private static MetroScore score(DesignOption rec, SolveRun run) {
        List<ScoreComponent> components = new ArrayList<>();
        double total = 0;
        for (double w : run.weights()) {
            total += w;
        }
        for (Map.Entry<String, ObjectiveValue> e : rec.values().entrySet()) {
            ScoreCategory category = category(e.getKey());
            Double score = rec.scores().get(e.getKey());
            if (category == null || score == null) {
                continue;
            }
            int index = run.objectiveIds().indexOf(e.getKey());
            double weight = index < 0 || total == 0 ? 0 : run.weights()[index] / total;
            components.add(ScoreComponent.builder().category(category).score(score).weight(weight)
                    .explanation(e.getValue().explanation()).build());
        }
        return new MetroScore(rec.weightedScore(), components);
    }

    private static ScoreCategory category(String objective) {
        return switch (objective) {
            case Objective.LATENCY -> ScoreCategory.LATENCY;
            case Objective.COST -> ScoreCategory.COST;
            case Objective.PROVIDER_COVERAGE -> ScoreCategory.PROVIDER_COVERAGE;
            case Objective.RESILIENCE -> ScoreCategory.REDUNDANCY;
            case Objective.COMPLIANCE -> ScoreCategory.COMPLIANCE;
            default -> null;
        };
    }

    private static List<String> reasons(MetroId metro, PlacementPlan plan, CatalogDesignContext context) {
        List<String> reasons = new ArrayList<>();
        List<String> sites = new ArrayList<>();
        for (SiteIntent site : context.spec().sites()) {
            LatencyFigure figure = context.latency(metro, site);
            if (figure.roundTripMs() != null) {
                sites.add(site.label() + " " + figure.render());
            }
        }
        if (!sites.isEmpty()) {
            reasons.add("Round trip to sites: " + String.join(", ", sites));
        }
        for (PlacementPlan.Placement p : plan.placements()) {
            if (metro.equals(p.primary())) {
                reasons.add("Runs '" + p.workload() + "': " + p.reasoning());
            } else if (metro.equals(p.standby())) {
                reasons.add("Standby for '" + p.workload() + "'");
            }
        }
        Set<String> clouds = context.reachableClouds(metro);
        if (!clouds.isEmpty()) {
            reasons.add("Reaches " + String.join(", ", clouds));
        }
        return reasons;
    }

    private static List<WorkloadPlacement> placementsIn(MetroId metro, PlacementPlan plan, boolean standby) {
        List<WorkloadPlacement> list = new ArrayList<>();
        if (plan == null) {
            return list;
        }
        for (PlacementPlan.Placement p : plan.placements()) {
            if (p.workload().startsWith(WorkloadPlacer.CLOUD_PREFIX)) {
                continue;
            }
            if (!standby && metro.equals(p.primary())) {
                list.add(WorkloadPlacement.builder().workloadLabel(p.workload()).assignedMetro(metro)
                        .reasoning(p.reasoning()).build());
            } else if (standby && metro.equals(p.standby())) {
                list.add(WorkloadPlacement.builder().workloadLabel(p.workload()).assignedMetro(metro)
                        .reasoning("standby for " + p.workload() + " (primary " + p.primary().code() + ")").build());
            }
        }
        return list;
    }

    private static List<ProviderAvailability> providers(MetroId metro, DesignSpec spec, CatalogDesignContext context) {
        List<ProviderAvailability> list = new ArrayList<>();
        Map<String, ProviderAvailability> chosen = context.providerAvailability(metro);
        for (CloudIntent cloud : spec.clouds()) {
            ProviderAvailability availability = chosen.get(cloud.id());
            if (availability != null) {
                list.add(availability);
            } else {
                list.add(ProviderAvailability.builder().providerLabel(cloud.id())
                        .available(context.reachableClouds(metro).contains(cloud.id()))
                        .sellerRegions(List.of()).profileOptions(List.of()).build());
            }
        }
        return list;
    }

    private static MetroCostBreakdown metroCost(MetroId metro, PortfolioCost cost) {
        if (cost == null) {
            return null;
        }
        Map<String, BigDecimal> items = new LinkedHashMap<>();
        BigDecimal monthly = BigDecimal.ZERO;
        BigDecimal oneTime = BigDecimal.ZERO;
        Set<String> currencies = new LinkedHashSet<>();
        Set<PriceSource> sources = new LinkedHashSet<>();
        for (CostLine line : cost.lines()) {
            if (metro.equals(line.metro())) {
                items.put(line.label(), line.monthly());
                monthly = monthly.add(line.monthly());
                oneTime = oneTime.add(line.oneTime());
                currencies.add(line.currency());
                sources.add(line.source());
            }
        }
        if (items.isEmpty() || currencies.size() != 1) {
            return null;
        }
        return new MetroCostBreakdown(metro, monthly, oneTime, items, currencies.iterator().next(),
                sources.size() == 1 ? sources.iterator().next() : PriceSource.COMPOSITE);
    }

    private static CostEstimate costEstimate(PortfolioCost cost, DesignSpec spec) {
        if (cost == null) {
            return null;
        }
        List<MetroCostBreakdown> perMetro = new ArrayList<>();
        Set<MetroId> metros = new LinkedHashSet<>();
        Map<String, BigDecimal> byCurrency = new TreeMap<>();
        Set<PriceSource> sources = new LinkedHashSet<>();
        for (CostLine line : cost.lines()) {
            if (line.metro() != null) {
                metros.add(line.metro());
            }
            byCurrency.merge(line.currency(), line.monthly(), BigDecimal::add);
            sources.add(line.source());
        }
        for (MetroId metro : metros) {
            MetroCostBreakdown b = metroCost(metro, cost);
            if (b != null) {
                perMetro.add(b);
            }
        }
        BigDecimal budget = spec.commercial().budgetMaxMonthly();
        String budgetCurrency = spec.commercial().currencyOrDefault();
        boolean within = budget == null || cost.monthlyTotal().isEmpty()
                || !cost.currency().map(budgetCurrency::equals).orElse(false)
                || cost.monthlyTotal().get().compareTo(budget) <= 0;
        StringBuilder disclaimer = new StringBuilder("Priced line by line by the run's cost model; ");
        disclaimer.append(cost.monthlyLow().isPresent() ? "plausible range " + cost.monthlyLow().get().toPlainString()
                + "-" + cost.monthlyHigh().get().toPlainString() + " per month; " : "");
        disclaimer.append(cost.isComplete() ? "nothing unpriced." : cost.unpriced().size()
                + " item(s) unpriced, so the total understates the cost.");
        return CostEstimate.builder()
                .monthlyTotal(cost.monthlyTotal().orElse(null))
                .setupTotal(cost.oneTimeTotal().orElse(null))
                .currency(cost.currency().orElse(null))
                .monthlyByCurrency(byCurrency)
                .perMetro(perMetro)
                .withinBudget(within)
                .costDisclaimer(disclaimer.toString())
                .source(sources.size() == 1 ? sources.iterator().next() : PriceSource.COMPOSITE)
                .build();
    }

    private static LatencyMatrix matrix(List<MetroId> metros, DesignSpec spec, CatalogDesignContext context) {
        List<String> labels = new ArrayList<>();
        for (SiteIntent site : spec.sites()) {
            labels.add(site.label());
        }
        List<List<LatencyEntry>> rows = new ArrayList<>();
        for (MetroId metro : metros) {
            List<LatencyEntry> row = new ArrayList<>();
            for (SiteIntent site : spec.sites()) {
                LatencyFigure figure = context.latency(metro, site);
                row.add(new LatencyEntry(metro, site.label(),
                        figure.roundTripMs() == null ? Double.NaN : figure.roundTripMs(), !figure.isMeasured()));
            }
            rows.add(row);
        }
        return new LatencyMatrix(new ArrayList<>(metros), labels, rows);
    }

    private static List<RiskFinding> findings(CatalogDesignContext context, DesignOption rec, PlacementPlan plan,
                                              SolveRun run, MulticloudEnvironmentCatalog environments) {
        DesignSpec spec = context.spec();
        List<RiskFinding> findings = new ArrayList<>();
        if (rec == null) {
            findings.add(finding(RiskSeverity.CRITICAL, "NO_FEASIBLE_DESIGN", "No design satisfies every constraint: "
                    + String.join(" ", run.solution().notes()), "Relax a constraint; the notes name the one that "
                    + "rejected the most designs", null));
            return findings;
        }
        for (ConstraintVerdict verdict : rec.verdicts()) {
            if (verdict.reason() != null && verdict.reason().contains("NOT applied")) {
                findings.add(finding(RiskSeverity.MEDIUM, "LATENCY_BOUND_NOT_EVALUATED", "The latency limit "
                        + verdict.reason() + "; it could not be evaluated, so it excluded nothing",
                        "Give sites a metro or coordinates", null));
            }
            if (verdict.reason() != null && verdict.reason().startsWith("budget not evaluable")) {
                findings.add(finding(RiskSeverity.MEDIUM, "BUDGET_NOT_EVALUABLE", verdict.reason(),
                        "State the budget in the currency the design is priced in", null));
            }
        }
        if (rec.portfolio().size() == 1) {
            findings.add(finding(RiskSeverity.HIGH, "SINGLE_POINT_OF_FAILURE", "The design uses one metro ("
                    + rec.portfolio().key() + "); losing it loses everything",
                    "Set constraints.minimumRedundancy or weight resilience higher", rec.portfolio().metros().get(0)));
        }
        if (rec.cost() != null && !CostComparability.missingRates(rec.cost(), spec.commercial().currencyOrDefault())
                .isEmpty()) {
            findings.add(finding(RiskSeverity.MEDIUM, "COST_MIXED_CURRENCY", "The design is priced in "
                    + rec.cost().currencies() + " but costs are compared in " + spec.commercial().currencyOrDefault()
                    + "; amounts in different currencies are not added, so its total and the cost objective are unknown",
                    "Supply commercial.exchangeRates for " + String.join(", ", CostComparability.missingRates(
                    rec.cost(), spec.commercial().currencyOrDefault())), null));
        }
        if (rec.cost() != null && !rec.cost().isComplete()) {
            findings.add(finding(RiskSeverity.MEDIUM, "COST_INCOMPLETE", rec.cost().unpriced().size()
                    + " item(s) could not be priced: " + String.join("; ", rec.cost().unpriced()),
                    "Size every workload and supply a rate card that covers these items", null));
        }
        for (PlacementPlan.Placement p : plan.placements()) {
            for (String issue : p.issues()) {
                findings.add(finding(RiskSeverity.HIGH, "PLACEMENT_GAP", "'" + p.workload() + "': " + issue, null,
                        p.primary()));
            }
        }
        for (CloudIntent cloud : spec.clouds()) {
            boolean anywhere = false;
            for (MetroFacts metro : context.metros()) {
                anywhere |= context.reachableClouds(metro.id()).contains(cloud.id());
            }
            if (!anywhere) {
                findings.add(finding(Boolean.TRUE.equals(cloud.required()) ? RiskSeverity.CRITICAL : RiskSeverity.MEDIUM,
                        "PROVIDER_UNRESOLVED", "Cloud '" + cloud.id() + "' is reachable from no metro in the catalogue",
                        "Check the provider, profile name or uuid", null));
            }
        }
        findings.addAll(nativeMulticloud(spec, environments));
        return findings;
    }

    private static List<RiskFinding> nativeMulticloud(DesignSpec spec, MulticloudEnvironmentCatalog environments) {
        List<RiskFinding> findings = new ArrayList<>();
        MulticloudEnvironmentCatalog catalog = environments != null ? environments : MulticloudEnvironmentCatalog.standard();
        if (catalog.isEmpty()) {
            return findings;
        }
        for (WorkloadIntent workload : spec.workloads()) {
            Map<CloudProviderType, CloudIntent> clouds = new TreeMap<>(Comparator.comparing(CloudProviderType::shortCode));
            for (String id : workload.clouds()) {
                CloudIntent cloud = spec.cloud(id);
                if (cloud != null && cloud.provider() != null && cloud.provider() != CloudProviderType.OTHER) {
                    clouds.putIfAbsent(cloud.provider(), cloud);
                }
            }
            List<CloudProviderType> list = new ArrayList<>(clouds.keySet());
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    CloudProviderType a = list.get(i);
                    CloudProviderType z = list.get(j);
                    List<MulticloudEnvironment> pairs = catalog.regionsFor(a, z);
                    if (pairs.isEmpty()) {
                        continue;
                    }
                    MulticloudEnvironment matched = catalog.find(a, clouds.get(a).region(), z, clouds.get(z).region())
                            .orElse(null);
                    String environmentsText = matched != null
                            ? "the environment " + matched.describe() + ", which matches the regions in this spec"
                            : pairs.size() + " environment(s) for the pair, none matched to a region in this spec";
                    findings.add(finding(RiskSeverity.INFO, "NATIVE_MULTICLOUD_ALTERNATIVE", "Workload '"
                            + workload.label() + "' depends on " + a.getProviderName() + " and " + z.getProviderName()
                            + ". The multicloud environment catalog" + (catalog.asOf() != null ? " (as of "
                            + catalog.asOf() + ")" : "") + " lists " + environmentsText + ". A native "
                            + "provider-to-provider link carries traffic between the two clouds without an Equinix "
                            + "metro. Informational: not a risk", "Plan with the Deployment Wizard's default "
                            + "CloudToCloudStrategy.COMPARE to price both paths", null));
                }
            }
        }
        return findings;
    }

    private static RiskFinding finding(RiskSeverity severity, String category, String description,
                                       String recommendation, MetroId metro) {
        return RiskFinding.builder().severity(severity).category(category).description(description)
                .recommendation(recommendation).affectedMetro(metro).build();
    }

    private static RiskSeverity overall(List<RiskFinding> findings) {
        RiskSeverity worst = RiskSeverity.INFO;
        for (RiskFinding f : findings) {
            if (f.getSeverity().ordinal() < worst.ordinal()) {
                worst = f.getSeverity();
            }
        }
        return worst;
    }

    private static List<String> assumptions(CatalogDesignContext context) {
        List<String> list = new ArrayList<>();
        list.add("All latencies are round trip. A site in the metro it names is assumed "
                + CatalogDesignContext.INTRA_METRO_RTT_MS + " ms from it; measured pairs use Fabric's "
                + "connectedMetros.avgLatency; others are estimated: " + context.calibration().describe());
        list.addAll(context.siteWeightingNotes());
        list.add("Each workload runs in one metro; in a design of two or more metros its cloud connections are "
                + "duplicated at full size in a standby metro (active/standby).");
        list.add("Cloud connections are Fabric virtual connections from a Cloud Router in each metro that carries "
                + "one; customer ports, colocation space and power, and site access circuits are not priced.");
        list.add("Data sent into a cloud is not charged by the providers; only data leaving a cloud is priced.");
        list.add("A metro that carries no workload and no cloud connection is not a design option "
                + "(every_metro_used).");
        return list;
    }

    private static String methodology(SolveRun run, int minMetros, int maxMetros) {
        return String.format(Locale.ROOT, "Every set of %d to %d metros the constraints admit was evaluated on "
                        + "latency, cost, provider coverage, resilience and compliance (%s). Each objective is scored "
                        + "0-100 on a fixed scale (cost by ratio to the cheapest design, latency against the latency "
                        + "ceiling or 200 ms, percentages as they are) and combined with the spec's weights; the Pareto frontier "
                        + "lists every design no other beats on all objectives. Hard limits (including any "
                        + "max-latency bound) reject designs outright; a limit that could not be evaluated is reported "
                        + "as NOT applied.", minMetros, maxMetros,
                run.solution().exact() ? "exhaustive search" : "search over a narrowed field");
    }

    private static String human(DesignOption rec, SolveRun run) {
        if (rec == null) {
            return "No design satisfies every constraint. " + String.join(" ", run.solution().notes());
        }
        StringBuilder sb = new StringBuilder("Recommended " + rec.portfolio().key() + ". ");
        for (ConstraintVerdict v : rec.verdicts()) {
            if (v.reason() != null && v.reason().contains("NOT applied")) {
                sb.append("A latency limit could not be evaluated. ");
            }
        }
        return sb.toString().trim();
    }

    private static GeoCoordinate coordinates(MetroFacts facts) {
        if (facts == null || !facts.hasCoordinates()) {
            return null;
        }
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("latitude", facts.latitude());
        map.put("longitude", facts.longitude());
        return Constants.mapper().convertValue(map, GeoCoordinate.class);
    }

    static OptimizationRequest request(DesignSpec spec, RateCard rateCard, Term term,
                                       MulticloudEnvironmentCatalog environments) {
        List<UserSite> sites = new ArrayList<>();
        for (SiteIntent s : spec.sites()) {
            sites.add(UserSite.builder().label(s.label()).nearestMetro(s.metro() == null ? null : MetroId.of(s.metro()))
                    .latitude(s.latitude()).longitude(s.longitude()).role(s.role())
                    .headcount(s.headcount() == null ? 0 : s.headcount()).weight(s.weight() == null ? 0 : s.weight())
                    .build());
        }
        Map<String, ProviderRequirement> providers = new LinkedHashMap<>();
        for (CloudIntent c : spec.clouds()) {
            providers.put(c.id(), ProviderRequirement.builder().cloudProvider(c.provider())
                    .serviceProfileName(c.serviceProfileName()).serviceProfileUuid(c.serviceProfileUuid())
                    .label(c.id()).preferredSellerRegions(c.region() == null ? List.of() : List.of(c.region()))
                    .required(Boolean.TRUE.equals(c.required())).build());
        }
        List<WorkloadSpec> workloads = new ArrayList<>();
        for (WorkloadIntent w : spec.workloads()) {
            List<ProviderRequirement> deps = new ArrayList<>();
            for (String id : w.clouds()) {
                if (providers.containsKey(id)) {
                    deps.add(providers.get(id));
                }
            }
            workloads.add(WorkloadSpec.builder().label(w.label()).type(w.type())
                    .latencySensitivity(w.latencySensitivity())
                    .bandwidthMbps(w.bandwidthMbps() == null ? 0 : w.bandwidthMbps())
                    .profileOverride(w.maxLatencyMs() == null ? null
                            : WorkloadProfile.builder().maxLatencyToleranceMs(w.maxLatencyMs()).build())
                    .dependsOnProviders(deps).build());
        }
        return OptimizationRequest.builder().sites(sites).providers(new ArrayList<>(providers.values()))
                .workloads(workloads).rateCard(rateCard).term(term).multicloudEnvironments(environments).build();
    }
}
