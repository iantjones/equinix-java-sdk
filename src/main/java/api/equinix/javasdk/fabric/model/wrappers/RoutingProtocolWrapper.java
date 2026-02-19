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
import api.equinix.javasdk.fabric.client.internal.implementation.RoutingProtocolClientImpl;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.json.RoutingProtocolJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class RoutingProtocolWrapper extends ResourceImpl<RoutingProtocol> implements RoutingProtocol {

    @Delegate(excludes = RoutingProtocolMutability.class)
    private RoutingProtocolJson jsonObject;
    @Getter
    private final Pageable<RoutingProtocol> serviceClient;

    public RoutingProtocolWrapper(RoutingProtocolJson routingProtocolJson, Pageable<RoutingProtocol> serviceClient) {
        this.jsonObject = routingProtocolJson;
        this.serviceClient = serviceClient;
    }

    public Boolean delete(String connectionId) {
        this.jsonObject = ((RoutingProtocolClientImpl)this.serviceClient).delete(connectionId, this.getUuid());
        return true;
    }

    public void refresh(String connectionId) {
        this.jsonObject = ((RoutingProtocolClientImpl)this.serviceClient).refresh(connectionId, this.getUuid());
    }

    private interface RoutingProtocolMutability {
        Boolean delete(String connectionId);
        void refresh(String connectionId);
    }
}
