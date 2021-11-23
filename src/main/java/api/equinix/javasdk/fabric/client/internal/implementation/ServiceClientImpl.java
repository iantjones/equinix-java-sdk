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
import api.equinix.javasdk.fabric.client.internal.ServiceClient;
import api.equinix.javasdk.fabric.model.Service;
import api.equinix.javasdk.fabric.model.json.ServiceJson;
import api.equinix.javasdk.fabric.model.wrappers.ServiceWrapper;

import java.util.Map;

/**
 * <p>ServicesClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceClientImpl extends PageableBase implements ServiceClient<Service> {

    /**
     * <p>Constructor for ServicesClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl} object.
     */
    public ServiceClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Services");
    }

    /**
     * <p>listServices.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    public Page<Service, ServiceJson> list() {
        EquinixRequest<Service> equinixRequest = this.buildRequest("ListServices", RequestType.PAGINATED, ServiceJson.pagedTypeRef());
        EquinixResponse<Service> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public ServiceJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<ServiceJson> equinixRequest = this.buildRequestWithPathParams("GetService", RequestType.SINGLE, pParams, ServiceJson.singleTypeRef());
        EquinixResponse<ServiceJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public ServiceJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public PaginatedList<Service> nextPage(PaginatedRequest<Service> equinixRequest) {
        EquinixResponse<Service> equinixResponse = this.invoke(equinixRequest);
        Page<Service, ServiceJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Service> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, ServiceWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
