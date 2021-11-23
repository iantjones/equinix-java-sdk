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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.networkedge.client.internal.implementation.BGPPeeringClientImpl;
import api.equinix.javasdk.networkedge.enums.BGPState;
import api.equinix.javasdk.networkedge.enums.BGPStatus;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import api.equinix.javasdk.networkedge.model.json.BGPPeeringJson;
import api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringOperator;
import api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringUpdaterJson;
import lombok.Getter;

/**
 * <p>BGPPeeringWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class BGPPeeringWrapper extends ResourceImpl<BGPPeering> implements BGPPeering {

    private BGPPeeringJson json;
    @Getter
    private final Pageable<BGPPeering> serviceClient;

    /**
     * <p>Constructor for BGPPeeringWrapper.</p>
     *
     * @param sshUserJson a {@link api.equinix.javasdk.networkedge.model.json.BGPPeeringJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public BGPPeeringWrapper(BGPPeeringJson sshUserJson, Pageable<BGPPeering> serviceClient) {
        this.json = sshUserJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUuid() {
        return  this.json.getUuid();
    }

    /**
     * <p>getConnectionUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getConnectionUuid() {
        return  this.json.getConnectionUuid();
    }

    /**
     * <p>getConnectionName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getConnectionName() {
        return  this.json.getConnectionName();
    }

    /**
     * <p>getVirtualDeviceUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVirtualDeviceUuid() {
        return  this.json.getVirtualDeviceUuid();
    }

    /**
     * <p>getLocalIpAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLocalIpAddress() {
        return  this.json.getLocalIpAddress();
    }

    /**
     * <p>getRemoteIpAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRemoteIpAddress() {
        return  this.json.getRemoteIpAddress();
    }

    /**
     * <p>getLocalAsn.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getLocalAsn() {
        return  this.json.getLocalAsn();
    }

    /**
     * <p>getRemoteAsn.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getRemoteAsn() {
        return  this.json.getRemoteAsn();
    }

    /**
     * <p>getAuthenticationKey.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAuthenticationKey() {
        return  this.json.getAuthenticationKey();
    }

    /**
     * <p>getProvisioningStatus.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.BGPStatus} object.
     */
    public BGPStatus getProvisioningStatus() {
        return  this.json.getProvisioningStatus();
    }

    /**
     * <p>getState.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.BGPState} object.
     */
    public BGPState getState() {
        return  this.json.getState();
    }

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringOperator.BGPPeeringUpdater} object.
     */
    public BGPPeeringOperator.BGPPeeringUpdater update() {
        return new BGPPeeringOperator(this.serviceClient).update(this.json);
    }

    /** {@inheritDoc} */
    public Boolean save(BGPPeeringUpdaterJson updaterJson) {
        this.json = ((BGPPeeringClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        return ((BGPPeeringClientImpl)this.serviceClient).delete(this.getUuid());
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean refresh() {
        this.json = ((BGPPeeringClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
