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
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.VPNClient;
import api.equinix.javasdk.networkedge.model.VPN;
import api.equinix.javasdk.networkedge.model.json.VPNJson;
import api.equinix.javasdk.networkedge.model.json.creators.VPNCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.VPNUpdaterJson;
import api.equinix.javasdk.networkedge.model.wrappers.VPNWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>VPNClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class VPNClientImpl extends PageableBase implements VPNClient<VPN> {

    /**
     * <p>Constructor for VPNClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public VPNClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "VPNs");
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public Page<VPN, VPNJson> list(RequestBuilder.VPN requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        EquinixRequest<VPN> equinixRequest = this.buildRequest("ListVPNs", RequestType.PAGINATED, null, qParams, VPNJson.getPagedTypeRef());
        EquinixResponse<VPN> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public VPNJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<VPNJson> equinixRequest = this.buildRequestWithPathParams("GetVPN", RequestType.SINGLE, pParams, VPNJson.getSingleTypeRef());
        EquinixResponse<VPNJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public VPNJson create(VPNCreatorJson vpnCreatorJson) {
        EquinixRequest<VPNJson> equinixRequest = this.buildRequest("CreateVPN", RequestType.SINGLE, VPNJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, vpnCreatorJson);
        EquinixResponse<VPNJson> equinixResponse = this.invoke(equinixRequest);
        String aclTemplateUuid = Utils.extractFromHeader(equinixResponse, "Location", Constants.UUID_PATTERN);
        return getByUuid(aclTemplateUuid);
    }

    /** {@inheritDoc} */
    public VPNJson update(String uuid, VPNUpdaterJson vpnUpdaterJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<VPNJson> equinixRequest = this.buildRequestWithPathParams("UpdateVPN", RequestType.SINGLE, pParams, VPNJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, vpnUpdaterJson);
        EquinixResponse<VPNJson> equinixResponse = this.invoke(equinixRequest);
        return getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public Boolean delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<VPN> equinixRequest = this.buildRequestWithPathParams("DeleteVPN", RequestType.SINGLE, pParams, VPNJson.getSingleTypeRef());
        EquinixResponse<VPN> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public VPNJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    /** {@inheritDoc} */
    @Override
    public PaginatedList<VPN> nextPage(PaginatedRequest<VPN> equinixRequest) {
        EquinixResponse<VPN> equinixResponse = this.invoke(equinixRequest);
        Page<VPN, VPNJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<VPN> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, VPNWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
