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
import api.equinix.javasdk.networkedge.client.BGPPeerings;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.internal.BGPPeeringClient;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import api.equinix.javasdk.networkedge.model.json.BGPPeeringJson;
import api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringOperator;
import api.equinix.javasdk.networkedge.model.wrappers.BGPPeeringWrapper;
import lombok.Getter;

/**
 * <p>BGPPeeringsImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class BGPPeeringsImpl implements BGPPeerings {

    private final NetworkEdge serviceManager;

    private final BGPPeeringClient<BGPPeering> serviceClient;

    /**
     * <p>Constructor for BGPPeeringsImpl.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.networkedge.client.internal.BGPPeeringClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.NetworkEdge} object.
     */
    public BGPPeeringsImpl(BGPPeeringClient<BGPPeering> serviceClient, NetworkEdge serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<BGPPeering> list() {
        return list(null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public PaginatedList<BGPPeering> list(RequestBuilder.BGP requestBuilder) {
        Page<BGPPeering, BGPPeeringJson> responsePage = serviceClient.list(requestBuilder);
        PaginatedList<BGPPeering> deviceList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, BGPPeeringWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /** {@inheritDoc} */
    public BGPPeering getByUuid(String uuid) {
        BGPPeeringJson deviceLinkJson = serviceClient.getByUuid(uuid);
        return new BGPPeeringWrapper(deviceLinkJson, this.serviceClient);
    }

    /**
     * <p>define.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringOperator.BGPPeeringBuilder} object.
     */
    public BGPPeeringOperator.BGPPeeringBuilder define() {
        return new BGPPeeringOperator(this.serviceClient).create();
    }
}
