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
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.BGPPeeringClient;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import api.equinix.javasdk.networkedge.model.json.BGPPeeringJson;
import api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringUpdaterJson;
import api.equinix.javasdk.networkedge.model.wrappers.BGPPeeringWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>BGPPeeringClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class BGPPeeringClientImpl extends ResourceClientBase<BGPPeering, BGPPeeringJson> implements BGPPeeringClient<BGPPeering> {

    /**
     * <p>Constructor for BGPPeeringClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public BGPPeeringClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "BGPPeering", BGPPeeringJson.class);
    }

    /** {@inheritDoc} */
    @Override
    protected BGPPeering wrap(BGPPeeringJson json) {
        return new BGPPeeringWrapper(json, this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public Page<BGPPeering, BGPPeeringJson> list(RequestBuilder.BGP requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        return listPage("ListBGP", qParams);
    }

    /** {@inheritDoc} */
    public BGPPeeringJson getByUuid(String uuid) {
        return getOne("GetBGP", uuid);
    }

    /** {@inheritDoc} */
    public BGPPeeringJson create(BGPPeeringCreatorJson bgpPeeringCreatorJson) {
        EquinixRequest<BGPPeeringJson> equinixRequest = this.buildRequest("CreateBGP", RequestType.SINGLE, BGPPeeringJson.class);
        Utils.serializeJson(equinixRequest, bgpPeeringCreatorJson);
        EquinixResponse<BGPPeeringJson> equinixResponse = this.invoke(equinixRequest);
        String aclTemplateUuid = Utils.extractFromHeader(equinixResponse, "Location", Constants.UUID_PATTERN);
        return getByUuid(aclTemplateUuid);
    }

    /** {@inheritDoc} */
    public BGPPeeringJson update(String uuid, BGPPeeringUpdaterJson bgpPeeringUpdaterJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<BGPPeeringJson> equinixRequest = this.buildRequestWithPathParams("UpdateBGP", RequestType.SINGLE, pParams, BGPPeeringJson.class);
        Utils.serializeJson(equinixRequest, bgpPeeringUpdaterJson);
        EquinixResponse<BGPPeeringJson> equinixResponse = this.invoke(equinixRequest);
        return getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public Boolean delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<BGPPeering> equinixRequest = this.buildRequestWithPathParams("DeleteBGP", RequestType.SINGLE, pParams, BGPPeeringJson.class);
        EquinixResponse<BGPPeering> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public BGPPeeringJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
