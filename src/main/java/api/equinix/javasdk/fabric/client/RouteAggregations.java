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

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.RouteAggregation;
import api.equinix.javasdk.fabric.model.json.creators.RouteAggregationOperator;

/**
 * Client interface for managing Equinix Fabric route aggregations. Route aggregations
 * combine multiple specific routes into summarized prefixes to reduce routing table size.
 */
public interface RouteAggregations {

    /**
     * Lists all route aggregations accessible to the current account.
     *
     * @return a paginated list of route aggregations
     */
    PaginatedList<RouteAggregation> list();

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
}
