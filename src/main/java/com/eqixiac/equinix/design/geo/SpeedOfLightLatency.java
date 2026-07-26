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

package com.eqixiac.equinix.design.geo;

import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.internetaccess.model.Ibx;
import com.eqixiac.equinix.internetaccess.model.implementation.GeoCoordinates;

/**
 * Estimates network latency from geographic distance using the speed of light in optical fiber.
 *
 * <p>Light in glass fiber travels at {@code c / n}, where {@code n} ≈ {@value #DEFAULT_FIBER_REFRACTIVE_INDEX}
 * is the refractive index — roughly 204,000&nbsp;km/s, or ~4.9&nbsp;µs per km one-way. The estimate is:</p>
 *
 * <pre>{@code one-way ms = distanceKm * routeFactor / (c / n / 1000)}</pre>
 *
 * <p>and round-trip is twice that. {@code routeFactor} (default {@value #DEFAULT_ROUTE_FACTOR}) inflates
 * the great-circle distance for non-direct fibre paths; leave it at 1.0 for the theoretical
 * speed-of-light floor, or set ~1.3–1.5 for a realistic terrestrial route. The result is a physical
 * lower bound (or estimate) — it excludes switching, queuing, and serialization delay.</p>
 *
 * <p>The {@linkplain Mode#ROUND_TRIP round-trip (RTT)} mode is the default, since RTT is what most
 * latency budgets are quoted in.</p>
 *
 * <p>Latency is computed <strong>IBX-to-IBX</strong> — between two specific Equinix data centers
 * using each IBX's own {@linkplain Ibx#getGeoCoordinates() coordinates} — not between metro
 * centroids, so two IBXes in the same metro have a real (small, non-zero) distance.</p>
 *
 * <h3>Endpoint types</h3>
 * <p>Latency can be computed between any mix of the SDK's location-bearing types:</p>
 * <ul>
 *   <li><b>IBX ↔ IBX</b> — {@link #millisBetween(Ibx, Ibx)}: the most precise figure, using each
 *       data center's own coordinates (from {@code internetAccess.ibxs()}); two IBXes in the same
 *       metro get a real, small, non-zero distance.</li>
 *   <li><b>Metro ↔ Metro</b> — {@link #millisBetween(Metro, Metro)}: metro-centroid figure using
 *       Fabric's metro coordinates (from {@code fabric.metros()} / {@code fabric.metroRegistry()}).
 *       Fine for city-to-city planning; same-metro pairs compute as 0.</li>
 *   <li><b>IBX ↔ Metro</b> — {@link #millisBetween(Ibx, Metro)} / {@link #millisBetween(Metro, Ibx)}:
 *       mixed precision, e.g. "from my cage in LA4 to the Dallas metro".</li>
 *   <li><b>Raw</b> — {@link #millisBetween(GeoCoordinate, GeoCoordinate)},
 *       {@link #distanceKm(double, double, double, double)} and {@link #millisForKm(double)} for
 *       arbitrary coordinates or known distances, fully offline.</li>
 * </ul>
 *
 * <p>Coordinates are optional in both source APIs, so every typed overload throws
 * {@link IllegalArgumentException} naming the offending IBX/metro when coordinates are missing —
 * fail loud rather than return a bogus zero.</p>
 *
 * <p>For metro-to-metro, compare the calculated floor against Equinix's <em>measured</em>
 * inter-metro RTT: {@code metro.getConnectedMetros()} carries {@code avgLatency} (ms) observed on
 * the Fabric backbone. The floor tells you what physics allows; {@code avgLatency} tells you what
 * the network delivers.</p>
 *
 * <pre>{@code
 * Ibx la4 = internetAccess.ibxs().getByCode("LA4");
 * Ibx sv5 = internetAccess.ibxs().getByCode("SV5");
 * Metro dc = fabric.metros().getByMetroCode(MetroCode.DC);
 * Metro sv = fabric.metros().getByMetroCode(MetroCode.SV);
 *
 * SpeedOfLightLatency rtt = SpeedOfLightLatency.roundTrip();   // default
 * double ibxToIbx     = rtt.millisBetween(la4, sv5);           // precise, IBX-to-IBX
 * double metroToMetro = rtt.millisBetween(dc, sv);             // metro centroids
 * double ibxToMetro   = rtt.millisBetween(la4, dc);            // mixed
 *
 * SpeedOfLightLatency realistic = SpeedOfLightLatency.builder()
 *         .mode(Mode.ONE_WAY).routeFactor(1.4).build();
 * double oneWay = realistic.millisForKm(1000);                 // ~6.85 ms
 * }</pre>
 *
 * @author ianjones
 */
public final class SpeedOfLightLatency {

    /** Whether to report a one-way or a round-trip (RTT) figure. */
    public enum Mode {
        ONE_WAY,
        ROUND_TRIP
    }

    /** Speed of light in vacuum, km/s. */
    public static final double SPEED_OF_LIGHT_KM_PER_S = 299_792.458;

    /** Refractive index of single-mode optical fibre (~1.467); fibre speed is {@code c / n}. */
    public static final double DEFAULT_FIBER_REFRACTIVE_INDEX = 1.467;

    /** Default path inflation over the great-circle distance (1.0 = theoretical straight-line floor). */
    public static final double DEFAULT_ROUTE_FACTOR = 1.0;

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final Mode mode;
    private final double refractiveIndex;
    private final double routeFactor;

    private SpeedOfLightLatency(Mode mode, double refractiveIndex, double routeFactor) {
        this.mode = mode;
        this.refractiveIndex = refractiveIndex;
        this.routeFactor = routeFactor;
    }

    /**
     * @return a round-trip (RTT) calculator with default fibre and route parameters
     */
    public static SpeedOfLightLatency roundTrip() {
        return builder().build();
    }

    /**
     * @return a one-way calculator with default fibre and route parameters
     */
    public static SpeedOfLightLatency oneWay() {
        return builder().mode(Mode.ONE_WAY).build();
    }

    /**
     * Starts a builder for a customized calculator (mode, refractive index, route factor).
     * The no-argument chain {@code builder().build()} is equivalent to {@link #roundTrip()}.
     *
     * @return a new builder with the defaults pre-set
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the configured mode (round-trip by default)
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Fibre latency for a great-circle distance.
     *
     * @param distanceKm the distance in kilometres (negative values are treated as 0)
     * @return the estimated latency in milliseconds, round-trip or one-way per {@link #getMode()}
     */
    public double millisForKm(double distanceKm) {
        double km = Math.max(0.0, distanceKm);
        double fibreSpeedKmPerMs = SPEED_OF_LIGHT_KM_PER_S / refractiveIndex / 1000.0;
        double oneWayMs = (km * routeFactor) / fibreSpeedKmPerMs;
        return mode == Mode.ROUND_TRIP ? oneWayMs * 2.0 : oneWayMs;
    }

    /**
     * Fibre latency between two coordinates (great-circle distance).
     *
     * @param a one endpoint
     * @param b the other endpoint
     * @return the estimated latency in milliseconds
     * @throws IllegalArgumentException if either coordinate is null or missing latitude/longitude
     */
    public double millisBetween(GeoCoordinate a, GeoCoordinate b) {
        return millisForKm(distanceKm(a, b));
    }

    /**
     * Fibre latency between two Equinix IBX data centers, using each IBX's own
     * {@linkplain Ibx#getGeoCoordinates() coordinates}. This is the IBX-to-IBX figure: two IBXes in
     * the same metro have a real (small) latency rather than zero.
     *
     * @param a one IBX
     * @param b the other IBX
     * @return the estimated latency in milliseconds
     * @throws IllegalArgumentException if either IBX is null or has no coordinates
     */
    public double millisBetween(Ibx a, Ibx b) {
        return millisForKm(distanceKm(a, b));
    }

    /**
     * Fibre latency between two Equinix metros, using each metro's Fabric centroid
     * {@linkplain Metro#geoCoordinates() coordinates}. This is a metro-level figure: two IBXes in
     * the same metro compute as 0 — use {@link #millisBetween(Ibx, Ibx)} for per-data-center
     * precision.
     *
     * @param a one metro
     * @param b the other metro
     * @return the estimated latency in milliseconds
     * @throws IllegalArgumentException if either metro is null or has no coordinates
     */
    public double millisBetween(Metro a, Metro b) {
        return millisForKm(distanceKm(a, b));
    }

    /**
     * Fibre latency between an IBX data center and a metro centroid (mixed precision) — e.g.
     * "from my cage in LA4 to the Dallas metro".
     *
     * @param a the IBX
     * @param b the metro
     * @return the estimated latency in milliseconds
     * @throws IllegalArgumentException if either endpoint is null or has no coordinates
     */
    public double millisBetween(Ibx a, Metro b) {
        return millisForKm(distanceKm(a, b));
    }

    /**
     * Fibre latency between a metro centroid and an IBX data center (mixed precision).
     *
     * @param a the metro
     * @param b the IBX
     * @return the estimated latency in milliseconds
     * @throws IllegalArgumentException if either endpoint is null or has no coordinates
     */
    public double millisBetween(Metro a, Ibx b) {
        return millisForKm(distanceKm(a, b));
    }

    /**
     * Great-circle (haversine) distance between two coordinates.
     *
     * @param a one endpoint
     * @param b the other endpoint
     * @return the distance in kilometres
     * @throws IllegalArgumentException if either coordinate is null or missing latitude/longitude
     */
    public static double distanceKm(GeoCoordinate a, GeoCoordinate b) {
        if (a == null || b == null || a.getLatitude() == null || a.getLongitude() == null
                || b.getLatitude() == null || b.getLongitude() == null) {
            throw new IllegalArgumentException("both coordinates must have latitude and longitude");
        }
        return distanceKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    /**
     * Great-circle (haversine) distance between two Equinix IBX data centers.
     *
     * @param a one IBX
     * @param b the other IBX
     * @return the distance in kilometres
     * @throws IllegalArgumentException if either IBX is null or is missing its coordinates
     */
    public static double distanceKm(Ibx a, Ibx b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("both IBXs must be non-null");
        }
        GeoCoordinates ca = a.getGeoCoordinates();
        GeoCoordinates cb = b.getGeoCoordinates();
        if (ca == null || cb == null || ca.getLatitude() == null || ca.getLongitude() == null
                || cb.getLatitude() == null || cb.getLongitude() == null) {
            throw new IllegalArgumentException("both IBXs must have geo coordinates (missing for "
                    + ibxLabel(a) + " or " + ibxLabel(b) + ")");
        }
        return distanceKm(ca.getLatitude(), ca.getLongitude(), cb.getLatitude(), cb.getLongitude());
    }

    /**
     * Great-circle (haversine) distance between two Equinix metro centroids.
     *
     * @param a one metro
     * @param b the other metro
     * @return the distance in kilometres
     * @throws IllegalArgumentException if either metro is null or is missing its coordinates
     */
    public static double distanceKm(Metro a, Metro b) {
        double[] ca = requireCoordinates(a);
        double[] cb = requireCoordinates(b);
        return distanceKm(ca[0], ca[1], cb[0], cb[1]);
    }

    /**
     * Great-circle (haversine) distance between an IBX data center and a metro centroid.
     *
     * @param a the IBX
     * @param b the metro
     * @return the distance in kilometres
     * @throws IllegalArgumentException if either endpoint is null or is missing its coordinates
     */
    public static double distanceKm(Ibx a, Metro b) {
        double[] ca = requireCoordinates(a);
        double[] cb = requireCoordinates(b);
        return distanceKm(ca[0], ca[1], cb[0], cb[1]);
    }

    /**
     * Great-circle (haversine) distance between a metro centroid and an IBX data center.
     *
     * @param a the metro
     * @param b the IBX
     * @return the distance in kilometres
     * @throws IllegalArgumentException if either endpoint is null or is missing its coordinates
     */
    public static double distanceKm(Metro a, Ibx b) {
        return distanceKm(b, a);
    }

    private static double[] requireCoordinates(Ibx ibx) {
        if (ibx == null) {
            throw new IllegalArgumentException("the IBX must be non-null");
        }
        GeoCoordinates c = ibx.getGeoCoordinates();
        if (c == null || c.getLatitude() == null || c.getLongitude() == null) {
            throw new IllegalArgumentException(
                    "IBX " + ibxLabel(ibx) + " has no geo coordinates");
        }
        return new double[] {c.getLatitude(), c.getLongitude()};
    }

    private static double[] requireCoordinates(Metro metro) {
        if (metro == null) {
            throw new IllegalArgumentException("the metro must be non-null");
        }
        GeoCoordinate c = metro.geoCoordinates();
        if (c == null || c.getLatitude() == null || c.getLongitude() == null) {
            throw new IllegalArgumentException(
                    "metro " + metroLabel(metro) + " has no geo coordinates");
        }
        return new double[] {c.getLatitude(), c.getLongitude()};
    }

    private static String ibxLabel(Ibx ibx) {
        String code = ibx.getIbxCode();
        return code != null ? code : "<unknown IBX>";
    }

    private static String metroLabel(Metro metro) {
        return metro.metroId() != null ? metro.metroId().code()
                : (metro.getName() != null ? metro.getName() : "<unknown metro>");
    }

    /**
     * Great-circle (haversine) distance between two latitude/longitude pairs.
     *
     * @return the distance in kilometres
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }

    /**
     * Fluent configuration for {@link SpeedOfLightLatency}.
     */
    public static final class Builder {
        private Mode mode = Mode.ROUND_TRIP;
        private double refractiveIndex = DEFAULT_FIBER_REFRACTIVE_INDEX;
        private double routeFactor = DEFAULT_ROUTE_FACTOR;

        /**
         * Sets the reporting mode. Unlike the other two setters, a {@code null} argument does not
         * throw — it is silently ignored and the current mode (initially
         * {@link Mode#ROUND_TRIP}) is kept.
         *
         * @param mode one-way or round-trip; {@code null} is ignored
         * @return this builder
         */
        public Builder mode(Mode mode) {
            if (mode != null) {
                this.mode = mode;
            }
            return this;
        }

        /**
         * Sets the fibre refractive index. Only {@code > 0} is enforced; note that values below
         * 1.0 are physically meaningless for fibre (they yield a floor faster than light in
         * vacuum) — real single-mode fibre is ~{@value #DEFAULT_FIBER_REFRACTIVE_INDEX}, and
         * meaningful values are {@code >= 1.0}.
         *
         * @param refractiveIndex the fibre refractive index (must be &gt; 0); defaults to
         *                        {@value #DEFAULT_FIBER_REFRACTIVE_INDEX}
         * @return this builder
         * @throws IllegalArgumentException if {@code refractiveIndex <= 0}
         */
        public Builder refractiveIndex(double refractiveIndex) {
            if (refractiveIndex <= 0) {
                throw new IllegalArgumentException("refractiveIndex must be > 0");
            }
            this.refractiveIndex = refractiveIndex;
            return this;
        }

        /**
         * Sets the path inflation applied to the great-circle distance: 1.0 is the theoretical
         * straight-line floor; ~1.3&ndash;1.5 models realistic terrestrial fibre routing.
         *
         * @param routeFactor path inflation over the great-circle distance (must be &gt;= 1.0);
         *                   defaults to {@value #DEFAULT_ROUTE_FACTOR}
         * @return this builder
         * @throws IllegalArgumentException if {@code routeFactor < 1.0}
         */
        public Builder routeFactor(double routeFactor) {
            if (routeFactor < 1.0) {
                throw new IllegalArgumentException("routeFactor must be >= 1.0");
            }
            this.routeFactor = routeFactor;
            return this;
        }

        /**
         * Builds the immutable calculator with the configured mode, refractive index, and route
         * factor.
         *
         * @return a new {@link SpeedOfLightLatency}
         */
        public SpeedOfLightLatency build() {
            return new SpeedOfLightLatency(mode, refractiveIndex, routeFactor);
        }
    }
}
