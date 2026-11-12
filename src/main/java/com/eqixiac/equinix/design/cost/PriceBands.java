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

import com.eqixiac.equinix.design.value.ratecard.PriceSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The plausible range of a price, derived only from where it came from. A live or caller-supplied
 * figure is exact; anything indicative carries a stated band around its expected value.
 *
 * <ul>
 *   <li>{@link PriceSource#EQUINIX_LIVE}, {@link PriceSource#CUSTOM}, {@link PriceSource#PROVIDER_API}: exact.</li>
 *   <li>{@link PriceSource#REFERENCE} and {@link PriceSource#COMPOSITE}: &plusmn;{@value #REFERENCE_BAND_PERCENT}%.</li>
 *   <li>{@link PriceSource#ESTIMATE}: &plusmn;{@value #ESTIMATE_BAND_PERCENT}%.</li>
 * </ul>
 * <p>A line restated in another currency is widened further by its rate's band ({@link #fxLow},
 * {@link #fxHigh}): 0 for a rate pinned in the spec (unless the spec states one),
 * &plusmn;{@value com.eqixiac.equinix.design.cost.FrankfurterExchangeRates#BAND_PERCENT}% for a live ECB reference
 * rate, &plusmn;{@value com.eqixiac.equinix.design.cost.BundledExchangeRates#BAND_PERCENT}% for the bundled table.</p>
 */
public final class PriceBands {

    /** Half-width of the band around a reference (dated, indicative) figure, in percent. */
    public static final int REFERENCE_BAND_PERCENT = 25;

    /** Half-width of the band around a derived estimate, in percent. */
    public static final int ESTIMATE_BAND_PERCENT = 50;

    private PriceBands() {
    }

    /**
     * @param source where the price came from
     * @return the band half-width as a fraction (0 for an exact source)
     */
    public static double halfWidth(PriceSource source) {
        if (source == null) {
            return ESTIMATE_BAND_PERCENT / 100.0;
        }
        return switch (source) {
            case EQUINIX_LIVE, CUSTOM, PROVIDER_API -> 0.0;
            case REFERENCE, COMPOSITE -> REFERENCE_BAND_PERCENT / 100.0;
            case ESTIMATE -> ESTIMATE_BAND_PERCENT / 100.0;
        };
    }

    /**
     * @param expected the expected amount
     * @param source   where it came from
     * @return the low end of its range
     */
    public static BigDecimal low(BigDecimal expected, PriceSource source) {
        if (halfWidth(source) == 0) {
            return expected;
        }
        return expected.multiply(BigDecimal.valueOf(1 - halfWidth(source))).setScale(2, RoundingMode.HALF_UP)
                .min(expected);
    }

    /**
     * @param expected the expected amount
     * @param source   where it came from
     * @return the high end of its range
     */
    public static BigDecimal high(BigDecimal expected, PriceSource source) {
        if (halfWidth(source) == 0) {
            return expected;
        }
        return expected.multiply(BigDecimal.valueOf(1 + halfWidth(source))).setScale(2, RoundingMode.HALF_UP)
                .max(expected);
    }

    /**
     * The low end of a line restated at an exchange rate, widened by the FX allowance:
     * {@code low x rate x (1 - bandPercent/100)}, rounded half-up to cents.
     *
     * @param low         the line's low end in its own currency
     * @param rate        units of the target currency per unit of the line's currency
     * @param bandPercent the FX allowance either side, in percent
     * @return the converted, widened low end
     */
    public static BigDecimal fxLow(BigDecimal low, BigDecimal rate, double bandPercent) {
        return low.multiply(rate).multiply(BigDecimal.valueOf(1 - bandPercent / 100.0)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * The high end of a converted line: {@code high x rate x (1 + bandPercent/100)}, rounded half-up.
     *
     * @param high        the line's high end in its own currency
     * @param rate        units of the target currency per unit of the line's currency
     * @param bandPercent the FX allowance either side, in percent
     * @return the converted, widened high end
     */
    public static BigDecimal fxHigh(BigDecimal high, BigDecimal rate, double bandPercent) {
        return high.multiply(rate).multiply(BigDecimal.valueOf(1 + bandPercent / 100.0)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * @param source where a price came from
     * @return a short statement of the band, for line notes
     */
    public static String describe(PriceSource source) {
        double half = halfWidth(source);
        return half == 0 ? "exact (" + source + ")" : "range +/-" + Math.round(half * 100) + "% (" + source + ")";
    }
}
