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

package com.eqixiac.equinix.networkedge.client;

import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.networkedge.model.ACLTemplate;
import com.eqixiac.equinix.networkedge.model.BGPPeering;
import com.eqixiac.equinix.networkedge.model.DeviceLink;
import com.eqixiac.equinix.networkedge.model.json.creators.ACLTemplateOperator;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceLinkOperator;

/**
 * Client interface for managing device link groups on Network Edge. Provides operations
 * to list, retrieve, and create device links that establish connectivity between
 * virtual devices across metros.
 *
 * @author ianjones
 */
public interface DeviceLinks {

    /**
     * Lists available Device Links.
     *
     * @return {@link com.eqixiac.equinix.core.http.response.PaginatedList}
     */
    PaginatedList<DeviceLink> list();

    /**
     * Lists Device Links based on the parameters provided.
     *
     * @param requestBuilder the desired query parameters.
     * @return {@link com.eqixiac.equinix.core.http.response.PaginatedList}
     */
    PaginatedList<DeviceLink> list(RequestBuilder.DeviceLink requestBuilder);

    /**
     * Gets the specified Device Link.
     *
     * @param uuid the unique identifier of the Device Link.
     * @return {@link com.eqixiac.equinix.networkedge.model.DeviceLink}
     */
    DeviceLink getByUuid(String uuid);

    /**
     * Returns an instance of DeviceLinkBuilder for defining a new Device Link.
     *
     * @param groupName the name of the new Device Link group.
     * @return {@link com.eqixiac.equinix.networkedge.model.json.creators.DeviceLinkOperator.DeviceLinkBuilder}
     */
    DeviceLinkOperator.DeviceLinkBuilder define(String groupName);
}
