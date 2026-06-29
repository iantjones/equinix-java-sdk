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

package api.equinix.javasdk.networkedge.client.implementation;

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.VPNs;
import api.equinix.javasdk.networkedge.client.internal.VPNClient;
import api.equinix.javasdk.networkedge.model.VPN;
import api.equinix.javasdk.networkedge.model.json.VPNJson;
import api.equinix.javasdk.networkedge.model.json.creators.VPNOperator;
import api.equinix.javasdk.networkedge.model.wrappers.VPNWrapper;
import lombok.Getter;

/**
 * <p>VPNsImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class VPNsImpl implements VPNs {

    private final NetworkEdge serviceManager;

    private final VPNClient<VPN> serviceClient;

    /**
     * <p>Constructor for VPNsImpl.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.networkedge.client.internal.VPNClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.NetworkEdge} object.
     */
    public VPNsImpl(VPNClient<VPN> serviceClient, NetworkEdge serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<VPN> list() {
        return list(null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public PaginatedList<VPN> list(RequestBuilder.VPN requestBuilder) {
        Page<VPN, VPNJson> responsePage = serviceClient.list(requestBuilder);
        PaginatedList<VPN> deviceList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, VPNWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /** {@inheritDoc} */
    public VPN getByUuid(String uuid) {
        VPNJson deviceLinkJson = serviceClient.getByUuid(uuid);
        return new VPNWrapper(deviceLinkJson, this.serviceClient);
    }

    /** {@inheritDoc} */
    public VPNOperator.VPNBuilder define(String configName) {
        return new VPNOperator(this.serviceClient).create(configName);
    }
}
