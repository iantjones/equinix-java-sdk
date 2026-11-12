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

package com.eqixiac.equinix.design.cost;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.placement.WorkloadPlacer;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.value.ratecard.ConnectionModel;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceLookup;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Growth and commitment-term decisions for one design: projects traffic month by month from
 * {@code commercial.annualGrowthRate} over {@code commercial.horizonMonths}, finds the Fabric bandwidth tier
 * each connection needs each month, prices the design month by month, recommends a commitment term per
 * item, and produces a scenario band.
 *
 * <h2>Model (every assumption is stated in the result)</h2>
 * <ul>
 *   <li>Traffic: month {@code n} (1-based) carries today's traffic &times; {@code (1 + g)^((n - 1) / 12)}:
 *       compound annual growth applied smoothly per month. Month 1 is today.</li>
 *   <li>Capacity: each cloud leg (from the same {@link WorkloadPlacer} legs the optimiser's
 *       {@link RateCardCostModel} prices) needs the smallest Fabric tier ({@link RateCardCostModel#TIERS_MBPS})
 *       at or above its projected Mbps; the provider's hosted-connection fee follows the same bandwidth.</li>
 *   <li>Items: per leg a Fabric {@code IP_VC} connection and the provider's hosted-connection fee; per metro
 *       with a leg one STANDARD Fabric Cloud Router; per cloud with {@code monthlyEgressGb} its egress over the
 *       private path (internet when the design does not connect to it), growing with traffic.</li>
 *   <li>Terms: an item is committed for {@code T} months at that term's price, renewing at the same term
 *       until the horizon; an upgrade within a term is billed at the committed term's price for the new tier
 *       from the upgrade month (an assumption: contracts may differ); months committed beyond the horizon are
 *       counted (the "commitment tail") at the last month's price; the one-time charge is paid once.</li>
 *   <li>Term prices come from the rate card's {@code connectionLookup}/{@code cloudRouterLookup} at each term.
 *       A term option is priced only from a live or caller-supplied price for that term: a
 *       {@link PriceSource#REFERENCE} figure ignores the term and a live row of another term is a
 *       substitution, so both leave the option unpriced with the reason. Provider fees and egress have no
 *       term axis (the providers publish none) and are on demand.</li>
 *   <li>Currency: amounts are compared in {@code commercial.currency}; another currency is converted through
 *       {@link CurrencyConverters} (pinned rates, a supplied converter, live ECB rates, the bundled table); only a
 *       currency no source covers leaves the item unpriced.</li>
 * </ul>
 */
public final class GrowthPlanner {

    /** The terms compared for every item, shortest first. */
    public static final List<Term> TERMS = List.of(Term.MONTH_1, Term.MONTH_12, Term.MONTH_24, Term.MONTH_36);

    private final DesignSpec spec;
    private final RateCard rateCard;
    private final ReferenceRateCard providerFees;
    private final String currency;
    private final CurrencyConverters.Chain fx;
    private final double growth;
    private final int horizon;
    private final List<Item> items;
    private final List<String> unpriced = new ArrayList<>();
    private final List<String> assumptions = new ArrayList<>();
    private final Map<String, PriceLookup<BigDecimal[]>> cache = new HashMap<>();

    private GrowthPlanner(DesignSpec spec, MetroPortfolio portfolio, DesignContext context, RateCard rateCard,
                          ReferenceRateCard providerFees, com.eqixiac.equinix.design.spi.CurrencyConverter converter) {
        this.spec = spec;
        this.rateCard = rateCard;
        this.providerFees = providerFees;
        this.currency = spec.commercial().currencyOrDefault();
        this.fx = CurrencyConverters.forSpec(spec, converter);
        this.growth = spec.commercial().annualGrowthRate() == null ? 0.0 : spec.commercial().annualGrowthRate();
        this.horizon = spec.commercial().horizonMonthsOrDefault();
        if (spec.commercial().annualGrowthRate() == null) {
            assumptions.add("commercial.annualGrowthRate not set: 0% growth");
        }
        if (spec.commercial().horizonMonths() == null) {
            assumptions.add("commercial.horizonMonths not set: 12 months");
        }
        this.items = items(portfolio, context);
    }

    /**
     * Plans a design.
     *
     * @param spec         the spec (growth, horizon, currency, exchange rates, clouds)
     * @param portfolio    the chosen metros
     * @param context      the run's facts (for workload placement)
     * @param rateCard     the card Equinix items and egress are priced from (term prices come from here)
     * @param providerFees the provider hosted-connection fee table; {@code null} leaves those fees unpriced
     * @return the planner
     */
    public static GrowthPlanner forSpec(DesignSpec spec, MetroPortfolio portfolio, DesignContext context,
                                        RateCard rateCard, ReferenceRateCard providerFees) {
        if (spec == null || portfolio == null || context == null || rateCard == null) {
            throw new IllegalArgumentException("GrowthPlanner needs a spec, a portfolio, a context and a rate card");
        }
        return forSpec(spec, portfolio, context, rateCard, providerFees, null);
    }

    /**
     * Plans a design with a converter for prices in other currencies (ranked below rates pinned in the spec and
     * above the default sources; see {@link CurrencyConverters}).
     *
     * @param spec         the spec
     * @param portfolio    the chosen metros
     * @param context      the run's facts
     * @param rateCard     the card Equinix items and egress are priced from
     * @param providerFees the provider hosted-connection fee table, or {@code null}
     * @param converter    the converter, or {@code null} for the defaults
     * @return the planner
     */
    public static GrowthPlanner forSpec(DesignSpec spec, MetroPortfolio portfolio, DesignContext context,
                                        RateCard rateCard, ReferenceRateCard providerFees,
                                        com.eqixiac.equinix.design.spi.CurrencyConverter converter) {
        if (spec == null || portfolio == null || context == null || rateCard == null) {
            throw new IllegalArgumentException("GrowthPlanner needs a spec, a portfolio, a context and a rate card");
        }
        return new GrowthPlanner(spec, portfolio, context, rateCard, providerFees, converter);
    }

    /**
     * Traffic multiplier for a month: {@code (1 + growth)^((month - 1) / 12)}.
     *
     * @param annualGrowth yearly growth as a fraction
     * @param month        1-based month
     * @return the multiplier
     */
    public static double multiplier(double annualGrowth, int month) {
        return Math.pow(1 + annualGrowth, (month - 1) / 12.0);
    }

    /**
     * The Fabric tier a leg needs each month.
     *
     * @param baseMbps     today's sized bandwidth
     * @param annualGrowth yearly growth as a fraction
     * @param horizon      months
     * @return one tier per month (month 1 first); {@code -1} for a month above the largest tier
     */
    public static int[] tiers(int baseMbps, double annualGrowth, int horizon) {
        int[] tiers = new int[horizon];
        for (int m = 1; m <= horizon; m++) {
            tiers[m - 1] = RateCardCostModel.tierFor((int) Math.ceil(baseMbps * multiplier(annualGrowth, m) - 1e-9));
        }
        return tiers;
    }

    /**
     * The month-by-month projection with a term recommendation per item.
     *
     * @return the plan
     */
    public GrowthPlan plan() {
        List<GrowthPlan.ItemPlan> plans = new ArrayList<>();
        for (Item item : items) {
            plans.add(planItem(item));
        }
        List<BigDecimal> monthly = monthlyTotals(growth, 1.0, null);
        return new GrowthPlan(spec.fingerprint(), currency, growth, horizon, plans, monthly, unpriced, statedAssumptions());
    }

    /**
     * Deterministic low/base/high cases and, optionally, a seeded Monte Carlo, all priced on demand.
     *
     * @param lowGrowth   the low annual growth rate (fraction)
     * @param highGrowth  the high annual growth rate (fraction); {@code low <= base <= high} where base is the spec's
     * @param monteCarlo  the simulation settings, or {@code null} for none
     * @return the band
     */
    public ScenarioBand scenarios(double lowGrowth, double highGrowth, MonteCarlo monteCarlo) {
        if (!(lowGrowth <= growth && growth <= highGrowth) || lowGrowth <= -1) {
            throw new IllegalArgumentException("The growth range must satisfy -1 < low <= base (" + growth
                    + ") <= high: got " + lowGrowth + ".." + highGrowth);
        }
        List<ScenarioBand.Case> cases = new ArrayList<>();
        cases.add(scenario("low", lowGrowth, 1.0));
        cases.add(scenario("base", growth, 1.0));
        cases.add(scenario("high", highGrowth, 1.0));
        ScenarioBand.Simulation simulation = null;
        if (monteCarlo != null) {
            simulation = simulate(lowGrowth, highGrowth, monteCarlo);
        }
        return new ScenarioBand(spec.fingerprint(), currency, horizon, cases, simulation, unpriced,
                statedAssumptions());
    }

    private List<String> statedAssumptions() {
        List<String> all = new ArrayList<>(assumptions);
        all.add("Month n carries today's traffic x (1 + " + growth + ")^((n - 1)/12); capacity is the next Fabric "
                + "tier at or above the projected Mbps");
        all.add("A term renews at the same term to the horizon; an upgrade within a term is billed at that term's "
                + "price for the new tier; months committed beyond the horizon are counted at the last month's price");
        all.add("Provider hosted-connection fees and egress have no term axis and are priced on demand");
        all.add("One-time charges fall in month 1; scenario totals are priced on demand (1-month prices)");
        return all;
    }

    // items

    private enum Kind { CONNECTION, PROVIDER_FEE, ROUTER, EGRESS }

    private record Item(Kind kind, String label, MetroId metro, CloudIntent cloud, int baseMbps, double baseGb,
                        boolean connected) {
    }

    private List<Item> items(MetroPortfolio portfolio, DesignContext context) {
        PlacementPlan plan = WorkloadPlacer.place(portfolio, context);
        List<Item> list = new ArrayList<>();
        Set<MetroId> routerMetros = new LinkedHashSet<>();
        Set<String> connected = new LinkedHashSet<>();
        for (PlacementPlan.CloudLeg leg : plan.legs()) {
            routerMetros.add(leg.metro());
            connected.add(leg.cloudId());
            if (leg.sizedMbps() <= 0) {
                unpriced.add("Connection to " + leg.cloudId() + " in " + leg.metro().code() + ": no workload on it states "
                        + "bandwidthMbps, so its growth cannot be projected");
                continue;
            }
            CloudIntent cloud = spec.cloud(leg.cloudId());
            list.add(new Item(Kind.CONNECTION, "Fabric IP_VC " + leg.metro().code() + " to " + leg.cloudId(),
                    leg.metro(), cloud, leg.sizedMbps(), 0, true));
            list.add(new Item(Kind.PROVIDER_FEE, "Provider hosted-connection fee " + leg.cloudId() + " in "
                    + leg.metro().code(), leg.metro(), cloud, leg.sizedMbps(), 0, true));
        }
        for (MetroId metro : routerMetros) {
            list.add(new Item(Kind.ROUTER, "Fabric Cloud Router STANDARD in " + metro.code(), metro, null, 0, 0, true));
        }
        for (CloudIntent cloud : spec.clouds()) {
            if (cloud.monthlyEgressGb() != null && cloud.monthlyEgressGb() > 0) {
                list.add(new Item(Kind.EGRESS, "Data out of " + cloud.id(), null, cloud, 0, cloud.monthlyEgressGb(),
                        connected.contains(cloud.id())));
            }
        }
        return list;
    }

    private static boolean termAxis(Kind kind) {
        return kind == Kind.CONNECTION || kind == Kind.ROUTER;
    }

    /**
     * The item's monthly price at a month, a growth and an egress factor, in the target currency.
     * {@code [monthly, oneTime]}, or unpriced with the reason.
     */
    private PriceLookup<BigDecimal[]> price(Item item, Term term, double annualGrowth, double egressFactor, int month) {
        int tier = item.baseMbps() > 0 ? RateCardCostModel.tierFor(
                (int) Math.ceil(item.baseMbps() * multiplier(annualGrowth, month) - 1e-9)) : 0;
        if (tier < 0) {
            return PriceLookup.unpriced(item.label() + ": projected traffic in month " + month
                    + " exceeds the largest Fabric tier");
        }
        if (item.kind() == Kind.EGRESS) {
            PriceLookup<BigDecimal[]> rate = cached("egress|" + item.cloud().id() + "|" + item.connected(), () -> egressRate(item));
            if (!rate.isPriced()) {
                return rate;
            }
            BigDecimal gb = BigDecimal.valueOf(item.baseGb() * multiplier(annualGrowth, month) * egressFactor);
            return PriceLookup.priced(new BigDecimal[]{rate.value()[0].multiply(gb).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO}, rate.answeredBy());
        }
        String key = item.kind() + "|" + item.label() + "|" + tier + "|" + term;
        return cached(key, () -> lookup(item, tier, term));
    }

    private PriceLookup<BigDecimal[]> cached(String key, java.util.function.Supplier<PriceLookup<BigDecimal[]>> f) {
        PriceLookup<BigDecimal[]> hit = cache.get(key);
        if (hit == null) {
            hit = f.get();
            cache.put(key, hit);
        }
        return hit;
    }

    private PriceLookup<BigDecimal[]> lookup(Item item, int tier, Term term) {
        PriceLookup<PriceQuote> quote;
        switch (item.kind()) {
            case CONNECTION -> quote = rateCard.connectionLookup(ConnectionType.IP_VC, tier,
                    item.metro().asMetroCode().orElse(null), term);
            case ROUTER -> quote = rateCard.cloudRouterLookup("STANDARD", item.metro().asMetroCode().orElse(null), term);
            default -> {
                if (providerFees == null) {
                    return PriceLookup.unpriced(item.label() + ": no provider fee table configured");
                }
                if (item.cloud() == null || item.cloud().provider() == null) {
                    return PriceLookup.unpriced(item.label() + ": the cloud names no provider");
                }
                quote = providerFees.providerConnection(item.cloud().provider(), ConnectionModel.HOSTED, tier);
            }
        }
        if (!quote.isPriced()) {
            return PriceLookup.unpriced(item.label() + " at " + tier + " Mbps, " + term + ": " + quote.unpricedReason());
        }
        PriceQuote q = quote.value();
        if (term != Term.MONTH_1 && termAxis(item.kind())) {
            if (q.getSource() == PriceSource.REFERENCE || q.getSource() == PriceSource.ESTIMATE) {
                return PriceLookup.unpriced(term + " price for " + item.label() + ": the rate card has only a "
                        + q.getSource() + " figure, which ignores the term");
            }
            if (q.getNote() != null && q.getNote().contains("substituted for requested")) {
                return PriceLookup.unpriced(term + " price for " + item.label() + ": the live catalogue has no row for "
                        + "this term (" + q.getNote() + ")");
            }
        }
        String from = q.getCurrency().getCurrencyCode();
        Optional<BigDecimal> m = convert(q.getMonthlyRecurring(), from);
        Optional<BigDecimal> o = convert(q.getNonRecurring(), from);
        if (m.isEmpty() || o.isEmpty()) {
            return PriceLookup.unpriced(item.label() + ": priced in " + from + "; missing exchange rate " + from + " to "
                    + currency + " in any source (" + fx.name() + ")");
        }
        return PriceLookup.priced(new BigDecimal[]{m.get(), o.get()}, quote.answeredBy());
    }

    private PriceLookup<BigDecimal[]> egressRate(Item item) {
        if (item.cloud().provider() == null) {
            return PriceLookup.unpriced(item.label() + ": the cloud names no provider");
        }
        EgressPath path = item.connected() ? EgressPath.PRIVATE : EgressPath.INTERNET;
        PriceLookup<EgressRate> rate = rateCard.egressLookup(item.cloud().provider(), item.cloud().region(), path,
                Term.MONTH_1);
        if (!rate.isPriced()) {
            return PriceLookup.unpriced(item.label() + ": " + rate.unpricedReason());
        }
        String from = rate.value().getCurrency().getCurrencyCode();
        Optional<BigDecimal> perGb = from.equals(currency) ? Optional.of(rate.value().getPricePerGb())
                : fx.rate(from, currency).map(r -> rate.value().getPricePerGb().multiply(r.rate()));
        if (perGb.isEmpty()) {
            return PriceLookup.unpriced(item.label() + ": priced in " + from + "; missing exchange rate " + from + " to "
                    + currency + " in any source (" + fx.name() + ")");
        }
        return PriceLookup.priced(new BigDecimal[]{perGb.get(), BigDecimal.ZERO}, rate.answeredBy());
    }

    private Optional<BigDecimal> convert(BigDecimal amount, String from) {
        if (from.equals(currency)) {
            return Optional.of(amount);
        }
        return fx.rate(from, currency).map(r -> r.apply(amount));
    }

    // term decisions

    private GrowthPlan.ItemPlan planItem(Item item) {
        int[] tiers = item.baseMbps() > 0 ? tiers(item.baseMbps(), growth, horizon) : null;
        Integer upgradeMonth = null;
        String upgrade = null;
        if (tiers != null) {
            for (int m = 1; m < horizon; m++) {
                if (tiers[m] != tiers[0]) {
                    upgradeMonth = m + 1;
                    upgrade = tiers[m] < 0
                            ? "growth exceeds the largest Fabric tier in month " + (m + 1)
                            : "growth forces an upgrade from " + tiers[0] + " to " + tiers[m] + " Mbps in month " + (m + 1);
                    break;
                }
            }
        }
        List<GrowthPlan.TermOption> options = new ArrayList<>();
        List<Term> terms = termAxis(item.kind()) ? TERMS : List.of(Term.MONTH_1);
        BigDecimal[] onDemandByMonth = monthlyPrices(item, Term.MONTH_1);
        for (Term term : terms) {
            options.add(option(item, term, onDemandByMonth));
        }
        GrowthPlan.TermOption best = null;
        for (GrowthPlan.TermOption o : options) {
            if (o.priced() && (best == null || o.totalOverHorizon().compareTo(best.totalOverHorizon()) < 0)) {
                best = o;
            }
        }
        String reason;
        if (best == null) {
            reason = "no term could be priced for every month of the horizon";
        } else if (!termAxis(item.kind())) {
            reason = "no term axis: priced on demand";
        } else {
            long pricedTerms = options.stream().filter(GrowthPlan.TermOption::priced).count();
            reason = best.term() + " has the lowest total over " + horizon + " months among " + pricedTerms
                    + " priced option(s) (ties go to the shorter term)";
        }
        return new GrowthPlan.ItemPlan(item.label(), item.kind().name(), tiers == null ? List.of()
                : Arrays.stream(tiers).boxed().toList(), upgradeMonth, upgrade, options,
                best == null ? null : best.term(), reason);
    }

    /** Monthly prices of an item for every month at one term, or {@code null} when any month is unpriced. */
    private BigDecimal[] monthlyPrices(Item item, Term term) {
        BigDecimal[] prices = new BigDecimal[horizon];
        for (int m = 1; m <= horizon; m++) {
            PriceLookup<BigDecimal[]> p = price(item, term, growth, 1.0, m);
            if (!p.isPriced()) {
                return null;
            }
            prices[m - 1] = p.value()[0];
        }
        return prices;
    }

    private GrowthPlan.TermOption option(Item item, Term term, BigDecimal[] onDemand) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal oneTime = null;
        BigDecimal last = null;
        BigDecimal first = null;
        BigDecimal[] prices = new BigDecimal[horizon];
        for (int m = 1; m <= horizon; m++) {
            PriceLookup<BigDecimal[]> p = price(item, term, growth, 1.0, m);
            if (!p.isPriced()) {
                return GrowthPlan.TermOption.unpriced(term, p.unpricedReason());
            }
            prices[m - 1] = p.value()[0];
            total = total.add(p.value()[0]);
            if (oneTime == null) {
                oneTime = p.value()[1];
                first = p.value()[0];
            }
            last = p.value()[0];
        }
        int t = term.months();
        int committedMonths = ((horizon + t - 1) / t) * t;
        int tailMonths = committedMonths - horizon;
        BigDecimal tail = last.multiply(BigDecimal.valueOf(tailMonths));
        total = total.add(tail).add(oneTime);
        Integer breakEven = null;
        if (term != Term.MONTH_1 && onDemand != null) {
            // The commitment's obligation is the whole first term; it pays off once on-demand spend reaches it.
            BigDecimal obligation = oneTime;
            for (int m = 0; m < t; m++) {
                obligation = obligation.add(m < horizon ? prices[m] : last);
            }
            BigDecimal cumulative = BigDecimal.ZERO;
            for (int m = 0; m < t; m++) {
                cumulative = cumulative.add(m < horizon ? onDemand[m] : onDemand[horizon - 1]);
                if (cumulative.compareTo(obligation) >= 0) {
                    breakEven = m + 1;
                    break;
                }
            }
        }
        return new GrowthPlan.TermOption(term, true, null, first, oneTime, total.setScale(2, RoundingMode.HALF_UP),
                tailMonths, tail.setScale(2, RoundingMode.HALF_UP), breakEven);
    }

    // scenarios

    /** Per-month on-demand totals over every item; {@code null} entries for months with an unpriced item. */
    private List<BigDecimal> monthlyTotals(double annualGrowth, double egressFactor, List<String> problems) {
        List<BigDecimal> totals = new ArrayList<>();
        for (int m = 1; m <= horizon; m++) {
            BigDecimal sum = BigDecimal.ZERO;
            boolean complete = true;
            for (Item item : items) {
                PriceLookup<BigDecimal[]> p = price(item, Term.MONTH_1, annualGrowth, egressFactor, m);
                if (!p.isPriced()) {
                    complete = false;
                    if (problems != null && !problems.contains(p.unpricedReason())) {
                        problems.add(p.unpricedReason());
                    }
                    break;
                }
                sum = sum.add(p.value()[0]).add(m == 1 ? p.value()[1] : BigDecimal.ZERO);
            }
            totals.add(complete ? sum : null);
        }
        return totals;
    }

    private ScenarioBand.Case scenario(String name, double annualGrowth, double egressFactor) {
        List<String> problems = new ArrayList<>();
        List<BigDecimal> monthly = monthlyTotals(annualGrowth, egressFactor, problems);
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal m : monthly) {
            if (m == null) {
                return new ScenarioBand.Case(name, annualGrowth, egressFactor, null, null, problems);
            }
            total = total.add(m);
        }
        return new ScenarioBand.Case(name, annualGrowth, egressFactor, total, monthly.get(horizon - 1), problems);
    }

    private ScenarioBand.Simulation simulate(double low, double high, MonteCarlo mc) {
        SplittableRandom random = new SplittableRandom(mc.seed());
        List<BigDecimal> totals = new ArrayList<>();
        int unpricedRuns = 0;
        for (int i = 0; i < mc.iterations(); i++) {
            double g = triangular(random.nextDouble(), low, growth, high);
            double e = mc.egressLow() == null ? 1.0 : triangular(random.nextDouble(), mc.egressLow(), 1.0, mc.egressHigh());
            ScenarioBand.Case c = scenario("run " + i, g, e);
            if (c.totalOverHorizon() == null) {
                unpricedRuns++;
            } else {
                totals.add(c.totalOverHorizon());
            }
        }
        totals.sort(BigDecimal::compareTo);
        String growthDistribution = "annual growth ~ Triangular(min " + low + ", mode " + growth + ", max " + high + ")";
        String egressDistribution = mc.egressLow() == null ? "egress volume fixed at the spec's monthlyEgressGb"
                : "egress volume factor ~ Triangular(min " + mc.egressLow() + ", mode 1.0, max " + mc.egressHigh()
                + "), independent of growth";
        return new ScenarioBand.Simulation(mc.seed(), mc.iterations(), growthDistribution, egressDistribution,
                "java.util.SplittableRandom(seed); percentiles by nearest rank", percentile(totals, 0.10),
                percentile(totals, 0.50), percentile(totals, 0.90), unpricedRuns);
    }

    /**
     * Inverse CDF of the triangular distribution.
     *
     * @param u    a uniform draw in [0, 1)
     * @param min  the minimum
     * @param mode the mode
     * @param max  the maximum
     * @return the sample
     */
    public static double triangular(double u, double min, double mode, double max) {
        if (max == min) {
            return min;
        }
        double cut = (mode - min) / (max - min);
        return u < cut ? min + Math.sqrt(u * (max - min) * (mode - min))
                : max - Math.sqrt((1 - u) * (max - min) * (max - mode));
    }

    /**
     * Nearest-rank percentile.
     *
     * @param sorted ascending values
     * @param p      the fraction, 0 to 1
     * @return the value, or {@code null} for an empty list
     */
    public static BigDecimal percentile(List<BigDecimal> sorted, double p) {
        if (sorted.isEmpty()) {
            return null;
        }
        int rank = (int) Math.ceil(p * sorted.size());
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, rank - 1)));
    }

    /**
     * Seeded Monte Carlo settings. There is no default seed: randomness without a seed is not offered.
     *
     * @param seed        the seed of {@link java.util.SplittableRandom}
     * @param iterations  the number of runs, 1 to 100,000
     * @param egressLow   the low egress-volume factor (for example 0.8), or {@code null} to keep volume fixed
     * @param egressHigh  the high egress-volume factor (for example 1.3); required with {@code egressLow}
     */
    public record MonteCarlo(long seed, int iterations, Double egressLow, Double egressHigh) {

        /** Validates the settings. */
        public MonteCarlo {
            if (iterations < 1 || iterations > 100_000) {
                throw new IllegalArgumentException("iterations must be 1 to 100,000");
            }
            if ((egressLow == null) != (egressHigh == null)) {
                throw new IllegalArgumentException("egressLow and egressHigh go together");
            }
            if (egressLow != null && !(egressLow > 0 && egressLow <= 1 && egressHigh >= 1)) {
                throw new IllegalArgumentException("egress factors must satisfy 0 < low <= 1 <= high");
            }
        }
    }
}
