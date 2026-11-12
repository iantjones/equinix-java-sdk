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

package com.eqixiac.equinix.design.spi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Restates an amount in another currency, so a design whose prices come back in several currencies
 * (the live Fabric price search quotes London in GBP, Frankfurt in EUR, Tokyo in JPY) is priced and
 * ranked in one. Implementations must be deterministic and pure: the same arguments give the same rate,
 * and no call reaches the network unless the implementation was supplied for that purpose.
 *
 * <p>The SDK resolves rates in this order (see {@code com.eqixiac.equinix.design.cost.CurrencyConverters}):
 * rates pinned in the spec's {@code commercial.exchangeRates} (part of the spec's fingerprint, never widened
 * unless the spec states a band), then a converter supplied by the caller or a {@link DesignPlugin}, then the
 * ECB reference rates fetched once per run from the Frankfurter API, then the bundled reference table. A currency absent from every source leaves the
 * line unpriced with that reason.</p>
 */
public interface CurrencyConverter {

    /** Where a rate came from, in precedence order. */
    enum Kind {
        /** Pinned in the spec ({@code commercial.exchangeRates}). */
        PINNED,
        /** From a converter the caller or a plug-in supplied. */
        SUPPLIED,
        /** A published reference rate fetched live (the default: ECB reference rates via Frankfurter). */
        LIVE,
        /** From the SDK's bundled reference table: an estimate. */
        REFERENCE
    }

    /**
     * A rate between two currencies.
     *
     * @param from        ISO 4217 code converted from
     * @param to          ISO 4217 code converted to
     * @param rate        units of {@code to} per one unit of {@code from}
     * @param kind        where it came from
     * @param source      a statement of the source (publisher, table, feed)
     * @param asOf        the date the rate was observed
     * @param bandPercent the widening, either side, to apply to converted ranges (0 for an exact pinned rate)
     */
    record Rate(String from, String to, BigDecimal rate, Kind kind, String source, String asOf, double bandPercent) {

        /** Requires a positive rate and the provenance fields. */
        public Rate {
            if (from == null || to == null || rate == null || rate.signum() <= 0 || kind == null || source == null
                    || asOf == null) {
                throw new IllegalArgumentException("A rate needs both currencies, a positive rate, a kind, a source "
                        + "and an as-of date");
            }
            if (!(bandPercent >= 0 && bandPercent <= 50)) {
                throw new IllegalArgumentException("bandPercent must be between 0 and 50");
            }
        }

        /**
         * @param amount an amount in {@code from}
         * @return the amount in {@code to}, rounded half-up to cents
         */
        public BigDecimal apply(BigDecimal amount) {
            return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * @param from ISO 4217 code
     * @param to   ISO 4217 code
     * @return the rate, or empty when this converter has none for the pair
     */
    Optional<Rate> rate(String from, String to);

    /** @return a short name for reasons and diagnostics */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Converts an amount.
     *
     * @param amount the amount in {@code from}
     * @param from   ISO 4217 code
     * @param to     ISO 4217 code
     * @return the converted amount (rounded half-up to cents) with the rate used, or empty when there is no rate
     */
    default Optional<Converted> convert(BigDecimal amount, String from, String to) {
        return rate(from, to).map(r -> new Converted(amount, r.apply(amount), r));
    }

    /**
     * An amount restated in another currency.
     *
     * @param original  the amount as given
     * @param converted the amount in the target currency
     * @param rate      the rate used, with its source and date
     */
    record Converted(BigDecimal original, BigDecimal converted, Rate rate) {
    }
}
