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
import api.equinix.javasdk.fabric.model.RouteFilterRule;
import api.equinix.javasdk.fabric.model.json.creators.RouteFilterRuleOperator;

/**
 * Client interface for managing rules within an Equinix Fabric route filter. Each rule defines
 * a prefix match and action (permit/deny) that controls route acceptance.
 */
public interface RouteFilterRules {

    /**
     * Lists all rules belonging to the specified route filter.
     *
     * @param routeFilterId the unique identifier of the parent route filter
     * @return a paginated list of route filter rules
     */
    PaginatedList<RouteFilterRule> list(String routeFilterId);

    /**
     * Retrieves a single route filter rule by its unique identifier.
     *
     * @param routeFilterId the unique identifier of the parent route filter
     * @param uuid the unique identifier of the route filter rule
     * @return the route filter rule matching the given UUID
     */
    RouteFilterRule getByUuid(String routeFilterId, String uuid);

    /**
     * Begins the fluent builder for creating a new route filter rule.
     * Call methods on the returned builder to configure the rule, then call {@code create()}.
     *
     * @param routeFilterId the unique identifier of the parent route filter
     * @return a builder for configuring the new route filter rule
     */
    RouteFilterRuleOperator.RouteFilterRuleBuilder define(String routeFilterId);
}
