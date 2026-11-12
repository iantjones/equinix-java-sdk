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

package com.eqixiac.equinix.fabric.model.implementation.filter;

import com.eqixiac.equinix.core.http.ParameterMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A valid {@code POST /fabric/v4/metrics/search} filter, built so that the two expressions the
 * service requires are always present.
 *
 * <p>Live behaviour (observed 2026-11-11): the service answers {@code HTTP 400 EQ-3045014}
 * "subject must be in the filter list with = or IN operator" or "name must be in the filter list
 * with IN or = operator" when either is missing, and {@code EQ-3045017} "subject only supports a
 * single value" when more than one subject is sent. A query therefore has exactly one subject and
 * at least one metric name; the time range and interval are optional.</p>
 *
 * <pre>{@code
 * MetricQuery q = MetricQuery.forConnection(uuid)
 *         .names("equinix.fabric.connection.bandwidth_tx.usage")
 *         .between(LocalDateTime.now(ZoneOffset.UTC).minusDays(1), LocalDateTime.now(ZoneOffset.UTC));
 * fabric.metrics().search(q);
 * }</pre>
 *
 * <p>UTC contract: {@code LocalDateTime} inputs are UTC wall clock.</p>
 */
public final class MetricQuery {

    /** Filter property of the metric subject. */
    public static final String SUBJECT = "/subject";
    /** Filter property of the metric name. */
    public static final String NAME = "/name";
    /** Filter property of the data-point end time. */
    public static final String END_DATE_TIME = "/dataPoints/endDateTime";
    /** Filter property of the data-point interval (ISO-8601 duration). */
    public static final String INTERVAL = "/interval";

    private final String subject;
    private final List<String> names = new ArrayList<>();
    private LocalDateTime from;
    private LocalDateTime to;
    private String interval;

    private MetricQuery(String subject) {
        this.subject = subject;
    }

    /**
     * Starts a query for any metric subject path, for example {@code /fabric/v4/ports/{uuid}}.
     *
     * @param subject the subject path (required)
     * @return a new query
     */
    public static MetricQuery forSubject(String subject) {
        requireText(subject, "subject");
        return new MetricQuery(subject);
    }

    /**
     * Starts a query for one connection ({@code /fabric/v4/connections/{uuid}}).
     *
     * @param connectionUuid the connection uuid (required)
     * @return a new query
     */
    public static MetricQuery forConnection(String connectionUuid) {
        requireText(connectionUuid, "connectionUuid");
        return new MetricQuery("/fabric/v4/connections/" + connectionUuid);
    }

    /**
     * Starts a query for one port ({@code /fabric/v4/ports/{uuid}}).
     *
     * @param portUuid the port uuid (required)
     * @return a new query
     */
    public static MetricQuery forPort(String portUuid) {
        requireText(portUuid, "portUuid");
        return new MetricQuery("/fabric/v4/ports/" + portUuid);
    }

    /**
     * Starts a query for one metro ({@code /fabric/v4/metros/{code}}), for example with the name
     * {@code equinix.fabric.metro.sv_dc.latency} (live-verified 2026-11-11).
     *
     * @param metroCode the metro code (required)
     * @return a new query
     */
    public static MetricQuery forMetro(String metroCode) {
        requireText(metroCode, "metroCode");
        return new MetricQuery("/fabric/v4/metros/" + metroCode);
    }

    /**
     * Adds one or more metric names (at least one is required before {@link #toFilter()}).
     *
     * @param metricNames the metric names, for example {@code equinix.fabric.port.bandwidth_rx.usage}
     * @return this query
     */
    public MetricQuery names(String... metricNames) {
        Objects.requireNonNull(metricNames, "metricNames");
        for (String n : metricNames) {
            requireText(n, "metric name");
            this.names.add(n);
        }
        return this;
    }

    /**
     * Restricts the data points to an end time between {@code from} and {@code to}.
     *
     * @param from the start, UTC wall clock (required)
     * @param to the end, UTC wall clock (required, not before {@code from})
     * @return this query
     */
    public MetricQuery between(LocalDateTime from, LocalDateTime to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must not be before 'from'");
        }
        this.from = from;
        this.to = to;
        return this;
    }

    /**
     * Sets the data-point interval, an ISO-8601 duration such as {@code PT5M} or {@code PT1H}
     * ({@code PT1H} live-verified 2026-11-11).
     *
     * @param isoDuration the interval
     * @return this query
     */
    public MetricQuery interval(String isoDuration) {
        requireText(isoDuration, "interval");
        this.interval = isoDuration;
        return this;
    }

    /**
     * Returns the subject path this query targets.
     *
     * @return the subject
     */
    public String subject() {
        return subject;
    }

    /**
     * Builds the search filter.
     *
     * @return a filter with the subject, the names and the optional range and interval
     * @throws IllegalStateException when no metric name was added
     */
    public FilterPropertyList toFilter() {
        if (names.isEmpty()) {
            throw new IllegalStateException("A metrics search needs at least one metric name (names(...))");
        }
        FilterPropertyList filter = Filter.filter().and().equals(SUBJECT, subject);
        filter.in(NAME, new ArrayList<>(names));
        if (from != null) {
            filter.between(END_DATE_TIME, ParameterMapper.dateTimeForQuery(from), ParameterMapper.dateTimeForQuery(to));
        }
        if (interval != null) {
            filter.equals(INTERVAL, interval);
        }
        return filter;
    }

    /**
     * Checks that a hand-built filter carries the expressions the metrics search requires.
     *
     * @param filter the filter to check
     * @throws IllegalArgumentException when {@code /subject} (= or IN, one value) or {@code /name}
     *         (= or IN) is missing
     */
    public static void validate(FilterPropertyList filter) {
        if (filter == null || !filter.hasProperty(NAME, FilterOperator.EQUALS, FilterOperator.IN)) {
            throw new IllegalArgumentException("A metrics search filter needs /name with = or IN; use MetricQuery");
        }
        boolean oneSubject = filter.getFilterProperties().stream()
                .anyMatch(p -> SUBJECT.equals(p.getProperty())
                        && Arrays.asList(FilterOperator.EQUALS, FilterOperator.IN).contains(p.getOperator())
                        && p.getValues() != null && p.getValues().size() == 1);
        if (!oneSubject) {
            throw new IllegalArgumentException("A metrics search filter needs exactly one /subject value with = or IN; use MetricQuery");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
