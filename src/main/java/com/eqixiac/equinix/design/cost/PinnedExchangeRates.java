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
import com.eqixiac.equinix.design.spi.CurrencyConverter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * The rates pinned in a spec's {@code commercial.exchangeRates}: units of the spec currency per one unit of
 * each keyed currency. They are part of the spec's fingerprint, so a pinned run is reproducible. They are
 * applied as given: a converted range is widened only by the spec's own {@code bandPercent} (default 0).
 */
public final class PinnedExchangeRates implements CurrencyConverter {

    private final CommercialTerms.ExchangeRates rates;
    private final String target;

    /**
     * @param rates  the pinned rates
     * @param target the spec currency they convert to
     */
    public PinnedExchangeRates(CommercialTerms.ExchangeRates rates, String target) {
        if (rates == null || target == null) {
            throw new IllegalArgumentException("Pinned rates need the rates and the target currency");
        }
        this.rates = rates;
        this.target = target;
    }

    @Override
    public Optional<Rate> rate(String from, String to) {
        String source = "pinned in spec (" + rates.source() + ")";
        if (to.equals(target)) {
            return rates.rate(from).map(r -> new Rate(from, to, r, Kind.PINNED, source, rates.asOf(),
                    rates.bandPercentOrDefault()));
        }
        if (from.equals(target)) {
            return rates.rate(to).map(r -> new Rate(from, to,
                    BigDecimal.ONE.divide(r, MathContext.DECIMAL64).setScale(8, RoundingMode.HALF_UP), Kind.PINNED,
                    source, rates.asOf(), rates.bandPercentOrDefault()));
        }
        return Optional.empty();
    }

    @Override
    public String name() {
        return "rates pinned in spec";
    }
}
