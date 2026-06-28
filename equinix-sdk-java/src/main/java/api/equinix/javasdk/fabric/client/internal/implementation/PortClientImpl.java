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
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.PortClient;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.PortVlan;
import api.equinix.javasdk.fabric.model.implementation.PhysicalPort;
import api.equinix.javasdk.fabric.model.implementation.PortCreatorJson;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.PhysicalPortsResponseJson;
import api.equinix.javasdk.fabric.model.json.PortJson;
import api.equinix.javasdk.fabric.model.json.PortVlanJson;
import api.equinix.javasdk.fabric.model.wrappers.PortWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Ports (read-only). Plumbing/paging provided by {@link ResourceClientBase}.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PortClientImpl extends ResourceClientBase<Port, PortJson> implements PortClient<Port> {

    public PortClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Ports", PortJson.class);
    }

    @Override
    protected Port wrap(PortJson json) {
        return new PortWrapper(json, this);
    }

    public Page<Port, PortJson> list() {
        return listPage("GetPorts");
    }

    public Page<Port, PortJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchPorts", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public PortJson getByUuid(String uuid) {
        return getOne("GetPort", uuid);
    }

    public List<PortVlan> getVlans(String portUuid) {
        EquinixRequest<PortVlan> request = buildRequestWithPathParams("GetVlans", RequestType.PAGINATED,
                Map.of("portUuid", portUuid), PortVlanJson.class);
        Page<PortVlan, PortVlanJson> page = Utils.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public PortJson refresh(String uuid) {
        return getByUuid(uuid);
    }

    public PortJson create(PortCreatorJson body) {
        return postOne("CreatePort", body);
    }

    public PortJson delete(String uuid) {
        return deleteOne("DeletePort", uuid);
    }

    public PortJson update(String uuid, List<PatchOperation> operations) {
        // PATCH /ports/{uuid} with an op/path/value array sent as application/json
        // (not json-patch+json), so updateOne (default content-type) is correct here.
        return updateOne("UpdatePort", uuid, operations);
    }

    public PhysicalPortsResponseJson addToLag(String portId, List<PhysicalPort> physicalPorts) {
        // POST /ports/{portId}/physicalPorts/bulk with a BulkPhysicalPort body ({"data": [...]}),
        // returning an AllPhysicalPortsResponse.
        EquinixRequest<PhysicalPortsResponseJson> request = buildRequestWithPathParams("AddToLag",
                RequestType.SINGLE, Map.of("portId", portId), PhysicalPortsResponseJson.class);
        Utils.serializeJson(request, Map.of("data", physicalPorts != null ? physicalPorts : Collections.emptyList()));
        return Utils.handleSingletonResponse(invoke(request), request);
    }
}
