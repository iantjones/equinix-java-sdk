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
import com.eqixiac.equinix.networkedge.client.internal.BGPPeeringClient;
import com.eqixiac.equinix.networkedge.model.BGPPeering;
import com.eqixiac.equinix.networkedge.model.implementation.UUIDResult;
import com.eqixiac.equinix.networkedge.model.json.BGPPeeringJson;
import com.eqixiac.equinix.networkedge.model.json.creators.BGPPeeringCreatorJson;
import com.eqixiac.equinix.networkedge.model.json.creators.BGPPeeringUpdaterJson;
import com.eqixiac.equinix.networkedge.model.wrappers.BGPPeeringWrapper;

import java.util.List;
import java.util.Map;

/**
 *
 * @author ianjones
 */
public class BGPPeeringClientImpl extends ResourceClientBase<BGPPeering, BGPPeeringJson> implements BGPPeeringClient<BGPPeering> {

    public BGPPeeringClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "BGPPeering", BGPPeeringJson.class);
    }

    @Override
    protected BGPPeering wrap(BGPPeeringJson json) {
        return new BGPPeeringWrapper(json, this);
    }

    /**
     * {@inheritDoc}
     *
     */
    public Page<BGPPeeringJson> list(RequestBuilder.BGP requestBuilder) {
        Map<String, List<String>> qParams = ParameterMapper.newMap(requestBuilder);
        return listPage("ListBGP", qParams);
    }

    public BGPPeeringJson getByUuid(String uuid) {
        return getOne("GetBGP", uuid);
    }

    public BGPPeeringJson create(BGPPeeringCreatorJson bgpPeeringCreatorJson) {
        // createBgpConfiguration returns 202 Accepted with a BgpAsyncResponse body ({uuid:...}),
        // not a Location header, so the new uuid is parsed from the response body (as for
        // Device / DeviceLink create) before re-fetching the created peering.
        UUIDResult uuidResult = postForType("CreateBGP", bgpPeeringCreatorJson, BGPPeeringJson.getCreateTypeRef());
        return getByUuid(uuidResult.getUuid());
    }

    public BGPPeeringJson update(String uuid, BGPPeeringUpdaterJson bgpPeeringUpdaterJson) {
        voidOp("UpdateBGP", RequestType.SINGLE, Map.of("uuid", uuid), null, bgpPeeringUpdaterJson);
        return getByUuid(uuid);
    }

    public Boolean delete(String uuid) {
        return booleanOp("DeleteBGP", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public BGPPeeringJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
