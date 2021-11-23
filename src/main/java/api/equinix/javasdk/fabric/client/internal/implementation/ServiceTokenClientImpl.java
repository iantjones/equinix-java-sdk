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
import api.equinix.javasdk.fabric.client.internal.ServiceTokenClient;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.json.SerializationFilters;
import api.equinix.javasdk.fabric.model.json.ServiceTokenJson;
import api.equinix.javasdk.fabric.model.json.creators.ServiceTokenCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.ServiceTokenWrapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import java.util.Map;

/**
 * <p>ServiceTokensClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceTokenClientImpl extends PageableBase implements ServiceTokenClient<ServiceToken> {

    /**
     * <p>Constructor for ServiceTokensClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl} object.
     */
    public ServiceTokenClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "ServiceTokens");
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    public Page<ServiceToken, ServiceTokenJson> list() {
        EquinixRequest<ServiceToken> equinixRequest = this.buildRequest("GetServiceTokens", RequestType.PAGINATED, ServiceTokenJson.getPagedTypeRef());
        EquinixResponse<ServiceToken> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public ServiceTokenJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<ServiceTokenJson> equinixRequest = this.buildRequestWithPathParams("GetServiceToken", RequestType.SINGLE, pParams, ServiceTokenJson.getSingleTypeRef());
        EquinixResponse<ServiceTokenJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public ServiceTokenJson create(ServiceTokenCreatorJson serviceTokenCreatorJson) {
        EquinixRequest<ServiceTokenJson> equinixRequest = this.buildRequest("PostServiceToken", RequestType.SINGLE, ServiceTokenJson.getSingleTypeRef());
        equinixRequest.setFilters(new SimpleFilterProvider().addFilter("createServiceTokenFilter", SerializationFilters.createServiceTokenFilter));
        Utils.serializeJson(equinixRequest, serviceTokenCreatorJson);
        EquinixResponse<ServiceTokenJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public ServiceTokenJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<ServiceToken> equinixRequest = this.buildRequestWithPathParams("DeleteServiceToken", RequestType.SINGLE, pParams, ServiceTokenJson.getSingleTypeRef());
        EquinixResponse<ServiceToken> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public ServiceTokenJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public PaginatedList<ServiceToken> nextPage(PaginatedRequest<ServiceToken> equinixRequest) {
        EquinixResponse<ServiceToken> equinixResponse = this.invoke(equinixRequest);
        Page<ServiceToken, ServiceTokenJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<ServiceToken> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, ServiceTokenWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
