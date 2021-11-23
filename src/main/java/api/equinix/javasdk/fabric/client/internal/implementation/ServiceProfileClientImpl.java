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
import api.equinix.javasdk.fabric.client.internal.ServiceProfileClient;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.json.ServiceProfileJson;
import api.equinix.javasdk.fabric.model.wrappers.ServiceProfileWrapper;

import java.util.Map;

/**
 * <p>ServiceProfilesClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceProfileClientImpl extends PageableBase implements ServiceProfileClient<ServiceProfile> {

    /**
     * <p>Constructor for ServiceProfilesClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl} object.
     */
    public ServiceProfileClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Services");
    }

    /**
     * <p>listServiceProfiles.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    public Page<ServiceProfile, ServiceProfileJson> list() {
        EquinixRequest<ServiceProfile> equinixRequest = this.buildRequest("ListServiceProfiles", RequestType.PAGINATED, ServiceProfileJson.pagedTypeRef());
        EquinixResponse<ServiceProfile> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public ServiceProfileJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<ServiceProfileJson> equinixRequest = this.buildRequestWithPathParams("GetServiceProfile", RequestType.SINGLE, pParams, ServiceProfileJson.singleTypeRef());
        EquinixResponse<ServiceProfileJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public ServiceProfileJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public PaginatedList<ServiceProfile> nextPage(PaginatedRequest<ServiceProfile> equinixRequest) {
        EquinixResponse<ServiceProfile> equinixResponse = this.invoke(equinixRequest);
        Page<ServiceProfile, ServiceProfileJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<ServiceProfile> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, ServiceProfileWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
