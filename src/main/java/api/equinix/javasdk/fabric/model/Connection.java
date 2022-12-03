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

import api.equinix.javasdk.core.enums.ConnectionState;
import api.equinix.javasdk.core.model.KeyValuePair;
import api.equinix.javasdk.fabric.enums.ConnectionOperationType;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.Direction;
import api.equinix.javasdk.fabric.model.implementation.*;

import java.util.List;

/**
 * <p>Connection interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Connection {

    String getUuid();

    ConnectionType getType();

    String getHref();

    String getName();

    ConnectionState getState();

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

    Change getChange();

    void performOperation(ConnectionOperationType connectionOperation, String description, Object bodyObject);

    void performOperation(ConnectionOperationType connectionOperation, String description);

    void performOperation(ConnectionOperationType connectionOperation);

    Boolean delete();

    void refresh();
}
