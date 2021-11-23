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

package api.equinix.javasdk.networkedge.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.DeviceTypeClient;
import api.equinix.javasdk.networkedge.model.DeviceType;
import api.equinix.javasdk.networkedge.model.json.DeviceTypeJson;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceTypeWrapper;

/**
 * <p>DeviceTypeClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceTypeClientImpl extends PageableBase implements DeviceTypeClient<DeviceType> {

    /**
     * <p>Constructor for DeviceTypeClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public DeviceTypeClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Devices");
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    public Page<DeviceType, DeviceTypeJson> list() {
        EquinixRequest<DeviceType> equinixRequest = this.buildRequest("ListDeviceTypes", RequestType.PAGINATED, DeviceTypeJson.pagedTypeRef());
        EquinixResponse<DeviceType> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    @Override
    public PaginatedList<DeviceType> nextPage(PaginatedRequest<DeviceType> equinixRequest) {
        EquinixResponse<DeviceType> equinixResponse = this.invoke(equinixRequest);
        Page<DeviceType, DeviceTypeJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<DeviceType> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, DeviceTypeWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
