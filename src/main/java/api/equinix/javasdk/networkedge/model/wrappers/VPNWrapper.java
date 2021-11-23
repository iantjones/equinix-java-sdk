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
import api.equinix.javasdk.networkedge.client.internal.implementation.VPNClientImpl;
import api.equinix.javasdk.networkedge.model.VPN;
import api.equinix.javasdk.networkedge.model.json.VPNJson;
import api.equinix.javasdk.networkedge.model.json.creators.VPNOperator;
import api.equinix.javasdk.networkedge.model.json.creators.VPNUpdaterJson;
import lombok.Getter;

/**
 * <p>VPNWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class VPNWrapper extends ResourceImpl<VPN> implements VPN {

    private VPNJson json;
    @Getter
    private final Pageable<VPN> serviceClient;

    /**
     * <p>Constructor for VPNWrapper.</p>
     *
     * @param sshUserJson a {@link api.equinix.javasdk.networkedge.model.json.VPNJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public VPNWrapper(VPNJson sshUserJson, Pageable<VPN> serviceClient) {
        this.json = sshUserJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getSecondary.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.VPN} object.
     */
    public VPN getSecondary() {
        return  this.json.getSecondary();
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
     * <p>getSiteName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSiteName() {
        return  this.json.getSiteName();
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
     * <p>getConfigName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getConfigName() {
        return  this.json.getConfigName();
    }

    /**
     * <p>getPeerIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPeerIp() {
        return  this.json.getPeerIp();
    }

    /**
     * <p>getPeerSharedKey.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPeerSharedKey() {
        return  this.json.getPeerSharedKey();
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
     * <p>getRemoteIpAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRemoteIpAddress() {
        return  this.json.getRemoteIpAddress();
    }

    /**
     * <p>getPassword.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPassword() {
        return  this.json.getPassword();
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
     * <p>getProjectId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getProjectId() {
        return  this.json.getProjectId();
    }

    /**
     * <p>getTunnelIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTunnelIp() {
        return  this.json.getTunnelIp();
    }

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.VPNOperator.VPNUpdater} object.
     */
    public VPNOperator.VPNUpdater update() {
        return new VPNOperator(this.serviceClient).update(this.json);
    }

    /** {@inheritDoc} */
    public Boolean save(VPNUpdaterJson updaterJson) {
        this.json = ((VPNClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        return ((VPNClientImpl)this.serviceClient).delete(this.getUuid());
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean refresh() {
        this.json = ((VPNClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
