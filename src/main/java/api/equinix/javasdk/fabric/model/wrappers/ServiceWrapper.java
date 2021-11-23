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
import api.equinix.javasdk.fabric.client.internal.implementation.ServiceClientImpl;
import api.equinix.javasdk.fabric.enums.ServiceProfileState;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.model.implementation.Account;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.ServiceConfig;
import api.equinix.javasdk.fabric.model.Service;
import api.equinix.javasdk.fabric.model.json.ServiceJson;
import lombok.Getter;

import java.util.List;

/**
 * <p>ServiceWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceWrapper extends ResourceImpl<Service> implements Service {

    private ServiceJson jsonObject;
    @Getter
    private final Pageable<Service> serviceClient;

    /**
     * <p>Constructor for ServiceWrapper.</p>
     *
     * @param jsonObject a {@link api.equinix.javasdk.fabric.model.json.ServiceJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public ServiceWrapper(ServiceJson jsonObject, Pageable<Service> serviceClient) {
        this.jsonObject = jsonObject;
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
     * <p>getConfig.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.ServiceConfig} object.
     */
    public ServiceConfig getConfig() {
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
     * @return a {@link api.equinix.javasdk.fabric.model.Service} object.
     */
    public Service refresh() {
        this.jsonObject = ((ServiceClientImpl)serviceClient).refresh(this.getUuid());
        return this;
    }
}
