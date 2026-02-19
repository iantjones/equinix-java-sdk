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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.PortClientImpl;
import api.equinix.javasdk.fabric.enums.AccessPointType;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.implementation.PortOperation;
import api.equinix.javasdk.fabric.model.implementation.SimpleAccessPoint;
import api.equinix.javasdk.fabric.model.json.PortJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 * <p>PortWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PortWrapper extends ResourceImpl<Port> implements Port {

    @Delegate(excludes = PortMutability.class)
    private PortJson jsonObject;
    @Getter
    private final Pageable<Port> serviceClient;

    /**
     * <p>Constructor for PortWrapper.</p>
     *
     * @param portJson a {@link api.equinix.javasdk.fabric.model.json.PortJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public PortWrapper(PortJson portJson, Pageable<Port> serviceClient) {
        this.jsonObject = portJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getOperation.</p>
     *
     * @return a {@link PortOperation} object.
     */
    public PortOperation getOperation() {
        return this.jsonObject.getPortOperation();
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.Port} object.
     */
    public Port refresh() {
        this.jsonObject = ((PortClientImpl)this.serviceClient).refresh(this.getUuid());
        return this;
    }

    public SimpleAccessPoint accessPoint() {
        return SimpleAccessPoint.define(AccessPointType.COLO).port(this.getUuid()).create();
    }

    private interface PortMutability {
        PortOperation getPortOperation();
        Port refresh();
        SimpleAccessPoint accessPoint();
    }
}
