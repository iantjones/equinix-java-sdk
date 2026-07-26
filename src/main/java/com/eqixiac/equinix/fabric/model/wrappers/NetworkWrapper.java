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
import com.eqixiac.equinix.fabric.client.internal.implementation.NetworkClientImpl;
import com.eqixiac.equinix.fabric.model.Network;
import com.eqixiac.equinix.fabric.model.json.NetworkJson;
import com.eqixiac.equinix.fabric.model.json.creators.NetworkOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

public class NetworkWrapper extends ResourceImpl<Network> implements Network {

    @Delegate(excludes = NetworkMutability.class)
    private NetworkJson jsonObject;
    @Getter
    private final Pageable<Network> serviceClient;

    public NetworkWrapper(NetworkJson networkJson, Pageable<Network> serviceClient) {
        this.jsonObject = networkJson;
        this.serviceClient = serviceClient;
    }

    public NetworkOperator.NetworkUpdater update() {
        return new NetworkOperator((NetworkClientImpl) this.serviceClient).update(this.getUuid());
    }

    public Boolean delete() {
        this.jsonObject = ((NetworkClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((NetworkClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface NetworkMutability {
        NetworkOperator.NetworkUpdater update();
        Boolean delete();
        void refresh();
    }
}
