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
import com.eqixiac.equinix.fabric.client.internal.implementation.CloudRouterClientImpl;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.json.CloudRouterJson;
import com.eqixiac.equinix.fabric.model.json.creators.CloudRouterOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

public class CloudRouterWrapper extends ResourceImpl<CloudRouter> implements CloudRouter {

    @Delegate(excludes = CloudRouterMutability.class)
    private CloudRouterJson jsonObject;
    @Getter
    private final Pageable<CloudRouter> serviceClient;

    public CloudRouterWrapper(CloudRouterJson cloudRouterJson, Pageable<CloudRouter> serviceClient) {
        this.jsonObject = cloudRouterJson;
        this.serviceClient = serviceClient;
    }

    public CloudRouterOperator.CloudRouterUpdater update() {
        return new CloudRouterOperator((CloudRouterClientImpl) this.serviceClient).update(this.getUuid());
    }

    public Boolean delete() {
        this.jsonObject = ((CloudRouterClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((CloudRouterClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface CloudRouterMutability {
        CloudRouterOperator.CloudRouterUpdater update();
        Boolean delete();
        void refresh();
    }
}
