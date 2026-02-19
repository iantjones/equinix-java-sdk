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
import api.equinix.javasdk.fabric.client.internal.implementation.NetworkClientImpl;
import api.equinix.javasdk.fabric.model.Network;
import api.equinix.javasdk.fabric.model.json.NetworkJson;
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

    public Boolean delete() {
        this.jsonObject = ((NetworkClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((NetworkClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface NetworkMutability {
        Boolean delete();
        void refresh();
    }
}
