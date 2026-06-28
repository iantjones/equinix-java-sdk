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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.RoutingProtocolClient;
import api.equinix.javasdk.fabric.enums.BGPActionType;
import api.equinix.javasdk.fabric.model.BGPAction;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.implementation.BGPActionRequest;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.implementation.RoutingProtocolBulkRequest;
import api.equinix.javasdk.fabric.model.json.BGPActionJson;
import api.equinix.javasdk.fabric.model.json.RoutingProtocolJson;
import api.equinix.javasdk.fabric.model.json.creators.RoutingProtocolCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.RoutingProtocolWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoutingProtocolClientImpl extends ResourceClientBase<RoutingProtocol, RoutingProtocolJson> implements RoutingProtocolClient<RoutingProtocol> {

    public RoutingProtocolClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RoutingProtocols", RoutingProtocolJson.class);
    }

    @Override
    protected RoutingProtocol wrap(RoutingProtocolJson json) {
        return new RoutingProtocolWrapper(json, this);
    }

    public Page<RoutingProtocol, RoutingProtocolJson> list(String connectionId) {
        return listPagePath("GetRoutingProtocols", Map.of("connectionId", connectionId));
    }

    public RoutingProtocolJson getByUuid(String connectionId, String uuid) {
        return getOne("GetRoutingProtocol", Map.of("connectionId", connectionId, "uuid", uuid));
    }

    public RoutingProtocolJson create(String connectionId, RoutingProtocolCreatorJson routingProtocolCreatorJson) {
        return postOne("PostRoutingProtocol", Map.of("connectionId", connectionId), routingProtocolCreatorJson);
    }

    public RoutingProtocolJson replace(String connectionId, String uuid, RoutingProtocolCreatorJson routingProtocolCreatorJson) {
        // PUT /connections/{connectionId}/routingProtocols/{uuid} replaces the full routing protocol;
        // the body has the same shape as create (a RoutingProtocolCreatorJson).
        return updateOne("ReplaceRoutingProtocol", Map.of("connectionId", connectionId, "uuid", uuid), routingProtocolCreatorJson);
    }

    public List<RoutingProtocol> createBulk(String connectionId, List<RoutingProtocolCreatorJson> routingProtocolCreatorJsonList) {
        // POST /connections/{connectionId}/routingProtocols/bulk wraps the configs in a {"data":[...]} body
        // and returns a paginated {"data":[...]} collection of the created routing protocols.
        EquinixRequest<RoutingProtocol> request = buildRequestWithPathParams("PostRoutingProtocolsBulk", RequestType.PAGINATED_POST,
                Map.of("connectionId", connectionId), RoutingProtocolJson.getPagedTypeRef());
        Utils.serializeJson(request, new RoutingProtocolBulkRequest(routingProtocolCreatorJsonList));
        Page<RoutingProtocol, RoutingProtocolJson> page = Utils.handlePaginatedListResponse(invoke(request), request);
        if (page == null || page.getItems() == null) {
            return Collections.emptyList();
        }
        return page.getItems().stream().map(this::wrap).collect(Collectors.toUnmodifiableList());
    }

    public RoutingProtocolJson update(String connectionId, String uuid, java.util.List<PatchOperation> operations) {
        // PATCH /connections/{connectionId}/routingProtocols/{uuid} with an op/path/value array
        // sent as application/json (not json-patch+json), so updateOne (default content-type) is correct here.
        return updateOne("UpdateRoutingProtocol", Map.of("connectionId", connectionId, "uuid", uuid), operations);
    }

    public RoutingProtocolJson delete(String connectionId, String uuid) {
        return deleteOne("DeleteRoutingProtocol", Map.of("connectionId", connectionId, "uuid", uuid));
    }

    public RoutingProtocolJson refresh(String connectionId, String uuid) {
        return getByUuid(connectionId, uuid);
    }

    public List<BGPAction> getBgpActions(String connectionId, String routingProtocolId) {
        EquinixRequest<BGPAction> request = buildRequestWithPathParams("GetBGPActions", RequestType.PAGINATED,
                Map.of("connectionId", connectionId, "uuid", routingProtocolId), BGPActionJson.getPagedTypeRef());
        Page<BGPAction, BGPActionJson> page = Utils.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public BGPActionJson createBgpAction(String connectionId, String routingProtocolId, BGPActionType type) {
        return postForType("PostBGPAction", Map.of("connectionId", connectionId, "uuid", routingProtocolId),
                new BGPActionRequest(type), BGPActionJson.getSingleTypeRef());
    }

    public BGPAction getBgpAction(String connectionId, String routingProtocolId, String actionId) {
        return getAs("GetBGPAction",
                Map.of("connectionId", connectionId, "uuid", routingProtocolId, "actionId", actionId),
                null, BGPActionJson.class);
    }

    public List<Change> getChanges(String connectionId, String routingProtocolId) {
        EquinixRequest<Change> request = buildRequestWithPathParams("GetRoutingProtocolChanges", RequestType.PAGINATED,
                Map.of("connectionId", connectionId, "uuid", routingProtocolId), Change.getPagedTypeRef());
        Page<Change, Change> page = Utils.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public Change getChange(String connectionId, String routingProtocolId, String changeId) {
        return getAs("GetRoutingProtocolChange",
                Map.of("connectionId", connectionId, "uuid", routingProtocolId, "changeId", changeId),
                null, Change.class);
    }
}
