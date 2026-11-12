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

package com.eqixiac.equinix.design.spec;

import com.eqixiac.equinix.design.value.ratecard.Term;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The commercial frame the design is priced and judged in.
 *
 * @param currency          ISO 4217 code budgets are stated in and totals are compared in; may be {@code null} (USD)
 * @param budgetMaxMonthly  most the design may cost per month, in {@code currency}; may be {@code null}
 * @param budgetMinMonthly  least expected monthly spend, in {@code currency}; may be {@code null}
 * @param term              commitment term to price at; may be {@code null} (let the engine compare terms)
 * @param annualGrowthRate  expected yearly traffic growth as a fraction ({@code 0.25} = 25%); may be {@code null}
 * @param horizonMonths     how far ahead to project cost and capacity; may be {@code null} (12)
 * @param exchangeRates     caller-supplied rates that restate prices in other currencies in {@code currency};
 *                          may be {@code null} (nothing is converted). The SDK never fetches or invents a rate.
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommercialTerms(String currency, BigDecimal budgetMaxMonthly, BigDecimal budgetMinMonthly, Term term,
                              Double annualGrowthRate, Integer horizonMonths, ExchangeRates exchangeRates) {

    /**
     * The terms without exchange rates.
     *
     * @param currency         see the record
     * @param budgetMaxMonthly see the record
     * @param budgetMinMonthly see the record
     * @param term             see the record
     * @param annualGrowthRate see the record
     * @param horizonMonths    see the record
     */
    public CommercialTerms(String currency, BigDecimal budgetMaxMonthly, BigDecimal budgetMinMonthly, Term term,
                           Double annualGrowthRate, Integer horizonMonths) {
        this(currency, budgetMaxMonthly, budgetMinMonthly, term, annualGrowthRate, horizonMonths, null);
    }

    /** @return the currency to use, {@code USD} when unset */
    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "USD" : currency;
    }

    /** @return the projection horizon in months, 12 when unset */
    public int horizonMonthsOrDefault() {
        return horizonMonths == null ? 12 : horizonMonths;
    }

    /**
     * Exchange rates pinned in the spec: they take precedence over every other rate source (a caller's
     * converter, the live ECB rates, the bundled table) and are part of the spec's fingerprint, so a run can be
     * reproduced exactly. The live Fabric price search quotes London in GBP and Frankfurt in EUR (observed
     * 2026-11-12) while the provider fees are in USD; a converted line keeps its original amount and currency
     * and records the rate, {@code asOf} and {@code source}. A pinned rate is taken as exact: the range is
     * widened only by {@code bandPercent} when one is stated.
     *
     * @param rates       units of the spec's currency per ONE unit of each keyed currency
     *                    ({@code {"GBP": 1.27}} with currency USD: 1 GBP = 1.27 USD); keys are ISO 4217
     *                    codes, values positive
     * @param asOf        when the rates were observed (free text, for example {@code "2026-11-12"});
     *                    required
     * @param source      where they came from (for example a treasury rate sheet); required
     * @param bandPercent half-width of an FX allowance applied to lines converted at these rates, 0 to 50;
     *                    {@code null} means {@value #DEFAULT_BAND_PERCENT} (pinned rates are exact)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExchangeRates(Map<String, BigDecimal> rates, String asOf, String source, Double bandPercent) {

        /** The FX allowance used when {@code bandPercent} is not set: none, a pinned rate is exact. */
        public static final double DEFAULT_BAND_PERCENT = 0.0;

        /** Validates the rates and sorts them by currency code. */
        public ExchangeRates {
            if (rates == null || rates.isEmpty()) {
                throw new IllegalArgumentException("commercial.exchangeRates.rates must name at least one currency");
            }
            Map<String, BigDecimal> sorted = new TreeMap<>();
            for (Map.Entry<String, BigDecimal> e : rates.entrySet()) {
                String code = e.getKey();
                try {
                    Currency.getInstance(code);
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("commercial.exchangeRates.rates: '" + code
                            + "' is not an ISO 4217 currency code");
                }
                if (e.getValue() == null || e.getValue().signum() <= 0) {
                    throw new IllegalArgumentException("commercial.exchangeRates.rates." + code + " must be positive");
                }
                sorted.put(code, e.getValue());
            }
            if (asOf == null || asOf.isBlank()) {
                throw new IllegalArgumentException("commercial.exchangeRates.asOf is required: a rate without a date "
                        + "cannot be judged");
            }
            if (source == null || source.isBlank()) {
                throw new IllegalArgumentException("commercial.exchangeRates.source is required");
            }
            if (bandPercent != null && !(bandPercent >= 0 && bandPercent <= 50)) {
                throw new IllegalArgumentException("commercial.exchangeRates.bandPercent must be between 0 and 50");
            }
            rates = java.util.Collections.unmodifiableMap(sorted);
        }

        /**
         * @param currency an ISO 4217 code
         * @return units of the spec currency per one unit of {@code currency}, or empty when not supplied
         */
        public Optional<BigDecimal> rate(String currency) {
            return Optional.ofNullable(rates.get(currency));
        }

        /** @return the FX allowance in percent either side */
        public double bandPercentOrDefault() {
            return bandPercent == null ? DEFAULT_BAND_PERCENT : bandPercent;
        }
    }
}
