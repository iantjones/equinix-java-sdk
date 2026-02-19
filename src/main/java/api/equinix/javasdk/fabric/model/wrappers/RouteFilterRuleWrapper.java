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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.RouteFilterRuleClientImpl;
import api.equinix.javasdk.fabric.model.RouteFilterRule;
import api.equinix.javasdk.fabric.model.json.RouteFilterRuleJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class RouteFilterRuleWrapper extends ResourceImpl<RouteFilterRule> implements RouteFilterRule {

    @Delegate(excludes = RouteFilterRuleMutability.class)
    private RouteFilterRuleJson jsonObject;
    @Getter
    private final Pageable<RouteFilterRule> serviceClient;

    public RouteFilterRuleWrapper(RouteFilterRuleJson routeFilterRuleJson, Pageable<RouteFilterRule> serviceClient) {
        this.jsonObject = routeFilterRuleJson;
        this.serviceClient = serviceClient;
    }

    public Boolean delete(String routeFilterId) {
        this.jsonObject = ((RouteFilterRuleClientImpl)this.serviceClient).delete(routeFilterId, this.getUuid());
        return true;
    }

    public void refresh(String routeFilterId) {
        this.jsonObject = ((RouteFilterRuleClientImpl)this.serviceClient).refresh(routeFilterId, this.getUuid());
    }

    private interface RouteFilterRuleMutability {
        Boolean delete(String routeFilterId);
        void refresh(String routeFilterId);
    }
}
