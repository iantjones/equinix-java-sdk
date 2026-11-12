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

package com.eqixiac.equinix.fabric.client;

import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.filter.MetricQuery;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Client interface for the Equinix Fabric Metrics API ({@code POST /fabric/v4/metrics/search}).
 * It replaces the per-asset {@code GET /{asset}/{id}/metrics} and the {@code /ports/{id}/stats} and
 * {@code /connections/{id}/stats} statistics endpoints, which spec 4.34.0 deprecates and this SDK
 * no longer calls.
 *
 * <p>The search requires exactly one {@code /subject} and at least one {@code /name}
 * (live-verified 2026-11-11). Build the filter with {@link MetricQuery}, or use the typed helpers.</p>
 *
 * @author ianjones
 * @see com.eqixiac.equinix.fabric.model.Metric
 */
public interface Metrics {

    /**
     * Searches metrics with a filter built by a {@link MetricQuery}.
     *
     * @param query the query (one subject, at least one metric name)
     * @return a paginated, filtered list of matching metrics
     */
    PaginatedFilteredList<Metric> search(MetricQuery query);

    /**
     * Searches metrics with a hand-built filter.
     *
     * @param filter the filter; it must contain one {@code /subject} and a {@code /name} expression
     * @param sort the sort criteria, or {@code null}
     * @return a paginated, filtered list of matching metrics
     * @throws IllegalArgumentException when the filter lacks the required expressions
     */
    PaginatedFilteredList<Metric> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Returns every page of the named metrics of one connection over a time range.
     *
     * @param connectionUuid the connection uuid
     * @param fromDateTime start of the range, UTC wall clock
     * @param toDateTime end of the range, UTC wall clock
     * @param names the metric names, for example {@code equinix.fabric.connection.bandwidth_tx.usage}
     * @return the metrics
     */
    List<Metric> forConnection(String connectionUuid, LocalDateTime fromDateTime, LocalDateTime toDateTime, String... names);

    /**
     * Returns every page of the named metrics of one port over a time range.
     *
     * @param portUuid the port uuid
     * @param fromDateTime start of the range, UTC wall clock
     * @param toDateTime end of the range, UTC wall clock
     * @param names the metric names, for example {@code equinix.fabric.port.bandwidth_rx.usage}
     * @return the metrics
     */
    List<Metric> forPort(String portUuid, LocalDateTime fromDateTime, LocalDateTime toDateTime, String... names);

    /**
     * Returns every page of the named metrics of one metro over a time range.
     *
     * @param metroCode the metro code, for example {@code SV}
     * @param fromDateTime start of the range, UTC wall clock
     * @param toDateTime end of the range, UTC wall clock
     * @param names the metric names, for example {@code equinix.fabric.metro.sv_dc.latency}
     * @return the metrics
     */
    List<Metric> forMetro(String metroCode, LocalDateTime fromDateTime, LocalDateTime toDateTime, String... names);

    /**
     * Retrieves metrics by wildcard metric name ({@code GET /fabric/v4/metrics}). Only the wildcard
     * metro metric types are supported (for example {@code equinix.fabric.metro.*.latency} or
     * {@code equinix.fabric.metro.*.jitter_avg}). This endpoint does not support time-range filtering.
     *
     * @param name the wildcard metric name (required)
     * @param value which value to retrieve (for example {@code last}; required)
     * @return the list of matching metrics
     */
    List<Metric> getMetricsByName(String name, String value);
}
