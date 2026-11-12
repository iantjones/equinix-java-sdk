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
import com.eqixiac.equinix.design.cost.GrowthPlan;
import com.eqixiac.equinix.design.cost.GrowthPlanner;
import com.eqixiac.equinix.design.cost.ScenarioBand;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.context.FabricCatalogLoader;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.design.value.tco.DeploymentArchetype;
import com.eqixiac.equinix.design.value.tco.TcoCalculator;
import com.eqixiac.equinix.design.value.tco.TcoComparison;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.eqixiac.equinix.mcp.server.Args.enumValue;
import static com.eqixiac.equinix.mcp.server.Args.optInt;
import static com.eqixiac.equinix.mcp.server.Args.optLong;
import static com.eqixiac.equinix.mcp.server.Args.optNumber;
import static com.eqixiac.equinix.mcp.server.Args.optString;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.Detail;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.capped;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.money;
import static com.eqixiac.equinix.mcp.server.Schemas.array;
import static com.eqixiac.equinix.mcp.server.Schemas.detail;
import static com.eqixiac.equinix.mcp.server.Schemas.integer;
import static com.eqixiac.equinix.mcp.server.Schemas.lowerNames;
import static com.eqixiac.equinix.mcp.server.Schemas.number;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.objectOrString;
import static com.eqixiac.equinix.mcp.server.Schemas.props;
import static com.eqixiac.equinix.mcp.server.Schemas.string;
import static com.eqixiac.equinix.mcp.server.Schemas.stringEnum;
import static com.eqixiac.equinix.mcp.server.Schemas.with;

/**
 * The value tools over a {@code DesignSpec}: growth and commitment terms, TCO and cloud-egress savings.
 * Each prices in the spec's {@code commercial.currency} through the Equinix rate-card chain under the
 * call's {@link PricingBudget} (live Equinix prices, then dated reference figures), converts other
 * currencies with the spec's pinned rates or the default converter, and reports anything it could not
 * price as unpriced with the reason, never as 0. All read-only.
 */
final class ValueTools {

    private ValueTools() {
    }

    // design_growth_and_terms

    static ToolRegistration growthAndTerms() {
        return ToolRegistration.builder()
                .name("design_growth_and_terms")
                .title("Project growth and compare commitment terms for a design")
                .description("Projects the design's monthly cost over commercial.horizonMonths (default 12) at "
                        + "commercial.annualGrowthRate (default 0%): per connection and Cloud Router, the bandwidth "
                        + "tier each month and the month an upgrade is needed, and each commitment term (on demand, "
                        + "12, 24, 36 months) priced over the horizon with its break-even month and a recommended "
                        + "term. A term is priced only from a live or caller price for that term; a reference figure "
                        + "has no term axis, so such an option is unpriced with the reason. Optional scenarios add "
                        + "low/high growth cases and a seeded Monte Carlo (p10/p50/p90). The design is 'portfolio', "
                        + "else the stored design_optimize recommendation for the spec, else the optimizer runs first. "
                        + "READ-ONLY; reads the live catalogue and prices. Cancellable.")
                .inputSchema(object(props(
                                SpecInput.ARG, objectOrString(SpecTools.SPEC_ARG_DESCRIPTION),
                                SpecInput.FINGERPRINT_ARG, DesignToolFactory.fingerprintSchema(),
                                "portfolio", DesignToolFactory.portfolioSchema("project"),
                                "scenarios", object(props(
                                        "low_growth", with(number("Low annual growth, fraction (e.g. 0.05); must be "
                                                + "<= the spec's annualGrowthRate."), "exclusiveMinimum", -1),
                                        "high_growth", with(number("High annual growth, fraction (e.g. 0.6); must be "
                                                + ">= the spec's annualGrowthRate."), "exclusiveMinimum", -1),
                                        "monte_carlo_runs", with(integer("Seeded Monte Carlo runs (growth drawn "
                                                + "triangular between low, base and high). Omit for none."),
                                                "minimum", 1, "maximum", 20000),
                                        "seed", integer("Monte Carlo seed (required with monte_carlo_runs: results "
                                                + "are reproducible)."),
                                        "egress_low", with(number("Low egress-volume factor, 0 < x <= 1."),
                                                "exclusiveMinimum", 0, "maximum", 1),
                                        "egress_high", with(number("High egress-volume factor, >= 1."),
                                                "minimum", 1)),
                                        "low_growth", "high_growth"),
                                "detail", detail("summary", "standard adds the month-by-month tiers and on-demand "
                                        + "totals."))))
                .outputSchema(DesignPayloads.outputSchema("portfolio, growth, horizon, items (tiers, upgrade month, "
                        + "term options, recommendation), on_demand_total, unpriced, assumptions, scenarios, "
                        + "spec_fingerprint, provenance.", "items", "spec_fingerprint"))
                .toolset(Toolset.DESIGN)
                .handler((ToolHandler.Cancellable) ValueTools::handleGrowth)
                .build();
    }

    private static ObjectNode handleGrowth(JsonNode args, ServerContext ctx, CancellationSignal cancellation)
            throws Exception {
        DesignToolFactory.SpecAndRun input = DesignToolFactory.specOf(args, ctx);
        DesignSpec spec = input.spec();
        Detail detail = Detail.of(args, Detail.SUMMARY);
        PricingBudget budget = ctx.pricingBudget();
        List<TimeoutRateCard> guards = new ArrayList<>();
        FabricGateway fabric = ctx.fabric();
        Chosen chosen = choosePortfolio(args, input, ctx, budget, guards, cancellation, true);
        RateCard card = TimeoutRateCard.equinixChain(fabric, budget, guards);
        JsonNode scenarios = args.get("scenarios");
        GrowthPlanner.MonteCarlo monteCarlo = monteCarlo(scenarios);
        Result result = EngineRunner.run("the growth projection", cancellation, () -> {
            CatalogDesignContext context = FabricCatalogLoader.load(fabric, spec, null);
            GrowthPlanner planner = GrowthPlanner.forSpec(spec, chosen.portfolio(), context, card,
                    ReferenceRateCard.standard());
            GrowthPlan plan = planner.plan();
            ScenarioBand band = null;
            if (scenarios != null && scenarios.isObject()) {
                band = planner.scenarios(scenarios.path("low_growth").asDouble(),
                        scenarios.path("high_growth").asDouble(), monteCarlo);
            }
            return new Result(plan, band);
        });

        GrowthPlan plan = result.plan();
        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("spec_fingerprint", plan.specFingerprint());
        payload.put("portfolio", chosen.portfolio().key());
        payload.put("portfolio_source", chosen.source());
        payload.put("currency", plan.currency());
        payload.put("annual_growth", plan.annualGrowth());
        payload.put("horizon_months", plan.horizonMonths());
        capped(payload, "items", plan.items(), detail.cap() * 2, (array, item) -> {
            ObjectNode n = array.addObject();
            UntrustedText.put(n, "label", item.label());
            n.put("kind", item.kind());
            if (!item.tierByMonth().isEmpty()) {
                n.put("start_tier_mbps", item.tierByMonth().get(0));
                n.put("end_tier_mbps", item.tierByMonth().get(item.tierByMonth().size() - 1));
            }
            if (item.upgradeMonth() != null) {
                n.put("upgrade_month", item.upgradeMonth());
            }
            if (item.upgradeNote() != null) {
                n.put("upgrade_note", item.upgradeNote());
            }
            if (detail.atLeast(Detail.STANDARD)) {
                ArrayNode tiers = n.putArray("tier_by_month_mbps");
                item.tierByMonth().forEach(tiers::add);
            }
            ArrayNode options = n.putArray("term_options");
            item.options().forEach(o -> {
                ObjectNode on = options.addObject();
                on.put("term", o.term().name());
                on.put("priced", o.priced());
                if (o.priced()) {
                    money(on, "monthly_at_start", o.monthlyAtStart());
                    money(on, "one_time", o.oneTime());
                    money(on, "total_over_horizon", o.totalOverHorizon());
                    if (o.tailMonths() > 0) {
                        on.put("tail_months_beyond_horizon", o.tailMonths());
                        money(on, "tail", o.tail());
                    }
                    if (o.breakEvenMonth() != null) {
                        on.put("break_even_month", o.breakEvenMonth());
                    }
                } else {
                    UntrustedText.put(on, "unpriced_reason", o.unpricedReason());
                }
            });
            n.put("recommended_term", item.recommended() == null ? null : item.recommended().name());
            UntrustedText.put(n, "recommendation", item.recommendation());
        });
        money(payload, "on_demand_total_over_horizon", plan.onDemandTotal());
        if (detail.atLeast(Detail.STANDARD)) {
            ArrayNode monthly = payload.putArray("monthly_on_demand");
            plan.monthlyOnDemand().forEach(m -> {
                if (m == null) {
                    monthly.addNull();
                } else {
                    monthly.add(m.setScale(2, java.math.RoundingMode.HALF_UP));
                }
            });
        }
        DesignPayloads.strings(payload, "unpriced", plan.unpriced(), detail.cap(), true);
        DesignPayloads.strings(payload, "assumptions", plan.assumptions(), detail.cap(), true);
        if (result.band() != null) {
            ScenarioBand band = result.band();
            ObjectNode s = payload.putObject("scenarios");
            ArrayNode cases = s.putArray("cases");
            band.cases().forEach(c -> {
                ObjectNode cn = cases.addObject();
                cn.put("name", c.name());
                cn.put("annual_growth", c.annualGrowth());
                cn.put("egress_factor", c.egressFactor());
                money(cn, "total_over_horizon", c.totalOverHorizon());
                money(cn, "final_monthly", c.finalMonthly());
            });
            if (band.simulation() != null) {
                ObjectNode sim = s.putObject("monte_carlo");
                sim.put("seed", band.simulation().seed());
                sim.put("runs", band.simulation().iterations());
                sim.put("method", band.simulation().method());
                money(sim, "p10", band.simulation().p10());
                money(sim, "p50", band.simulation().p50());
                money(sim, "p90", band.simulation().p90());
                sim.put("unpriced_runs", band.simulation().unpricedRuns());
            }
            DesignPayloads.strings(s, "assumptions", band.assumptions(), detail.cap(), true);
        }
        DesignToolFactory.reportEvicted(payload, chosen.evicted(), DesignStore.Kind.OPTIMIZATION);
        TimeoutRateCard.reportEquinix(payload, guards, budget);
        DesignPayloads.provenance(payload, "mixed", null, plan.specFingerprint(),
                List.of("Equinix prices per term (live, under the call's pricing budget)",
                        "provider hosted-connection fees: reference table " + ReferenceRateCard.standard().asOf(),
                        "growth projection: computed from commercial.annualGrowthRate"));
        return payload;
    }

    private record Result(GrowthPlan plan, ScenarioBand band) {
    }

    private static GrowthPlanner.MonteCarlo monteCarlo(JsonNode scenarios) {
        if (scenarios == null || !scenarios.isObject()) {
            return null;
        }
        if (!scenarios.path("low_growth").isNumber() || !scenarios.path("high_growth").isNumber()) {
            throw new IllegalArgumentException("'scenarios' needs low_growth and high_growth (fractions around the "
                    + "spec's commercial.annualGrowthRate).");
        }
        Optional<Integer> runs = optInt(scenarios, "monte_carlo_runs");
        if (runs.isEmpty()) {
            return null;
        }
        Optional<Long> seed = optLong(scenarios, "seed");
        if (seed.isEmpty()) {
            throw new IllegalArgumentException("'scenarios.seed' is required with monte_carlo_runs: randomness "
                    + "without a seed is not offered, so results stay reproducible.");
        }
        return new GrowthPlanner.MonteCarlo(seed.get(), runs.get(), optNumber(scenarios, "egress_low").orElse(null),
                optNumber(scenarios, "egress_high").orElse(null));
    }

    /**
     * The portfolio a value tool prices.
     *
     * @param portfolio the portfolio, or {@code null} when none is known
     * @param source    where it came from
     * @param evicted   store entries evicted when this call stored an optimization
     */
    record Chosen(MetroPortfolio portfolio, String source, List<String> evicted) {
    }

    static Chosen choosePortfolio(JsonNode args, DesignToolFactory.SpecAndRun input, ServerContext ctx,
                                  PricingBudget budget, List<TimeoutRateCard> guards, CancellationSignal cancellation,
                                  boolean runOptimizerIfMissing) throws Exception {
        Optional<MetroPortfolio> given = DesignToolFactory.portfolio(args);
        if (given.isPresent()) {
            return new Chosen(given.get(), "given", List.of());
        }
        Optional<MetroPortfolio> stored = DesignToolFactory.recommended(input.optimized());
        if (stored.isPresent()) {
            return new Chosen(stored.get(), "stored design_optimize recommendation", List.of());
        }
        if (input.optimized() != null) {
            throw new IllegalArgumentException("The stored optimization found no feasible design for this spec; give "
                    + "'portfolio' or relax the constraints.");
        }
        if (!runOptimizerIfMissing) {
            return new Chosen(null, "none: no portfolio given and no stored design_optimize result for this spec",
                    List.of());
        }
        DesignToolFactory.Optimized run = DesignToolFactory.optimize(input.spec(), ctx, budget, guards, cancellation);
        Optional<MetroPortfolio> rec = DesignToolFactory.recommended(run.result());
        if (rec.isEmpty()) {
            throw new IllegalArgumentException("The optimizer found no feasible design for this spec; give "
                    + "'portfolio' or relax the constraints (see design_optimize findings).");
        }
        return new Chosen(rec.get(), "design_optimize run by this call (now stored)", run.evicted());
    }

    // design_estimate_tco

    static ToolRegistration estimateTco() {
        return ToolRegistration.builder()
                .name("design_estimate_tco")
                .title("Compare total cost of ownership for a DesignSpec's cloud")
                .description("Runs the TCO engine for one cloud of the spec (cloud_id, default the first cloud with a "
                        + "provider): the same traffic as public cloud over the internet, on-prem, and Equinix private "
                        + "interconnect (an IP_VC from a STANDARD Fabric Cloud Router in the design's first metro), "
                        + "monthly and setup, each line with its price provenance, in commercial.currency at "
                        + "commercial.term (on demand when unset). Egress volume comes from the cloud's "
                        + "monthlyEgressGb and bandwidth from the workloads reaching it (rounded up to a Fabric tier). "
                        + "Beta, cloud-to-cloud: peer_cloud_id (another cloud of the spec) makes it two-sided and adds "
                        + "the native multicloud interconnect archetype. An archetype with an unpriced part is "
                        + "fully_priced=false and never recommended. The metro is 'portfolio', else the stored "
                        + "design_optimize recommendation, else none (reference prices, said in assumptions). "
                        + "READ-ONLY; reads live Equinix prices under the call's pricing budget.")
                .inputSchema(object(props(
                                SpecInput.ARG, objectOrString(SpecTools.SPEC_ARG_DESCRIPTION),
                                SpecInput.FINGERPRINT_ARG, DesignToolFactory.fingerprintSchema(),
                                "cloud_id", string("The spec cloud (clouds[].id) to compare. Default: the first cloud "
                                        + "with a provider."),
                                "portfolio", DesignToolFactory.portfolioSchema("price"),
                                "peer_cloud_id", string("Beta. Another cloud of the spec: makes the comparison "
                                        + "cloud-to-cloud (reverse volume assumed equal)."),
                                "path_tier", with(integer("Beta. AWS connectivity-scope tier of the native link, 1 "
                                        + "local (default) to 5 maximum scope. Requires peer_cloud_id."),
                                        "minimum", 1, "maximum", 5),
                                "archetypes", array("Restrict the comparison to these archetypes "
                                                + "(native_multicloud_interconnect requires peer_cloud_id).",
                                        stringEnum("Deployment archetype.", lowerNames(DeploymentArchetype.class))),
                                "power_kw", with(number("On-prem power draw, kW (default 5)."), "exclusiveMinimum", 0),
                                "on_prem_transit_per_mbps_month", with(number("Override: on-prem IP transit per Mbps per "
                                        + "month, in commercial.currency."), "minimum", 0),
                                "on_prem_hardware_monthly", with(number("Override: amortised on-prem hardware per "
                                        + "month."), "minimum", 0),
                                "on_prem_cross_connect_monthly", with(number("Override: on-prem cross-connect per "
                                        + "month."), "minimum", 0),
                                "on_prem_power_per_kw_month", with(number("Override: on-prem power per kW per month."),
                                        "minimum", 0))))
                .outputSchema(DesignPayloads.outputSchema("breakdowns per archetype (totals, line items, fully_priced, "
                        + "unpriced, note), recommended and baseline archetype, savings, assumptions, "
                        + "spec_fingerprint, provenance.", "breakdowns", "spec_fingerprint"))
                .toolset(Toolset.DESIGN)
                .handler(ValueTools::handleTco)
                .build();
    }

    private static ObjectNode handleTco(JsonNode args, ServerContext ctx) throws Exception {
        DesignToolFactory.SpecAndRun input = DesignToolFactory.specOf(args, ctx);
        DesignSpec spec = input.spec();
        PricingBudget budget = ctx.pricingBudget();
        List<TimeoutRateCard> guards = new ArrayList<>();
        Chosen chosen = choosePortfolio(args, input, ctx, budget, guards, CancellationSignal.none(), false);
        String cloudId = optString(args, "cloud_id").orElse(null);
        TcoCalculator.Builder builder;
        try {
            builder = TcoCalculator.forSpec(ctx.fabric(), spec, cloudId, chosen.portfolio());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + ". Declared clouds: " + cloudIds(spec) + ".");
        }
        builder.rateCard(TimeoutRateCard.equinixChain(ctx.fabric(), budget, guards));
        Optional<String> peerId = optString(args, "peer_cloud_id");
        if (peerId.isPresent()) {
            CloudIntent peer = spec.cloud(peerId.get());
            if (peer == null || peer.provider() == null) {
                throw new IllegalArgumentException("'peer_cloud_id' '" + peerId.get() + "' must be a cloud of the spec "
                        + "that names a provider. Declared clouds: " + cloudIds(spec) + ".");
            }
            CloudIntent from = cloudId != null ? spec.cloud(cloudId)
                    : spec.clouds().stream().filter(c -> c.provider() != null).findFirst().orElseThrow();
            if (peer.provider() == from.provider()) {
                throw new IllegalArgumentException("'peer_cloud_id' must name a cloud of a different provider than "
                        + "'cloud_id' (both are " + peer.provider() + ").");
            }
            builder.toCloud(peer.provider());
            if (peer.region() != null) {
                builder.toRegion(peer.region());
            }
            Optional<Integer> tier = optInt(args, "path_tier");
            if (tier.isPresent()) {
                if (tier.get() < 1 || tier.get() > 5) {
                    throw new IllegalArgumentException("'path_tier' must be between 1 and 5: " + tier.get() + ".");
                }
                builder.pathTier(tier.get());
            }
        } else if (args.has("path_tier")) {
            throw new IllegalArgumentException("'path_tier' requires 'peer_cloud_id': it prices the native multicloud "
                    + "link of a cloud-to-cloud comparison.");
        }
        List<String> archetypes = Args.stringList(args, "archetypes");
        if (peerId.isEmpty() && archetypes.stream().anyMatch(a -> a.equalsIgnoreCase(
                DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT.name()))) {
            throw new IllegalArgumentException("'archetypes' value 'native_multicloud_interconnect' requires "
                    + "'peer_cloud_id'.");
        }
        if (!archetypes.isEmpty()) {
            builder.archetypes(archetypes.stream().map(a -> enumValue(DeploymentArchetype.class, a, "archetypes"))
                    .toArray(DeploymentArchetype[]::new));
        }
        optNumber(args, "power_kw").ifPresent(builder::powerKw);
        optNumber(args, "on_prem_transit_per_mbps_month")
                .ifPresent(v -> builder.onPremTransitPerMbpsMonth(BigDecimal.valueOf(v)));
        optNumber(args, "on_prem_hardware_monthly").ifPresent(v -> builder.onPremHardwareMonthly(BigDecimal.valueOf(v)));
        optNumber(args, "on_prem_cross_connect_monthly")
                .ifPresent(v -> builder.onPremCrossConnectMonthly(BigDecimal.valueOf(v)));
        optNumber(args, "on_prem_power_per_kw_month")
                .ifPresent(v -> builder.onPremPowerPerKwMonth(BigDecimal.valueOf(v)));

        TcoComparison comparison = builder.compare();
        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("spec_fingerprint", comparison.getSpecFingerprint() != null ? comparison.getSpecFingerprint()
                : spec.fingerprint());
        payload.put("portfolio", chosen.portfolio() == null ? null : chosen.portfolio().key());
        payload.put("portfolio_source", chosen.source());
        ArrayNode breakdowns = payload.putArray("breakdowns");
        if (comparison.getBreakdowns() != null) {
            comparison.getBreakdowns().forEach(b -> {
                ObjectNode bn = breakdowns.addObject();
                bn.put("archetype", String.valueOf(b.getArchetype()).toLowerCase(Locale.ROOT));
                money(bn, "monthly_total", b.getMonthlyTotal());
                money(bn, "setup_total", b.getSetupTotal());
                money(bn, "total_over_term", b.getTotalOverTerm());
                bn.put("currency", b.getCurrency());
                bn.put("fully_priced", b.isPriced());
                ObjectNode items = bn.putObject("line_items");
                if (b.getLineItems() != null) {
                    b.getLineItems().forEach((k, v) -> money(items, k, v));
                }
                DesignPayloads.strings(bn, "unpriced", b.getUnpriced(), 10, true);
                UntrustedText.put(bn, "note", b.getNote());
                if (b.getProvenance() != null) {
                    DesignPayloads.strings(bn, "provenance", b.getProvenance(), 10, true);
                }
            });
        }
        if (comparison.getTrafficNote() != null) {
            payload.put("traffic_note", comparison.getTrafficNote());
        }
        payload.put("recommended_archetype", lower(comparison.getRecommended()));
        payload.put("baseline_archetype", lower(comparison.getBaseline()));
        UntrustedText.put(payload, "recommendation_note", comparison.getRecommendationNote());
        money(payload, "monthly_savings_vs_baseline", comparison.getMonthlySavingsVsBaseline());
        money(payload, "annual_savings_vs_baseline", comparison.getAnnualSavingsVsBaseline());
        payload.put("term", comparison.getTerm() == null ? null : comparison.getTerm().name());
        payload.put("currency", comparison.getCurrency());
        DesignPayloads.strings(payload, "assumptions", comparison.getAssumptions(), 20, true);
        payload.put("disclaimer", comparison.getDisclaimer());
        TimeoutRateCard.reportEquinix(payload, guards, budget);
        DesignPayloads.provenance(payload, "mixed", comparison.getAsOf(), payload.get("spec_fingerprint").asText(),
                List.of("Equinix prices (live under the call's pricing budget, else reference "
                        + ReferenceRateCard.standard().asOf() + ")", "cloud egress and port fees: reference table",
                        "on-prem: bundled reference midpoints unless overridden"));
        return payload;
    }

    private static String lower(Object value) {
        return value == null ? null : String.valueOf(value).toLowerCase(Locale.ROOT);
    }

    private static List<String> cloudIds(DesignSpec spec) {
        return spec.clouds().stream().map(CloudIntent::id).toList();
    }

    // design_compare_cloud_egress

    static ToolRegistration compareCloudEgress() {
        return ToolRegistration.builder()
                .name("design_compare_cloud_egress")
                .title("Compare a cloud's egress: internet vs Equinix private interconnect")
                .description("Runs the Savings Calculator for one cloud of the spec (cloud_id, default the first cloud "
                        + "with a provider): its monthlyEgressGb over the internet at the provider's current rate "
                        + "versus over Equinix private interconnect (an IP_VC from a STANDARD Cloud Router sized from "
                        + "the workloads reaching the cloud), with net monthly and annual savings, break-even GB per "
                        + "month and payback. Fetches the provider's live egress rate (AWS, Azure and Oracle public "
                        + "price APIs; Google when GCP_BILLING_API_KEY is set) and live Equinix prices, both under the "
                        + "call's pricing budget; a slow or failing source falls back to dated reference rates and is "
                        + "named. In commercial.currency at commercial.term. READ-ONLY.")
                .inputSchema(object(props(
                                SpecInput.ARG, objectOrString(SpecTools.SPEC_ARG_DESCRIPTION),
                                SpecInput.FINGERPRINT_ARG, DesignToolFactory.fingerprintSchema(),
                                "cloud_id", string("The spec cloud (clouds[].id). Default: the first cloud with a "
                                        + "provider."),
                                "portfolio", DesignToolFactory.portfolioSchema("price"))))
                .outputSchema(DesignPayloads.outputSchema("rates, monthly costs, net savings, break-even, payback, "
                        + "complete, live_pricing, spec_fingerprint, provenance.", "complete", "spec_fingerprint"))
                .toolset(Toolset.DESIGN)
                .handler(ValueTools::handleEgress)
                .build();
    }

    private static ObjectNode handleEgress(JsonNode args, ServerContext ctx) throws Exception {
        DesignToolFactory.SpecAndRun input = DesignToolFactory.specOf(args, ctx);
        DesignSpec spec = input.spec();
        PricingBudget budget = ctx.pricingBudget();
        List<TimeoutRateCard> guards = new ArrayList<>();
        Chosen chosen = choosePortfolio(args, input, ctx, budget, guards, CancellationSignal.none(), false);
        String cloudId = optString(args, "cloud_id").orElse(null);
        SavingsCalculator.Builder builder;
        try {
            builder = SavingsCalculator.forSpec(ctx.fabric(), spec, cloudId, chosen.portfolio());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + ". Declared clouds: " + cloudIds(spec) + ".");
        }
        CloudIntent cloud = cloudId != null ? spec.cloud(cloudId)
                : spec.clouds().stream().filter(c -> c.provider() != null).findFirst().orElseThrow();
        CloudProviderType provider = cloud.provider();
        Optional<RateCard> liveCard = ctx.providerRateCard(provider);
        TimeoutRateCard guarded = liveCard.map(card -> new TimeoutRateCard(provider.name(), card, budget)).orElse(null);
        RateCard equinix = TimeoutRateCard.equinixChain(ctx.fabric(), budget, guards);
        builder.rateCard(guarded != null ? RateCard.layered(guarded, equinix) : equinix);
        SavingsEstimate estimate = builder.calculate();

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("spec_fingerprint", estimate.getSpecFingerprint() != null ? estimate.getSpecFingerprint()
                : spec.fingerprint());
        payload.put("cloud_id", cloud.id());
        payload.put("provider", provider.name());
        payload.put("region", estimate.getRegion());
        payload.put("metro", estimate.getMetro() == null ? null : estimate.getMetro().name());
        payload.put("portfolio_source", chosen.source());
        money(payload, "monthly_egress_gb", estimate.getMonthlyEgressGb());
        payload.put("internet_rate_per_gb", estimate.getInternetRatePerGb());
        payload.put("private_rate_per_gb", estimate.getPrivateRatePerGb());
        money(payload, "internet_egress_monthly_cost", estimate.getInternetEgressMonthlyCost());
        money(payload, "private_egress_monthly_cost", estimate.getPrivateEgressMonthlyCost());
        money(payload, "monthly_egress_savings", estimate.getMonthlyEgressSavings());
        money(payload, "equinix_monthly_cost", estimate.getEquinixMonthlyCost());
        money(payload, "equinix_setup_cost", estimate.getEquinixSetupCost());
        money(payload, "cloud_provider_port_monthly_cost", estimate.getCloudProviderPortMonthlyCost());
        money(payload, "net_monthly_savings", estimate.getNetMonthlySavings());
        money(payload, "annual_net_savings", estimate.getAnnualNetSavings());
        money(payload, "first_year_net_savings", estimate.getFirstYearNetSavings());
        money(payload, "break_even_gb_per_month", estimate.getBreakEvenGbPerMonth());
        payload.put("payback_months", estimate.getPaybackMonths());
        payload.put("currency", estimate.getCurrency());
        payload.put("egress_priced", estimate.isEgressPriced());
        payload.put("equinix_priced", estimate.isEquinixPriced());
        payload.put("complete", estimate.isComplete());
        payload.put("disclaimer", estimate.getDisclaimer());

        ObjectNode live = payload.putObject("live_pricing");
        live.put("attempted", guarded != null);
        if (guarded == null) {
            live.put("note", provider == CloudProviderType.GOOGLE_CLOUD
                    ? "No live GCP adapter: set GCP_BILLING_API_KEY to enable the Cloud Billing Catalog lookup. "
                    + "Reference rates were used."
                    : "No live pricing adapter exists for " + provider.name() + "; reference rates were used.");
        } else if (guarded.degraded()) {
            live.put("degraded", true);
            ArrayNode failures = live.putArray("failures");
            guarded.failures().forEach(f -> UntrustedText.add(failures, f));
            live.put("note", "Live " + provider.name() + " pricing was unavailable within the call's "
                    + budget.budgetMillis() + " ms pricing budget; reference rates were used.");
        } else {
            live.put("degraded", false);
        }
        TimeoutRateCard.reportEquinix(payload, guards, budget);
        boolean liveUsed = guarded != null && !guarded.degraded();
        DesignPayloads.provenance(payload, liveUsed ? "mixed" : "reference", null,
                payload.get("spec_fingerprint").asText(),
                List.of(liveUsed ? provider.name() + " public price API (live)" : provider.name()
                                + " egress: reference table " + ReferenceRateCard.standard().asOf(),
                        "Equinix prices (live under the call's pricing budget, else reference)"));
        return payload;
    }
}
