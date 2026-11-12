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
package com.eqixiac.equinix.design.sustainability;

import com.eqixiac.equinix.core.internal.Constants;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Grid carbon intensity per metro and per country, with its provenance.
 *
 * <p><strong>Beta.</strong> This type and the bundled reference table may change in a minor release.</p>
 *
 * <p>A caller can supply their own figures (a grid operator, a national inventory, a commercial data
 * provider, a facility's own reporting) per metro, per country, or both; a metro figure wins over its
 * country's. {@link #bundledReference()} returns the table shipped with the SDK: national annual averages,
 * so every metro in a country gets the same value.</p>
 *
 * @param gramsCo2ePerKwh          intensity per metro code, in grams of CO2-equivalent per kWh; finite, non-negative
 * @param gramsCo2ePerKwhByCountry intensity per ISO 3166-1 alpha-2 country code, same unit; used for a metro
 *                                 with no figure of its own
 * @param source                   where the figures come from (publisher and dataset); required
 * @param asOf                     the period or date the figures describe (for example {@code 2025 annual average}); required
 * @param method                   {@code location-based}, {@code market-based} or another stated method; required
 */
public record CarbonIntensityTable(Map<String, Double> gramsCo2ePerKwh, Map<String, Double> gramsCo2ePerKwhByCountry,
                                   String source, String asOf, String method) {

    /** The unit of every figure. */
    public static final String UNIT = "gCO2e/kWh";

    /** The classpath resource of the bundled reference table. */
    public static final String BUNDLED_RESOURCE = "/json/carbon_intensity_reference_2026_11.json";

    /** Requires provenance and finite, non-negative figures; normalises codes to upper case. */
    public CarbonIntensityTable {
        if (blank(source) || blank(asOf) || blank(method)) {
            throw new IllegalArgumentException("A carbon-intensity table needs a source, an as-of period and a method");
        }
        gramsCo2ePerKwh = checked(gramsCo2ePerKwh);
        gramsCo2ePerKwhByCountry = checked(gramsCo2ePerKwhByCountry);
    }

    /**
     * A table of per-metro figures only.
     *
     * @param gramsCo2ePerKwh intensity per metro code
     * @param source          where the figures come from
     * @param asOf            the period the figures describe
     * @param method          the accounting method
     */
    public CarbonIntensityTable(Map<String, Double> gramsCo2ePerKwh, String source, String asOf, String method) {
        this(gramsCo2ePerKwh, Map.of(), source, asOf, method);
    }

    /**
     * The reference table bundled with the SDK (Beta): national annual average carbon intensity of
     * electricity generation, location-based, published by Ember and processed by Our World in Data under
     * CC BY 4.0, retrieved 2026-11-12. Country-level only, so it cannot rank metros within one country, and
     * it does not reflect an operator's renewable energy purchases. Hong Kong and Lagos have metro entries
     * because the Fabric catalogue does not give them a usable country.
     *
     * @return the bundled table
     */
    public static CarbonIntensityTable bundledReference() {
        return Bundled.TABLE;
    }

    /**
     * @param metro a metro code
     * @return the metro's own intensity, or {@code null} when the table has none for the metro
     */
    public Double intensity(String metro) {
        return metro == null ? null : gramsCo2ePerKwh.get(metro.toUpperCase(Locale.ROOT));
    }

    /**
     * @param metro   a metro code
     * @param country the metro's ISO 3166-1 alpha-2 country code; may be {@code null}
     * @return the metro's own intensity, else its country's, else {@code null}
     */
    public Double intensity(String metro, String country) {
        Double own = intensity(metro);
        if (own != null || country == null) {
            return own;
        }
        return gramsCo2ePerKwhByCountry.get(country.trim().toUpperCase(Locale.ROOT));
    }

    private static Map<String, Double> checked(Map<String, Double> figures) {
        Map<String, Double> copy = new TreeMap<>();
        if (figures != null) {
            for (Map.Entry<String, Double> e : figures.entrySet()) {
                Double v = e.getValue();
                if (blank(e.getKey()) || v == null || !(v >= 0 && Double.isFinite(v))) {
                    throw new IllegalArgumentException("Intensity for '" + e.getKey() + "' must be finite and "
                            + "non-negative, got " + v);
                }
                copy.put(e.getKey().trim().toUpperCase(Locale.ROOT), v);
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /** Loads the bundled table once. */
    private static final class Bundled {
        static final CarbonIntensityTable TABLE = load();

        private static CarbonIntensityTable load() {
            try (InputStream in = CarbonIntensityTable.class.getResourceAsStream(BUNDLED_RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException("Missing bundled resource " + BUNDLED_RESOURCE);
                }
                JsonNode root = Constants.mapper().readTree(in);
                Map<String, Double> countries = figures(root.path("countries"));
                Map<String, Double> metros = figures(root.path("metroOverrides"));
                int oldest = Integer.MAX_VALUE;
                int newest = Integer.MIN_VALUE;
                for (JsonNode entry : root.path("countries")) {
                    oldest = Math.min(oldest, entry.path("year").asInt());
                    newest = Math.max(newest, entry.path("year").asInt());
                }
                String period = oldest == newest ? oldest + " annual average"
                        : oldest + " to " + newest + " annual averages (latest year published per country)";
                return new CarbonIntensityTable(metros, countries,
                        root.path("source").asText() + ", " + root.path("licence").asText() + ", retrieved "
                                + root.path("retrieved").asText() + " (bundled reference, Beta)",
                        period, root.path("method").asText());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static Map<String, Double> figures(JsonNode node) {
            Map<String, Double> out = new TreeMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> e = it.next();
                out.put(e.getKey(), e.getValue().path("gramsCo2ePerKwh").asDouble());
            }
            return out;
        }
    }
}
