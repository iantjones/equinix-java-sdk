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

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;

import java.math.BigDecimal;

/**
 * One priced item of a portfolio, with where the figure came from and how far it could be off.
 *
 * @param label        what is being paid for, for example {@code Fabric connection EVPL_VC 1000 Mbps to AWS}
 * @param metro        the metro the item belongs to, or {@code null} for a portfolio-wide item
 * @param monthly      the expected recurring monthly amount
 * @param oneTime      the one-time amount (setup, installation); zero when none
 * @param currency     ISO 4217 code of every amount on this line
 * @param source       where the figure came from (live API, reference table, estimate...)
 * @param monthlyLow   the low end of the plausible monthly range; equals {@code monthly} for a live price
 * @param monthlyHigh  the high end of the plausible monthly range; equals {@code monthly} for a live price
 * @param note         provenance detail (rate-card note, as-of date, assumption); may be {@code null}
 * @param conversion   when the item was priced in another currency: the original amounts and currency and the
 *                     rate used ({@code monthly}, {@code oneTime} and {@code currency} are then the converted
 *                     figures); {@code null} when the item was priced in {@code currency}
 */
public record CostLine(String label, MetroId metro, BigDecimal monthly, BigDecimal oneTime, String currency,
                       PriceSource source, BigDecimal monthlyLow, BigDecimal monthlyHigh, String note,
                       Conversion conversion) {

    /**
     * A line priced in its own currency (no conversion).
     *
     * @param label       see the record
     * @param metro       see the record
     * @param monthly     see the record
     * @param oneTime     see the record
     * @param currency    see the record
     * @param source      see the record
     * @param monthlyLow  see the record
     * @param monthlyHigh see the record
     * @param note        see the record
     */
    public CostLine(String label, MetroId metro, BigDecimal monthly, BigDecimal oneTime, String currency,
                    PriceSource source, BigDecimal monthlyLow, BigDecimal monthlyHigh, String note) {
        this(label, metro, monthly, oneTime, currency, source, monthlyLow, monthlyHigh, note, null);
    }

    /**
     * How a line was restated in the comparison currency.
     *
     * @param originalMonthly  the monthly amount as priced
     * @param originalOneTime  the one-time amount as priced
     * @param originalCurrency the currency it was priced in
     * @param rate             units of the line's {@code currency} per one unit of {@code originalCurrency}
     * @param kind             where the rate came from: {@link CurrencyConverter.Kind}
     * @param source           the rate's source, as stated by the converter
     * @param asOf             the rate's date
     * @param bandPercent      the widening applied to the line's range for exchange-rate uncertainty, either side
     */
    public record Conversion(BigDecimal originalMonthly, BigDecimal originalOneTime, String originalCurrency,
                             BigDecimal rate, CurrencyConverter.Kind kind, String source, String asOf,
                             double bandPercent) {
    }

    /** @return {@code true} when the line was converted from another currency */
    public boolean converted() {
        return conversion != null;
    }

    /** Fills defaults and rejects a range that does not contain the expected amount. */
    public CostLine {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("A cost line needs a label");
        }
        if (monthly == null || currency == null || source == null) {
            throw new IllegalArgumentException("A cost line needs a monthly amount, a currency and a source");
        }
        oneTime = oneTime == null ? BigDecimal.ZERO : oneTime;
        monthlyLow = monthlyLow == null ? monthly : monthlyLow;
        monthlyHigh = monthlyHigh == null ? monthly : monthlyHigh;
        if (monthlyLow.compareTo(monthly) > 0 || monthlyHigh.compareTo(monthly) < 0) {
            throw new IllegalArgumentException("The range " + monthlyLow + ".." + monthlyHigh
                    + " must contain the expected amount " + monthly);
        }
    }

    /**
     * A line whose figure is exact (no range), for example a live Equinix price.
     *
     * @param label    what is being paid for
     * @param metro    the metro, or {@code null}
     * @param monthly  the recurring monthly amount
     * @param oneTime  the one-time amount, or {@code null} for none
     * @param currency ISO 4217 code
     * @param source   where the figure came from
     * @param note     provenance detail, or {@code null}
     * @return the line
     */
    public static CostLine exact(String label, MetroId metro, BigDecimal monthly, BigDecimal oneTime, String currency,
                                 PriceSource source, String note) {
        return new CostLine(label, metro, monthly, oneTime, currency, source, monthly, monthly, note);
    }
}
