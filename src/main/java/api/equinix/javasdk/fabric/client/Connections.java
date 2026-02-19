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
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.ConnectionStatistic;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;

import java.time.LocalDateTime;

/**
 * Client interface for managing Equinix Fabric connections. Provides operations for searching,
 * retrieving, creating, and monitoring connections between Fabric endpoints.
 *
 * @author ianjones
 * @version $Id: $Id
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
     * Begins a batch builder for creating multiple connections in a single request.
     *
     * @return a batch builder for configuring multiple connections
     */
    ConnectionOperator.BatchConnectionBuilder startBatch();

    /**
     * Retrieves bandwidth statistics for a connection over the specified time range, viewed from a specific side.
     *
     * @param uuid the unique identifier of the connection
     * @param startDateTime the start of the statistics time range
     * @param endDateTime the end of the statistics time range
     * @param viewPoint the side (A-side or Z-side) from which to view the statistics
     * @return the connection statistics for the specified time range and viewpoint
     */
    ConnectionStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint);

    /**
     * Retrieves bandwidth statistics for a connection over the specified time range.
     *
     * @param uuid the unique identifier of the connection
     * @param startDateTime the start of the statistics time range
     * @param endDateTime the end of the statistics time range
     * @return the connection statistics for the specified time range
     */
    ConnectionStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime);
}
