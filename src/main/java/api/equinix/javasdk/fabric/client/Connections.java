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

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.Direction;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.ConnectionStatistic;
import api.equinix.javasdk.fabric.model.RouteAggregationAttachment;
import api.equinix.javasdk.fabric.model.RouteFilterAttachment;
import api.equinix.javasdk.fabric.model.RouteTableEntry;
import api.equinix.javasdk.fabric.model.ValidateConnectionResult;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Client interface for managing Equinix Fabric connections. Provides operations for searching,
 * retrieving, creating, and monitoring connections between Fabric endpoints.
 *
 * @author ianjones
 */
public interface Connections {

    /**
     * Searches for connections using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching connections
     */
    PaginatedFilteredList<Connection> search();

    /**
     * Searches for connections matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching connections
     */
    PaginatedFilteredList<Connection> search(FilterPropertyList filter);

    /**
     * Searches for connections with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching connections
     */
    PaginatedFilteredList<Connection> search(SortPropertyList sort);

    /**
     * Searches for connections matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching connections
     */
    PaginatedFilteredList<Connection> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Validates one or more prospective connections against the supplied filter criteria,
     * for example a cloud provider authorization key or VLAN availability, without creating
     * the connection. Mirrors the SDK's existing Fabric search filter shape.
     *
     * @param filter the validation filter criteria (auth key or VLAN)
     * @return the list of connection specifications matching the validation request
     */
    List<ValidateConnectionResult> validate(FilterPropertyList filter);

    /**
     * Retrieves a single connection by its unique identifier.
     *
     * @param uuid the unique identifier of the connection
     * @return the connection matching the given UUID
     */
    Connection getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new connection.
     * Call methods on the returned builder to configure the connection, then call {@code create()}.
     *
     * @param connectionType the type of connection to create
     * @return a builder for configuring the new connection
     */
    ConnectionOperator.ConnectionBuilder define(ConnectionType connectionType);

    /**
     * Retrieves bandwidth statistics for a connection over the specified time range, viewed from a specific side.
     *
     * <p>UTC contract: {@code LocalDateTime} inputs are UTC wall clock (matching every timestamp
     * the SDK returns); use {@code LocalDateTime.now(ZoneOffset.UTC)} for the current time.</p>
     *
     * @param uuid the unique identifier of the connection
     * @param startDateTime the start of the statistics time range, as UTC wall clock
     * @param endDateTime the end of the statistics time range, as UTC wall clock
     * @param viewPoint the side (A-side or Z-side) from which to view the statistics
     * @return the connection statistics for the specified time range and viewpoint
     * @deprecated the {@code /stats} endpoint is deprecated by Equinix; use
     *             {@link #getMetrics(String, String, LocalDateTime, LocalDateTime)} or
     *             {@link Metrics#search(FilterPropertyList)} instead.
     */
    @Deprecated
    ConnectionStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint);

    /**
     * Retrieves bandwidth statistics for a connection over the specified time range.
     *
     * <p>UTC contract: {@code LocalDateTime} inputs are UTC wall clock (matching every timestamp
     * the SDK returns); use {@code LocalDateTime.now(ZoneOffset.UTC)} for the current time.</p>
     *
     * @param uuid the unique identifier of the connection
     * @param startDateTime the start of the statistics time range, as UTC wall clock
     * @param endDateTime the end of the statistics time range, as UTC wall clock
     * @return the connection statistics for the specified time range
     * @deprecated the {@code /stats} endpoint is deprecated by Equinix; use
     *             {@link #getMetrics(String, String, LocalDateTime, LocalDateTime)} or
     *             {@link Metrics#search(FilterPropertyList)} instead.
     */
    @Deprecated
    ConnectionStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * Retrieves metrics for a single connection over the specified time range. This is the
     * non-deprecated replacement for {@link #getStatistics(String, LocalDateTime, LocalDateTime)}.
     *
     * <p>UTC contract: {@code LocalDateTime} inputs are UTC wall clock (matching every timestamp
     * the SDK returns); use {@code LocalDateTime.now(ZoneOffset.UTC)} for the current time.</p>
     *
     * @param uuid the unique identifier of the connection
     * @param name the metric name to retrieve (for example {@code equinix.fabric.connection.bandwidth_tx.usage}), or {@code null} for all metrics
     * @param fromDateTime the start of the metrics time range, as UTC wall clock
     * @param toDateTime the end of the metrics time range, as UTC wall clock
     * @return the list of metrics for the connection over the specified time range
     */
    List<Metric> getMetrics(String uuid, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime);

    /**
     * Searches the routes a connection's Cloud Router is advertising to the customer, with default
     * filter and sort criteria.
     *
     * @param uuid the unique identifier of the connection
     * @return a paginated, filtered list of advertised route table entries
     */
    PaginatedFilteredList<RouteTableEntry> searchAdvertisedRoutes(String uuid);

    /**
     * Searches the routes a connection's Cloud Router is advertising to the customer.
     *
     * @param uuid the unique identifier of the connection
     * @param filter the filter criteria to apply (may be {@code null})
     * @param sort the sort criteria to apply (may be {@code null})
     * @return a paginated, filtered list of advertised route table entries
     */
    PaginatedFilteredList<RouteTableEntry> searchAdvertisedRoutes(String uuid, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Searches the routes a connection's Cloud Router has received from the customer, with default
     * filter and sort criteria.
     *
     * @param uuid the unique identifier of the connection
     * @return a paginated, filtered list of received route table entries
     */
    PaginatedFilteredList<RouteTableEntry> searchReceivedRoutes(String uuid);

    /**
     * Searches the routes a connection's Cloud Router has received from the customer.
     *
     * @param uuid the unique identifier of the connection
     * @param filter the filter criteria to apply (may be {@code null})
     * @param sort the sort criteria to apply (may be {@code null})
     * @return a paginated, filtered list of received route table entries
     */
    PaginatedFilteredList<RouteTableEntry> searchReceivedRoutes(String uuid, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Lists all Route Aggregations attached to a connection.
     *
     * @param connectionId the unique identifier of the connection
     * @return the list of route aggregation attachments
     */
    List<RouteAggregationAttachment> getRouteAggregations(String connectionId);

    /**
     * Retrieves a single Route Aggregation attached to a connection.
     *
     * @param connectionId the unique identifier of the connection
     * @param routeAggregationId the unique identifier of the route aggregation
     * @return the route aggregation attachment
     */
    RouteAggregationAttachment getRouteAggregation(String connectionId, String routeAggregationId);

    /**
     * Attaches a Route Aggregation to a connection.
     *
     * @param connectionId the unique identifier of the connection
     * @param routeAggregationId the unique identifier of the route aggregation to attach
     * @return the resulting route aggregation attachment
     */
    RouteAggregationAttachment attachRouteAggregation(String connectionId, String routeAggregationId);

    /**
     * Detaches a Route Aggregation from a connection.
     *
     * @param connectionId the unique identifier of the connection
     * @param routeAggregationId the unique identifier of the route aggregation to detach
     * @return {@code true} if the detach request was accepted
     */
    Boolean detachRouteAggregation(String connectionId, String routeAggregationId);

    /**
     * Lists all Route Filters attached to a connection.
     *
     * @param connectionId the unique identifier of the connection
     * @return the list of route filter attachments
     */
    List<RouteFilterAttachment> getRouteFilters(String connectionId);

    /**
     * Retrieves a single Route Filter attached to a connection.
     *
     * @param connectionId the unique identifier of the connection
     * @param routeFilterId the unique identifier of the route filter
     * @return the route filter attachment
     */
    RouteFilterAttachment getRouteFilter(String connectionId, String routeFilterId);

    /**
     * Attaches a Route Filter to a connection in the given direction.
     *
     * @param connectionId the unique identifier of the connection
     * @param routeFilterId the unique identifier of the route filter to attach
     * @param direction the direction to apply the route filter ({@code INBOUND} or {@code OUTBOUND})
     * @return the resulting route filter attachment
     */
    RouteFilterAttachment attachRouteFilter(String connectionId, String routeFilterId, Direction direction);

    /**
     * Detaches a Route Filter from a connection.
     *
     * @param connectionId the unique identifier of the connection
     * @param routeFilterId the unique identifier of the route filter to detach
     * @return {@code true} if the detach request was accepted
     */
    Boolean detachRouteFilter(String connectionId, String routeFilterId);
}
