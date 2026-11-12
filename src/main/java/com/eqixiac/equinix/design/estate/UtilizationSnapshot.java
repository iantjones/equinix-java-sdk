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

package com.eqixiac.equinix.design.estate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Bandwidth utilisation of the resources in an estate over one time window, reduced to
 * percentiles. A resource with no data has an entry whose {@link Stats#known()} is {@code false}
 * and whose reason says why: missing data is unknown, never zero.
 *
 * @param windowStart  the start of the window
 * @param windowEnd    the end of the window
 * @param source       where the data came from (for example {@code POST /fabric/v4/metrics/search})
 * @param stats        resource id to its utilisation
 * @param coverageGaps metrics that could not be read
 */
public record UtilizationSnapshot(Instant windowStart, Instant windowEnd, String source, Map<String, Stats> stats,
                                  List<EstateInventory.CoverageGap> coverageGaps) {

    /** Copies the collections (stats sorted by resource id). */
    public UtilizationSnapshot {
        stats = stats == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(stats));
        coverageGaps = coverageGaps == null ? List.of() : List.copyOf(coverageGaps);
    }

    /**
     * A snapshot with no utilisation data (the probe was not run).
     *
     * @return the empty snapshot
     */
    public static UtilizationSnapshot none() {
        return new UtilizationSnapshot(null, null, "not collected", Map.of(), List.of());
    }

    /**
     * The utilisation of one resource, when known.
     *
     * @param resourceId the resource id
     * @return the stats with data, or empty when unknown or not collected
     */
    public Optional<Stats> known(String resourceId) {
        Stats s = stats.get(resourceId);
        return s != null && s.known() ? Optional.of(s) : Optional.empty();
    }

    /**
     * The window length.
     *
     * @return the window, or {@code null} when not collected
     */
    @JsonIgnore
    public Duration window() {
        return windowStart == null || windowEnd == null ? null : Duration.between(windowStart, windowEnd);
    }

    /**
     * How many resources have data.
     *
     * @return the count of entries with data
     */
    @JsonIgnore
    public int withData() {
        return (int) stats.values().stream().filter(Stats::known).count();
    }

    /**
     * Serializes the snapshot to JSON.
     *
     * @return the JSON document
     */
    public String toJson() {
        return EstateJson.write(this);
    }

    /**
     * Reads a snapshot written by {@link #toJson()}.
     *
     * @param json the JSON document
     * @return the snapshot
     */
    public static UtilizationSnapshot fromJson(String json) {
        return EstateJson.read(json, UtilizationSnapshot.class);
    }

    /**
     * Utilisation of one resource. Rates are in Mbps; each sample is the larger of the transmit and
     * receive rates for the same interval, because a Fabric connection's bandwidth limit applies to
     * each direction separately.
     *
     * @param resourceId     the resource id
     * @param provisionedMbps the provisioned bandwidth the percentages are relative to; {@code null} when unknown
     * @param samples        the number of intervals with data
     * @param interval       the sample interval as the API reported it (for example {@code PT1H})
     * @param p50Mbps        the median, or {@code null} when unknown
     * @param p95Mbps        the 95th percentile, or {@code null} when unknown
     * @param peakMbps       the maximum, or {@code null} when unknown
     * @param unknownReason  why the figures are unknown; {@code null} when known
     */
    public record Stats(String resourceId, Integer provisionedMbps, int samples, String interval, Double p50Mbps,
                        Double p95Mbps, Double peakMbps, String unknownReason) {

        /**
         * Stats for a resource with no usable data.
         *
         * @param resourceId      the resource id
         * @param provisionedMbps the provisioned bandwidth, or {@code null}
         * @param reason          why the data is missing
         * @return the unknown stats
         */
        public static Stats unknown(String resourceId, Integer provisionedMbps, String reason) {
            return new Stats(resourceId, provisionedMbps, 0, null, null, null, null, reason);
        }

        /**
         * Reduces samples to percentiles. An empty sample list gives unknown stats.
         *
         * @param resourceId      the resource id
         * @param provisionedMbps the provisioned bandwidth, or {@code null}
         * @param interval        the sample interval label, or {@code null}
         * @param samplesMbps     the samples in Mbps (nulls and NaN are ignored)
         * @return the stats
         */
        public static Stats reduce(String resourceId, Integer provisionedMbps, String interval,
                                   List<Double> samplesMbps) {
            List<Double> s = new ArrayList<>();
            for (Double d : samplesMbps) {
                if (d != null && !d.isNaN() && !d.isInfinite() && d >= 0) {
                    s.add(d);
                }
            }
            if (s.isEmpty()) {
                return unknown(resourceId, provisionedMbps, "no datapoints in the window");
            }
            Collections.sort(s);
            return new Stats(resourceId, provisionedMbps, s.size(), interval, percentile(s, 50), percentile(s, 95),
                    s.get(s.size() - 1), null);
        }

        /**
         * Nearest-rank percentile of sorted values.
         *
         * @param sorted the values in ascending order (not empty)
         * @param p      the percentile, 0 to 100
         * @return the value
         */
        static double percentile(List<Double> sorted, int p) {
            int rank = (int) Math.ceil(p / 100.0 * sorted.size());
            return sorted.get(Math.max(0, Math.min(sorted.size() - 1, rank - 1)));
        }

        /**
         * Whether the figures are known.
         *
         * @return {@code true} when there is data
         */
        public boolean known() {
            return unknownReason == null && p95Mbps != null;
        }

        /**
         * The 95th percentile as a percent of the provisioned bandwidth.
         *
         * @return the percent, or empty when either figure is unknown
         */
        public Optional<Double> p95Percent() {
            return known() && provisionedMbps != null && provisionedMbps > 0
                    ? Optional.of(p95Mbps * 100.0 / provisionedMbps) : Optional.empty();
        }

        /**
         * The peak as a percent of the provisioned bandwidth.
         *
         * @return the percent, or empty when unknown
         */
        public Optional<Double> peakPercent() {
            return known() && provisionedMbps != null && provisionedMbps > 0
                    ? Optional.of(peakMbps * 100.0 / provisionedMbps) : Optional.empty();
        }
    }
}
