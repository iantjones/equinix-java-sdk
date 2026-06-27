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
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.RoutingProtocolClient;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.json.RoutingProtocolJson;
import api.equinix.javasdk.fabric.model.json.creators.RoutingProtocolCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.RoutingProtocolWrapper;

import java.util.Map;

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
}
