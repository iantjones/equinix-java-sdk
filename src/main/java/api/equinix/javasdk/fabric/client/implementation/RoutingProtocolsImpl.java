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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.RoutingProtocols;
import api.equinix.javasdk.fabric.client.internal.RoutingProtocolClient;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.json.RoutingProtocolJson;
import api.equinix.javasdk.fabric.model.json.creators.RoutingProtocolOperator;
import api.equinix.javasdk.fabric.model.wrappers.RoutingProtocolWrapper;

public class RoutingProtocolsImpl implements RoutingProtocols {

    private final Fabric serviceManager;

    private final RoutingProtocolClient<RoutingProtocol> serviceClient;

    public RoutingProtocolsImpl(RoutingProtocolClient<RoutingProtocol> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<RoutingProtocol> list(String connectionId) {
        Page<RoutingProtocol, RoutingProtocolJson> responsePage = this.serviceClient.list(connectionId);
        PaginatedList<RoutingProtocol> routingProtocolList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, RoutingProtocolWrapper::new);
        return new PaginatedList<>(routingProtocolList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RoutingProtocol getByUuid(String connectionId, String uuid) {
        RoutingProtocolJson routingProtocolJson = this.serviceClient.getByUuid(connectionId, uuid);
        return new RoutingProtocolWrapper(routingProtocolJson, this.serviceClient);
    }

    public RoutingProtocolOperator.RoutingProtocolBuilder define() {
        return new RoutingProtocolOperator(this.serviceClient).create();
    }
}
