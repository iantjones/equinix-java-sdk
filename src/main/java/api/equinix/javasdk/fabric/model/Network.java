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

import api.equinix.javasdk.fabric.enums.NetworkScope;
import api.equinix.javasdk.fabric.enums.NetworkState;
import api.equinix.javasdk.fabric.enums.NetworkType;
import api.equinix.javasdk.fabric.model.implementation.Account;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Link;
import api.equinix.javasdk.fabric.model.implementation.LocationCode;
import api.equinix.javasdk.fabric.model.implementation.NetworkOperation;
import api.equinix.javasdk.fabric.model.implementation.Notification;
import api.equinix.javasdk.fabric.model.json.creators.NetworkOperator;

import java.util.List;

public interface Network {

    String getUuid();

    String getHref();

    String getName();

    NetworkType getType();

    NetworkState getState();

    NetworkScope getScope();

    LocationCode getLocation();

    Project getProject();

    Account getAccount();

    /**
     * Preferences for notifications on network configuration or status changes.
     *
     * @return the notification preferences
     */
    List<Notification> getNotifications();

    NetworkOperation getOperation();

    ChangeLog getChangeLog();

    Change getChange();

    List<Link> getLinks();

    Integer getConnectionsCount();

    /**
     * Begins a fluent JSON Patch update of this network, e.g.
     * {@code network.update().name("New-Name").save()}.
     *
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.NetworkOperator.NetworkUpdater}
     */
    NetworkOperator.NetworkUpdater update();

    Boolean delete();

    void refresh();
}
