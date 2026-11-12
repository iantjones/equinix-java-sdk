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

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.cost.CurrencyConverters;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.CurrencyConverter;
import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prices existing resources through a {@link RateCard} and turns each quote into a {@link CostLine}
 * with a range that reflects its provenance: live Equinix, contract ({@code CUSTOM}) and provider-API
 * prices are exact; reference prices get ± {@link EstateThresholds#referencePriceRangePercent()};
 * estimates get ± {@link EstateThresholds#estimatePriceRangePercent()}.
 *
 * <p>Every line is restated in one reporting currency ({@link #currency()}, USD unless set) through
 * {@code CurrencyConverters}, so every finding's saving is in that currency and savings can be totalled: a line
 * quoted in another currency (live Fabric pricing quotes London in GBP) keeps its original amount and currency,
 * the rate and the rate's source and date in {@code CostLine.conversion()}. A price whose currency no rate
 * source covers is unpriced with that reason.</p>
 *
 * <p>A lookup that cannot be priced returns a {@link Priced} with a reason, never a zero. Lookups are
 * cached for the life of this object, so a rule that prices the same tier many times makes one
 * request.</p>
 */
public final class EstatePricing {

    /** The reporting currency when none is set. */
    public static final String DEFAULT_CURRENCY = "USD";

    private final RateCard rateCard;
    private final EstateThresholds thresholds;
    private final String currency;
    private final CurrencyConverters.Chain converter;
    private final Map<String, Priced> cache = new ConcurrentHashMap<>();

    /**
     * Creates pricing over a rate card, reporting in USD with the default rate sources (live ECB rates, then the
     * bundled table).
     *
     * @param rateCard   the rate card, or {@code null} to leave everything unpriced
     * @param thresholds the thresholds (for the price ranges)
     */
    public EstatePricing(RateCard rateCard, EstateThresholds thresholds) {
        this(rateCard, thresholds, DEFAULT_CURRENCY, null, null);
    }

    /**
     * Creates pricing over a rate card in a stated reporting currency.
     *
     * @param rateCard     the rate card, or {@code null} to leave everything unpriced
     * @param thresholds   the thresholds (for the price ranges)
     * @param currency     the ISO 4217 reporting currency; {@code null} for USD
     * @param supplied     a converter ranked above the default rate sources, or {@code null}
     * @param defaultRates replaces the default rate sources (for example {@code CurrencyConverters.bundledOnly()});
     *                     {@code null} for the defaults
     */
    public EstatePricing(RateCard rateCard, EstateThresholds thresholds, String currency, CurrencyConverter supplied,
                         CurrencyConverter defaultRates) {
        this.rateCard = rateCard;
        this.thresholds = thresholds == null ? EstateThresholds.defaults() : thresholds;
        this.currency = currency == null || currency.isBlank() ? DEFAULT_CURRENCY : currency;
        this.converter = CurrencyConverters.forTerms(CommercialTerms.builder().currency(this.currency).build(),
                supplied, defaultRates);
    }

    /**
     * The currency every priced line is restated in.
     *
     * @return an ISO 4217 code
     */
    public String currency() {
        return currency;
    }

    /**
     * The converter in use (for its fallback notes).
     *
     * @return the converter chain
     */
    public CurrencyConverters.Chain converter() {
        return converter;
    }

    /**
     * What happened to currencies so far: one advisory per source currency converted, and any rate source that
     * fell back.
     *
     * @return the notes, empty when every price was already in {@link #currency()}
     */
    public List<String> currencyNotes() {
        List<CostLine> lines = new ArrayList<>();
        for (Priced p : cache.values()) {
            if (p.isPriced()) {
                lines.add(p.line());
            }
        }
        lines.sort(java.util.Comparator.comparing(CostLine::label).thenComparing(CostLine::currency));
        List<String> notes = new ArrayList<>();
        CurrencyConverters.advisory(new PortfolioCost(lines, List.of())).ifPresent(notes::add);
        notes.addAll(converter.notes());
        return notes;
    }

    /**
     * Pricing that prices nothing, for analyses run without a rate card.
     *
     * @return unavailable pricing
     */
    public static EstatePricing unavailable() {
        return new EstatePricing(null, EstateThresholds.defaults());
    }

    /**
     * Prices one virtual connection.
     *
     * @param label         the cost-line label
     * @param type          the connection type as the API reports it (for example {@code EVPL_VC})
     * @param bandwidthMbps the bandwidth
     * @param metro         the metro code, or {@code null}
     * @param term          the term
     * @return the priced line, or the reason it could not be priced
     */
    public Priced connection(String label, String type, Integer bandwidthMbps, String metro, Term term) {
        if (rateCard == null) {
            return Priced.unpriced("no rate card was supplied");
        }
        if (bandwidthMbps == null || bandwidthMbps <= 0) {
            return Priced.unpriced("the bandwidth is unknown or zero");
        }
        ConnectionType ct = connectionType(type);
        MetroCode mc = MetroCode.lookup(metro).orElse(null);
        String key = "c|" + ct + "|" + bandwidthMbps + "|" + mc + "|" + term;
        Priced p = cache.computeIfAbsent(key, k -> quote(() -> rateCard.connection(ct, bandwidthMbps, mc, term),
                "no " + type + " " + bandwidthMbps + " Mbps price" + (mc == null ? "" : " in " + mc) + " for " + term));
        return p.relabel(label, metro);
    }

    /**
     * Prices one Cloud Router.
     *
     * @param label       the cost-line label
     * @param packageCode the package code
     * @param metro       the metro code, or {@code null}
     * @param term        the term
     * @return the priced line, or the reason it could not be priced
     */
    public Priced router(String label, String packageCode, String metro, Term term) {
        if (rateCard == null) {
            return Priced.unpriced("no rate card was supplied");
        }
        if (packageCode == null) {
            return Priced.unpriced("the router package is unknown");
        }
        MetroCode mc = MetroCode.lookup(metro).orElse(null);
        String key = "r|" + packageCode + "|" + mc + "|" + term;
        Priced p = cache.computeIfAbsent(key, k -> quote(() -> rateCard.cloudRouter(packageCode, mc, term),
                "no Cloud Router " + packageCode + " price" + (mc == null ? "" : " in " + mc) + " for " + term));
        return p.relabel(label, metro);
    }

    /**
     * The term a resource is billed on today.
     *
     * @param termMonths the recorded term length, or {@code null}
     * @return the matching term ({@link Term#MONTH_1} for on-demand or an unlisted length)
     */
    public static Term termOf(Integer termMonths) {
        if (termMonths == null) {
            return Term.MONTH_1;
        }
        for (Term t : Term.values()) {
            if (t.months() == termMonths) {
                return t;
            }
        }
        return Term.MONTH_1;
    }

    private Priced quote(java.util.function.Supplier<Optional<PriceQuote>> lookup, String missing) {
        try {
            Optional<PriceQuote> q = lookup.get();
            if (q.isEmpty()) {
                return Priced.unpriced(missing);
            }
            CostLine line = toLine(q.get());
            Optional<CostLine> restated = CurrencyConverters.convert(line, currency, converter);
            if (restated.isEmpty()) {
                return Priced.unpriced("priced in " + line.currency() + " " + line.monthly().toPlainString()
                        + "/month; " + converter.missingReason(line.currency(), currency));
            }
            return Priced.of(restated.get());
        } catch (RuntimeException e) {
            return Priced.unpriced("the rate card failed: " + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : " " + e.getMessage()));
        }
    }

    private CostLine toLine(PriceQuote q) {
        BigDecimal monthly = q.getMonthlyRecurring();
        double pct = switch (q.getSource()) {
            case EQUINIX_LIVE, CUSTOM, PROVIDER_API -> 0;
            case ESTIMATE -> thresholds.estimatePriceRangePercent();
            case REFERENCE, COMPOSITE -> thresholds.referencePriceRangePercent();
        };
        if (pct == 0) {
            return CostLine.exact("priced item", null, monthly, q.getNonRecurring(), q.getCurrency().getCurrencyCode(),
                    q.getSource(), q.getNote());
        }
        BigDecimal delta = monthly.multiply(BigDecimal.valueOf(pct / 100.0)).setScale(2, RoundingMode.HALF_UP);
        return new CostLine("priced item", null, monthly, q.getNonRecurring(), q.getCurrency().getCurrencyCode(),
                q.getSource(), monthly.subtract(delta), monthly.add(delta), q.getNote());
    }

    private static ConnectionType connectionType(String type) {
        if (type == null) {
            return null;
        }
        try {
            ConnectionType ct = ConnectionType.valueOf(type);
            return ct == ConnectionType.UNKNOWN ? null : ct;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * The result of a price lookup: a cost line, or the reason there is none.
     *
     * @param line   the cost line, or {@code null}
     * @param reason why it is unpriced, or {@code null}
     */
    public record Priced(CostLine line, String reason) {

        static Priced of(CostLine line) {
            return new Priced(line, null);
        }

        static Priced unpriced(String reason) {
            return new Priced(null, reason);
        }

        /**
         * Whether a price was found.
         *
         * @return {@code true} when priced
         */
        public boolean isPriced() {
            return line != null;
        }

        /**
         * Whether the price is live or contracted (exact), as opposed to reference or estimate.
         *
         * @return {@code true} for {@code EQUINIX_LIVE}, {@code CUSTOM} or {@code PROVIDER_API}
         */
        public boolean isExact() {
            return line != null && (line.source() == PriceSource.EQUINIX_LIVE || line.source() == PriceSource.CUSTOM
                    || line.source() == PriceSource.PROVIDER_API);
        }

        Priced relabel(String label, String metro) {
            if (line == null) {
                return this;
            }
            return of(new CostLine(label, metro == null ? null : MetroId.of(metro), line.monthly(), line.oneTime(),
                    line.currency(), line.source(), line.monthlyLow(), line.monthlyHigh(), line.note(),
                    line.conversion()));
        }
    }
}
