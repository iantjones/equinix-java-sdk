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
import api.equinix.javasdk.core.model.ResourcePostImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.ServiceProfileClientImpl;
import api.equinix.javasdk.fabric.enums.AccessPointType;
import api.equinix.javasdk.fabric.enums.ServiceProfileState;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.enums.ServiceProfileVisibility;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.*;
import api.equinix.javasdk.fabric.model.json.MetroJson;
import api.equinix.javasdk.fabric.model.json.ServiceProfileJson;
import lombok.Getter;

import java.util.List;

public class ServiceProfileWrapper extends ResourcePostImpl<ServiceProfile> implements ServiceProfile {

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
        return this.jsonObject.getUuid();
    }

    /**
     * <p>getState.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.ServiceProfileState} object.
     */
    public ServiceProfileState getState() {
        return this.jsonObject.getState();
    }

    @Override
    public String getHref() {
        return this.jsonObject.getHref();
    }

    /**
     * <p>getTags.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getTags() {
        return this.jsonObject.getTags();
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return this.jsonObject.getName();
    }

    /**
     * <p>getDescription.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDescription() {
        return this.jsonObject.getDescription();
    }

    @Override
    public ServiceProfileVisibility getVisibility() {
        return this.jsonObject.getVisibility();
    }

    /**
     * <p>getType.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.ServiceProfileType} object.
     */
    public ServiceProfileType getType() {
        return this.jsonObject.getType();
    }

    /**
     * <p>getNotifications.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Notification> getNotifications() {
        return this.jsonObject.getNotifications();
    }

    @Override
    public List<AccessPointTypeConfig> getAccessPointTypeConfigs() {
        return this.jsonObject.getAccessPointTypeConfigs();
    }

    @Override
    public List<ServiceProfileMetro> metros() {
        return this.jsonObject.getMetros();
    }

    @Override
    public MarketingInfo getMarketingInfo() {
        return this.jsonObject.getMarketingInfo();
    }

    @Override
    public Boolean getSelfProfile() {
        return this.jsonObject.getSelfProfile();
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
     * <p>getAccount.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Account} object.
     */
    public Account getAccount() {
        return this.jsonObject.getAccount();
    }

    public List<CustomField> getCustomFields() {
        return this.jsonObject.getCustomFields();
    }

    public List<String> getAllowedEmails() {
        return this.jsonObject.getAllowedEmails();
    }

    public Boolean delete() {
        this.jsonObject = ((ServiceProfileClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((ServiceProfileClientImpl)serviceClient).refresh(this.getUuid());
    }

    public SimpleAccessPoint accessPoint() {
        return SimpleAccessPoint.define(AccessPointType.SP).port(this.getUuid()).create();
    }
}
