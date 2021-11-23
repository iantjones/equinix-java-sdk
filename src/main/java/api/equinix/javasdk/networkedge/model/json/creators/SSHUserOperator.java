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

package api.equinix.javasdk.networkedge.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.networkedge.client.internal.implementation.SSHUserClientImpl;
import api.equinix.javasdk.networkedge.model.SSHUser;
import api.equinix.javasdk.networkedge.model.json.SSHUserJson;
import api.equinix.javasdk.networkedge.model.wrappers.SSHUserWrapper;
import lombok.Getter;

/**
 * <p>SSHUserOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class SSHUserOperator extends ResourceImpl<SSHUser> {

    @Getter
    private final Pageable<SSHUser> serviceClient;

    /**
     * <p>Constructor for SSHUserOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public SSHUserOperator(Pageable<SSHUser> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @param username a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.SSHUserOperator.SSHUserBuilder} object.
     */
    public SSHUserBuilder create(String username, String password, String deviceUuid) {
        return new SSHUserBuilder(username, password, deviceUuid);
    }

    @Getter
    public class SSHUserBuilder {

        private final String username;
        private final String password;
        private final String deviceUuid;

        protected SSHUserBuilder(String username, String password, String deviceUuid) {
            this.username = username;
            this.password = password;
            this.deviceUuid = deviceUuid;
        }

        public SSHUser create() {
            SSHUserCreatorJson sshUserCreatorJson = new SSHUserCreatorJson(this);
            SSHUserJson sshUserJson = ((SSHUserClientImpl) SSHUserOperator.this.getServiceClient()).create(sshUserCreatorJson);
           return new SSHUserWrapper(sshUserJson, SSHUserOperator.this.getServiceClient());
        }
    }
}
