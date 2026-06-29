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
import api.equinix.javasdk.networkedge.model.DeviceType;
import api.equinix.javasdk.networkedge.model.json.DeviceTypeJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 * <p>DeviceTypeWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceTypeWrapper extends ResourceImpl<DeviceType> implements DeviceType {

    @Delegate
    private final DeviceTypeJson jsonObject;
    @Getter
    private final Pageable<DeviceType> serviceClient;

    /**
     * <p>Constructor for DeviceTypeWrapper.</p>
     *
     * @param deviceTypeJson a {@link api.equinix.javasdk.networkedge.model.json.DeviceTypeJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public DeviceTypeWrapper(DeviceTypeJson deviceTypeJson, Pageable<DeviceType> serviceClient) {
        this.jsonObject = deviceTypeJson;
        this.serviceClient = serviceClient;
    }
}
