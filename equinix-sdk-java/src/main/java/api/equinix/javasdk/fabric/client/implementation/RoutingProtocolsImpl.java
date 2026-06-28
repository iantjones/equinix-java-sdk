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

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.RoutingProtocols;
import api.equinix.javasdk.fabric.client.internal.RoutingProtocolClient;
import api.equinix.javasdk.fabric.enums.BGPActionType;
import api.equinix.javasdk.fabric.model.BGPAction;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.json.RoutingProtocolJson;
import api.equinix.javasdk.fabric.model.json.creators.RoutingProtocolCreatorJson;
import api.equinix.javasdk.fabric.model.json.creators.RoutingProtocolOperator;
import api.equinix.javasdk.fabric.model.wrappers.RoutingProtocolWrapper;

import java.util.List;
import java.util.stream.Collectors;

public class RoutingProtocolsImpl implements RoutingProtocols {

    private final RoutingProtocolClient<RoutingProtocol> serviceClient;

    public RoutingProtocolsImpl(RoutingProtocolClient<RoutingProtocol> serviceClient) {
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

    public RoutingProtocol replace(String connectionId, String uuid, RoutingProtocolOperator.RoutingProtocolBuilder builder) {
        RoutingProtocolJson routingProtocolJson = this.serviceClient.replace(connectionId, uuid, new RoutingProtocolCreatorJson(builder));
        return new RoutingProtocolWrapper(routingProtocolJson, this.serviceClient);
    }

    public List<RoutingProtocol> createBulk(String connectionId, List<RoutingProtocolOperator.RoutingProtocolBuilder> builders) {
        List<RoutingProtocolCreatorJson> creators = builders.stream()
                .map(RoutingProtocolCreatorJson::new)
                .collect(Collectors.toList());
        return this.serviceClient.createBulk(connectionId, creators);
    }

    public List<BGPAction> getBgpActions(String connectionId, String routingProtocolId) {
        return this.serviceClient.getBgpActions(connectionId, routingProtocolId);
    }

    public BGPAction createBgpAction(String connectionId, String routingProtocolId, BGPActionType type) {
        return this.serviceClient.createBgpAction(connectionId, routingProtocolId, type);
    }

    public BGPAction getBgpAction(String connectionId, String routingProtocolId, String actionId) {
        return this.serviceClient.getBgpAction(connectionId, routingProtocolId, actionId);
    }

    public List<Change> getChanges(String connectionId, String routingProtocolId) {
        return this.serviceClient.getChanges(connectionId, routingProtocolId);
    }

    public Change getChange(String connectionId, String routingProtocolId, String changeId) {
        return this.serviceClient.getChange(connectionId, routingProtocolId, changeId);
    }
}
