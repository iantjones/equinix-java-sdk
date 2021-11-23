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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.PortClient;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.json.PortJson;
import api.equinix.javasdk.fabric.model.wrappers.PortWrapper;

import java.util.Map;

/**
 * <p>PortsClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PortClientImpl extends PageableBase implements PortClient<Port> {

    /**
     * <p>Constructor for PortsClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl} object.
     */
    public PortClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Ports");
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    public Page<Port, PortJson> list() {
        EquinixRequest<Port> equinixRequest = this.buildRequest("GetPorts", RequestType.PAGINATED, PortJson.pagedTypeRef());
        EquinixResponse<Port> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public PortJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<Port> equinixRequest = this.buildRequestWithPathParams("GetPort", RequestType.SINGLE, pParams, PortJson.singleTypeRef());
        EquinixResponse<Port> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public PortJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public PaginatedList<Port> nextPage(PaginatedRequest<Port> equinixRequest) {
        EquinixResponse<Port> equinixResponse = this.invoke(equinixRequest);
        Page<Port, PortJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Port> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, PortWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
