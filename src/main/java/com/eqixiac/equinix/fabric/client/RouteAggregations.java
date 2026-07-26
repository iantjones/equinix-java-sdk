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
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.RouteAggregation;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationOperator;

import java.util.List;

/**
 * Client interface for managing Equinix Fabric route aggregations. Route aggregations
 * combine multiple specific routes into summarized prefixes to reduce routing table size.
 */
public interface RouteAggregations {

    /**
     * Searches all route aggregations accessible to the current account
     * (Fabric exposes route aggregations via {@code POST /routeAggregations/search}, not a GET list).
     *
     * @return a paginated, filtered list of route aggregations
     */
    PaginatedFilteredList<RouteAggregation> search();

    /**
     * Searches route aggregations matching the supplied filter criteria.
     *
     * @param filter the filter criteria
     * @return a paginated, filtered list of matching route aggregations
     */
    PaginatedFilteredList<RouteAggregation> search(FilterPropertyList filter);

    /**
     * Searches all route aggregations, applying the supplied sort order.
     *
     * @param sort the sort criteria
     * @return a paginated, filtered list of route aggregations
     */
    PaginatedFilteredList<RouteAggregation> search(SortPropertyList sort);

    /**
     * Searches route aggregations matching the supplied filter criteria, applying the supplied sort order.
     *
     * @param filter the filter criteria
     * @param sort the sort criteria
     * @return a paginated, filtered list of matching route aggregations
     */
    PaginatedFilteredList<RouteAggregation> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single route aggregation by its unique identifier.
     *
     * @param uuid the unique identifier of the route aggregation
     * @return the route aggregation matching the given UUID
     */
    RouteAggregation getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new route aggregation.
     * Call methods on the returned builder to configure the aggregation, then call {@code create()}.
     *
     * @return a builder for configuring the new route aggregation
     */
    RouteAggregationOperator.RouteAggregationBuilder define();

    /**
     * Retrieves the change history for a route aggregation.
     *
     * @param uuid the unique identifier of the route aggregation
     * @return the list of changes applied to the route aggregation
     */
    List<Change> getChanges(String uuid);

    /**
     * Retrieves a single change applied to a route aggregation by its change identifier.
     *
     * @param uuid the unique identifier of the route aggregation
     * @param changeId the unique identifier of the change
     * @return the matching change
     */
    Change getChange(String uuid, String changeId);

    /**
     * Retrieves the connections currently using a route aggregation.
     *
     * @param uuid the unique identifier of the route aggregation
     * @return the list of connections attached to the route aggregation
     */
    List<Connection> getConnections(String uuid);
}
