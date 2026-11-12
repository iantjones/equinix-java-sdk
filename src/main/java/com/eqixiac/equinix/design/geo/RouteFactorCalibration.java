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

import com.eqixiac.equinix.core.enums.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * How much slower real Equinix Fabric round trips are than the straight-fibre physics floor, fitted
 * from the metro pairs Fabric actually measures, so that an <em>unmeasured</em> pair can be estimated
 * from distance with a factor the catalogue itself supports instead of a hard-coded guess.
 *
 * <p>For every measured pair the ratio {@code measured round trip / great-circle fibre floor} is
 * taken (the floor is {@link SpeedOfLightLatency#roundTrip()} over the haversine distance). The
 * factor is the <strong>median</strong> of those ratios, which a few unusual routes cannot drag
 * around. Pairs whose floor is below {@link #MIN_FLOOR_MS} are left out of the fit: over a few
 * hundred kilometres fixed equipment and handoff delay dominate and the ratio says nothing about
 * routing. A factor is fitted for the whole catalogue and, separately, for every region pair
 * (for example {@code AMER/EMEA}) with at least {@link #MIN_PAIRS_PER_REGION_PAIR} usable pairs;
 * a region pair with fewer falls back to the overall factor.</p>
 *
 * <p>With no usable measured pair at all the calibration is {@link #uncalibrated()}: it uses
 * {@link #DEFAULT_FACTOR}, a single live observation (Fabric SV&harr;DC 65.8&nbsp;ms against a
 * 37.6&nbsp;ms floor, 2026-11-11), and says so in {@link #describe()}.</p>
 *
 * @param overall        the catalogue-wide fit
 * @param byRegionPair   the per-region-pair fits, keyed {@code AMER/EMEA} (names sorted); only pairs with
 *                       enough data appear
 * @param pairsExcluded  measured pairs left out of the fit (floor below {@link #MIN_FLOOR_MS}, or no coordinates)
 */
public record RouteFactorCalibration(Fit overall, Map<String, Fit> byRegionPair, int pairsExcluded) {

    /**
     * The factor used when nothing can be fitted: Fabric measured SV&harr;DC at 65.8&nbsp;ms round trip
     * on 2026-11-11 against a 37.6&nbsp;ms straight-fibre floor (3,843&nbsp;km), a ratio of 1.75.
     */
    public static final double DEFAULT_FACTOR = 1.75;

    /** Fewest usable pairs a region pair needs for its own factor. */
    public static final int MIN_PAIRS_PER_REGION_PAIR = 5;

    /** Pairs whose straight-fibre round-trip floor is below this (about 200 km) are not used to fit. */
    public static final double MIN_FLOOR_MS = 2.0;

    private static final SpeedOfLightLatency FLOOR = SpeedOfLightLatency.roundTrip();

    /** Makes the map immutable and ordered. */
    public RouteFactorCalibration {
        if (overall == null) {
            throw new IllegalArgumentException("A calibration needs an overall fit");
        }
        byRegionPair = byRegionPair == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(byRegionPair));
    }

    /**
     * One fitted factor.
     *
     * @param scope   {@code overall}, a region pair such as {@code AMER/EMEA}, or {@code default}
     * @param factor  the median ratio of measured round trip to fibre floor
     * @param pairs   how many measured pairs it was fitted from (0 for the default)
     * @param low     the 25th-percentile ratio (equals {@code factor} for the default)
     * @param high    the 75th-percentile ratio (equals {@code factor} for the default)
     */
    public record Fit(String scope, double factor, int pairs, double low, double high) {
    }

    /**
     * One Fabric-measured metro pair.
     *
     * @param regionA             region of one metro; may be {@code null}
     * @param regionB             region of the other metro; may be {@code null}
     * @param distanceKm          great-circle distance between the metros' coordinates
     * @param measuredRoundTripMs Fabric's {@code connectedMetros.avgLatency} for the pair (round trip)
     */
    public record MeasuredPair(Region regionA, Region regionB, double distanceKm, double measuredRoundTripMs) {
    }

    /** @return the calibration used when no measured pair is available: {@link #DEFAULT_FACTOR} */
    public static RouteFactorCalibration uncalibrated() {
        return new RouteFactorCalibration(new Fit("default", DEFAULT_FACTOR, 0, DEFAULT_FACTOR, DEFAULT_FACTOR),
                Map.of(), 0);
    }

    /**
     * Fits the factor from measured pairs. Each pair should appear once (not once per direction).
     *
     * @param pairs the measured pairs; non-finite or non-positive figures are ignored
     * @return the calibration; {@link #uncalibrated()} (with the exclusion count) when no pair is usable
     */
    public static RouteFactorCalibration fit(Collection<MeasuredPair> pairs) {
        List<Double> all = new ArrayList<>();
        Map<String, List<Double>> byRegion = new TreeMap<>();
        int excluded = 0;
        for (MeasuredPair pair : pairs == null ? List.<MeasuredPair>of() : pairs) {
            if (pair == null || !Double.isFinite(pair.distanceKm()) || !Double.isFinite(pair.measuredRoundTripMs())
                    || pair.measuredRoundTripMs() <= 0) {
                excluded++;
                continue;
            }
            double floor = FLOOR.millisForKm(pair.distanceKm());
            if (floor < MIN_FLOOR_MS) {
                excluded++;
                continue;
            }
            double ratio = pair.measuredRoundTripMs() / floor;
            all.add(ratio);
            if (pair.regionA() != null && pair.regionB() != null) {
                byRegion.computeIfAbsent(regionPairKey(pair.regionA(), pair.regionB()), k -> new ArrayList<>()).add(ratio);
            }
        }
        if (all.isEmpty()) {
            RouteFactorCalibration fallback = uncalibrated();
            return new RouteFactorCalibration(fallback.overall(), Map.of(), excluded);
        }
        Map<String, Fit> fits = new TreeMap<>();
        for (Map.Entry<String, List<Double>> entry : byRegion.entrySet()) {
            if (entry.getValue().size() >= MIN_PAIRS_PER_REGION_PAIR) {
                fits.put(entry.getKey(), fitOf(entry.getKey(), entry.getValue()));
            }
        }
        return new RouteFactorCalibration(fitOf("overall", all), fits, excluded);
    }

    /**
     * @param a one region; may be {@code null}
     * @param b the other region; may be {@code null}
     * @return the fit used for a pair of metros in these regions: the region pair's own fit when there
     *         is one, otherwise the overall fit
     */
    public Fit fitFor(Region a, Region b) {
        if (a != null && b != null) {
            Fit fit = byRegionPair.get(regionPairKey(a, b));
            if (fit != null) {
                return fit;
            }
        }
        return overall;
    }

    /**
     * Estimates a round trip from distance with the calibrated factor.
     *
     * @param distanceKm great-circle distance, km (finite)
     * @param a          region of one end; may be {@code null}
     * @param b          region of the other end; may be {@code null}
     * @return the estimated round trip in ms
     */
    public double estimateRoundTripMs(double distanceKm, Region a, Region b) {
        return FLOOR.millisForKm(distanceKm) * fitFor(a, b).factor();
    }

    /** @return {@code true} when the factor came from measured pairs rather than {@link #DEFAULT_FACTOR} */
    public boolean isCalibrated() {
        return overall.pairs() > 0;
    }

    /** @return one sentence stating the factor, what it was fitted from, and any per-region-pair factors */
    public String describe() {
        if (!isCalibrated()) {
            return String.format(Locale.ROOT, "Unmeasured latencies are estimated as %.2f x the straight-fibre "
                    + "round-trip floor: no Fabric-measured metro pair was usable to calibrate it, so the factor is "
                    + "the single live observation SV-DC 65.8 ms vs a 37.6 ms floor (2026-11-11)%s.",
                    overall.factor(), pairsExcluded > 0 ? " (" + pairsExcluded + " measured pair(s) were too short "
                            + "or lacked coordinates)" : "");
        }
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT,
                "Unmeasured latencies are estimated as %.2f x the straight-fibre round-trip floor (median of %d "
                        + "Fabric-measured metro pairs, interquartile %.2f-%.2f; %d short or uncoordinated pair(s) "
                        + "left out)", overall.factor(), overall.pairs(), overall.low(), overall.high(),
                pairsExcluded));
        if (!byRegionPair.isEmpty()) {
            sb.append("; per region pair: ");
            List<String> parts = new ArrayList<>();
            for (Fit fit : byRegionPair.values()) {
                parts.add(String.format(Locale.ROOT, "%s %.2f (%d pairs)", fit.scope(), fit.factor(), fit.pairs()));
            }
            sb.append(String.join(", ", parts));
        }
        return sb.append('.').toString();
    }

    private static Fit fitOf(String scope, List<Double> ratios) {
        List<Double> sorted = new ArrayList<>(ratios);
        Collections.sort(sorted);
        return new Fit(scope, quantile(sorted, 0.5), sorted.size(), quantile(sorted, 0.25), quantile(sorted, 0.75));
    }

    /** Linear-interpolated quantile of a sorted, non-empty list. */
    private static double quantile(List<Double> sorted, double q) {
        double position = q * (sorted.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        double fraction = position - lower;
        return sorted.get(lower) + (sorted.get(upper) - sorted.get(lower)) * fraction;
    }

    /**
     * @param a one region
     * @param b the other region
     * @return the key {@link #byRegionPair()} uses: the two names sorted and joined with {@code /}
     */
    public static String regionPairKey(Region a, Region b) {
        String x = a.name();
        String y = b.name();
        return x.compareTo(y) <= 0 ? x + "/" + y : y + "/" + x;
    }
}
