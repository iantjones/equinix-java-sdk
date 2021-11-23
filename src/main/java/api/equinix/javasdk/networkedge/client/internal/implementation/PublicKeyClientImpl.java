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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.PublicKeyClient;
import api.equinix.javasdk.networkedge.model.PublicKey;
import api.equinix.javasdk.networkedge.model.json.PublicKeyJson;
import api.equinix.javasdk.networkedge.model.json.creators.PublicKeyCreatorJson;
import api.equinix.javasdk.networkedge.model.wrappers.PublicKeyWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>PublicKeyClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PublicKeyClientImpl extends PageableBase implements PublicKeyClient<PublicKey> {

    /**
     * <p>Constructor for PublicKeyClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public PublicKeyClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "PublicKeys");
    }

    /** {@inheritDoc} */
    public List<PublicKeyJson> list(String accountUcmId) {
        Map<String, List<String>> qParams = Utils.singleParamMap("accountUcmId" , accountUcmId);
        EquinixRequest<PublicKey> equinixRequest = this.buildRequest("ListPublicKeys", RequestType.LIST, null, qParams, PublicKeyJson.getListTypeRef());
        EquinixResponse<PublicKey> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public PublicKeyJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<PublicKeyJson> equinixRequest = this.buildRequestWithPathParams("GetPublicKey", RequestType.SINGLE, pParams, PublicKeyJson.getSingleTypeRef());
        EquinixResponse<PublicKeyJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }
    
    /** {@inheritDoc} */
    public PublicKeyJson create(PublicKeyCreatorJson publicKeyCreatorJson) {
        EquinixRequest<PublicKeyJson> equinixRequest = this.buildRequest("CreatePublicKey", RequestType.SINGLE, PublicKeyJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, publicKeyCreatorJson);
        EquinixResponse<PublicKeyJson> equinixResponse = this.invoke(equinixRequest);
        String publicKeyUuid = Utils.extractFromHeader(equinixResponse, "Location", Constants.UUID_PATTERN);
        return getByUuid(publicKeyUuid);
    }

    /** {@inheritDoc} */
    public Boolean delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<PublicKey> equinixRequest = this.buildRequestWithPathParams("DeletePublicKey", RequestType.SINGLE, pParams, PublicKeyJson.getSingleTypeRef());
        EquinixResponse<PublicKey> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public PublicKeyJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    /** {@inheritDoc} */
    @Override
    public PaginatedList<PublicKey> nextPage(PaginatedRequest<PublicKey> equinixRequest) {
        EquinixResponse<PublicKey> equinixResponse = this.invoke(equinixRequest);
        Page<PublicKey, PublicKeyJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<PublicKey> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, PublicKeyWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
