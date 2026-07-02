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

import api.equinix.javasdk.fabric.enums.ConnectionState;
import api.equinix.javasdk.core.model.KeyValuePair;
import api.equinix.javasdk.fabric.enums.ConnectionOperationType;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.Direction;
import api.equinix.javasdk.fabric.enums.GeoScopeType;
import api.equinix.javasdk.fabric.model.implementation.*;

import java.util.List;

/**
 *
 * @author ianjones
 */
public interface Connection {

    String getUuid();

    ConnectionType getType();

    String getHref();

    String getName();

    String getDescription();

    ConnectionState getState();

    GeoScopeType getGeoScope();

    Order getOrder();

    ConnectionOperation getOperation();

    List<Notification> getNotifications();

    Account getAccount();

    ChangeLog getChangeLog();

    Integer getBandwidth();

    Redundancy getRedundancy();

    Boolean getIsRemote();

    Direction getDirection();

    ConnectionSide getASide();

    ConnectionSide getZSide();

    List<KeyValuePair> getAdditionalInfo();

    MarketplaceSubscriptionRef getMarketplaceSubscription();

    Project getProject();

    Change getChange();

    /**
     * Performs a connection action (accept, reject, retry...) and returns the action result
     * (spec schema {@code ConnectionAction}). Call {@code refresh()} afterwards to re-read the
     * connection's updated state.
     *
     * @param connectionOperation the action type
     * @param description action description (e.g. rejection reason)
     * @param bodyObject optional action data
     * @return the {@link ConnectionAction} returned by the API
     */
    ConnectionAction performOperation(ConnectionOperationType connectionOperation, String description, Object bodyObject);

    ConnectionAction performOperation(ConnectionOperationType connectionOperation, String description);

    ConnectionAction performOperation(ConnectionOperationType connectionOperation);

    api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator.ConnectionUpdater update();

    Boolean delete();

    void refresh();
}
