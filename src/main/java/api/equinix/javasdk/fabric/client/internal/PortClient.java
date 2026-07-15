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

package api.equinix.javasdk.fabric.client.internal;

import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.PortVlan;
import api.equinix.javasdk.fabric.model.implementation.PhysicalPort;
import api.equinix.javasdk.fabric.model.implementation.PortCreatorJson;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.PhysicalPortsResponseJson;
import api.equinix.javasdk.fabric.model.json.PortJson;

import java.util.List;

/**
 *
 * @author ianjones
 */
public interface PortClient<T> extends PageablePost<T> {

    Page<PortJson> list();

    Page<PortJson> search(FilterPropertyList filter, SortPropertyList sort);

    List<PortVlan> getVlans(String portUuid);

    PortJson getByUuid(String uuid);

    PortJson refresh(String uuid);

    /**
     * Creates a single port ({@code POST /fabric/v4/ports}).
     *
     * @param body the typed create request
     * @return the created port
     */
    PortJson create(PortCreatorJson body);

    /**
     * Validate-only variant of {@link #create(PortCreatorJson)}
     * ({@code POST /fabric/v4/ports?dryRun=true}; spec: "option to verify that API calls will
     * succeed"). No port is ordered — the 200 response is the validated port order echoed back
     * (no uuid/name/state), versus the real create's 202.
     *
     * @param body the typed create request to validate
     * @return the validated port order echoed back by the server
     */
    PortJson dryRunCreate(PortCreatorJson body);

    /**
     * Deletes a port by uuid ({@code DELETE /fabric/v4/ports/{uuid}}).
     *
     * @param uuid the unique identifier of the port
     * @return the deleted port as returned by the server
     */
    PortJson delete(String uuid);

    /**
     * Validate-only variant of {@link #delete(String)}
     * ({@code DELETE /fabric/v4/ports/{uuid}?dryRun=true}; spec: "option to verify that API calls
     * will succeed"). Nothing is deleted — the 200 response is the existing port entity (with
     * uuid/name) that WOULD be deleted, versus the real delete's 202.
     *
     * @param uuid the unique identifier of the port
     * @return the port that would be deleted
     */
    PortJson dryRunDelete(String uuid);

    /**
     * Updates a port by uuid with a JSON Patch operations array
     * ({@code PATCH /fabric/v4/ports/{uuid}}).
     *
     * @param uuid the unique identifier of the port
     * @param operations the op/path/value change operations
     * @return the updated port
     */
    PortJson update(String uuid, List<PatchOperation> operations);

    /**
     * Validate-only variant of {@link #update(String, List)}
     * ({@code PATCH /fabric/v4/ports/{uuid}?dryRun=true}; spec: "option to verify that API calls
     * will succeed"). Nothing is changed — and unlike the real update's bare {@code Port}, the
     * dry-run 200 body is an {@code AllPortsResponse} paginated envelope
     * ({@code {pagination, data:[Port]}}); implementations deserialize that envelope and return
     * its {@code data[0]}, the simulated updated port.
     *
     * @param uuid the unique identifier of the port
     * @param operations the op/path/value change operations to validate
     * @return the simulated updated port unwrapped from the envelope's {@code data[0]}
     */
    PortJson dryRunUpdate(String uuid, List<PatchOperation> operations);

    /**
     * Adds physical ports to a virtual port's LAG
     * ({@code POST /fabric/v4/ports/{portId}/physicalPorts/bulk}).
     *
     * @param portId the virtual port uuid
     * @param physicalPorts the physical ports to add
     * @return the full set of physical ports backing the virtual port after the addition
     */
    PhysicalPortsResponseJson addToLag(String portId, List<PhysicalPort> physicalPorts);
}
