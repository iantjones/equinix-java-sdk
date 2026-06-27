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
import api.equinix.javasdk.fabric.model.RouteAggregationRule;
import api.equinix.javasdk.fabric.model.json.creators.RouteAggregationRuleOperator;

/**
 * Client interface for managing rules within an Equinix Fabric route aggregation. Each rule
 * defines a prefix that participates in the parent route aggregation.
 */
public interface RouteAggregationRules {

    /**
     * Lists all rules belonging to the specified route aggregation.
     *
     * @param routeAggregationId the unique identifier of the parent route aggregation
     * @return a paginated list of route aggregation rules
     */
    PaginatedList<RouteAggregationRule> list(String routeAggregationId);

    /**
     * Retrieves a single route aggregation rule by its unique identifier.
     *
     * @param routeAggregationId the unique identifier of the parent route aggregation
     * @param uuid the unique identifier of the route aggregation rule
     * @return the route aggregation rule matching the given UUID
     */
    RouteAggregationRule getByUuid(String routeAggregationId, String uuid);

    /**
     * Begins the fluent builder for creating a new route aggregation rule.
     * Call methods on the returned builder to configure the rule, then call {@code create()}.
     *
     * @param routeAggregationId the unique identifier of the parent route aggregation
     * @return a builder for configuring the new route aggregation rule
     */
    RouteAggregationRuleOperator.RouteAggregationRuleBuilder define(String routeAggregationId);
}
