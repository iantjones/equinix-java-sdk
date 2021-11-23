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
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.SSHUsers;
import api.equinix.javasdk.networkedge.client.internal.SSHUserClient;
import api.equinix.javasdk.networkedge.model.SSHUser;
import api.equinix.javasdk.networkedge.model.json.creators.SSHUserOperator;
import api.equinix.javasdk.networkedge.model.json.SSHUserJson;
import api.equinix.javasdk.networkedge.model.wrappers.SSHUserWrapper;
import lombok.Getter;

/**
 * <p>SSHUsersImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class SSHUsersImpl implements SSHUsers {

    private final NetworkEdge serviceManager;

    private final SSHUserClient<SSHUser> serviceClient;

    /**
     * <p>Constructor for SSHUsersImpl.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.networkedge.client.internal.SSHUserClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.NetworkEdge} object.
     */
    public SSHUsersImpl(SSHUserClient<SSHUser> serviceClient,
                        NetworkEdge serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<SSHUser> list() {
        return list(null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public PaginatedList<SSHUser> list(RequestBuilder.SSHUser requestBuilder) {
        Page<SSHUser, SSHUserJson> responsePage = serviceClient.list(requestBuilder);
        PaginatedList<SSHUser> deviceList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, SSHUserWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /** {@inheritDoc} */
    public SSHUser getByUuid(String uuid) {
        SSHUserJson publicKeyJson = serviceClient.getByUuid(uuid);
        return new SSHUserWrapper(publicKeyJson, this.serviceClient);
    }

    /** {@inheritDoc} */
    public SSHUserOperator.SSHUserBuilder define(String username, String password, String deviceUuid) {
        return new SSHUserOperator(this.serviceClient).create(username, password, deviceUuid);
    }

    /** {@inheritDoc} */
    public Boolean checkUsernameAvailability(String username) {
        return serviceClient.checkUsernameAvailability(username);
    }
}
