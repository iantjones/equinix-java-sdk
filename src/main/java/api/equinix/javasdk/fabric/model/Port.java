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

package api.equinix.javasdk.fabric.model;

import api.equinix.javasdk.core.enums.PortType;
import api.equinix.javasdk.fabric.enums.PortState;
import api.equinix.javasdk.fabric.model.implementation.Account;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Device;
import api.equinix.javasdk.fabric.model.implementation.Encapsulation;
import api.equinix.javasdk.fabric.model.implementation.LinkAggregationGroup;
import api.equinix.javasdk.fabric.model.implementation.Location;
import api.equinix.javasdk.fabric.model.implementation.Operation;
import api.equinix.javasdk.fabric.model.implementation.PhysicalPort;
import api.equinix.javasdk.fabric.model.implementation.PortSettings;

import java.util.List;

/**
 * <p>Port interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Port {

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    /**
     * <p>getType.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.PortType} object.
     */
    PortType getType();

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getName();

    /**
     * <p>getHref.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getHref();

    /**
     * <p>getState.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.PortState} object.
     */
    PortState getState();

    /**
     * <p>getCvpId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getCvpId();

    /**
     * <p>getBandwidth.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getBandwidth();

    /**
     * <p>getUsedBandwidth.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getUsedBandwidth();

    /**
     * <p>getAvailableBandwidth.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getAvailableBandwidth();

    /**
     * <p>getLocation.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Location} object.
     */
    Location getLocation();

    /**
     * <p>getDevice.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Device} object.
     */
    Device getDevice();

    /**
     * <p>getEncapsulation.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Encapsulation} object.
     */
    Encapsulation getEncapsulation();

    /**
     * <p>getLag.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.LinkAggregationGroup} object.
     */
    LinkAggregationGroup getLag();

    /**
     * <p>getSettings.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.PortSettings} object.
     */
    PortSettings getSettings();

    /**
     * <p>getPhysicalPorts.</p>
     *
     * @return a {@link java.util.List} object.
     */
    List<PhysicalPort> getPhysicalPorts();

    /**
     * <p>getOperation.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Operation} object.
     */
    Operation getOperation();

    /**
     * <p>getAccount.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Account} object.
     */
    Account getAccount();

    /**
     * <p>getChangeLog.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.ChangeLog} object.
     */
    ChangeLog getChangeLog();
}
