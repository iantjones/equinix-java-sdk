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
import com.eqixiac.equinix.networkedge.client.BGPPeerings;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.client.internal.BGPPeeringClient;
import com.eqixiac.equinix.networkedge.model.BGPPeering;
import com.eqixiac.equinix.networkedge.model.json.BGPPeeringJson;
import com.eqixiac.equinix.networkedge.model.json.creators.BGPPeeringOperator;
import com.eqixiac.equinix.networkedge.model.wrappers.BGPPeeringWrapper;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class BGPPeeringsImpl implements BGPPeerings {

    private final BGPPeeringClient<BGPPeering> serviceClient;

    private final NetworkEdge serviceManager;

    public PaginatedList<BGPPeering> list() {
        return list(null);
    }

    /**
     * {@inheritDoc}
     *
     */
    public PaginatedList<BGPPeering> list(RequestBuilder.BGP requestBuilder) {
        Page<BGPPeeringJson> responsePage = serviceClient.list(requestBuilder);
        PaginatedList<BGPPeering> deviceList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, BGPPeeringWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public BGPPeering getByUuid(String uuid) {
        BGPPeeringJson deviceLinkJson = serviceClient.getByUuid(uuid);
        return new BGPPeeringWrapper(deviceLinkJson, this.serviceClient);
    }

    public BGPPeeringOperator.BGPPeeringBuilder define() {
        return new BGPPeeringOperator(this.serviceClient).create();
    }
}
