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
import api.equinix.javasdk.networkedge.client.internal.implementation.DeviceClientImpl;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.json.DeviceJson;
import api.equinix.javasdk.networkedge.model.json.Pricing;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceUpdaterJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 * <p>DeviceWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceWrapper extends ResourceImpl<Device> implements Device {

    @Delegate
    private DeviceJson json;
    @Getter
    private final Pageable<Device> serviceClient;

    /**
     * <p>Constructor for DeviceWrapper.</p>
     *
     * @param deviceJson a {@link api.equinix.javasdk.networkedge.model.json.DeviceJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public DeviceWrapper(DeviceJson deviceJson, Pageable<Device> serviceClient) {
        this.json = deviceJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getPricing.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.Pricing} object.
     */
    public Pricing getPricing() {
        return ((DeviceClientImpl)this.serviceClient).getPricing(this.getUuid());
    }

    /**
     * {@inheritDoc}
     *
     * <p>restoreFromBackup.</p>
     *
     * @param backup a {@link api.equinix.javasdk.networkedge.model.Backup} object.
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean restoreFromBackup(Backup backup){
        return restoreFromBackup(backup.getUuid());
    }

    /** {@inheritDoc} */
    public Boolean restoreFromBackup(String backupUuid){
        return ((DeviceClientImpl)this.serviceClient).restore(this.getUuid(), backupUuid);
    }

    /** {@inheritDoc} */
    public Boolean updateAdditionalBandwidth(Integer additionalBandwidth) {
        this.json =  ((DeviceClientImpl)this.serviceClient).updateAdditionalBandwidth(this.getUuid(), additionalBandwidth);
        return true;
    }

    /**
     * <p>postLicenseFile.</p>
     *
     * @param fileContents a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String postLicenseFile(String fileContents) {
        return ((DeviceClientImpl)this.serviceClient).postLicenseFile(this.getUuid(), fileContents);
    }

    /**
     * <p>updateLicenseToken.</p>
     *
     * @param licenseToken a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String updateLicenseToken(String licenseToken) {
        return ((DeviceClientImpl)this.serviceClient).updateLicenseToken(this.getUuid(), licenseToken);
    }

    /**
     * <p>ping.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean ping() {
        return ((DeviceClientImpl)this.serviceClient).ping(this.getUuid());
    }

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator.DeviceUpdater} object.
     */
    public DeviceOperator.DeviceUpdater update() {
        return new DeviceOperator(this.serviceClient).update(this.json);
    }

    /** {@inheritDoc} */
    public Boolean save(DeviceUpdaterJson updaterJson) {
        this.json = ((DeviceClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        return ((DeviceClientImpl)this.serviceClient).delete(this.getUuid());
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean refresh() {
        this.json = ((DeviceClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
