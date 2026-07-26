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
import com.eqixiac.equinix.networkedge.client.internal.implementation.DeviceLinkClientImpl;
import com.eqixiac.equinix.networkedge.model.DeviceLink;
import com.eqixiac.equinix.networkedge.model.json.DeviceLinkJson;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceLinkOperator;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceLinkUpdaterJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 *
 * @author ianjones
 */
public class DeviceLinkWrapper extends ResourceImpl<DeviceLink> implements DeviceLink {

    @Delegate
    private DeviceLinkJson json;

    @Getter
    private final Pageable<DeviceLink> serviceClient;

    public DeviceLinkWrapper(DeviceLinkJson deviceLinkJson, Pageable<DeviceLink> serviceClient) {
        this.json = deviceLinkJson;
        this.serviceClient = serviceClient;
    }

    public DeviceLinkOperator.DeviceLinkUpdater update() {
        return new DeviceLinkOperator(this.serviceClient).update(this.json);
    }

    public Boolean save(DeviceLinkUpdaterJson updaterJson) {
        this.json = ((DeviceLinkClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    public Boolean delete() {
        return ((DeviceLinkClientImpl)this.serviceClient).delete(this.getUuid());
    }

    public Boolean refresh() {
        this.json = ((DeviceLinkClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
