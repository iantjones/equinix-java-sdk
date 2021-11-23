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
import api.equinix.javasdk.fabric.client.internal.implementation.ServiceTokenClientImpl;
import api.equinix.javasdk.fabric.enums.ServiceTokenState;
import api.equinix.javasdk.fabric.enums.ServiceTokenType;
import api.equinix.javasdk.fabric.model.implementation.BasicAccount;
import api.equinix.javasdk.fabric.model.implementation.Notification;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Connection;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.json.ServiceTokenJson;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>ServiceTokenWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceTokenWrapper extends ResourceImpl<ServiceToken> implements ServiceToken {

    private ServiceTokenJson jsonObject;
    @Getter
    private final Pageable<ServiceToken> serviceClient;

    /**
     * <p>Constructor for ServiceTokenWrapper.</p>
     *
     * @param serviceTokenJson a {@link api.equinix.javasdk.fabric.model.json.ServiceTokenJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public ServiceTokenWrapper(ServiceTokenJson serviceTokenJson, Pageable<ServiceToken> serviceClient) {
        this.jsonObject = serviceTokenJson;
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
     * @return a {@link api.equinix.javasdk.fabric.enums.ServiceTokenType} object.
     */
    public ServiceTokenType getType() {
        return this.jsonObject.getType();
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
     * @return a {@link api.equinix.javasdk.fabric.enums.ServiceTokenState} object.
     */
    public ServiceTokenState getState() {
        return this.jsonObject.getState();
    }

    /**
     * <p>getExpiry.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getExpiry() {
        return this.jsonObject.getExpiry();
    }

    /**
     * <p>getExpirationDateTime.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    public LocalDateTime getExpirationDateTime() {
        return this.jsonObject.getExpirationDateTime();
    }

    /**
     * <p>getConnection.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Connection} object.
     */
    public Connection getConnection() {
        return this.jsonObject.getConnection();
    }

    /**
     * <p>getNotifications.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Notification> getNotifications() {
        return this.jsonObject.getNotifications();
    }

    /**
     * <p>getAccount.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.BasicAccount} object.
     */
    public BasicAccount getAccount() {
        return this.jsonObject.getAccount();
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
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        this.jsonObject = ((ServiceTokenClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    /**
     * <p>refresh.</p>
     */
    public void refresh() {
        this.jsonObject = ((ServiceTokenClientImpl)this.serviceClient).refresh(this.getUuid());
    }
}
