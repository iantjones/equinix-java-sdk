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

package com.eqixiac.equinix.networkedge.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.client.implementation.NetworkEdgeConfigImpl;
import com.eqixiac.equinix.networkedge.client.internal.VPNClient;
import com.eqixiac.equinix.networkedge.model.VPN;
import com.eqixiac.equinix.networkedge.model.json.VPNJson;
import com.eqixiac.equinix.networkedge.model.json.creators.VPNCreatorJson;
import com.eqixiac.equinix.networkedge.model.json.creators.VPNUpdaterJson;
import com.eqixiac.equinix.networkedge.model.wrappers.VPNWrapper;

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
    public Page<VPNJson> list(RequestBuilder.VPN requestBuilder) {
        Map<String, List<String>> qParams = ParameterMapper.newMap(requestBuilder);
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
