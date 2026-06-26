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
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.ServiceTokenClient;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.json.SerializationFilters;
import api.equinix.javasdk.fabric.model.json.ServiceTokenJson;
import api.equinix.javasdk.fabric.model.json.creators.ServiceTokenCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.ServiceTokenWrapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * Internal client for Fabric Service Tokens. Standard plumbing/paging come from
 * {@link ResourceClientBase}; {@code create}/{@code dryRunCreate} remain bespoke because they apply
 * the {@code createServiceTokenFilter} Jackson serialization filter.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceTokenClientImpl extends ResourceClientBase<ServiceToken, ServiceTokenJson> implements ServiceTokenClient<ServiceToken> {

    public ServiceTokenClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "ServiceTokens", ServiceTokenJson.class);
    }

    @Override
    protected ServiceToken wrap(ServiceTokenJson json) {
        return new ServiceTokenWrapper(json, this);
    }

    public Page<ServiceToken, ServiceTokenJson> list() {
        return listPage("GetServiceTokens");
    }

    public ServiceTokenJson getByUuid(String uuid) {
        return getOne("GetServiceToken", uuid);
    }

    public ServiceTokenJson create(ServiceTokenCreatorJson serviceTokenCreatorJson) {
        EquinixRequest<ServiceTokenJson> equinixRequest = this.buildRequest("PostServiceToken", RequestType.SINGLE, ServiceTokenJson.class);
        equinixRequest.setFilters(new SimpleFilterProvider().addFilter("createServiceTokenFilter", SerializationFilters.createServiceTokenFilter));
        Utils.serializeJson(equinixRequest, serviceTokenCreatorJson);
        EquinixResponse<ServiceTokenJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ServiceTokenJson dryRunCreate(ServiceTokenCreatorJson serviceTokenCreatorJson) {
        EquinixRequest<ServiceTokenJson> equinixRequest = this.buildRequest("PostServiceToken", RequestType.SINGLE, ServiceTokenJson.class);
        equinixRequest.addSingleQueryParameter("dryRun", "true");
        equinixRequest.setFilters(new SimpleFilterProvider().addFilter("createServiceTokenFilter", SerializationFilters.createServiceTokenFilter));
        Utils.serializeJson(equinixRequest, serviceTokenCreatorJson);
        EquinixResponse<ServiceTokenJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ServiceTokenJson delete(String uuid) {
        return deleteOne("DeleteServiceToken", uuid);
    }

    public ServiceTokenJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
