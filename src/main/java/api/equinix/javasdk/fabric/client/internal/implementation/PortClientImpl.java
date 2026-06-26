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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.PortClient;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.json.PortJson;
import api.equinix.javasdk.fabric.model.wrappers.PortWrapper;

/**
 * Internal client for Fabric Ports (read-only). Plumbing/paging provided by {@link ResourceClientBase}.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PortClientImpl extends ResourceClientBase<Port, PortJson> implements PortClient<Port> {

    public PortClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Ports", PortJson.class);
    }

    @Override
    protected Port wrap(PortJson json) {
        return new PortWrapper(json, this);
    }

    public Page<Port, PortJson> list() {
        return listPage("GetPorts");
    }

    public PortJson getByUuid(String uuid) {
        return getOne("GetPort", uuid);
    }

    public PortJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
