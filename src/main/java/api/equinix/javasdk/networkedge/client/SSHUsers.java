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

package api.equinix.javasdk.networkedge.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.model.SSHUser;
import api.equinix.javasdk.networkedge.model.VPN;
import api.equinix.javasdk.networkedge.model.json.creators.SSHUserOperator;
import api.equinix.javasdk.networkedge.model.json.creators.VPNOperator;

/**
 * Client interface for managing SSH users on Network Edge devices. Provides operations
 * to list, retrieve, and create SSH users, as well as check username availability.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface SSHUsers {

    /**
     * Lists available SSH Users.
     *
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<SSHUser> list();

    /**
     * Lists SSH Users based on the parameters provided.
     *
     * @param requestBuilder the desired query parameters.
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<SSHUser> list(RequestBuilder.SSHUser requestBuilder);

    /**
     * Gets the specified SSH User.
     *
     * @param uuid the unique identifier of the SSH User.
     * @return {@link api.equinix.javasdk.networkedge.model.SSHUser}
     */
    SSHUser getByUuid(String uuid);

    /**
     * Returns an instance of SSHUserBuilder for defining a new SSH User.
     *
     * @param username the username for the new SSH User.
     * @param password the password for the new SSH User.
     * @param deviceUuid the unique identifier of the device to add the SSH User to.
     * @return {@link api.equinix.javasdk.networkedge.model.json.creators.SSHUserOperator.SSHUserBuilder}
     */
    SSHUserOperator.SSHUserBuilder define(String username, String password, String deviceUuid);

    /**
     * Returns true if the username is available, otherwise returns 400 bad request.
     *
     * @param username the username to check the availability of.
     * @return {@link java.lang.Boolean}
     */
    Boolean checkUsernameAvailability(String username);
}
