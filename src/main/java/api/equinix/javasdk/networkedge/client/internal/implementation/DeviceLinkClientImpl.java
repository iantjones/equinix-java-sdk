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

package api.equinix.javasdk.networkedge.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.DeviceLinkClient;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.implementation.UUIDResult;
import api.equinix.javasdk.networkedge.model.json.DeviceLinkJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkUpdaterJson;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceLinkWrapper;

import java.util.List;
import java.util.Map;

/**
 *
 * @author ianjones
 */
public class DeviceLinkClientImpl extends ResourceClientBase<DeviceLink, DeviceLinkJson> implements DeviceLinkClient<DeviceLink> {

    public DeviceLinkClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "DeviceLinks", DeviceLinkJson.class);
    }

    @Override
    protected DeviceLink wrap(DeviceLinkJson json) {
        return new DeviceLinkWrapper(json, this);
    }

    /**
     * {@inheritDoc}
     *
     */
    public Page<DeviceLink, DeviceLinkJson> list(RequestBuilder.DeviceLink requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        return listPage("ListDeviceLinks", qParams);
    }

    public DeviceLinkJson getByUuid(String uuid) {
        return getOne("GetDeviceLink", uuid);
    }

    public DeviceLinkJson create(DeviceLinkCreatorJson deviceLinkCreatorJson) {
        UUIDResult uuidResult = postForType("CreateDeviceLink", deviceLinkCreatorJson, DeviceLinkJson.getCreateTypeRef());
        return getByUuid(uuidResult.getUuid());
    }

    public DeviceLinkJson update(String uuid, DeviceLinkUpdaterJson deviceLinkUpdaterJson) {
        voidOp("UpdateDeviceLink", RequestType.SINGLE, Map.of("uuid", uuid), null, deviceLinkUpdaterJson);
        return getByUuid(uuid);
    }

    public Boolean delete(String uuid) {
        return booleanOp("DeleteDeviceLink", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public DeviceLinkJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
