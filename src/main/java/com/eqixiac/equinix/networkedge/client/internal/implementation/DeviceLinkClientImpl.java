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
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.client.implementation.NetworkEdgeConfigImpl;
import com.eqixiac.equinix.networkedge.client.internal.DeviceLinkClient;
import com.eqixiac.equinix.networkedge.model.DeviceLink;
import com.eqixiac.equinix.networkedge.model.implementation.UUIDResult;
import com.eqixiac.equinix.networkedge.model.json.DeviceLinkJson;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceLinkCreatorJson;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceLinkUpdaterJson;
import com.eqixiac.equinix.networkedge.model.wrappers.DeviceLinkWrapper;

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
    public Page<DeviceLinkJson> list(RequestBuilder.DeviceLink requestBuilder) {
        Map<String, List<String>> qParams = ParameterMapper.newMap(requestBuilder);
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
