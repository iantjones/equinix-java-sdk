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

import api.equinix.javasdk.fabric.enums.ServiceTokenState;
import api.equinix.javasdk.fabric.enums.ServiceTokenType;
import api.equinix.javasdk.fabric.model.implementation.BasicAccount;
import api.equinix.javasdk.fabric.model.implementation.Notification;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Connection;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
public interface ServiceToken {

    String getUuid();

    ServiceTokenType getType();

    String getHref();

    ServiceTokenState getState();

    Integer getExpiry();

    LocalDateTime getExpirationDateTime();

    Connection getConnection();

    List<Notification> getNotifications();

    BasicAccount getAccount();

    ChangeLog getChangeLog();

    Boolean delete();

    void refresh();
}
