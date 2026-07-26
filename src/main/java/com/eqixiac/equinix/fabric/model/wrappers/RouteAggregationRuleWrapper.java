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

package com.eqixiac.equinix.fabric.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.RouteAggregationRuleClientImpl;
import com.eqixiac.equinix.fabric.model.RouteAggregationRule;
import com.eqixiac.equinix.fabric.model.json.RouteAggregationRuleJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationRuleOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

public class RouteAggregationRuleWrapper extends ResourceImpl<RouteAggregationRule> implements RouteAggregationRule {

    @Delegate(excludes = RouteAggregationRuleMutability.class)
    private RouteAggregationRuleJson jsonObject;
    @Getter
    private final Pageable<RouteAggregationRule> serviceClient;

    public RouteAggregationRuleWrapper(RouteAggregationRuleJson routeAggregationRuleJson, Pageable<RouteAggregationRule> serviceClient) {
        this.jsonObject = routeAggregationRuleJson;
        this.serviceClient = serviceClient;
    }

    public RouteAggregationRuleOperator.RouteAggregationRuleUpdater update(String routeAggregationId) {
        return new RouteAggregationRuleOperator((RouteAggregationRuleClientImpl) this.serviceClient, routeAggregationId).update(this.getUuid());
    }

    public Boolean delete(String routeAggregationId) {
        this.jsonObject = ((RouteAggregationRuleClientImpl)this.serviceClient).delete(routeAggregationId, this.getUuid());
        return true;
    }

    public void refresh(String routeAggregationId) {
        this.jsonObject = ((RouteAggregationRuleClientImpl)this.serviceClient).refresh(routeAggregationId, this.getUuid());
    }

    private interface RouteAggregationRuleMutability {
        RouteAggregationRuleOperator.RouteAggregationRuleUpdater update(String routeAggregationId);
        Boolean delete(String routeAggregationId);
        void refresh(String routeAggregationId);
    }
}
