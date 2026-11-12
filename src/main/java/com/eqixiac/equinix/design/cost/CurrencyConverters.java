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

import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.CurrencyConverter;
import com.eqixiac.equinix.design.spi.PortfolioCost;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Builds the currency converter every cost model uses, and reports what it did.
 *
 * <h2>Precedence</h2>
 * <ol>
 *   <li>rates pinned in the spec ({@code commercial.exchangeRates}; in the fingerprint; no widening unless the
 *       spec states a band);</li>
 *   <li>a converter the caller or a {@code DesignPlugin} supplies;</li>
 *   <li>the default: live ECB reference rates via Frankfurter ({@link FrankfurterExchangeRates}, one call per run,
 *       &plusmn;{@value FrankfurterExchangeRates#BAND_PERCENT}%);</li>
 *   <li>the fallback: the bundled table ({@link BundledExchangeRates}, &plusmn;{@value BundledExchangeRates#BAND_PERCENT}%).</li>
 * </ol>
 * <p>Air-gapped use: {@link #bundledOnly()}, or the system property {@value #OFFLINE_PROPERTY}{@code =true},
 * removes step 3 everywhere. A currency missing from every source leaves the line unpriced with that reason.</p>
 */
public final class CurrencyConverters {

    /** System property that disables the live FX source when {@code true}. */
    public static final String OFFLINE_PROPERTY = "equinix.design.fx.offline";

    private CurrencyConverters() {
    }

    /** @return the bundled reference table alone (no network) */
    public static CurrencyConverter bundledOnly() {
        return BundledExchangeRates.standard();
    }

    /** @return a converter with no rates: every conversion is unpriced (for tests and strict runs) */
    public static CurrencyConverter none() {
        return new CurrencyConverter() {
            @Override
            public Optional<Rate> rate(String from, String to) {
                return Optional.empty();
            }

            @Override
            public String name() {
                return "no exchange rates";
            }
        };
    }

    /**
     * The default: live ECB rates for {@code target} (unless {@value #OFFLINE_PROPERTY} is set) then the bundled
     * table.
     *
     * @param target the comparison currency
     * @return the converter
     */
    public static CurrencyConverter standard(String target) {
        if (Boolean.getBoolean(OFFLINE_PROPERTY)) {
            return chain(bundledOnly());
        }
        return chain(new FrankfurterExchangeRates(target, null, null), bundledOnly());
    }

    /**
     * The converter for a spec: pinned rates, then {@code supplied}, then {@link #standard}.
     *
     * @param spec     the spec (currency and pinned rates)
     * @param supplied a caller or plug-in converter, or {@code null}
     * @return the converter
     */
    public static Chain forSpec(DesignSpec spec, CurrencyConverter supplied) {
        return forTerms(spec.commercial(), supplied, null);
    }

    /**
     * @param terms     the commercial terms (currency and pinned rates)
     * @param supplied  a caller or plug-in converter, or {@code null}
     * @param fallbacks what to use after {@code supplied}; {@code null} for {@link #standard}
     * @return the converter
     */
    public static Chain forTerms(CommercialTerms terms, CurrencyConverter supplied, CurrencyConverter fallbacks) {
        List<CurrencyConverter> parts = new ArrayList<>();
        if (terms.exchangeRates() != null) {
            parts.add(new PinnedExchangeRates(terms.exchangeRates(), terms.currencyOrDefault()));
        }
        if (supplied != null) {
            parts.add(supplied);
        }
        parts.add(fallbacks != null ? fallbacks : standard(terms.currencyOrDefault()));
        return chain(parts.toArray(new CurrencyConverter[0]));
    }

    /**
     * @param converters in precedence order
     * @return a converter that answers from the first that has the pair
     */
    public static Chain chain(CurrencyConverter... converters) {
        return new Chain(Arrays.stream(converters).filter(Objects::nonNull).toList());
    }

    /** First-answer-wins composition that can report fallbacks. */
    public static final class Chain implements CurrencyConverter {
        private final List<CurrencyConverter> parts;

        private Chain(List<CurrencyConverter> parts) {
            List<CurrencyConverter> flat = new ArrayList<>();
            for (CurrencyConverter c : parts) {
                if (c instanceof Chain chain) {
                    flat.addAll(chain.parts);
                } else {
                    flat.add(c);
                }
            }
            this.parts = List.copyOf(flat);
        }

        @Override
        public Optional<Rate> rate(String from, String to) {
            for (CurrencyConverter c : parts) {
                Optional<Rate> r = c.rate(from, to);
                if (r.isPresent()) {
                    return r;
                }
            }
            return Optional.empty();
        }

        @Override
        public String name() {
            List<String> names = new ArrayList<>();
            for (CurrencyConverter c : parts) {
                names.add(c.name());
            }
            return String.join(" > ", names);
        }

        /** @return notes from sources that fell back (e.g. the live call failed) */
        public List<String> notes() {
            List<String> notes = new ArrayList<>();
            for (CurrencyConverter c : parts) {
                if (c instanceof FrankfurterExchangeRates live) {
                    live.fallbackNote().ifPresent(notes::add);
                }
            }
            return notes;
        }

        /**
         * @param from ISO 4217 code
         * @param to   ISO 4217 code
         * @return the reason no source converts the pair
         */
        public String missingReason(String from, String to) {
            return "no exchange rate " + from + " to " + to + " in any source (" + name() + ")";
        }
    }

    /**
     * Restates one cost line in {@code target}: the line keeps its original amounts, currency and the rate used
     * (with the rate's kind, source and date) in {@link CostLine#conversion()}, its note states the rate, and its
     * range is widened by the rate's band. A line converted at a bundled reference rate becomes
     * {@link com.eqixiac.equinix.design.value.ratecard.PriceSource#ESTIMATE}. This is the one conversion every cost
     * figure in the design package goes through (the optimiser's cost model, the wizard's plan pricing and the
     * estate analysis).
     *
     * @param line      the line as priced
     * @param target    the ISO 4217 code to restate it in
     * @param converter the rate source
     * @return the line in {@code target} (the line itself when already in it), or empty when no source has the rate
     */
    public static Optional<CostLine> convert(CostLine line, String target, CurrencyConverter converter) {
        if (line == null || target == null || target.equals(line.currency())) {
            return Optional.ofNullable(line);
        }
        Optional<CurrencyConverter.Rate> found = converter == null ? Optional.empty()
                : converter.rate(line.currency(), target);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        CurrencyConverter.Rate rate = found.get();
        double band = rate.bandPercent();
        BigDecimal monthly = rate.apply(line.monthly());
        BigDecimal low = PriceBands.fxLow(line.monthlyLow(), rate.rate(), band).min(monthly);
        BigDecimal high = PriceBands.fxHigh(line.monthlyHigh(), rate.rate(), band).max(monthly);
        com.eqixiac.equinix.design.value.ratecard.PriceSource source = rate.kind() == CurrencyConverter.Kind.REFERENCE
                ? com.eqixiac.equinix.design.value.ratecard.PriceSource.ESTIMATE : line.source();
        String bandText = BigDecimal.valueOf(band).stripTrailingZeros().toPlainString();
        return Optional.of(new CostLine(line.label(), line.metro(), monthly, rate.apply(line.oneTime()), target,
                source, low, high, (line.note() == null ? "" : line.note() + "; ") + "converted from "
                        + line.currency() + " " + line.monthly().toPlainString() + " at " + rate.rate().stripTrailingZeros()
                        .toPlainString() + " " + target + "/" + line.currency() + " (" + rate.source()
                        + ", as of " + rate.asOf() + ")" + (band == 0 ? "" : "; range widened +/-" + bandText
                        + "% for exchange-rate uncertainty"),
                new CostLine.Conversion(line.monthly(), line.oneTime(), line.currency(), rate.rate(), rate.kind(),
                        rate.source(), rate.asOf(), band)));
    }

    /**
     * A non-blocking advisory, e.g. "3 of 12 line(s) converted to USD: GBP at ECB reference rate via Frankfurter,
     * 2026-11-11; SGD at bundled reference table, as of 2026-11-11".
     *
     * @param cost a priced portfolio
     * @return the advisory, empty when nothing was converted
     */
    public static Optional<String> advisory(PortfolioCost cost) {
        if (cost == null) {
            return Optional.empty();
        }
        int converted = 0;
        Set<String> parts = new LinkedHashSet<>();
        String target = null;
        for (CostLine line : cost.lines()) {
            if (line.converted()) {
                converted++;
                target = line.currency();
                parts.add(line.conversion().originalCurrency() + " at " + line.conversion().source());
            }
        }
        if (converted == 0) {
            return Optional.empty();
        }
        return Optional.of(converted + " of " + cost.lines().size() + " line(s) converted to " + target + ": "
                + String.join("; ", new TreeSet<>(parts)));
    }

    /**
     * The exact rates the given costs used, as pinnable spec rates, so a run can be reproduced.
     *
     * @param target the comparison currency
     * @param costs  priced portfolios
     * @return the rates, or empty when nothing was converted
     */
    public static Optional<CommercialTerms.ExchangeRates> usedRates(String target, List<PortfolioCost> costs) {
        Map<String, BigDecimal> rates = new TreeMap<>();
        Set<String> sources = new TreeSet<>();
        Set<String> dates = new TreeSet<>();
        for (PortfolioCost cost : costs) {
            if (cost == null) {
                continue;
            }
            for (CostLine line : cost.lines()) {
                if (line.converted() && line.currency().equals(target)) {
                    rates.putIfAbsent(line.conversion().originalCurrency(), line.conversion().rate());
                    sources.add(line.conversion().source());
                    dates.add(line.conversion().asOf());
                }
            }
        }
        if (rates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CommercialTerms.ExchangeRates(rates, String.join(", ", dates),
                "rates used by a previous run: " + String.join("; ", sources), null));
    }

    /**
     * The spec with the rates the given costs used pinned in {@code commercial.exchangeRates} (rates already
     * pinned are kept and take precedence), so re-running it reproduces the result.
     *
     * @param spec  the spec that was run
     * @param costs the costs that run produced
     * @return the spec with pinned rates (unchanged when nothing was converted)
     */
    public static DesignSpec pinUsedRates(DesignSpec spec, List<PortfolioCost> costs) {
        String target = spec.commercial().currencyOrDefault();
        Optional<CommercialTerms.ExchangeRates> used = usedRates(target, costs);
        if (used.isEmpty()) {
            return spec;
        }
        CommercialTerms.ExchangeRates merged = used.get();
        CommercialTerms.ExchangeRates existing = spec.commercial().exchangeRates();
        if (existing != null) {
            Map<String, BigDecimal> all = new TreeMap<>(merged.rates());
            all.putAll(existing.rates());
            merged = new CommercialTerms.ExchangeRates(all, existing.asOf() + "; " + merged.asOf(),
                    existing.source() + "; " + merged.source(), existing.bandPercent());
        }
        return spec.toBuilder().commercial(spec.commercial().toBuilder().exchangeRates(merged).build()).build();
    }
}
