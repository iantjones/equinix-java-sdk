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

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import com.eqixiac.equinix.design.estate.EstateInventory.ConnectionRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.CoverageGap;
import com.eqixiac.equinix.design.estate.EstateInventory.PortRecord;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.implementation.MetricDatapoint;
import com.eqixiac.equinix.fabric.model.implementation.filter.MetricQuery;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Reads bandwidth utilisation for the live connections (and optionally the active ports) of an
 * inventory through {@code POST /fabric/v4/metrics/search} and reduces it to p50 / p95 / peak.
 *
 * <p>Verified live on 2026-11-12 (read-only, one project): a search with one {@code /subject}, the
 * names {@value #CONNECTION_TX} and {@value #CONNECTION_RX}, and a seven-day
 * {@code /dataPoints/endDateTime BETWEEN} returned both metrics with {@code unit "bit/s"},
 * {@code type "GAUGE"} and {@code interval "PT1H"}; connections that were not provisioned returned
 * an empty {@code datapoints} array. The service requires a single subject per search, so the probe
 * makes one request per resource, pausing between requests and stopping at a ceiling.</p>
 *
 * <p>A sample is the larger of transmit and receive for the same interval (a Fabric bandwidth limit
 * applies to each direction). Missing data is recorded as unknown with a reason, never as zero.</p>
 */
public final class UtilizationProbe {

    /** Connection transmit-rate metric name (live 2026-11-12, unit bit/s). */
    public static final String CONNECTION_TX = "equinix.fabric.connection.bandwidth_tx.usage";
    /** Connection receive-rate metric name (live 2026-11-12, unit bit/s). */
    public static final String CONNECTION_RX = "equinix.fabric.connection.bandwidth_rx.usage";
    /** Port transmit-rate metric name (from the Fabric v4 4.34.0 spec). */
    public static final String PORT_TX = "equinix.fabric.port.bandwidth_tx.usage";
    /** Port receive-rate metric name (from the Fabric v4 4.34.0 spec). */
    public static final String PORT_RX = "equinix.fabric.port.bandwidth_rx.usage";

    /** Fetches the metrics of one search. */
    @FunctionalInterface
    public interface MetricFetcher {
        /**
         * Runs one metrics search.
         *
         * @param query the query (one subject)
         * @return the metrics returned
         */
        List<Metric> fetch(MetricQuery query);
    }

    /** Pauses between requests (replaceable in tests). */
    @FunctionalInterface
    public interface Pacer {
        /**
         * Waits.
         *
         * @param millis how long
         */
        void pause(long millis);
    }

    private final MetricFetcher fetcher;
    private final Duration window;
    private final Clock clock;
    private final int maxSubjects;
    private final long pauseMillis;
    private final boolean includePorts;
    private final Pacer pacer;

    private UtilizationProbe(Builder b) {
        this.fetcher = Objects.requireNonNull(b.fetcher, "fetcher");
        this.window = Objects.requireNonNull(b.window, "window");
        if (window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("The utilisation window must be positive");
        }
        this.clock = b.clock;
        this.maxSubjects = b.maxSubjects;
        this.pauseMillis = b.pauseMillis;
        this.includePorts = b.includePorts;
        this.pacer = b.pacer;
    }

    /**
     * A probe over the Fabric metrics API.
     *
     * @param fabric the Fabric client
     * @return a builder
     */
    public static Builder over(Fabric fabric) {
        Objects.requireNonNull(fabric, "fabric");
        return new Builder(q -> fabric.metrics().search(q).loadAll().toList());
    }

    /**
     * A probe over any metric source (tests, recorded data).
     *
     * @param fetcher the source
     * @return a builder
     */
    public static Builder over(MetricFetcher fetcher) {
        return new Builder(fetcher);
    }

    /**
     * Probes the live connections (and, when enabled, active ports) of an inventory.
     *
     * @param inventory the inventory
     * @return the utilisation snapshot
     */
    public UtilizationSnapshot probe(EstateInventory inventory) {
        Instant end = clock.instant();
        Instant start = end.minus(window);
        LocalDateTime from = LocalDateTime.ofInstant(start, ZoneOffset.UTC);
        LocalDateTime to = LocalDateTime.ofInstant(end, ZoneOffset.UTC);
        List<Subject> subjects = new ArrayList<>();
        for (ConnectionRecord c : inventory.connections()) {
            if (c.isLive()) {
                subjects.add(new Subject(c.id(), c.bandwidthMbps(), MetricQuery.forConnection(c.id())
                        .names(CONNECTION_TX, CONNECTION_RX).between(from, to)));
            }
        }
        if (includePorts) {
            for (PortRecord p : inventory.ports()) {
                if ("ACTIVE".equals(p.state())) {
                    subjects.add(new Subject(p.id(), p.speedMbps(), MetricQuery.forPort(p.id())
                            .names(PORT_TX, PORT_RX).between(from, to)));
                }
            }
        }
        Map<String, UtilizationSnapshot.Stats> stats = new TreeMap<>();
        Map<String, int[]> failures = new TreeMap<>();
        List<CoverageGap> gaps = new ArrayList<>();
        int rateLimitedInARow = 0;
        for (int i = 0; i < subjects.size(); i++) {
            Subject s = subjects.get(i);
            if (i >= maxSubjects) {
                gaps.add(new CoverageGap("metrics", "TRUNCATED", null, (subjects.size() - maxSubjects)
                        + " resource(s) were not probed (ceiling " + maxSubjects + " metric searches)"));
                break;
            }
            if (i > 0 && pauseMillis > 0) {
                pacer.pause(pauseMillis);
            }
            try {
                stats.put(s.id, reduce(s.id, s.provisionedMbps, fetcher.fetch(s.query)));
                rateLimitedInARow = 0;
            } catch (EquinixServiceException e) {
                String reason = EstateCollector.reason(e);
                stats.put(s.id, UtilizationSnapshot.Stats.unknown(s.id, s.provisionedMbps,
                        "metrics search failed: " + reason + (e.getStatusCode() == null ? "" : " (HTTP " + e.getStatusCode() + ")")));
                failures.computeIfAbsent(reason + "|" + e.getStatusCode(), k -> new int[1])[0]++;
                rateLimitedInARow = "RATE_LIMITED".equals(reason) ? rateLimitedInARow + 1 : 0;
                if (rateLimitedInARow >= 3) {
                    gaps.add(new CoverageGap("metrics", "RATE_LIMITED", 429, "stopped after three rate-limited "
                            + "searches in a row; " + (subjects.size() - i - 1) + " resource(s) not probed"));
                    break;
                }
            } catch (RuntimeException e) {
                stats.put(s.id, UtilizationSnapshot.Stats.unknown(s.id, s.provisionedMbps,
                        "metrics search failed: " + e.getClass().getSimpleName()));
                failures.computeIfAbsent("ERROR|null", k -> new int[1])[0]++;
            }
        }
        failures.forEach((k, n) -> {
            String[] parts = k.split("\\|");
            gaps.add(new CoverageGap("metrics", parts[0], "null".equals(parts[1]) ? null : Integer.valueOf(parts[1]),
                    n[0] + " metrics search(es) failed; those resources have unknown utilisation"));
        });
        return new UtilizationSnapshot(start, end, "POST /fabric/v4/metrics/search (" + CONNECTION_TX + ", "
                + CONNECTION_RX + (includePorts ? ", " + PORT_TX + ", " + PORT_RX : "") + ")", stats, gaps);
    }

    /**
     * Reduces the metrics of one resource: converts rates to Mbps, takes the larger direction per
     * interval, and computes percentiles.
     *
     * @param resourceId      the resource id
     * @param provisionedMbps the provisioned bandwidth, or {@code null}
     * @param metrics         the metrics returned for the resource
     * @return the stats (unknown when there are no datapoints or the unit is not a rate)
     */
    public static UtilizationSnapshot.Stats reduce(String resourceId, Integer provisionedMbps, List<Metric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return UtilizationSnapshot.Stats.unknown(resourceId, provisionedMbps, "no metrics returned");
        }
        Map<String, Double> perInterval = new TreeMap<>();
        String interval = null;
        for (Metric m : metrics) {
            if (m.getDatapoints() == null || m.getDatapoints().isEmpty()) {
                continue;
            }
            Double factor = toMbps(m.getUnit());
            if (factor == null) {
                return UtilizationSnapshot.Stats.unknown(resourceId, provisionedMbps,
                        "unrecognised metric unit '" + m.getUnit() + "'");
            }
            interval = interval == null ? m.getInterval() : interval;
            int idx = 0;
            for (MetricDatapoint d : m.getDatapoints()) {
                String key = d.getEndDateTime() != null ? d.getEndDateTime().toString() : "#" + idx;
                idx++;
                if (d.getValue() == null) {
                    continue;
                }
                perInterval.merge(key, d.getValue() * factor, Math::max);
            }
        }
        return UtilizationSnapshot.Stats.reduce(resourceId, provisionedMbps, interval,
                new ArrayList<>(perInterval.values()));
    }

    private static Double toMbps(String unit) {
        if (unit == null) {
            return null;
        }
        return switch (unit.trim().toLowerCase(Locale.ROOT)) {
            case "bit/s", "bps", "bits/s" -> 1e-6;
            case "kbit/s", "kbps" -> 1e-3;
            case "mbit/s", "mbps" -> 1.0;
            case "gbit/s", "gbps" -> 1000.0;
            default -> null;
        };
    }

    private record Subject(String id, Integer provisionedMbps, MetricQuery query) {
    }

    /** Configures a probe. */
    public static final class Builder {
        private final MetricFetcher fetcher;
        private Duration window = Duration.ofDays(7);
        private Clock clock = Clock.systemUTC();
        private int maxSubjects = 200;
        private long pauseMillis = 100;
        private boolean includePorts;
        private Pacer pacer = millis -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        private Builder(MetricFetcher fetcher) {
            this.fetcher = fetcher;
        }

        /**
         * Sets the window ending now (default seven days).
         *
         * @param window the window
         * @return this builder
         */
        public Builder window(Duration window) {
            this.window = window;
            return this;
        }

        /**
         * Sets the clock that defines "now".
         *
         * @param clock the clock
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Sets the most resources probed in one run (default 200).
         *
         * @param max the ceiling
         * @return this builder
         */
        public Builder maxSubjects(int max) {
            this.maxSubjects = max;
            return this;
        }

        /**
         * Sets the pause between metric searches (default 100 ms).
         *
         * @param millis the pause
         * @return this builder
         */
        public Builder pauseMillis(long millis) {
            this.pauseMillis = millis;
            return this;
        }

        /**
         * Also probes active ports (default off: port metrics are the sum of connections).
         *
         * @param include whether to probe ports
         * @return this builder
         */
        public Builder includePorts(boolean include) {
            this.includePorts = include;
            return this;
        }

        /**
         * Replaces the pause implementation (tests).
         *
         * @param pacer the pacer
         * @return this builder
         */
        public Builder pacer(Pacer pacer) {
            this.pacer = Objects.requireNonNull(pacer, "pacer");
            return this;
        }

        /**
         * Builds the probe.
         *
         * @return the probe
         */
        public UtilizationProbe build() {
            return new UtilizationProbe(this);
        }
    }
}
