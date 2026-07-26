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

package com.eqixiac.equinix.fabric.model;
import com.eqixiac.equinix.fabric.enums.RouteAggregationRuleType;

import com.eqixiac.equinix.fabric.enums.RouteAggregationRuleState;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationRuleOperator;

public interface RouteAggregationRule {

    String getUuid();

    String getHref();

    String getName();

    RouteAggregationRuleType getType();

    RouteAggregationRuleState getState();

    String getPrefix();

    String getDescription();

    ChangeLog getChangeLog();

    Change getChange();

    /**
     * Begins a fluent update of this route aggregation rule, e.g.
     * {@code rule.update(routeAggregationId).name("New-Name").save()}.
     *
     * @param routeAggregationId the unique identifier of the parent route aggregation
     * @return a {@link com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationRuleOperator.RouteAggregationRuleUpdater}
     */
    RouteAggregationRuleOperator.RouteAggregationRuleUpdater update(String routeAggregationId);

    Boolean delete(String routeAggregationId);

    void refresh(String routeAggregationId);
}
