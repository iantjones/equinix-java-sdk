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
import com.eqixiac.equinix.fabric.client.internal.implementation.RouteFilterClientImpl;
import com.eqixiac.equinix.fabric.model.RouteFilter;
import com.eqixiac.equinix.fabric.model.json.RouteFilterJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteFilterOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

public class RouteFilterWrapper extends ResourceImpl<RouteFilter> implements RouteFilter {

    @Delegate(excludes = RouteFilterMutability.class)
    private RouteFilterJson jsonObject;
    @Getter
    private final Pageable<RouteFilter> serviceClient;

    public RouteFilterWrapper(RouteFilterJson routeFilterJson, Pageable<RouteFilter> serviceClient) {
        this.jsonObject = routeFilterJson;
        this.serviceClient = serviceClient;
    }

    public RouteFilterOperator.RouteFilterUpdater update() {
        return new RouteFilterOperator((RouteFilterClientImpl) this.serviceClient).update(this.getUuid());
    }

    public Boolean delete() {
        this.jsonObject = ((RouteFilterClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((RouteFilterClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface RouteFilterMutability {
        RouteFilterOperator.RouteFilterUpdater update();
        Boolean delete();
        void refresh();
    }
}
