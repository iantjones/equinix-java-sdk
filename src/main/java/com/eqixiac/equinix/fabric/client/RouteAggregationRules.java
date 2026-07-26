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
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.model.RouteAggregationRule;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationRuleCreatorJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationRuleOperator;

import java.util.List;

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

    /**
     * Replaces an existing route aggregation rule's configuration in full.
     *
     * @param routeAggregationId the unique identifier of the parent route aggregation
     * @param uuid the unique identifier of the route aggregation rule to replace
     * @param routeAggregationRuleCreatorJson the new rule configuration
     * @return the replaced route aggregation rule
     */
    RouteAggregationRule replace(String routeAggregationId, String uuid, RouteAggregationRuleCreatorJson routeAggregationRuleCreatorJson);

    /**
     * Creates multiple route aggregation rules under a route aggregation in a single request.
     *
     * @param routeAggregationId the unique identifier of the parent route aggregation
     * @param routeAggregationRuleCreatorJsonList the rule configurations to create
     * @return the list of created route aggregation rules
     */
    List<RouteAggregationRule> createBulk(String routeAggregationId, List<RouteAggregationRuleCreatorJson> routeAggregationRuleCreatorJsonList);

    /**
     * Searches the rules belonging to a route aggregation using the given filter and sort criteria.
     *
     * @param routeAggregationId the unique identifier of the parent route aggregation
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching route aggregation rules
     */
    PaginatedFilteredList<RouteAggregationRule> search(String routeAggregationId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves the change history for a route aggregation rule.
     *
     * @param routeAggregationId the unique identifier of the parent route aggregation
     * @param uuid the unique identifier of the route aggregation rule
     * @return the list of changes applied to the route aggregation rule
     */
    List<Change> getChanges(String routeAggregationId, String uuid);

    /**
     * Retrieves a single change applied to a route aggregation rule by its change identifier.
     *
     * @param routeAggregationId the unique identifier of the parent route aggregation
     * @param uuid the unique identifier of the route aggregation rule
     * @param changeId the unique identifier of the change
     * @return the matching change
     */
    Change getChange(String routeAggregationId, String uuid, String changeId);
}
