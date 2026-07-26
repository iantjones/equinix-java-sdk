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

package com.eqixiac.equinix.networkedge.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.client.VPNs;
import com.eqixiac.equinix.networkedge.client.internal.VPNClient;
import com.eqixiac.equinix.networkedge.model.VPN;
import com.eqixiac.equinix.networkedge.model.json.VPNJson;
import com.eqixiac.equinix.networkedge.model.json.creators.VPNOperator;
import com.eqixiac.equinix.networkedge.model.wrappers.VPNWrapper;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class VPNsImpl implements VPNs {

    private final VPNClient<VPN> serviceClient;

    private final NetworkEdge serviceManager;

    public PaginatedList<VPN> list() {
        return list(null);
    }

    /**
     * {@inheritDoc}
     *
     */
    public PaginatedList<VPN> list(RequestBuilder.VPN requestBuilder) {
        Page<VPNJson> responsePage = serviceClient.list(requestBuilder);
        PaginatedList<VPN> deviceList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, VPNWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public VPN getByUuid(String uuid) {
        VPNJson deviceLinkJson = serviceClient.getByUuid(uuid);
        return new VPNWrapper(deviceLinkJson, this.serviceClient);
    }

    public VPNOperator.VPNBuilder define(String configName) {
        return new VPNOperator(this.serviceClient).create(configName);
    }
}
