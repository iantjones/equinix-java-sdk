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
 * <p>ServiceToken interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface ServiceToken {

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    /**
     * <p>getType.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.ServiceTokenType} object.
     */
    ServiceTokenType getType();

    /**
     * <p>getHref.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getHref();

    /**
     * <p>getState.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.ServiceTokenState} object.
     */
    ServiceTokenState getState();

    /**
     * <p>getExpiry.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getExpiry();

    /**
     * <p>getExpirationDateTime.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    LocalDateTime getExpirationDateTime();

    /**
     * <p>getConnection.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Connection} object.
     */
    Connection getConnection();

    /**
     * <p>getNotifications.</p>
     *
     * @return a {@link java.util.List} object.
     */
    List<Notification> getNotifications();

    /**
     * <p>getAccount.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.BasicAccount} object.
     */
    BasicAccount getAccount();

    /**
     * <p>getChangeLog.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.ChangeLog} object.
     */
    ChangeLog getChangeLog();

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean delete();

    /**
     * <p>refresh.</p>
     */
    void refresh();
}
