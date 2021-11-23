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
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.SSHUserClient;
import api.equinix.javasdk.networkedge.model.SSHUser;
import api.equinix.javasdk.networkedge.model.implementation.UUIDResult;
import api.equinix.javasdk.networkedge.model.json.SSHUserJson;
import api.equinix.javasdk.networkedge.model.json.creators.SSHUserCreatorJson;
import api.equinix.javasdk.networkedge.model.wrappers.SSHUserWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>SSHUserClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class SSHUserClientImpl extends PageableBase implements SSHUserClient<SSHUser> {

    /**
     * <p>Constructor for SSHUserClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public SSHUserClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "SSHUsers");
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public Page<SSHUser, SSHUserJson> list(RequestBuilder.SSHUser requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        EquinixRequest<SSHUser> equinixRequest = this.buildRequest("ListSSHUsers", RequestType.PAGINATED, null, qParams, SSHUserJson.getPagedTypeRef());
        EquinixResponse<SSHUser> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public SSHUserJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<SSHUserJson> equinixRequest = this.buildRequestWithPathParams("GetSSHUser", RequestType.SINGLE, pParams, SSHUserJson.getSingleTypeRef());
        EquinixResponse<SSHUserJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public SSHUserJson create(SSHUserCreatorJson sshUserCreatorJson) {
        EquinixRequest<SSHUserJson> equinixRequest = this.buildRequest("CreateSSHUser", RequestType.SINGLE, SSHUserJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, sshUserCreatorJson);
        EquinixResponse<SSHUserJson> equinixResponse = this.invoke(equinixRequest);
        UUIDResult uuidResult = Utils.handleSingletonResponse(equinixResponse, equinixRequest);
        return getByUuid(uuidResult.getUuid());
    }

    /** {@inheritDoc} */
    public Boolean deleteDevice(String uuid, String deviceUuid) {
        Map<String, String> pParams = Map.of("uuid", uuid, "deviceUuid", deviceUuid);
        EquinixRequest<SSHUser> equinixRequest = this.buildRequestWithPathParams("DeleteSSHUser", RequestType.SINGLE, pParams, SSHUserJson.getSingleTypeRef());
        EquinixResponse<SSHUser> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public Boolean addDevice(String uuid, String deviceUuid) {
        Map<String, String> pParams = Map.of("uuid", uuid, "deviceUuid", deviceUuid);
        EquinixRequest<SSHUser> equinixRequest = this.buildRequestWithPathParams("SSHUserAddDevice", RequestType.SINGLE, pParams, SSHUserJson.getSingleTypeRef());
        EquinixResponse<SSHUser> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public Boolean updatePassword(String uuid, String newPassword) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        Map<String, String> payload = Map.of("password", newPassword);
        EquinixRequest<SSHUser> equinixRequest = this.buildRequestWithPathParams("UpdateSSHUserPassword", RequestType.SINGLE, pParams, SSHUserJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, payload);
        EquinixResponse<SSHUser> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public Boolean checkUsernameAvailability(String username) {
        Map<String, List<String>> qParams = Map.of("username", Utils.singleParamList(username));
        EquinixRequest<SSHUser> equinixRequest = this.buildRequest("GetSSHUsernameAvailability", RequestType.SINGLE, null, qParams, SSHUserJson.getSingleTypeRef());
        EquinixResponse<SSHUser> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public SSHUserJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    /** {@inheritDoc} */
    @Override
    public PaginatedList<SSHUser> nextPage(PaginatedRequest<SSHUser> equinixRequest) {
        EquinixResponse<SSHUser> equinixResponse = this.invoke(equinixRequest);
        Page<SSHUser, SSHUserJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<SSHUser> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, SSHUserWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
