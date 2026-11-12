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

package com.eqixiac.equinix.design.optimizer.context;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.geo.RouteFactorCalibration;
import com.eqixiac.equinix.design.geo.SpeedOfLightLatency;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spi.CostModel;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroFacts;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The {@link DesignContext} of one Metro Optimizer run: the metro catalogue, Fabric's measured
 * metro-to-metro round trips, cloud reachability per metro and the cost model, held in memory and
 * never changed after {@link Builder#build()}.
 *
 * <p>Built from live Fabric data by {@link FabricCatalogLoader}, or directly with {@link #builder(DesignSpec)}
 * (tests, offline what-ifs, a cached catalogue).</p>
 *
 * <h2>Latency rules (all round trip)</h2>
 * <ol>
 *   <li>A site that names a metro, to that same metro: {@value #INTRA_METRO_RTT_MS} ms, an
 *       assumption ({@link LatencyFigure.Basis#INTRA_METRO}).</li>
 *   <li>A site that names a metro, to a metro Fabric measures it against: the measurement
 *       ({@link LatencyFigure.Basis#MEASURED}).</li>
 *   <li>Otherwise, when both ends have coordinates (the site's own, else its metro's): the
 *       straight-fibre floor times the {@link RouteFactorCalibration calibrated route factor}
 *       ({@link LatencyFigure.Basis#ESTIMATED}).</li>
 *   <li>Otherwise unknown.</li>
 * </ol>
 *
 * <h2>Site weights</h2>
 * <p>A site's raw weight is its explicit {@code weight}; else its {@code headcount} times its role's
 * importance multiplier (1.0 without a role); else, when it has neither, its role multiplier (1.0
 * without a role) times the mean raw weight of the sites that do carry one (1 when none does), that
 * is, "an average site for its role". Raw weights are normalised to sum to 1.
 * {@link #siteWeightingNotes()} names every site whose weight was inferred.</p>
 */
public final class CatalogDesignContext implements DesignContext {

    /** Round trip assumed between a site and the metro it names (no Fabric figure exists for it). */
    public static final double INTRA_METRO_RTT_MS = 0.5;

    private static final SpeedOfLightLatency FLOOR = SpeedOfLightLatency.roundTrip();

    private final DesignSpec spec;
    private final Map<MetroId, MetroFacts> metros;
    private final List<MetroFacts> metroList;
    private final Map<String, Double> measured;
    private final Map<MetroId, Set<String>> reachable;
    private final Map<MetroId, Map<String, ProviderAvailability>> availability;
    private final CostModel costModel;
    private final Instant asOf;
    private final RouteFactorCalibration calibration;
    private final List<String> dataNotes;
    private final Map<String, Double> siteWeights;
    private final List<String> siteWeightingNotes;

    private CatalogDesignContext(Builder b) {
        this.spec = b.spec;
        this.metros = Collections.unmodifiableMap(new HashMap<>(b.metros));
        List<MetroFacts> sorted = new ArrayList<>(b.metros.values());
        sorted.sort(java.util.Comparator.comparing(m -> m.id().code()));
        this.metroList = List.copyOf(sorted);
        this.measured = Map.copyOf(b.measured);
        Map<MetroId, Set<String>> reach = new HashMap<>();
        b.reachable.forEach((k, v) -> reach.put(k, Collections.unmodifiableSet(new TreeSet<>(v))));
        this.reachable = Collections.unmodifiableMap(reach);
        Map<MetroId, Map<String, ProviderAvailability>> avail = new HashMap<>();
        b.availability.forEach((k, v) -> avail.put(k, Collections.unmodifiableMap(new TreeMap<>(v))));
        this.availability = Collections.unmodifiableMap(avail);
        this.costModel = b.costModel;
        this.asOf = b.asOf == null ? Instant.now() : b.asOf;
        this.calibration = b.calibration != null ? b.calibration : fitFrom(b.metros, b.measured);
        this.dataNotes = List.copyOf(b.notes);
        List<String> weightingNotes = new ArrayList<>();
        this.siteWeights = Collections.unmodifiableMap(weigh(spec.sites(), weightingNotes));
        this.siteWeightingNotes = List.copyOf(weightingNotes);
    }

    /**
     * @param spec the design the context describes
     * @return an empty builder
     */
    public static Builder builder(DesignSpec spec) {
        return new Builder(spec);
    }

    @Override
    public DesignSpec spec() {
        return spec;
    }

    @Override
    public List<MetroFacts> metros() {
        return metroList;
    }

    @Override
    public Optional<MetroFacts> metro(MetroId metro) {
        return Optional.ofNullable(metros.get(metro));
    }

    @Override
    public OptionalDouble roundTripLatencyMs(MetroId metro, SiteIntent site) {
        return latency(metro, site).value();
    }

    @Override
    public boolean latencyMeasured(MetroId metro, SiteIntent site) {
        return latency(metro, site).isMeasured();
    }

    @Override
    public OptionalDouble roundTripLatencyMs(MetroId a, MetroId b) {
        return latency(a, b).value();
    }

    /**
     * Round trip from a metro to a site, with its basis. See the class description for the rules.
     *
     * @param metro a metro
     * @param site  a site of the spec
     * @return the figure; {@link LatencyFigure.Basis#UNKNOWN} when it cannot be known
     */
    public LatencyFigure latency(MetroId metro, SiteIntent site) {
        MetroId siteMetro = site.metro() == null ? null : MetroId.of(site.metro());
        if (siteMetro != null) {
            if (siteMetro.equals(metro)) {
                return new LatencyFigure(INTRA_METRO_RTT_MS, LatencyFigure.Basis.INTRA_METRO,
                        "site is in " + metro.code() + "; intra-metro round trip assumed");
            }
            Double ms = measured.get(key(metro, siteMetro));
            if (ms != null) {
                return new LatencyFigure(ms, LatencyFigure.Basis.MEASURED,
                        "Fabric measured " + metro.code() + "-" + siteMetro.code());
            }
        }
        MetroFacts facts = metros.get(metro);
        if (facts == null || !facts.hasCoordinates()) {
            return LatencyFigure.unknown(metro.code() + " has no coordinates in the catalogue");
        }
        Double lat = site.latitude();
        Double lon = site.longitude();
        Region siteRegion = null;
        String from = "the site's coordinates";
        if (!site.hasCoordinates() && siteMetro != null) {
            MetroFacts home = metros.get(siteMetro);
            if (home != null && home.hasCoordinates()) {
                lat = home.latitude();
                lon = home.longitude();
                siteRegion = home.region();
                from = siteMetro.code() + "'s coordinates";
            }
        }
        if (lat == null || lon == null) {
            return LatencyFigure.unknown("site '" + site.label() + "' has no coordinates and no metro the catalogue "
                    + "locates");
        }
        double km = SpeedOfLightLatency.distanceKm(lat, lon, facts.latitude(), facts.longitude());
        double ms = Math.max(INTRA_METRO_RTT_MS, calibration.estimateRoundTripMs(km, siteRegion, facts.region()));
        return new LatencyFigure(ms, LatencyFigure.Basis.ESTIMATED, String.format(Locale.ROOT,
                "%.0f km from %s x route factor %.2f", km, from,
                calibration.fitFor(siteRegion, facts.region()).factor()));
    }

    /**
     * Round trip between two metros with its basis: the same metro is {@value #INTRA_METRO_RTT_MS} ms
     * (assumed), a measured pair is Fabric's figure, otherwise a calibrated estimate, otherwise unknown.
     *
     * @param a one metro
     * @param b the other metro
     * @return the figure
     */
    public LatencyFigure latency(MetroId a, MetroId b) {
        if (a.equals(b)) {
            return new LatencyFigure(INTRA_METRO_RTT_MS, LatencyFigure.Basis.INTRA_METRO, "same metro");
        }
        Double ms = measured.get(key(a, b));
        if (ms != null) {
            return new LatencyFigure(ms, LatencyFigure.Basis.MEASURED, "Fabric measured " + a.code() + "-" + b.code());
        }
        MetroFacts fa = metros.get(a);
        MetroFacts fb = metros.get(b);
        if (fa == null || fb == null || !fa.hasCoordinates() || !fb.hasCoordinates()) {
            return LatencyFigure.unknown("no measurement and no coordinates for " + a.code() + "-" + b.code());
        }
        double km = SpeedOfLightLatency.distanceKm(fa.latitude(), fa.longitude(), fb.latitude(), fb.longitude());
        return new LatencyFigure(calibration.estimateRoundTripMs(km, fa.region(), fb.region()),
                LatencyFigure.Basis.ESTIMATED, String.format(Locale.ROOT, "%.0f km x route factor %.2f", km,
                calibration.fitFor(fa.region(), fb.region()).factor()));
    }

    @Override
    public double siteWeight(SiteIntent site) {
        Double weight = siteWeights.get(site.label());
        return weight == null ? 0 : weight;
    }

    @Override
    public Set<String> reachableClouds(MetroId metro) {
        return reachable.getOrDefault(metro, Set.of());
    }

    /**
     * @param metro a metro
     * @return the service profile chosen per cloud id at that metro (only clouds reachable there)
     */
    public Map<String, ProviderAvailability> providerAvailability(MetroId metro) {
        return availability.getOrDefault(metro, Map.of());
    }

    @Override
    public CostModel costModel() {
        return costModel;
    }

    @Override
    public Instant asOf() {
        return asOf;
    }

    /** @return the route factor used for unmeasured pairs and what it was fitted from */
    public RouteFactorCalibration calibration() {
        return calibration;
    }

    /** @return how the facts were gathered and how complete they are (catalogue scans, unresolved clouds) */
    public List<String> dataNotes() {
        return dataNotes;
    }

    /** @return statements naming every site whose weight was inferred, and how */
    public List<String> siteWeightingNotes() {
        return siteWeightingNotes;
    }

    /** @return how many metro pairs Fabric measures in this catalogue */
    public int measuredPairCount() {
        return measured.size() / 2;
    }

    /**
     * A copy of this context with another cost model (the facts are shared).
     *
     * @param model the cost model
     * @return the new context
     */
    public CatalogDesignContext withCostModel(CostModel model) {
        Builder b = toBuilder();
        b.costModel = model;
        return b.build();
    }

    private Builder toBuilder() {
        Builder b = new Builder(spec);
        b.metros.putAll(metros);
        b.measured.putAll(measured);
        reachable.forEach((k, v) -> b.reachable.put(k, new LinkedHashSet<>(v)));
        availability.forEach((k, v) -> b.availability.put(k, new LinkedHashMap<>(v)));
        b.costModel = costModel;
        b.asOf = asOf;
        b.calibration = calibration;
        b.notes.addAll(dataNotes);
        return b;
    }

    private static Map<String, Double> weigh(List<SiteIntent> sites, List<String> notes) {
        Map<String, Double> raw = new LinkedHashMap<>();
        List<String> inferred = new ArrayList<>();
        double carried = 0;
        int carriedCount = 0;
        for (SiteIntent site : sites) {
            Double value = null;
            if (site.weight() != null) {
                value = site.weight();
            } else if (site.headcount() != null && site.headcount() > 0) {
                value = site.headcount() * multiplier(site);
            }
            if (value != null) {
                carried += value;
                carriedCount++;
            }
            raw.put(site.label(), value);
        }
        double mean = carriedCount == 0 ? 1.0 : carried / carriedCount;
        double total = 0;
        for (SiteIntent site : sites) {
            if (raw.get(site.label()) == null) {
                raw.put(site.label(), multiplier(site) * mean);
                inferred.add(site.label());
            }
            total += raw.get(site.label());
        }
        if (!inferred.isEmpty()) {
            notes.add(inferred.size() == sites.size()
                    ? "No site carries a weight or a headcount, so sites are weighted by role only."
                    : "Sites weighted by explicit weight, else headcount x role; " + String.join(", ", inferred)
                            + " carried neither and were weighted as an average site for their role.");
        }
        Map<String, Double> normalised = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : raw.entrySet()) {
            normalised.put(e.getKey(), total > 0 ? e.getValue() / total : 0);
        }
        return normalised;
    }

    private static double multiplier(SiteIntent site) {
        return site.role() == null ? 1.0 : site.role().getImportanceMultiplier();
    }

    private static RouteFactorCalibration fitFrom(Map<MetroId, MetroFacts> metros, Map<String, Double> measured) {
        List<RouteFactorCalibration.MeasuredPair> pairs = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        int excluded = 0;
        for (Map.Entry<String, Double> e : measured.entrySet()) {
            String[] codes = e.getKey().split("\\|");
            String canonical = codes[0].compareTo(codes[1]) < 0 ? e.getKey() : codes[1] + "|" + codes[0];
            if (!seen.add(canonical)) {
                continue;
            }
            MetroFacts a = metros.get(MetroId.of(codes[0]));
            MetroFacts b = metros.get(MetroId.of(codes[1]));
            if (a == null || b == null || !a.hasCoordinates() || !b.hasCoordinates()) {
                excluded++;
                continue;
            }
            pairs.add(new RouteFactorCalibration.MeasuredPair(a.region(), b.region(),
                    SpeedOfLightLatency.distanceKm(a.latitude(), a.longitude(), b.latitude(), b.longitude()),
                    e.getValue()));
        }
        RouteFactorCalibration fit = RouteFactorCalibration.fit(pairs);
        return new RouteFactorCalibration(fit.overall(), fit.byRegionPair(), fit.pairsExcluded() + excluded);
    }

    private static String key(MetroId a, MetroId b) {
        return a.code() + "|" + b.code();
    }

    /** Assembles a {@link CatalogDesignContext}. Not thread-safe. */
    public static final class Builder {
        private final DesignSpec spec;
        private final Map<MetroId, MetroFacts> metros = new LinkedHashMap<>();
        private final Map<String, Double> measured = new HashMap<>();
        private final Map<MetroId, Set<String>> reachable = new HashMap<>();
        private final Map<MetroId, Map<String, ProviderAvailability>> availability = new HashMap<>();
        private final List<String> notes = new ArrayList<>();
        private CostModel costModel;
        private Instant asOf;
        private RouteFactorCalibration calibration;

        private Builder(DesignSpec spec) {
            if (spec == null) {
                throw new IllegalArgumentException("A context needs a spec");
            }
            this.spec = spec;
        }

        /**
         * @param facts a catalogue metro
         * @return this builder
         */
        public Builder metro(MetroFacts facts) {
            metros.put(facts.id(), facts);
            return this;
        }

        /**
         * @param code      metro code
         * @param region    region, may be {@code null}
         * @param latitude  decimal degrees, may be {@code null}
         * @param longitude decimal degrees, may be {@code null}
         * @param geoZones  geo scopes / zones the metro belongs to
         * @return this builder
         */
        public Builder metro(String code, Region region, Double latitude, Double longitude, String... geoZones) {
            return metro(new MetroFacts(MetroId.of(code), code, region, null, latitude, longitude, Set.of(geoZones)));
        }

        /**
         * Records a Fabric-measured round trip (both directions).
         *
         * @param a           one metro code
         * @param b           the other metro code
         * @param roundTripMs Fabric's {@code avgLatency}
         * @return this builder
         */
        public Builder measuredLatency(String a, String b, double roundTripMs) {
            if (!(roundTripMs > 0) || !Double.isFinite(roundTripMs)) {
                return this;
            }
            MetroId x = MetroId.of(a);
            MetroId y = MetroId.of(b);
            if (x.equals(y)) {
                return this;
            }
            measured.put(key(x, y), roundTripMs);
            measured.putIfAbsent(key(y, x), roundTripMs);
            return this;
        }

        /**
         * @param metro    metro code
         * @param cloudIds spec cloud ids reachable privately from the metro
         * @return this builder
         */
        public Builder reachable(String metro, String... cloudIds) {
            reachable.computeIfAbsent(MetroId.of(metro), k -> new LinkedHashSet<>()).addAll(List.of(cloudIds));
            return this;
        }

        /**
         * Records the service profile chosen for a cloud at a metro and marks the cloud reachable there.
         *
         * @param metro        the metro
         * @param cloudId      the spec cloud id
         * @param availability the chosen profile
         * @return this builder
         */
        public Builder providerAvailability(MetroId metro, String cloudId, ProviderAvailability availability) {
            this.availability.computeIfAbsent(metro, k -> new LinkedHashMap<>()).put(cloudId, availability);
            reachable.computeIfAbsent(metro, k -> new LinkedHashSet<>()).add(cloudId);
            return this;
        }

        /**
         * @param model the cost model
         * @return this builder
         */
        public Builder costModel(CostModel model) {
            this.costModel = model;
            return this;
        }

        /**
         * @param asOf when the facts were gathered
         * @return this builder
         */
        public Builder asOf(Instant asOf) {
            this.asOf = asOf;
            return this;
        }

        /**
         * Overrides the route factor; by default it is fitted from the measured pairs given.
         *
         * @param calibration the calibration
         * @return this builder
         */
        public Builder calibration(RouteFactorCalibration calibration) {
            this.calibration = calibration;
            return this;
        }

        /**
         * @param note a statement about how complete the facts are
         * @return this builder
         */
        public Builder note(String note) {
            notes.add(note);
            return this;
        }

        /** @return the context */
        public CatalogDesignContext build() {
            return new CatalogDesignContext(this);
        }
    }
}
