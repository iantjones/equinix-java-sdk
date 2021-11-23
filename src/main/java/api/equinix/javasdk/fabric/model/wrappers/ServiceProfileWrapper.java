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
import api.equinix.javasdk.fabric.client.internal.implementation.ServiceProfileClientImpl;
import api.equinix.javasdk.fabric.enums.ServiceProfileState;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.model.implementation.Account;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Notification;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileConfig;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.json.ServiceProfileJson;
import lombok.Getter;

import java.util.List;

/**
 * <p>ServiceProfileWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceProfileWrapper extends ResourceImpl<ServiceProfile> implements ServiceProfile {

    private ServiceProfileJson jsonObject;
    @Getter
    private final Pageable<ServiceProfile> serviceClient;

    /**
     * <p>Constructor for ServiceProfileWrapper.</p>
     *
     * @param jsonObject a {@link api.equinix.javasdk.fabric.model.json.ServiceProfileJson} object.
     * @param serviceProfileClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public ServiceProfileWrapper(ServiceProfileJson jsonObject, Pageable<ServiceProfile> serviceProfileClient) {
        this.jsonObject = jsonObject;
        this.serviceClient = serviceProfileClient;
    }

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUuid() {
        return  this.jsonObject.getUuid();
    }

    /**
     * <p>getState.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.ServiceProfileState} object.
     */
    public ServiceProfileState getState() {
        return  this.jsonObject.getState();
    }

    /**
     * <p>getTags.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getTags() {
        return  this.jsonObject.getTags();
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return  this.jsonObject.getName();
    }

    /**
     * <p>getDescription.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDescription() {
        return  this.jsonObject.getDescription();
    }

    /**
     * <p>getType.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.ServiceProfileType} object.
     */
    public ServiceProfileType getType() {
        return  this.jsonObject.getType();
    }

    /**
     * <p>getNotifications.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Notification> getNotifications() {
        return  this.jsonObject.getNotifications();
    }

    /**
     * <p>getConfig.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.ServiceProfileConfig} object.
     */
    public ServiceProfileConfig getConfig() {
        return  this.jsonObject.getConfig();
    }

    /**
     * <p>getChangeLog.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.ChangeLog} object.
     */
    public ChangeLog getChangeLog() {
        return  this.jsonObject.getChangeLog();
    }

    /**
     * <p>getAccount.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Account} object.
     */
    public Account getAccount() {
        return  this.jsonObject.getAccount();
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.ServiceProfile} object.
     */
    public ServiceProfile refresh() {
        this.jsonObject = ((ServiceProfileClientImpl)serviceClient).refresh(this.getUuid());
        return this;
    }
}
