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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Live ECB reference rates from the Frankfurter API (no key): one {@code GET <baseUrl>/latest?base=<target>}
 * per converter, made on the first conversion that needs it and cached for the converter's life; never
 * retried. The request carries only the currency code and a generic User-Agent. Shape observed 2026-11-12:
 * {@code {"amount":1.0,"base":"USD","date":"2026-11-11","rates":{"EUR":0.89871,"GBP":0.75149,...}}} (no AED).
 *
 * <p>When the call fails, times out (3 s connect, 5 s overall) or returns another shape, or a currency is
 * absent, {@link #rate} is empty so the chain falls back to the bundled table; {@link #fallbackNote()} says
 * why. Never throws. Rates are reference mid rates, not dealing rates, so converted ranges are widened by
 * {@value #BAND_PERCENT}% either side.</p>
 */
public final class FrankfurterExchangeRates implements CurrencyConverter {

    /** The default endpoint root. */
    public static final String DEFAULT_BASE_URL = "https://api.frankfurter.dev/v1";

    /** Widening for live ECB reference rates, either side, in percent. */
    public static final double BAND_PERCENT = 2.0;

    /** Fetches a URL's body; injectable so tests never reach the internet. */
    @FunctionalInterface
    public interface Fetcher {
        /**
         * @param uri the URL
         * @return the response body
         * @throws Exception on any failure (treated as "live rates unavailable")
         */
        String get(URI uri) throws Exception;
    }

    private final String target;
    private final String baseUrl;
    private final Fetcher fetcher;
    private boolean attempted;
    private Map<String, BigDecimal> perTarget;
    private String date;
    private String fallback;

    /**
     * @param target  the currency rates are fetched for (the request's {@code base})
     * @param baseUrl the endpoint root, {@code null} for {@link #DEFAULT_BASE_URL}
     * @param fetcher the HTTP fetch, {@code null} for {@link #httpFetcher()}
     */
    public FrankfurterExchangeRates(String target, String baseUrl, Fetcher fetcher) {
        this.target = target;
        this.baseUrl = baseUrl == null ? DEFAULT_BASE_URL : baseUrl.replaceAll("/+$", "");
        this.fetcher = fetcher == null ? httpFetcher() : fetcher;
    }

    /** @return a java.net.http fetch with a 3 s connect and 5 s request timeout and a generic User-Agent */
    public static Fetcher httpFetcher() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        return uri -> {
            HttpResponse<String> r = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "equinix-sdk-java").header("Accept", "application/json").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                throw new IllegalStateException("HTTP " + r.statusCode());
            }
            return r.body();
        };
    }

    private synchronized void load() {
        if (attempted) {
            return;
        }
        attempted = true;
        try {
            JsonNode root = new ObjectMapper().readTree(fetcher.get(URI.create(baseUrl + "/latest?base=" + target)));
            if (!target.equals(root.path("base").asText()) || !root.path("rates").isObject()
                    || root.path("date").asText().isBlank()) {
                throw new IllegalStateException("unexpected response shape");
            }
            Map<String, BigDecimal> rates = new HashMap<>();
            root.path("rates").fields().forEachRemaining(e -> {
                if (e.getValue().isNumber() && e.getValue().decimalValue().signum() > 0) {
                    rates.put(e.getKey(), e.getValue().decimalValue());
                }
            });
            rates.put(target, BigDecimal.ONE);
            perTarget = rates;
            date = root.path("date").asText();
        } catch (Exception e) {
            fallback = "live ECB rates via Frankfurter unavailable (" + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : ": " + e.getMessage()) + "); the bundled reference table was used";
        }
    }

    @Override
    public Optional<Rate> rate(String from, String to) {
        load();
        if (perTarget == null) {
            return Optional.empty();
        }
        BigDecimal f = perTarget.get(from);
        BigDecimal t = perTarget.get(to);
        if (f == null || t == null) {
            return Optional.empty();
        }
        BigDecimal rate = t.divide(f, MathContext.DECIMAL64).setScale(8, RoundingMode.HALF_UP);
        return Optional.of(new Rate(from, to, rate, Kind.LIVE, "ECB reference rate via Frankfurter, " + date, date,
                BAND_PERCENT));
    }

    /** @return why live rates were not used, empty when they were fetched (or no conversion needed them yet) */
    public synchronized Optional<String> fallbackNote() {
        return Optional.ofNullable(fallback);
    }

    /** @return whether the single fetch has been attempted */
    public synchronized boolean attempted() {
        return attempted;
    }

    @Override
    public String name() {
        return "ECB reference rates via Frankfurter";
    }
}
