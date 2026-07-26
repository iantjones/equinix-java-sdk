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

package com.eqixiac.equinix.networkedge.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.networkedge.client.internal.implementation.DeviceClientImpl;
import com.eqixiac.equinix.networkedge.model.Backup;
import com.eqixiac.equinix.networkedge.model.Device;
import com.eqixiac.equinix.networkedge.model.json.DeviceJson;
import com.eqixiac.equinix.networkedge.model.json.Pricing;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceOperator;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceUpdaterJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 *
 * @author ianjones
 */
public class DeviceWrapper extends ResourceImpl<Device> implements Device {

    @Delegate
    private DeviceJson json;
    @Getter
    private final Pageable<Device> serviceClient;

    public DeviceWrapper(DeviceJson deviceJson, Pageable<Device> serviceClient) {
        this.json = deviceJson;
        this.serviceClient = serviceClient;
    }

    public Pricing getPricing() {
        return ((DeviceClientImpl)this.serviceClient).getPricing(this.getUuid());
    }

    /**
     * {@inheritDoc}
     *
     *
     */
    public Boolean restoreFromBackup(Backup backup){
        return restoreFromBackup(backup.getUuid(), backup.getName());
    }

    public Boolean restoreFromBackup(String backupUuid, String backupName){
        return ((DeviceClientImpl)this.serviceClient).restore(backupUuid, backupName);
    }

    public Boolean updateAdditionalBandwidth(Integer additionalBandwidth) {
        this.json =  ((DeviceClientImpl)this.serviceClient).updateAdditionalBandwidth(this.getUuid(), additionalBandwidth);
        return true;
    }

    public String postLicenseFile(String fileContents) {
        return ((DeviceClientImpl)this.serviceClient).postLicenseFile(this.getUuid(), fileContents);
    }

    public String updateLicenseToken(String licenseToken) {
        return ((DeviceClientImpl)this.serviceClient).updateLicenseToken(this.getUuid(), licenseToken);
    }

    public Boolean ping() {
        return ((DeviceClientImpl)this.serviceClient).ping(this.getUuid());
    }

    public DeviceOperator.DeviceUpdater update() {
        return new DeviceOperator(this.serviceClient).update(this.json);
    }

    public Boolean save(DeviceUpdaterJson updaterJson) {
        this.json = ((DeviceClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    public Boolean delete() {
        return ((DeviceClientImpl)this.serviceClient).delete(this.getUuid());
    }

    public Boolean refresh() {
        this.json = ((DeviceClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
