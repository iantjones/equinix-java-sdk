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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.fabric.client.ServiceTokens;
import api.equinix.javasdk.fabric.client.internal.ServiceTokenClient;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.json.ServiceTokenJson;
import api.equinix.javasdk.fabric.model.json.creators.ServiceTokenOperator;
import api.equinix.javasdk.fabric.model.wrappers.ServiceTokenWrapper;

/**
 * <p>ServiceTokensImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceTokensImpl implements ServiceTokens {

    private final Fabric serviceManager;

    private final ServiceTokenClient<ServiceToken> serviceClient;

    /**
     * <p>Constructor for ServiceTokensImpl.</p>
     *
     * @param serviceClient a {@link ServiceTokenClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.Fabric} object.
     */
    public ServiceTokensImpl(ServiceTokenClient<ServiceToken> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<ServiceToken> list() {
        Page<ServiceToken, ServiceTokenJson> responsePage = this.serviceClient.list();
        PaginatedList<ServiceToken> serviceTokenList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, ServiceTokenWrapper::new);
        return new PaginatedList<>(serviceTokenList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /** {@inheritDoc} */
    public ServiceToken getByUuid(String uuid) {
        ServiceTokenJson serviceTokenJson = this.serviceClient.getByUuid(uuid);
        return new ServiceTokenWrapper(serviceTokenJson, this.serviceClient);
    }

    /** {@inheritDoc} */
    public ServiceTokenOperator.ServiceTokenBuilder define(Side issuerSide) {
        return new ServiceTokenOperator(this.serviceClient).create(issuerSide);
    }
}
