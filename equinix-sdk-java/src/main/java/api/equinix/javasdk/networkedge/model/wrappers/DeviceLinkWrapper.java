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
import api.equinix.javasdk.networkedge.client.internal.implementation.DeviceLinkClientImpl;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.json.DeviceLinkJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkOperator;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkUpdaterJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 * <p>DeviceLinkWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceLinkWrapper extends ResourceImpl<DeviceLink> implements DeviceLink {

    @Delegate
    private DeviceLinkJson json;

    @Getter
    private final Pageable<DeviceLink> serviceClient;

    /**
     * <p>Constructor for DeviceLinkWrapper.</p>
     *
     * @param deviceLinkJson a {@link api.equinix.javasdk.networkedge.model.json.DeviceLinkJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public DeviceLinkWrapper(DeviceLinkJson deviceLinkJson, Pageable<DeviceLink> serviceClient) {
        this.json = deviceLinkJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkOperator.DeviceLinkUpdater} object.
     */
    public DeviceLinkOperator.DeviceLinkUpdater update() {
        return new DeviceLinkOperator(this.serviceClient).update(this.json);
    }

    /** {@inheritDoc} */
    public Boolean save(DeviceLinkUpdaterJson updaterJson) {
        this.json = ((DeviceLinkClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        return ((DeviceLinkClientImpl)this.serviceClient).delete(this.getUuid());
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean refresh() {
        this.json = ((DeviceLinkClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
