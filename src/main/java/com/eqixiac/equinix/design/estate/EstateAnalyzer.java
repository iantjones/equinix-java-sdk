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

package com.eqixiac.equinix.design.estate;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.design.estate.EstateInventory.ConnectionRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.RouterRecord;
import com.eqixiac.equinix.design.estate.rules.DefaultRules;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Analyses what a customer already runs on Equinix and produces a priced, evidence-backed
 * improvement report. Read-only: it reads inventory, metrics and prices and recommends; it never
 * changes a resource.
 *
 * <pre>{@code
 * EstateReport report = EstateAnalyzer.builder(fabric)
 *         .scope(EstateScope.projects("626329c0-..."))
 *         .window(Duration.ofDays(30))          // also reads utilisation
 *         .analyze();
 * System.out.println(report.toMarkdown());
 *
 * // Offline, reproducible: analyse a stored snapshot.
 * EstateReport again = EstateAnalyzer.fromSnapshot(EstateInventory.fromJson(json)).analyze();
 * }</pre>
 *
 * <p>Defaults: the rules of {@link DefaultRules#all()}, {@link EstateThresholds#defaults()}, and
 * {@code RateCard.standardChain(fabric)} (live Equinix prices, then the bundled reference card) for a
 * live run or {@code ReferenceRateCard.standard()} for an offline run. A live run must be scoped;
 * see {@link EstateScope}.</p>
 */
public final class EstateAnalyzer {

    private EstateAnalyzer() {
    }

    /**
     * Starts a live analysis.
     *
     * @param fabric the Fabric client
     * @return a builder
     */
    public static Builder builder(Fabric fabric) {
        return new Builder(Objects.requireNonNull(fabric, "fabric"), null);
    }

    /**
     * Starts an offline analysis of a stored snapshot. No API is called unless a rate card that
     * calls one is supplied.
     *
     * @param inventory the snapshot
     * @return a builder
     */
    public static Builder fromSnapshot(EstateInventory inventory) {
        return new Builder(null, Objects.requireNonNull(inventory, "inventory"));
    }

    /** Configures and runs an analysis. */
    public static final class Builder {
        private final Fabric fabric;
        private final EstateInventory snapshot;
        private NetworkEdge networkEdge;
        private EstateScope scope;
        private Duration window;
        private UtilizationSnapshot utilization;
        private RateCard rateCard;
        private boolean rateCardSet;
        private String currency = EstatePricing.DEFAULT_CURRENCY;
        private com.eqixiac.equinix.design.spi.CurrencyConverter currencyConverter;
        private com.eqixiac.equinix.design.spi.CurrencyConverter defaultRates;
        private List<EstateRule> rules = new ArrayList<>(DefaultRules.all());
        private EstateThresholds thresholds = EstateThresholds.defaults();
        private Clock clock = Clock.systemUTC();
        private Consumer<String> progress;
        private int maxMetricSearches = 200;
        private long metricPauseMillis = 100;
        private boolean includePortMetrics;

        private Builder(Fabric fabric, EstateInventory snapshot) {
            this.fabric = fabric;
            this.snapshot = snapshot;
        }

        /**
         * Also reads the Network Edge devices that in-scope connections use.
         *
         * @param networkEdge the Network Edge client
         * @return this builder
         */
        public Builder networkEdge(NetworkEdge networkEdge) {
            this.networkEdge = networkEdge;
            return this;
        }

        /**
         * Sets the scope (required for a live run).
         *
         * @param scope the scope
         * @return this builder
         */
        public Builder scope(EstateScope scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Reads utilisation over this window, ending now (live runs only).
         *
         * @param window the window
         * @return this builder
         */
        public Builder window(Duration window) {
            this.window = window;
            return this;
        }

        /**
         * Uses an already collected utilisation snapshot (offline runs, tests).
         *
         * @param utilization the snapshot
         * @return this builder
         */
        public Builder utilization(UtilizationSnapshot utilization) {
            this.utilization = utilization;
            return this;
        }

        /**
         * Sets the most metric searches in one run (default 200) and the pause between them (default 100 ms).
         *
         * @param maxSearches the ceiling
         * @param pauseMillis the pause
         * @return this builder
         */
        public Builder metricLimits(int maxSearches, long pauseMillis) {
            this.maxMetricSearches = maxSearches;
            this.metricPauseMillis = pauseMillis;
            return this;
        }

        /**
         * Also reads port utilisation (default off).
         *
         * @param include whether to read it
         * @return this builder
         */
        public Builder includePortMetrics(boolean include) {
            this.includePortMetrics = include;
            return this;
        }

        /**
         * Sets the rate card; {@code null} leaves every financial impact unpriced.
         *
         * @param rateCard the rate card
         * @return this builder
         */
        public Builder rateCard(RateCard rateCard) {
            this.rateCard = rateCard;
            this.rateCardSet = true;
            return this;
        }

        /**
         * Sets the reporting currency (default USD): every price is restated in it, so every finding's saving and
         * the addressable total are in one currency. A converted line keeps its original amount and currency, the
         * rate, and the rate's source and date; a price whose currency no rate source covers is unpriced.
         *
         * @param currency an ISO 4217 code
         * @return this builder
         * @throws IllegalArgumentException when {@code currency} is not an ISO 4217 code
         */
        public Builder currency(String currency) {
            try {
                java.util.Currency.getInstance(currency);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("currency must be an ISO 4217 code: " + currency);
            }
            this.currency = currency;
            return this;
        }

        /**
         * Supplies exchange rates ranked above the default sources (live ECB rates, then the bundled table).
         *
         * @param converter the converter, or {@code null} for none
         * @return this builder
         */
        public Builder currencyConverter(com.eqixiac.equinix.design.spi.CurrencyConverter converter) {
            this.currencyConverter = converter;
            return this;
        }

        /**
         * Replaces the default rate sources, for example with {@code CurrencyConverters.bundledOnly()} offline.
         *
         * @param defaults the fallback converter; {@code null} restores the default
         * @return this builder
         */
        public Builder defaultRates(com.eqixiac.equinix.design.spi.CurrencyConverter defaults) {
            this.defaultRates = defaults;
            return this;
        }

        /**
         * Replaces the rules (the defaults are {@link DefaultRules#all()}).
         *
         * @param rules the rules
         * @return this builder
         */
        public Builder rules(List<EstateRule> rules) {
            this.rules = new ArrayList<>(rules);
            return this;
        }

        /**
         * Adds a rule to the current list.
         *
         * @param rule the rule
         * @return this builder
         */
        public Builder addRule(EstateRule rule) {
            this.rules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        /**
         * Sets the thresholds.
         *
         * @param thresholds the thresholds
         * @return this builder
         */
        public Builder thresholds(EstateThresholds thresholds) {
            this.thresholds = Objects.requireNonNull(thresholds, "thresholds");
            return this;
        }

        /**
         * Sets the clock (as-of time of a live snapshot, end of the utilisation window, report time).
         *
         * @param clock the clock
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Receives one progress line per collection step.
         *
         * @param progress the consumer
         * @return this builder
         */
        public Builder progress(Consumer<String> progress) {
            this.progress = progress;
            return this;
        }

        /**
         * Collects the snapshot only (live), or returns the given snapshot (offline).
         *
         * @return the snapshot
         * @throws IllegalStateException when a live run has no scope
         */
        public EstateInventory collect() {
            if (snapshot != null) {
                return snapshot;
            }
            if (scope == null) {
                throw new IllegalStateException("A live estate analysis needs a scope: EstateScope.projects(...), "
                        + "accounts(...), resources(...) or, explicitly, entireAccount(max)");
            }
            return new EstateCollector(fabric, networkEdge, scope, clock, progress).collect();
        }

        /**
         * Runs the analysis.
         *
         * @return the report
         */
        public EstateReport analyze() {
            EstateInventory inv = collect();
            UtilizationSnapshot util = utilization;
            if (util == null && window != null && fabric != null) {
                long t = System.nanoTime();
                util = UtilizationProbe.over(fabric).window(window).clock(clock).maxSubjects(maxMetricSearches)
                        .pauseMillis(metricPauseMillis).includePorts(includePortMetrics).build().probe(inv);
                if (progress != null) {
                    progress.accept("metrics: " + util.withData() + " of " + util.stats().size()
                            + " resources with data, " + (System.nanoTime() - t) / 1_000_000 + " ms");
                }
            }
            if (util == null) {
                util = UtilizationSnapshot.none();
            }
            RateCard card = rateCardSet ? rateCard
                    : fabric != null ? RateCard.standardChain(fabric) : ReferenceRateCard.standard();
            EstatePricing pricing = new EstatePricing(card, thresholds, currency, currencyConverter, defaultRates);
            RuleContext ctx = new RuleContext(pricing, thresholds, inv.asOf());
            List<EstateFinding> findings = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<String> ran = new ArrayList<>();
            for (EstateRule rule : rules) {
                ran.add(rule.name());
                try {
                    findings.addAll(rule.evaluate(inv, util, ctx));
                } catch (RuntimeException e) {
                    warnings.add("Rule " + rule.name() + " failed and its findings are missing: "
                            + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " " + e.getMessage()));
                }
            }
            return EstateReport.assemble(inv, util, dedupe(findings, warnings), notAnalysed(inv, util, card), warnings,
                    ran, clock.instant(), pricing.currency(), pricing.currencyNotes());
        }

        private static List<EstateFinding> dedupe(List<EstateFinding> findings, List<String> warnings) {
            Map<String, EstateFinding> byId = new TreeMap<>();
            for (EstateFinding f : findings) {
                if (byId.putIfAbsent(f.id(), f) != null) {
                    warnings.add("Duplicate finding id " + f.id() + " (rule " + f.rule() + "); the first was kept");
                }
            }
            return new ArrayList<>(byId.values());
        }

        private List<String> notAnalysed(EstateInventory inv, UtilizationSnapshot util, RateCard card) {
            List<String> out = new ArrayList<>();
            long live = inv.connections().stream().filter(ConnectionRecord::isLive).count();
            long notLive = inv.connections().size() - live;
            if (util.windowStart() == null) {
                out.add("Utilisation was not collected (set window(...) on a live run or pass a UtilizationSnapshot): "
                        + "right-sizing and idle-connection checks did not run");
            } else {
                long withData = inv.connections().stream().filter(c -> c.isLive() && util.known(c.id()).isPresent())
                        .count();
                if (withData < live) {
                    out.add("Utilisation: " + (live - withData) + " of " + live
                            + " live connection(s) had no usable datapoints in the window (unknown, not zero), so "
                            + "they were not right-sized");
                }
            }
            if (notLive > 0) {
                Map<String, Integer> states = new TreeMap<>();
                inv.connections().stream().filter(c -> !c.isLive())
                        .forEach(c -> states.merge(String.valueOf(c.state()), 1, Integer::sum));
                out.add(notLive + " connection(s) are not provisioned " + states
                        + ": they were checked for hygiene only, not for utilisation, term or redundancy");
            }
            Set<String> outside = new TreeSet<>();
            for (ConnectionRecord c : inv.connections()) {
                for (String p : new String[]{c.aSide().portId(), c.zSide().portId()}) {
                    if (p != null && inv.port(p).isEmpty()) {
                        outside.add(p);
                    }
                }
            }
            if (!outside.isEmpty()) {
                out.add(outside.size() + " port(s) used by in-scope connections are outside the scope (another "
                        + "project or account): their capacity and redundancy were not assessed");
            }
            Set<String> noLimit = new TreeSet<>();
            for (RouterRecord r : inv.routers()) {
                if (r.isLive() && r.packageCode() != null && !inv.routerPackageLimits().containsKey(r.packageCode())
                        && !thresholds.routerPackageConnectionLimits().containsKey(r.packageCode())) {
                    noLimit.add(r.packageCode());
                }
            }
            if (!noLimit.isEmpty()) {
                out.add("Cloud Router capacity: no connection limit is known for package(s) " + noLimit
                        + " (the live /routerPackages response has no vcCountMax); supply "
                        + "EstateThresholds.routerPackageConnectionLimits to assess it");
            }
            if (card == null) {
                out.add("Pricing: no rate card was supplied, so every financial impact is unpriced");
            }
            if (!inv.ports().isEmpty()) {
                out.add("Port prices: the rate card has no Fabric port prices, so idle-port and port-capacity "
                        + "findings are unpriced");
            }
            out.add("Out of scope for this analyser: cross-connects, colocation space and power, Internet Access "
                    + "services, Network Edge licences and device prices, and cloud-provider charges");
            return out;
        }
    }
}
