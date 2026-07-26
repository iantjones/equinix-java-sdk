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

package com.eqixiac.equinix.networkedge.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.networkedge.client.implementation.NetworkEdgeConfigImpl;
import com.eqixiac.equinix.networkedge.client.internal.DeviceTypeClient;
import com.eqixiac.equinix.networkedge.model.DeviceType;
import com.eqixiac.equinix.networkedge.model.json.DeviceTypeJson;
import com.eqixiac.equinix.networkedge.model.wrappers.DeviceTypeWrapper;

/**
 *
 * @author ianjones
 */
public class DeviceTypeClientImpl extends ResourceClientBase<DeviceType, DeviceTypeJson> implements DeviceTypeClient<DeviceType> {

    public DeviceTypeClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Devices", DeviceTypeJson.class);
    }

    @Override
    protected DeviceType wrap(DeviceTypeJson json) {
        return new DeviceTypeWrapper(json, this);
    }

    public Page<DeviceTypeJson> list() {
        return listPage("ListDeviceTypes");
    }
}
