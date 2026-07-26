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
import com.eqixiac.equinix.fabric.client.internal.implementation.RouteAggregationClientImpl;
import com.eqixiac.equinix.fabric.model.RouteAggregation;
import com.eqixiac.equinix.fabric.model.json.RouteAggregationJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

public class RouteAggregationWrapper extends ResourceImpl<RouteAggregation> implements RouteAggregation {

    @Delegate(excludes = RouteAggregationMutability.class)
    private RouteAggregationJson jsonObject;
    @Getter
    private final Pageable<RouteAggregation> serviceClient;

    public RouteAggregationWrapper(RouteAggregationJson routeAggregationJson, Pageable<RouteAggregation> serviceClient) {
        this.jsonObject = routeAggregationJson;
        this.serviceClient = serviceClient;
    }

    public RouteAggregationOperator.RouteAggregationUpdater update() {
        return new RouteAggregationOperator((RouteAggregationClientImpl) this.serviceClient).update(this.getUuid());
    }

    public Boolean delete() {
        this.jsonObject = ((RouteAggregationClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((RouteAggregationClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface RouteAggregationMutability {
        RouteAggregationOperator.RouteAggregationUpdater update();
        Boolean delete();
        void refresh();
    }
}
