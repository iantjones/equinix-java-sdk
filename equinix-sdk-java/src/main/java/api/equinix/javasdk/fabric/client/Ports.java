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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.Sortable;
import api.equinix.javasdk.fabric.enums.StatisticDuration;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.PortStatistic;
import api.equinix.javasdk.fabric.model.PortVlan;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Client interface for managing Equinix Fabric ports. Provides operations for listing,
 * retrieving, and monitoring port bandwidth statistics.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Ports {

    /**
     * Lists all ports accessible to the current account.
     *
     * @return a paginated list of ports
     */
    PaginatedList<Port> list();

    /**
     * Searches for ports using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching ports
     */
    PaginatedFilteredList<Port> search();

    /**
     * Searches for ports matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching ports
     */
    PaginatedFilteredList<Port> search(FilterPropertyList filter);

    /**
     * Searches for ports with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching ports
     */
    PaginatedFilteredList<Port> search(SortPropertyList sort);

    /**
     * Searches for ports matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching ports
     */
    PaginatedFilteredList<Port> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single port by its unique identifier.
     *
     * @param uuid the unique identifier of the port
     * @return the port matching the given UUID
     */
    Port getByUuid(String uuid);

    /**
     * Lists the VLANs (link protocols) configured on a port.
     *
     * @param portUuid the unique identifier of the port
     * @return the list of VLANs configured on the port
     */
    List<PortVlan> getVlans(String portUuid);

    /**
     * Retrieves bandwidth statistics for a port over the specified time range.
     *
     * @param uuid the unique identifier of the port
     * @param startDateTime the start of the statistics time range
     * @param endDateTime the end of the statistics time range
     * @return the port statistics for the specified time range
     * @deprecated the {@code /stats} endpoint is deprecated by Equinix; use
     *             {@link #getMetrics(String, String, LocalDateTime, LocalDateTime)} or
     *             {@link Metrics#search(FilterPropertyList)} instead.
     */
    @Deprecated
    PortStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * Retrieves metrics for a single port over the specified time range. This is the
     * non-deprecated replacement for {@link #getStatistics(String, LocalDateTime, LocalDateTime)}.
     *
     * @param uuid the unique identifier of the port
     * @param name the metric name to retrieve (for example {@code equinix.fabric.port.bandwidth_rx.usage}), or {@code null} for all metrics
     * @param fromDateTime the start of the metrics time range
     * @param toDateTime the end of the metrics time range
     * @return the list of metrics for the port over the specified time range
     */
    List<Metric> getMetrics(String uuid, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime);

    /**
     * Retrieves top port statistics ranked by bandwidth usage for the specified duration.
     *
     * @param duration the time duration to aggregate statistics over
     * @param sortable the sort configuration for ranking results
     * @return a paginated list of top port statistics
     */
    PaginatedList<PortStatistic> getTopStatistics(StatisticDuration duration, Sortable sortable);

    /**
     * Retrieves top port statistics ranked by bandwidth usage with additional request options.
     *
     * @param duration the time duration to aggregate statistics over
     * @param sortable the sort configuration for ranking results
     * @param requestBuilder additional request parameters for filtering top statistics
     * @return a paginated list of top port statistics
     */
    PaginatedList<PortStatistic> getTopStatistics(StatisticDuration duration, Sortable sortable, RequestBuilder.TopPortStatistics requestBuilder);
}
