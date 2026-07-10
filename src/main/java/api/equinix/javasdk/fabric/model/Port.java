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

import api.equinix.javasdk.fabric.enums.BmmrType;
import api.equinix.javasdk.fabric.enums.ConnectivitySourceType;
import api.equinix.javasdk.fabric.enums.PhysicalPortType;
import api.equinix.javasdk.fabric.enums.PortServiceCode;
import api.equinix.javasdk.fabric.enums.PortServiceType;
import api.equinix.javasdk.fabric.enums.PortType;
import api.equinix.javasdk.fabric.enums.PortState;
import api.equinix.javasdk.fabric.model.implementation.*;

import java.util.List;

/**
 *
 * @author ianjones
 */
public interface Port extends AccessPointable {

    String getUuid();

    PortType getType();

    String getName();

    String getHref();

    String getDescription();

    PortState getState();

    String getCvpId();

    Integer getBandwidth();

    Integer getUsedBandwidth();

    Integer getAvailableBandwidth();

    Integer getPhysicalPortsSpeed();

    PhysicalPortType getPhysicalPortsType();

    Integer getPhysicalPortsCount();

    Integer getPhysicalPortQuantity();

    Integer getConnectionsCount();

    ConnectivitySourceType getConnectivitySourceType();

    BmmrType getBmmrType();

    PortServiceType getServiceType();

    PortServiceCode getServiceCode();

    Long getAsn();

    Location getLocation();

    Device getDevice();

    PortInterface getPortInterface();

    String getDemarcationPointIbx();

    String getTetherIbx();

    DemarcationPoint getDemarcationPoint();

    Encapsulation getEncapsulation();

    LinkAggregationGroup getLag();

    PackageRef getPortPackage();

    PortSettings getSettings();

    List<PhysicalPort> getPhysicalPorts();

    Redundancy getRedundancy();

    PortOperation getOperation();

    Boolean getLagEnabled();

    Account getAccount();

    PortOrder getOrder();

    PortChange getChange();

    Project getProject();

    String getProjectId();

    List<PortNotification> getNotifications();

    List<PortAdditionalInfo> getAdditionalInfo();

    EndCustomer getEndCustomer();

    List<PortLoa> getLoas();

    MarketplaceSubscriptionRef getMarketplaceSubscription();

    ChangeLog getChangeLog();

    /**
     * Re-fetches this port from the API and updates this instance in place.
     *
     * @return this port, refreshed
     */
    Port refresh();
}
