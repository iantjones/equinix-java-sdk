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

package api.equinix.javasdk.networkedge.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.networkedge.client.internal.implementation.DeviceLinkClientImpl;
import api.equinix.javasdk.networkedge.enums.DeviceLinkStatus;
import api.equinix.javasdk.networkedge.enums.Source;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkOperator;
import api.equinix.javasdk.networkedge.model.implementation.DeviceLinkSupportDetail;
import api.equinix.javasdk.networkedge.model.implementation.Link;
import api.equinix.javasdk.networkedge.model.implementation.LinkDevice;
import api.equinix.javasdk.networkedge.model.json.DeviceLinkJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkUpdaterJson;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>DeviceLinkWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceLinkWrapper extends ResourceImpl<DeviceLink> implements DeviceLink {

    private DeviceLinkJson json;

    @Getter
    private final Pageable<DeviceLink> serviceClient;

    /**
     * <p>Constructor for DeviceLinkWrapper.</p>
     *
     * @param deviceLinkJson a {@link api.equinix.javasdk.networkedge.model.json.DeviceLinkJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public DeviceLinkWrapper(DeviceLinkJson deviceLinkJson, Pageable<DeviceLink> serviceClient) {
        this.json = deviceLinkJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUuid() {
        return  this.json.getUuid();
    }

    /**
     * <p>getSource.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.Source} object.
     */
    public Source getSource() {
        return  this.json.getSource();
    }

    /**
     * <p>getGroupName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getGroupName() {
        return  this.json.getGroupName();
    }

    /**
     * <p>getSubnet.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSubnet() {
        return  this.json.getSubnet();
    }

    /**
     * <p>getStatus.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.DeviceLinkStatus} object.
     */
    public DeviceLinkStatus getStatus() {
        return  this.json.getStatus();
    }

    /**
     * <p>getLinks.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Link> getLinks() {
        return  this.json.getLinks();
    }

    /**
     * <p>getLinkDevices.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<LinkDevice> getLinkDevices() {
        return  this.json.getLinkDevices();
    }

    /**
     * <p>getSupportDetails.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<DeviceLinkSupportDetail> getSupportDetails() {
        return  this.json.getSupportDetails();
    }

    /**
     * <p>getCreatedBy.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCreatedBy() {
        return  this.json.getCreatedBy();
    }

    /**
     * <p>getCreatedDate.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    public LocalDateTime getCreatedDate() {
        return  this.json.getCreatedDate();
    }

    /**
     * <p>getLastUpdatedBy.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLastUpdatedBy() {
        return  this.json.getLastUpdatedBy();
    }

    /**
     * <p>getLastUpdatedDate.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    public LocalDateTime getLastUpdatedDate() {
        return  this.json.getLastUpdatedDate();
    }

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkOperator.DeviceLinkUpdater} object.
     */
    public DeviceLinkOperator.DeviceLinkUpdater update() {
        return new DeviceLinkOperator(this.serviceClient).update(this.json);
    }

    /** {@inheritDoc} */
    public Boolean save(DeviceLinkUpdaterJson updaterJson) {
        this.json = ((DeviceLinkClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        return ((DeviceLinkClientImpl)this.serviceClient).delete(this.getUuid());
    }
    
    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean refresh() {
        this.json = ((DeviceLinkClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
