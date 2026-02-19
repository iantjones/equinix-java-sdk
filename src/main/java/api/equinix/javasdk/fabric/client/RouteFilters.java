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
import api.equinix.javasdk.fabric.model.RouteFilter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.RouteFilterOperator;

/**
 * Client interface for managing Equinix Fabric route filters. Route filters control which
 * routes are accepted or rejected on a connection's routing protocol.
 */
public interface RouteFilters {

    /**
     * Searches for route filters using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching route filters
     */
    PaginatedFilteredList<RouteFilter> search();

    /**
     * Searches for route filters matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching route filters
     */
    PaginatedFilteredList<RouteFilter> search(FilterPropertyList filter);

    /**
     * Searches for route filters with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching route filters
     */
    PaginatedFilteredList<RouteFilter> search(SortPropertyList sort);

    /**
     * Searches for route filters matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching route filters
     */
    PaginatedFilteredList<RouteFilter> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single route filter by its unique identifier.
     *
     * @param uuid the unique identifier of the route filter
     * @return the route filter matching the given UUID
     */
    RouteFilter getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new route filter.
     * Call methods on the returned builder to configure the filter, then call {@code create()}.
     *
     * @return a builder for configuring the new route filter
     */
    RouteFilterOperator.RouteFilterBuilder define();
}
