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

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.placement.WorkloadPlacer;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.CostModel;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.PortfolioCost;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link CostModel} of the Metro Optimizer: prices what a portfolio needs, metro by metro, from a
 * {@link RateCard}. The same {@link PortfolioCost} feeds the cost objective, the budget constraint and
 * the result's cost estimate, so they cannot disagree.
 *
 * <p>What is priced, from the portfolio's {@link WorkloadPlacer placement}:</p>
 * <ul>
 *   <li>one Fabric connection ({@link Builder#connectionType default IP_VC}) per (metro, cloud) leg,
 *       sized as the sum of the sized workloads it carries (primary and standby) rounded up to the next
 *       Fabric bandwidth tier ({@link #TIERS_MBPS});</li>
 *   <li>one Fabric Cloud Router ({@link Builder#routerPackage default STANDARD}) in every metro that
 *       carries a cloud leg: router-originated connections need no customer port, which is how the
 *       Deployment Wizard builds the design;</li>
 *   <li>the cloud provider's own fee per leg, from the reference table as a
 *       {@link ConnectionModel#HOSTED} (partner) connection, because a Fabric cloud connection is the
 *       provider's partner product ({@code ReferenceRateCard.providerConnection}): the exact tier, or the
 *       next tier up with a "rounded up from X to Y Mbps" note, or unpriced with the reason;</li>
 *   <li>data leaving each cloud ({@code CloudIntent.monthlyEgressGb}) at the provider's private-path rate
 *       when the portfolio connects to the cloud, otherwise at its internet rate.</li>
 * </ul>
 * <p>A metro that carries no leg is charged nothing: there is no per-metro base fee. Data sent into a
 * cloud ({@code monthlyIngressGb}) is not charged by the providers the reference table covers, so no
 * line is added for it. Colocation space and power, and site access circuits, are not priced.</p>
 *
 * <p>Every line is priced in the target currency ({@code commercial.currency}, default USD). A line quoted in
 * another currency is converted through {@link CurrencyConverters} (pinned spec rates, then a supplied
 * converter, then live ECB reference rates, then the bundled table): it keeps its original amount and currency
 * in {@link CostLine#conversion()}, its note states the rate, source and date, and its range is widened by the
 * rate's band. Amounts in different currencies are never added raw. Only a currency no source covers leaves
 * the line unpriced, with that reason.</p>
 *
 * <p>Every line carries its source and a range from {@link PriceBands}. Anything that cannot be priced
 * (an unsized workload, a metro the card has no figure for, a provider with no port row) is listed in
 * {@link PortfolioCost#unpriced()}, never counted as zero. Each distinct item (metro, item, bandwidth tier, term) is priced once per model
 * and cached; {@link #prepare} prices a solver's batch ahead on a small bounded pool.</p>
 */
public final class RateCardCostModel implements CostModel {

    /** Lookups {@link #prepare} runs at once by default: small, so the live price search is not flooded. */
    public static final int DEFAULT_PREFETCH_THREADS = 6;
    /** How long one lookup may run during {@link #prepare} before the fallback card is used. */
    public static final java.time.Duration DEFAULT_LOOKUP_TIMEOUT = java.time.Duration.ofSeconds(20);
    /** Timeouts in one run after which {@link #prepare} stops asking the primary card. */
    public static final int DEFAULT_MAX_TIMEOUTS = 2;

    /** Fabric connection bandwidths a leg is rounded up to, in Mbps. */
    public static final int[] TIERS_MBPS = {50, 100, 200, 500, 1_000, 2_000, 5_000, 10_000, 25_000, 50_000, 100_000};

    private final RateCard rateCard;
    private final ReferenceRateCard providerPorts;
    private final Term term;
    private final ConnectionType connectionType;
    private final String routerPackage;
    private final String targetCurrency;
    private final CommercialTerms.ExchangeRates exchangeRates;
    private final CurrencyConverters.Chain converter;
    private final Map<String, Optional<CostLine>> cache = new ConcurrentHashMap<>();
    private final Map<String, String> unpricedReasons = new ConcurrentHashMap<>();
    private final RateCard fallbackCard;
    private final int prefetchThreads;
    private final java.time.Duration lookupTimeout;
    private final int maxTimeouts;
    private final List<String> notes = java.util.Collections.synchronizedList(new ArrayList<>());

    private RateCardCostModel(Builder b) {
        this.rateCard = b.rateCard;
        this.providerPorts = b.providerPorts;
        this.term = b.term;
        this.connectionType = b.connectionType;
        this.routerPackage = b.routerPackage;
        this.targetCurrency = b.targetCurrency;
        this.exchangeRates = b.exchangeRates;
        this.converter = CurrencyConverters.forTerms(CommercialTerms.builder().currency(b.targetCurrency)
                .exchangeRates(b.exchangeRates).build(), b.supplied, b.fallbacks);
        this.fallbackCard = b.fallbackCard;
        this.prefetchThreads = b.prefetchThreads;
        this.lookupTimeout = b.lookupTimeout;
        this.maxTimeouts = b.maxTimeouts;
    }

    /**
     * @param rateCard the card Equinix items and egress are priced from
     * @return a builder
     */
    public static Builder builder(RateCard rateCard) {
        return new Builder(rateCard);
    }

    /**
     * A builder preset from a spec: the term ({@code commercial.term}, default {@link Term#MONTH_1}),
     * the target currency ({@code commercial.currency}, default USD) and {@code commercial.exchangeRates}.
     *
     * @param spec     the design spec
     * @param rateCard the card Equinix items and egress are priced from
     * @return a builder
     */
    public static Builder forSpec(DesignSpec spec, RateCard rateCard) {
        CommercialTerms c = spec.commercial();
        return builder(rateCard).term(c.term()).currency(c.currencyOrDefault()).exchangeRates(c.exchangeRates());
    }

    /** @return the converter in use (pinned rates, supplied converter, live ECB rates, bundled table) */
    public CurrencyConverters.Chain converter() {
        return converter;
    }

    /** @return the commitment term prices are looked up at */
    public Term term() {
        return term;
    }

    /** @return the currency lines are restated in, USD unless set */
    public String currency() {
        return targetCurrency;
    }

    /** @return the caller's exchange rates, or {@code null} when none were supplied */
    public CommercialTerms.ExchangeRates exchangeRates() {
        return exchangeRates;
    }

    /**
     * The currencies whose exchange rate to {@link #currency()} is missing for this cost: every line
     * currency other than the target. Empty when the cost is comparable in the target currency.
     *
     * @param cost a cost this model produced
     * @return the missing rates' currency codes, sorted
     */
    public java.util.SortedSet<String> missingRates(PortfolioCost cost) {
        return CostComparability.missingRates(cost, targetCurrency);
    }

    @Override
    public PortfolioCost price(MetroPortfolio portfolio, DesignContext context) {
        PlacementPlan plan = WorkloadPlacer.place(portfolio, context);
        List<CostLine> lines = new ArrayList<>();
        List<String> unpriced = new ArrayList<>();
        walk(plan, context, unpriced, need -> add(lines, unpriced, resolve(need)));
        return new PortfolioCost(lines, unpriced);
    }

    /**
     * Prices, in one batch, every distinct item these portfolios need that has not been priced yet: each
     * (item, metro, bandwidth, term) once, on a small bounded pool ({@link Builder#prefetch}). The answers are
     * the ones {@link #price} would get on its own, because each item is priced by the same lookup and stored
     * under the same key; only the order the lookups run in differs. A lookup that has not answered within the
     * lookup timeout is priced from the fallback card ({@link Builder#fallbackRateCard}, default the reference
     * table) with a note saying so, and after {@link Builder#prefetch maxTimeouts} timeouts the remaining lookups
     * are priced from the fallback card without waiting; {@link #notes()} lists every such substitution.
     *
     * @param portfolios the portfolios about to be priced
     * @param context    the facts of this run
     */
    @Override
    public void prepare(java.util.Collection<MetroPortfolio> portfolios, DesignContext context) {
        if (prefetchThreads < 1 || portfolios == null || portfolios.isEmpty()) {
            return;
        }
        Map<String, Need> todo = new java.util.TreeMap<>();
        List<String> ignored = new ArrayList<>();
        for (MetroPortfolio portfolio : portfolios) {
            walk(WorkloadPlacer.place(portfolio, context), context, ignored, need -> {
                if (!cache.containsKey(need.key())) {
                    todo.putIfAbsent(need.key(), need);
                }
            });
        }
        if (todo.isEmpty()) {
            return;
        }
        int threads = Math.min(prefetchThreads, todo.size());
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "equinix-design-pricing-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        try {
            Map<String, java.util.concurrent.Future<Resolved>> futures = new java.util.LinkedHashMap<>();
            Map<String, java.util.concurrent.atomic.AtomicLong> started = new java.util.HashMap<>();
            for (Need need : todo.values()) {
                java.util.concurrent.atomic.AtomicLong start = new java.util.concurrent.atomic.AtomicLong();
                started.put(need.key(), start);
                futures.put(need.key(), pool.submit(() -> {
                    start.set(System.nanoTime());
                    return need.load().apply(rateCard);
                }));
            }
            long timeoutNanos = lookupTimeout.toNanos();
            int timeouts = 0;
            int abandoned = 0;
            for (Need need : todo.values()) {
                java.util.concurrent.Future<Resolved> future = futures.get(need.key());
                java.util.concurrent.atomic.AtomicLong start = started.get(need.key());
                Resolved resolved = null;
                while (resolved == null) {
                    if (timeouts >= maxTimeouts && start.get() == 0 && future.cancel(false)) {
                        abandoned++;
                        resolved = fallback(need, null);
                        break;
                    }
                    long began = start.get();
                    if (began != 0 && System.nanoTime() - began > timeoutNanos && !future.isDone()) {
                        future.cancel(true);
                        timeouts++;
                        resolved = fallback(need, "the price lookup did not answer within "
                                + lookupTimeout.toSeconds() + " s");
                        break;
                    }
                    try {
                        resolved = future.get(50, java.util.concurrent.TimeUnit.MILLISECONDS);
                    } catch (java.util.concurrent.TimeoutException e) {
                        // keep waiting, re-checking the per-lookup deadline
                    } catch (java.util.concurrent.CancellationException e) {
                        resolved = fallback(need, "the price lookup was cancelled");
                    } catch (java.util.concurrent.ExecutionException e) {
                        Throwable cause = e.getCause() == null ? e : e.getCause();
                        resolved = fallback(need, "the price lookup failed (" + cause.getClass().getSimpleName()
                                + (cause.getMessage() == null ? "" : ": " + cause.getMessage()) + ")");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                store(need.key(), resolved);
            }
            if (abandoned > 0) {
                notes.add(abandoned + " price lookup(s) were taken from " + fallbackName() + " without asking the "
                        + "primary card, because " + timeouts + " lookup(s) had already timed out in this run");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Substitutions made while pricing: lookups that timed out or failed and were priced from the fallback card.
     *
     * @return the notes, empty when every lookup answered
     */
    public List<String> notes() {
        synchronized (notes) {
            return List.copyOf(notes);
        }
    }

    private String fallbackName() {
        return fallbackCard == null ? "no fallback card" : fallbackCard.cardName();
    }

    /** Prices an item from the fallback card after the primary lookup timed out or failed. */
    private Resolved fallback(Need need, String why) {
        String reason = why == null ? "the primary card was not asked after earlier timeouts" : why;
        if (fallbackCard == null) {
            return new Resolved(Optional.empty(), need.label() + ": " + reason + " and no fallback card is set");
        }
        Resolved r;
        try {
            r = need.load().apply(fallbackCard);
        } catch (RuntimeException e) {
            return new Resolved(Optional.empty(), need.label() + ": " + reason + "; the fallback card failed too ("
                    + e.getClass().getSimpleName() + ")");
        }
        if (r.line().isEmpty()) {
            return new Resolved(Optional.empty(), r.unpricedReason() + " (" + reason + "; fallback "
                    + fallbackName() + " has no figure)");
        }
        if (why != null) {
            notes.add(need.label() + ": " + why + "; priced from " + fallbackName());
        }
        CostLine l = r.line().get();
        return new Resolved(Optional.of(new CostLine(l.label(), l.metro(), l.monthly(), l.oneTime(), l.currency(),
                l.source(), l.monthlyLow(), l.monthlyHigh(), (l.note() == null ? "" : l.note() + "; ") + reason
                + ", so this figure is from " + fallbackName(), l.conversion())), null);
    }

    /**
     * Walks what a placement needs priced, in the order {@link #price} lists it: per cloud leg the Fabric
     * connection and the provider fee, then a Cloud Router per metro with a leg, then egress per cloud. Items
     * that cannot be priced at all are added to {@code unpriced}; every priceable item is handed to {@code need}.
     */
    private void walk(PlacementPlan plan, DesignContext context, List<String> unpriced,
                      java.util.function.Consumer<Need> need) {
        Set<MetroId> routerMetros = new LinkedHashSet<>();
        Set<String> connectedClouds = new LinkedHashSet<>();
        for (PlacementPlan.CloudLeg leg : plan.legs()) {
            routerMetros.add(leg.metro());
            connectedClouds.add(leg.cloudId());
            CloudIntent cloud = context.spec().cloud(leg.cloudId());
            for (String workload : leg.unsizedWorkloads()) {
                unpriced.add("Connection share of '" + workload + "' to " + leg.cloudId() + " in " + leg.metro().code()
                        + ": the workload has no bandwidthMbps");
            }
            if (leg.sizedMbps() <= 0) {
                continue;
            }
            int tier = tierFor(leg.sizedMbps());
            if (tier < 0) {
                unpriced.add("Connection to " + leg.cloudId() + " in " + leg.metro().code() + ": " + leg.sizedMbps()
                        + " Mbps exceeds the largest Fabric connection tier");
                continue;
            }
            need.accept(connection(leg.metro(), tier, leg.cloudId()));
            need.accept(providerPort(leg.metro(), tier, cloud));
        }
        for (MetroId metro : routerMetros) {
            need.accept(router(metro));
        }
        for (CloudIntent cloud : context.spec().clouds()) {
            if (cloud.monthlyEgressGb() != null && cloud.monthlyEgressGb() > 0) {
                need.accept(egress(cloud, connectedClouds.contains(cloud.id())));
            }
        }
    }

    /** One distinct priced item: its cache key, its label, and how to price it from a card. */
    private record Need(String key, String label, java.util.function.Function<RateCard, Resolved> load) {
    }

    /** A priced line, or the reason the item is unpriced. */
    private record Resolved(Optional<CostLine> line, String unpricedReason) {
        static Resolved priced(CostLine line) {
            return new Resolved(Optional.of(line), null);
        }

        static Resolved unpriced(String reason) {
            return new Resolved(Optional.empty(), reason);
        }
    }

    private String resolve(Need need) {
        if (!cache.containsKey(need.key())) {
            store(need.key(), need.load().apply(rateCard));
        }
        return need.key();
    }

    private void store(String key, Resolved resolved) {
        if (resolved.unpricedReason() != null) {
            unpricedReasons.putIfAbsent(key, resolved.unpricedReason());
        }
        cache.putIfAbsent(key, resolved.line());
    }

    private void add(List<CostLine> lines, List<String> unpriced, String key) {
        Optional<CostLine> line = cache.get(key);
        if (line != null && line.isPresent()) {
            CostLine converted = convert(line.get());
            if (converted == null) {
                unpriced.add(line.get().label() + ": priced in " + line.get().currency() + " "
                        + line.get().monthly().toPlainString() + "/month; "
                        + converter.missingReason(line.get().currency(), targetCurrency));
            } else {
                lines.add(converted);
            }
        } else {
            unpriced.add(unpricedReasons.getOrDefault(key, key));
        }
    }

    private Need connection(MetroId metro, int tier, String cloudId) {
        String key = metro.code() + "|connection|" + tier + "|" + cloudId;
        String label = "Fabric " + connectionType + " " + tier + " Mbps " + metro.code() + " to " + cloudId;
        return new Need(key, label, card -> {
            Optional<MetroCode> code = metro.asMetroCode();
            PriceLookup<PriceQuote> quote = card.connectionLookup(connectionType, tier, code.orElse(null), term);
            if (!quote.isPriced()) {
                return Resolved.unpriced(label + ": " + quote.unpricedReason());
            }
            return Resolved.priced(line(label, metro, quote.value()));
        });
    }

    private Need router(MetroId metro) {
        String key = metro.code() + "|router|" + routerPackage;
        String label = "Fabric Cloud Router " + routerPackage + " in " + metro.code();
        return new Need(key, label, card -> {
            PriceLookup<PriceQuote> quote = card.cloudRouterLookup(routerPackage, metro.asMetroCode().orElse(null),
                    term);
            if (!quote.isPriced()) {
                return Resolved.unpriced(label + ": " + quote.unpricedReason());
            }
            return Resolved.priced(line(label, metro, quote.value()));
        });
    }

    private Need providerPort(MetroId metro, int tier, CloudIntent cloud) {
        String provider = cloud == null || cloud.provider() == null ? "none" : cloud.provider().name();
        String key = metro.code() + "|provider-port|" + tier + "|" + provider;
        String id = cloud == null ? "?" : cloud.id();
        String label = "Provider hosted-connection fee " + id + " " + tier + " Mbps in " + metro.code();
        // The provider fee always comes from the provider-fee table, whichever card prices Equinix items.
        return new Need(key, label, card -> {
            if (cloud == null || cloud.provider() == null) {
                return Resolved.unpriced(label + ": the cloud names no provider, so its fee is unknown");
            }
            if (providerPorts == null) {
                return Resolved.unpriced(label + ": no provider fee table configured");
            }
            PriceLookup<PriceQuote> quote = providerPorts.providerConnection(cloud.provider(), ConnectionModel.HOSTED,
                    tier);
            if (!quote.isPriced()) {
                return Resolved.unpriced(label + ": " + quote.unpricedReason());
            }
            return Resolved.priced(line(label, metro, quote.value()));
        });
    }

    private Need egress(CloudIntent cloud, boolean connected) {
        EgressPath path = connected ? EgressPath.PRIVATE : EgressPath.INTERNET;
        String key = "-|egress|" + cloud.id() + "|" + path;
        String label = String.format(Locale.ROOT, "Data out of %s, %.0f GB/month over the %s path", cloud.id(),
                cloud.monthlyEgressGb(), path.name().toLowerCase(Locale.ROOT));
        return new Need(key, label, card -> {
            if (cloud.provider() == null) {
                return Resolved.unpriced(label + ": the cloud names no provider, so its egress rate is unknown");
            }
            PriceLookup<EgressRate> rate = card.egressLookup(cloud.provider(), cloud.region(), path, term);
            if (!rate.isPriced()) {
                return Resolved.unpriced(label + ": " + rate.unpricedReason());
            }
            EgressRate r = rate.value();
            BigDecimal monthly = r.costFor(BigDecimal.valueOf(cloud.monthlyEgressGb())).setScale(2, RoundingMode.HALF_UP);
            String note = (r.getNote() == null ? "" : r.getNote() + "; ") + r.getPricePerGb().toPlainString() + "/GB; "
                    + (connected ? "" : "the design does not connect to this cloud, so traffic uses the internet; ")
                    + PriceBands.describe(r.getSource());
            return Resolved.priced(new CostLine(label, null, monthly, BigDecimal.ZERO, r.getCurrency().getCurrencyCode(),
                    r.getSource(), PriceBands.low(monthly, r.getSource()), PriceBands.high(monthly, r.getSource()), note));
        });
    }

    /**
     * Restates a line in the target currency through the converter chain; {@code null} when no source has the
     * rate. A line converted at a bundled reference rate is marked {@link PriceSource#ESTIMATE}.
     */
    private CostLine convert(CostLine line) {
        return CurrencyConverters.convert(line, targetCurrency, converter).orElse(null);
    }

    private static String fmt(double percent) {
        return BigDecimal.valueOf(percent).stripTrailingZeros().toPlainString();
    }

    private static BigDecimal scale(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private static CostLine line(String label, MetroId metro, PriceQuote quote) {
        PriceSource source = quote.getSource() == null ? PriceSource.ESTIMATE : quote.getSource();
        BigDecimal monthly = quote.getMonthlyRecurring();
        return new CostLine(label, metro, monthly, quote.getNonRecurring(), quote.getCurrency().getCurrencyCode(), source,
                PriceBands.low(monthly, source), PriceBands.high(monthly, source),
                note(quote) + "; " + PriceBands.describe(source));
    }

    private static String note(PriceQuote quote) {
        return quote.getNote() == null ? String.valueOf(quote.getSource()) : quote.getNote();
    }

    /**
     * @param mbps a bandwidth
     * @return the smallest tier at or above it, or -1 above the largest
     */
    public static int tierFor(int mbps) {
        for (int tier : TIERS_MBPS) {
            if (tier >= mbps) {
                return tier;
            }
        }
        return -1;
    }

    /** Configures a {@link RateCardCostModel}. */
    public static final class Builder {
        private final RateCard rateCard;
        private ReferenceRateCard providerPorts = ReferenceRateCard.standard();
        private Term term = Term.MONTH_1;
        private ConnectionType connectionType = ConnectionType.IP_VC;
        private String routerPackage = "STANDARD";
        private String targetCurrency = "USD";
        private CommercialTerms.ExchangeRates exchangeRates;
        private com.eqixiac.equinix.design.spi.CurrencyConverter supplied;
        private com.eqixiac.equinix.design.spi.CurrencyConverter fallbacks;
        private RateCard fallbackCard = ReferenceRateCard.standard();
        private int prefetchThreads = DEFAULT_PREFETCH_THREADS;
        private java.time.Duration lookupTimeout = DEFAULT_LOOKUP_TIMEOUT;
        private int maxTimeouts = DEFAULT_MAX_TIMEOUTS;

        private Builder(RateCard rateCard) {
            if (rateCard == null) {
                throw new IllegalArgumentException("A cost model needs a rate card");
            }
            this.rateCard = rateCard;
        }

        /**
         * @param table the reference table cloud-provider port fees come from; {@code null} leaves them unpriced
         * @return this builder
         */
        public Builder providerPorts(ReferenceRateCard table) {
            this.providerPorts = table;
            return this;
        }

        /**
         * @param term the commitment term to price at; {@code null} keeps {@link Term#MONTH_1}
         * @return this builder
         */
        public Builder term(Term term) {
            if (term != null) {
                this.term = term;
            }
            return this;
        }

        /**
         * @param type the Fabric connection type of every cloud leg (default {@code IP_VC}, router-originated)
         * @return this builder
         */
        public Builder connectionType(ConnectionType type) {
            if (type != null) {
                this.connectionType = type;
            }
            return this;
        }

        /**
         * @param packageCode the Cloud Router package (default {@code STANDARD})
         * @return this builder
         */
        public Builder routerPackage(String packageCode) {
            if (packageCode != null) {
                this.routerPackage = packageCode;
            }
            return this;
        }

        /**
         * @param currency ISO 4217 code totals are compared in (normally the spec's {@code commercial.currency});
         *                 {@code null} keeps USD
         * @return this builder
         */
        public Builder currency(String currency) {
            if (currency != null && !currency.isBlank()) {
                this.targetCurrency = currency;
            }
            return this;
        }

        /**
         * Pins exchange rates (normally the spec's {@code commercial.exchangeRates}); they take precedence over
         * every other source.
         *
         * @param rates the rates, their date and source; {@code null} pins none
         * @return this builder
         */
        public Builder exchangeRates(CommercialTerms.ExchangeRates rates) {
            this.exchangeRates = rates;
            return this;
        }

        /**
         * @param converter a converter (for example a live FX feed) ranked below pinned rates and above the default
         *                  live ECB rates and bundled table; {@code null} for none
         * @return this builder
         */
        public Builder currencyConverter(com.eqixiac.equinix.design.spi.CurrencyConverter converter) {
            this.supplied = converter;
            return this;
        }

        /**
         * Replaces the default sources after the supplied converter (live ECB rates, then the bundled table), for
         * example with {@link CurrencyConverters#bundledOnly()} for an offline run.
         *
         * @param defaults the fallback converter; {@code null} restores the default
         * @return this builder
         */
        public Builder defaultRates(com.eqixiac.equinix.design.spi.CurrencyConverter defaults) {
            this.fallbacks = defaults;
            return this;
        }

        /**
         * The card an item is priced from when the primary card's lookup times out or fails during
         * {@link RateCardCostModel#prepare} (default: the reference table, {@code ReferenceRateCard.standard()}).
         *
         * @param card the fallback card; {@code null} leaves such items unpriced
         * @return this builder
         */
        public Builder fallbackRateCard(RateCard card) {
            this.fallbackCard = card;
            return this;
        }

        /**
         * How {@link RateCardCostModel#prepare} prices ahead. Defaults: {@link RateCardCostModel#DEFAULT_PREFETCH_THREADS} threads,
         * {@link RateCardCostModel#DEFAULT_LOOKUP_TIMEOUT} per lookup, fallback after
         * {@link RateCardCostModel#DEFAULT_MAX_TIMEOUTS} timeouts.
         *
         * @param threads       lookups run at once (0 disables pricing ahead; each item is then priced on first use)
         * @param lookupTimeout how long one lookup may run before the fallback card is used instead
         * @param maxTimeouts   after this many timeouts in one run, lookups not yet started use the fallback card
         *                      without asking the primary card
         * @return this builder
         */
        public Builder prefetch(int threads, java.time.Duration lookupTimeout, int maxTimeouts) {
            if (threads < 0 || lookupTimeout == null || lookupTimeout.isNegative() || lookupTimeout.isZero()
                    || maxTimeouts < 1) {
                throw new IllegalArgumentException("prefetch needs threads >= 0, a positive timeout and maxTimeouts >= 1");
            }
            this.prefetchThreads = threads;
            this.lookupTimeout = lookupTimeout;
            this.maxTimeouts = maxTimeouts;
            return this;
        }

        /** @return the cost model */
        public RateCardCostModel build() {
            return new RateCardCostModel(this);
        }
    }
}
