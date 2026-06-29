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

package api.equinix.javasdk.networkedge.client.implementation;

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.networkedge.client.DeviceLinks;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.internal.DeviceLinkClient;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkOperator;
import api.equinix.javasdk.networkedge.model.json.DeviceLinkJson;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceLinkWrapper;
import lombok.Getter;

/**
 * <p>DeviceLinksImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class DeviceLinksImpl implements DeviceLinks {

    private final NetworkEdge serviceManager;

    private final DeviceLinkClient<DeviceLink> serviceClient;

    /**
     * <p>Constructor for DeviceLinksImpl.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.networkedge.client.internal.DeviceLinkClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.NetworkEdge} object.
     */
    public DeviceLinksImpl(DeviceLinkClient<DeviceLink> serviceClient, NetworkEdge serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<DeviceLink> list() {
        return list(null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public PaginatedList<DeviceLink> list(RequestBuilder.DeviceLink requestBuilder) {
        Page<DeviceLink, DeviceLinkJson> responsePage = serviceClient.list(requestBuilder);
        PaginatedList<DeviceLink> deviceList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, DeviceLinkWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /** {@inheritDoc} */
    public DeviceLink getByUuid(String uuid) {
        DeviceLinkJson deviceLinkJson = serviceClient.getByUuid(uuid);
        return new DeviceLinkWrapper(deviceLinkJson, this.serviceClient);
    }

    /** {@inheritDoc} */
    public DeviceLinkOperator.DeviceLinkBuilder define(String groupName) {
        return new DeviceLinkOperator(this.serviceClient).create(groupName);
    }
}
