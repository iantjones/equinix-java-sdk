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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.core.enums.PortType;
import api.equinix.javasdk.fabric.client.internal.implementation.PortClientImpl;
import api.equinix.javasdk.fabric.enums.AccessPointType;
import api.equinix.javasdk.fabric.enums.PortState;
import api.equinix.javasdk.fabric.model.implementation.*;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.json.PortJson;
import lombok.Getter;

import java.util.List;

/**
 * <p>PortWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PortWrapper extends ResourceImpl<Port> implements Port {

    private PortJson jsonObject;
    @Getter
    private final Pageable<Port> serviceClient;

    /**
     * <p>Constructor for PortWrapper.</p>
     *
     * @param portJson a {@link api.equinix.javasdk.fabric.model.json.PortJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public PortWrapper(PortJson portJson, Pageable<Port> serviceClient) {
        this.jsonObject = portJson;
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
     * <p>getType.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.PortType} object.
     */
    public PortType getType() {
        return this.jsonObject.getType();
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return this.jsonObject.getName();
    }

    /**
     * <p>getHref.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getHref() {
        return this.jsonObject.getHref();
    }

    /**
     * <p>getState.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.PortState} object.
     */
    public PortState getState() {
        return this.jsonObject.getState();
    }

    /**
     * <p>getCvpId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCvpId() {
        return this.jsonObject.getCvpId();
    }

    /**
     * <p>getBandwidth.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getBandwidth() {
        return this.jsonObject.getBandwidth();
    }

    /**
     * <p>getUsedBandwidth.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getUsedBandwidth() {
        return this.jsonObject.getUsedBandwidth();
    }

    /**
     * <p>getAvailableBandwidth.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getAvailableBandwidth() {
        return this.jsonObject.getAvailableBandwidth();
    }

    /**
     * <p>getLocation.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Location} object.
     */
    public Location getLocation() {
        return this.jsonObject.getLocation();
    }

    /**
     * <p>getDevice.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Device} object.
     */
    public Device getDevice() {
        return this.jsonObject.getDevice();
    }

    /**
     * <p>getEncapsulation.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Encapsulation} object.
     */
    public Encapsulation getEncapsulation() {
        return this.jsonObject.getEncapsulation();
    }

    /**
     * <p>getLag.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.LinkAggregationGroup} object.
     */
    public LinkAggregationGroup getLag() {
        return this.jsonObject.getLag();
    }

    /**
     * <p>getSettings.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.PortSettings} object.
     */
    public PortSettings getSettings() {
        return this.jsonObject.getSettings();
    }

    /**
     * <p>getPhysicalPorts.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<PhysicalPort> getPhysicalPorts() {
        return this.jsonObject.getPhysicalPorts();
    }

    public Redundancy getRedundancy() {
        return this.jsonObject.getRedundancy();
    }

    public Boolean getLagEnabled() {
        return this.jsonObject.getLagEnabled();
    }

    /**
     * <p>getOperation.</p>
     *
     * @return a {@link PortOperation} object.
     */
    public PortOperation getOperation() {
        return this.jsonObject.getPortOperation();
    }

    /**
     * <p>getAccount.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Account} object.
     */
    public Account getAccount() {
        return this.jsonObject.getAccount();
    }

    public String getProjectId() {
        return this.jsonObject.getProjectId();
    }

    /**
     * <p>getChangeLog.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.ChangeLog} object.
     */
    public ChangeLog getChangeLog() {
        return this.jsonObject.getChangeLog();
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.Port} object.
     */
    public Port refresh() {
        this.jsonObject = ((PortClientImpl)this.serviceClient).refresh(this.getUuid());
        return this;
    }

    public SimpleAccessPoint accessPoint() {
        return SimpleAccessPoint.define(AccessPointType.COLO).port(this.getUuid()).create();
    }
}
