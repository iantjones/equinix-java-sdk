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

package api.equinix.javasdk.networkedge.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.json.DeviceLinkJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkCreatorJson;

/**
 * <p>DeviceLinkClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface DeviceLinkClient<T> extends Pageable<T> {

    /**
     * <p>list.</p>
     *
     * @param requestBuilder a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.DeviceLink} object.
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    Page<DeviceLink, DeviceLinkJson> list(RequestBuilder.DeviceLink requestBuilder);

    /**
     * <p>getByUuid.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.DeviceLinkJson} object.
     */
    DeviceLinkJson getByUuid(String uuid);

    /**
     * <p>create.</p>
     *
     * @param deviceLinkCreatorJson a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkCreatorJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.DeviceLinkJson} object.
     */
    DeviceLinkJson create(DeviceLinkCreatorJson deviceLinkCreatorJson);

    /**
     * <p>delete.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean delete(String uuid);

    /**
     * <p>refresh.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.DeviceLinkJson} object.
     */
    DeviceLinkJson refresh(String uuid);
}
