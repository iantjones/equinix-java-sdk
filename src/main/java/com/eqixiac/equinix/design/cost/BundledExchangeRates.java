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

import com.eqixiac.equinix.design.spi.CurrencyConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The SDK's bundled reference exchange rates: the European Central Bank's euro reference rates of
 * 2026-11-11 (retrieved 2026-11-12 from {@code https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml}),
 * plus the AED, which the ECB does not publish, derived from its peg of 3.6725 AED per USD. Loaded from
 * {@code /json/fx_reference_2026_11.json}. Cross rates are computed through the euro. Every rate is
 * {@link CurrencyConverter.Kind#REFERENCE}: an estimate whose converted ranges are widened by
 * {@value #BAND_PERCENT}% either side, because the table ages from the day it was bundled.
 */
public final class BundledExchangeRates implements CurrencyConverter {

    /** Widening applied to lines converted at a bundled rate, either side, in percent. */
    public static final double BAND_PERCENT = 5.0;

    private static final String RESOURCE = "/json/fx_reference_2026_11.json";
    private static volatile BundledExchangeRates standard;

    private final Map<String, BigDecimal> perEuro;
    private final String asOf;
    private final String source;

    private BundledExchangeRates(JsonNode root) {
        Map<String, BigDecimal> rates = new TreeMap<>();
        rates.put("EUR", BigDecimal.ONE);
        root.path("rates").fields().forEachRemaining(e -> rates.put(e.getKey(), e.getValue().decimalValue()));
        for (JsonNode peg : root.path("pegged")) {
            BigDecimal anchor = rates.get(peg.path("anchor").asText());
            if (anchor != null) {
                rates.put(peg.path("currency").asText(), anchor.multiply(peg.path("perAnchor").decimalValue()));
            }
        }
        this.perEuro = Collections.unmodifiableMap(rates);
        this.asOf = root.path("asOf").asText();
        this.source = "bundled reference table, as of " + asOf + " (ECB euro reference rates; AED via its USD peg)";
    }

    /** @return the bundled table, loaded once */
    public static BundledExchangeRates standard() {
        BundledExchangeRates local = standard;
        if (local == null) {
            synchronized (BundledExchangeRates.class) {
                local = standard;
                if (local == null) {
                    try (InputStream in = BundledExchangeRates.class.getResourceAsStream(RESOURCE)) {
                        if (in == null) {
                            throw new IllegalStateException("Bundled exchange rates not found: " + RESOURCE);
                        }
                        local = new BundledExchangeRates(new ObjectMapper().readTree(in));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    standard = local;
                }
            }
        }
        return local;
    }

    /** @return the date of the bundled rates */
    public String asOf() {
        return asOf;
    }

    /** @return the currencies the table covers, sorted */
    public java.util.Set<String> currencies() {
        return perEuro.keySet();
    }

    @Override
    public Optional<Rate> rate(String from, String to) {
        BigDecimal f = perEuro.get(from);
        BigDecimal t = perEuro.get(to);
        if (f == null || t == null) {
            return Optional.empty();
        }
        BigDecimal rate = t.divide(f, MathContext.DECIMAL64).setScale(8, RoundingMode.HALF_UP);
        return Optional.of(new Rate(from, to, rate, Kind.REFERENCE, source, asOf, BAND_PERCENT));
    }

    @Override
    public String name() {
        return "bundled reference table (" + asOf + ")";
    }
}
