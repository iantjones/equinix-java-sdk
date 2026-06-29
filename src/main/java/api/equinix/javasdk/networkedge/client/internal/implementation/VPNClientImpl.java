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

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
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
 *
 * @author ianjones
 */
public class VPNClientImpl extends ResourceClientBase<VPN, VPNJson> implements VPNClient<VPN> {

    public VPNClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "VPNs", VPNJson.class);
    }

    @Override
    protected VPN wrap(VPNJson json) {
        return new VPNWrapper(json, this);
    }

    /**
     * {@inheritDoc}
     *
     */
    public Page<VPN, VPNJson> list(RequestBuilder.VPN requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        return listPage("ListVPNs", qParams);
    }

    public VPNJson getByUuid(String uuid) {
        return getOne("GetVPN", uuid);
    }

    public VPNJson create(VPNCreatorJson vpnCreatorJson) {
        return getByUuid(createReturningLocationUuid("CreateVPN", null, null, vpnCreatorJson));
    }

    public VPNJson update(String uuid, VPNUpdaterJson vpnUpdaterJson) {
        voidOp("UpdateVPN", RequestType.SINGLE, Map.of("uuid", uuid), null, vpnUpdaterJson);
        return getByUuid(uuid);
    }

    public Boolean delete(String uuid) {
        return booleanOp("DeleteVPN", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public VPNJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
