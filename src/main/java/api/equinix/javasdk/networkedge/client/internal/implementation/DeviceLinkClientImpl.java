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
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.DeviceLinkClient;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.implementation.UUIDResult;
import api.equinix.javasdk.networkedge.model.json.DeviceLinkJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkUpdaterJson;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceLinkWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>DeviceLinkClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceLinkClientImpl extends PageableBase implements DeviceLinkClient<DeviceLink> {

    /**
     * <p>Constructor for DeviceLinkClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public DeviceLinkClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "DeviceLinks");
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public Page<DeviceLink, DeviceLinkJson> list(RequestBuilder.DeviceLink requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        EquinixRequest<DeviceLink> equinixRequest = this.buildRequest("ListDeviceLinks", RequestType.PAGINATED, null, qParams, DeviceLinkJson.getPagedTypeRef());
        EquinixResponse<DeviceLink> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public DeviceLinkJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<DeviceLinkJson> equinixRequest = this.buildRequestWithPathParams("GetDeviceLink", RequestType.SINGLE, pParams, DeviceLinkJson.getSingleTypeRef());
        EquinixResponse<DeviceLinkJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public DeviceLinkJson create(DeviceLinkCreatorJson deviceLinkCreatorJson) {
        EquinixRequest<DeviceLinkJson> equinixRequest = this.buildRequest("CreateDeviceLink", RequestType.SINGLE, DeviceLinkJson.getCreateTypeRef());
        Utils.serializeJson(equinixRequest, deviceLinkCreatorJson);
        EquinixResponse<DeviceLinkJson> equinixResponse = this.invoke(equinixRequest);
        UUIDResult uuidResult = Utils.handleSingletonResponse(equinixResponse, equinixRequest);
        return getByUuid(uuidResult.getUuid());
    }

    /**
     * <p>update.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param deviceLinkUpdaterJson a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkUpdaterJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.DeviceLinkJson} object.
     */
    public DeviceLinkJson update(String uuid, DeviceLinkUpdaterJson deviceLinkUpdaterJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<DeviceLinkJson> equinixRequest = this.buildRequestWithPathParams("UpdateDeviceLink", RequestType.SINGLE, pParams, DeviceLinkJson.getCreateTypeRef());
        Utils.serializeJson(equinixRequest, deviceLinkUpdaterJson);
        EquinixResponse<DeviceLinkJson> equinixResponse = this.invoke(equinixRequest);
        return getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public Boolean delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<DeviceLink> equinixRequest = this.buildRequestWithPathParams("DeleteDeviceLink", RequestType.SINGLE, pParams, DeviceLinkJson.getSingleTypeRef());
        EquinixResponse<DeviceLink> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public DeviceLinkJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    /** {@inheritDoc} */
    @Override
    public PaginatedList<DeviceLink> nextPage(PaginatedRequest<DeviceLink> equinixRequest) {
        EquinixResponse<DeviceLink> equinixResponse = this.invoke(equinixRequest);
        Page<DeviceLink, DeviceLinkJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<DeviceLink> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, DeviceLinkWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
