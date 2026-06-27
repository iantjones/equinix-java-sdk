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

package api.equinix.javasdk.networkedge.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.model.SSHUser;
import api.equinix.javasdk.networkedge.model.json.SSHUserJson;
import api.equinix.javasdk.networkedge.model.json.creators.SSHUserCreatorJson;

/**
 * <p>SSHUserClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface SSHUserClient<T> extends Pageable<T> {

    /**
     * <p>list.</p>
     *
     * @param requestBuilder a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.SSHUser} object.
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    Page<SSHUser, SSHUserJson> list(RequestBuilder.SSHUser requestBuilder);

    /**
     * <p>getByUuid.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.SSHUserJson} object.
     */
    SSHUserJson getByUuid(String uuid);

    /**
     * <p>create.</p>
     *
     * @param sshUserCreatorJson a {@link api.equinix.javasdk.networkedge.model.json.creators.SSHUserCreatorJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.SSHUserJson} object.
     */
    SSHUserJson create(SSHUserCreatorJson sshUserCreatorJson);

    /**
     * <p>deleteDevice.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean deleteDevice(String uuid, String deviceUuid);

    /**
     * <p>addDevice.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean addDevice(String uuid, String deviceUuid);

    /**
     * <p>updatePassword.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param newPassword a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean updatePassword(String uuid, String newPassword);

    /**
     * <p>checkUsernameAvailability.</p>
     *
     * @param username a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean checkUsernameAvailability(String username);

    /**
     * <p>refresh.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.SSHUserJson} object.
     */
    SSHUserJson refresh(String uuid);
}
