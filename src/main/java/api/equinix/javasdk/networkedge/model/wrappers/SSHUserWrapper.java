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

package api.equinix.javasdk.networkedge.model.wrappers;

import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.networkedge.client.internal.implementation.SSHUserClientImpl;
import api.equinix.javasdk.networkedge.enums.SSHUserAction;
import api.equinix.javasdk.networkedge.model.SSHUser;
import api.equinix.javasdk.networkedge.model.json.SSHUserJson;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * <p>SSHUserWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class SSHUserWrapper extends ResourceImpl<SSHUser> implements SSHUser {

    private SSHUserJson jsonObject;
    @Getter
    private final Pageable<SSHUser> serviceClient;

    /**
     * <p>Constructor for SSHUserWrapper.</p>
     *
     * @param sshUserJson a {@link api.equinix.javasdk.networkedge.model.json.SSHUserJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public SSHUserWrapper(SSHUserJson sshUserJson, Pageable<SSHUser> serviceClient) {
        this.jsonObject = sshUserJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUuid() {
        return this.jsonObject.getUuid();
    }

    /**
     * <p>getUsername.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUsername() {
        return this.jsonObject.getUsername();
    }

    /**
     * <p>getDeviceUuids.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getDeviceUuids() {
        return this.jsonObject.getDeviceUuids();
    }

    /**
     * <p>getMetroStatusMap.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, SSHUserJson.Status> getMetroStatusMap() {
        return this.jsonObject.getMetroStatusMap();
    }

    /**
     * <p>getAction.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.SSHUserAction} object.
     */
    public SSHUserAction getAction() {
        return this.jsonObject.getAction();
    }

    /** {@inheritDoc} */
    public Boolean addDevice(String deviceUuid) {
        return ((SSHUserClientImpl)this.serviceClient).addDevice(this.getUuid(), deviceUuid);
    }

    /** {@inheritDoc} */
    public Boolean updatePassword(String newPassword) {
        return ((SSHUserClientImpl)this.serviceClient).updatePassword(this.getUuid(), newPassword);
    }

    /** {@inheritDoc} */
    public Boolean deleteDevice(String deviceUuid) {
        return ((SSHUserClientImpl)this.serviceClient).deleteDevice(this.getUuid(), deviceUuid);
    }

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        if(getDeviceUuids() != null && getDeviceUuids().size() > 0) {
            for (String deviceUuid : getDeviceUuids()) {
                ((SSHUserClientImpl) this.serviceClient).deleteDevice(this.getUuid(), deviceUuid);
            }
        }
        else {
            throw new EquinixClientException("To delete an SSHUser, all devices must be disassociated first. Use RequestBuilder.SSHUser to load verbose details including list of associated deviceUuids.");
        }

        return true;
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean refresh() {
        this.jsonObject = ((SSHUserClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
