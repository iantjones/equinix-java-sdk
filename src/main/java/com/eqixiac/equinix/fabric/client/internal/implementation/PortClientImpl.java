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

package com.eqixiac.equinix.fabric.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.SerializationHelper;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.PortClient;
import com.eqixiac.equinix.fabric.model.Port;
import com.eqixiac.equinix.fabric.model.PortVlan;
import com.eqixiac.equinix.fabric.model.implementation.PhysicalPort;
import com.eqixiac.equinix.fabric.model.implementation.PortCreatorJson;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.PhysicalPortsResponseJson;
import com.eqixiac.equinix.fabric.model.json.PortJson;
import com.eqixiac.equinix.fabric.model.json.PortVlanJson;
import com.eqixiac.equinix.fabric.model.wrappers.PortWrapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Ports. Plumbing/paging provided by {@link ResourceClientBase}.
 *
 * @author ianjones
 */
public class PortClientImpl extends ResourceClientBase<Port, PortJson> implements PortClient<Port> {

    public PortClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Ports", PortJson.class);
    }

    @Override
    protected Port wrap(PortJson json) {
        return new PortWrapper(json, this);
    }

    public Page<PortJson> list() {
        return listPage("GetPorts");
    }

    public Page<PortJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchPorts", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public PortJson getByUuid(String uuid) {
        return getOne("GetPort", uuid);
    }

    public List<PortVlan> getVlans(String portUuid) {
        EquinixRequest<PortVlan> request = buildRequestWithPathParams("GetVlans", RequestType.PAGINATED,
                Map.of("portUuid", portUuid), PortVlanJson.class);
        Page<PortVlanJson> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public PortJson refresh(String uuid) {
        return getByUuid(uuid);
    }

    public PortJson create(PortCreatorJson body) {
        return postOne("CreatePort", body);
    }

    /**
     * {@code POST /fabric/v4/ports?dryRun=true} (spec: "option to verify that API calls will
     * succeed") via the shared {@code ResourceClientBase.dryRunCreate} helper. The 200 body is
     * the validated port order echoed back — no uuid/name/state — versus the real create's 202.
     */
    public PortJson dryRunCreate(PortCreatorJson body) {
        return dryRunCreate("CreatePort", body);
    }

    public PortJson delete(String uuid) {
        return deleteOne("DeletePort", uuid);
    }

    /**
     * {@code DELETE /fabric/v4/ports/{uuid}?dryRun=true} (spec: "option to verify that API calls
     * will succeed"). Nothing is deleted; the 200 body is the existing port entity that WOULD be
     * deleted, deserialized exactly like the real delete's response.
     */
    public PortJson dryRunDelete(String uuid) {
        EquinixRequest<PortJson> request = buildRequestWithPathParams("DeletePort", RequestType.SINGLE,
                Map.of("uuid", uuid), PortJson.class);
        request.addSingleQueryParameter("dryRun", "true");
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    public PortJson update(String uuid, List<PatchOperation> operations) {
        // PATCH /ports/{uuid} with an op/path/value array sent as application/json
        // (not json-patch+json), so updateOne (default content-type) is correct here.
        return updateOne("UpdatePort", uuid, operations);
    }

    /**
     * {@code PATCH /fabric/v4/ports/{uuid}?dryRun=true} (spec: "option to verify that API calls
     * will succeed"). Nothing is changed — and the dry-run 200 schema is {@code AllPortsResponse},
     * a paginated envelope {@code {pagination, data:[Port]}} carrying the simulated updated port
     * in {@code data[0]} (spec example {@code PortUpdateDryRunResponse}). That is a DIFFERENT wire
     * shape from the real update's bare {@code Port}, so this method deserializes
     * {@code Page<PortJson>} and unwraps {@code data[0]} instead of reusing {@code updateOne}.
     */
    public PortJson dryRunUpdate(String uuid, List<PatchOperation> operations) {
        EquinixRequest<Page<PortJson>> request = newRequest("UpdatePort")
                .withType(RequestType.SINGLE)
                .withPathParams(Map.of("uuid", uuid))
                .withTypeRef(new TypeReference<Page<PortJson>>() {})
                .build();
        request.addSingleQueryParameter("dryRun", "true");
        // Same body contract as update(): an op/path/value array sent as application/json.
        SerializationHelper.serializeJson(request, operations);
        Page<PortJson> envelope = ResponseHandler.handleSingletonResponse(invoke(request), request);
        if (envelope == null || envelope.getItems() == null || envelope.getItems().isEmpty()) {
            throw new EquinixClientException("Dry-run port update returned an empty AllPortsResponse"
                    + " envelope (expected the simulated updated port in data[0]).");
        }
        return envelope.getItems().get(0);
    }

    public PhysicalPortsResponseJson addToLag(String portId, List<PhysicalPort> physicalPorts) {
        // POST /ports/{portId}/physicalPorts/bulk with a BulkPhysicalPort body ({"data": [...]}),
        // returning an AllPhysicalPortsResponse.
        EquinixRequest<PhysicalPortsResponseJson> request = buildRequestWithPathParams("AddToLag",
                RequestType.SINGLE, Map.of("portId", portId), PhysicalPortsResponseJson.class);
        SerializationHelper.serializeJson(request, Map.of("data", physicalPorts != null ? physicalPorts : Collections.emptyList()));
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }
}
