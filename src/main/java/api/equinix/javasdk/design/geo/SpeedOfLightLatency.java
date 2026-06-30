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

package api.equinix.javasdk.design.geo;

import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.internetaccess.model.Ibx;
import api.equinix.javasdk.internetaccess.model.implementation.GeoCoordinates;

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
 * <p>The EIA availability response only guarantees {@code href}; {@code geoCoordinates} is optional,
 * so {@link #millisBetween(Ibx, Ibx)} / {@link #distanceKm(Ibx, Ibx)} throw
 * {@link IllegalArgumentException} (naming the offending IBX code) when an IBX has no coordinates —
 * fail loud rather than return a bogus zero. Callers iterating {@code availability(...)} results
 * should handle that per IBX.</p>
 *
 * <pre>{@code
 * Ibx la4 = internetAccess.ibxs().getByCode("LA4");
 * Ibx sv5 = internetAccess.ibxs().getByCode("SV5");
 *
 * SpeedOfLightLatency rtt = SpeedOfLightLatency.roundTrip();   // default
 * double ms = rtt.millisBetween(la4, sv5);                     // round-trip floor, LA4 <-> SV5
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

    private static String ibxLabel(Ibx ibx) {
        String code = ibx.getIbxCode();
        return code != null ? code : "<unknown IBX>";
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
         * @param mode one-way or round-trip; defaults to {@link Mode#ROUND_TRIP}
         */
        public Builder mode(Mode mode) {
            if (mode != null) {
                this.mode = mode;
            }
            return this;
        }

        /**
         * @param refractiveIndex the fibre refractive index (must be &gt; 0); defaults to
         *                        {@value #DEFAULT_FIBER_REFRACTIVE_INDEX}
         */
        public Builder refractiveIndex(double refractiveIndex) {
            if (refractiveIndex <= 0) {
                throw new IllegalArgumentException("refractiveIndex must be > 0");
            }
            this.refractiveIndex = refractiveIndex;
            return this;
        }

        /**
         * @param routeFactor path inflation over the great-circle distance (must be &gt;= 1.0);
         *                   defaults to {@value #DEFAULT_ROUTE_FACTOR}
         */
        public Builder routeFactor(double routeFactor) {
            if (routeFactor < 1.0) {
                throw new IllegalArgumentException("routeFactor must be >= 1.0");
            }
            this.routeFactor = routeFactor;
            return this;
        }

        public SpeedOfLightLatency build() {
            return new SpeedOfLightLatency(mode, refractiveIndex, routeFactor);
        }
    }
}
