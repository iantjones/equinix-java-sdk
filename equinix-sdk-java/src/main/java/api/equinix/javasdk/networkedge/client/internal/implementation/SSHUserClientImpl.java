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
public class SSHUserClientImpl extends ResourceClientBase<SSHUser, SSHUserJson> implements SSHUserClient<SSHUser> {

    /**
     * <p>Constructor for SSHUserClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public SSHUserClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "SSHUsers", SSHUserJson.class);
    }

    /** {@inheritDoc} */
    @Override
    protected SSHUser wrap(SSHUserJson json) {
        return new SSHUserWrapper(json, this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public Page<SSHUser, SSHUserJson> list(RequestBuilder.SSHUser requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        return listPage("ListSSHUsers", qParams);
    }

    /** {@inheritDoc} */
    public SSHUserJson getByUuid(String uuid) {
        return getOne("GetSSHUser", uuid);
    }

    /** {@inheritDoc} */
    public SSHUserJson create(SSHUserCreatorJson sshUserCreatorJson) {
        UUIDResult uuidResult = postAs("CreateSSHUser", sshUserCreatorJson, UUIDResult.class);
        return getByUuid(uuidResult.getUuid());
    }

    /** {@inheritDoc} */
    public Boolean deleteDevice(String uuid, String deviceUuid) {
        return booleanOp("DeleteSSHUser", RequestType.SINGLE, Map.of("uuid", uuid, "deviceUuid", deviceUuid), null, null);
    }

    /** {@inheritDoc} */
    public Boolean addDevice(String uuid, String deviceUuid) {
        return booleanOp("SSHUserAddDevice", RequestType.SINGLE, Map.of("uuid", uuid, "deviceUuid", deviceUuid), null, null);
    }

    /** {@inheritDoc} */
    public Boolean updatePassword(String uuid, String newPassword) {
        return booleanOp("UpdateSSHUserPassword", RequestType.SINGLE, Map.of("uuid", uuid), null, Map.of("password", newPassword));
    }

    /** {@inheritDoc} */
    public Boolean checkUsernameAvailability(String username) {
        return booleanOp("GetSSHUsernameAvailability", RequestType.SINGLE, null, Map.of("username", Utils.singleParamList(username)), null);
    }

    /** {@inheritDoc} */
    public SSHUserJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
