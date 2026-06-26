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

package api.equinix.javasdk.internetaccess.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.RoutingConfigClient;
import api.equinix.javasdk.internetaccess.model.RoutingConfig;
import api.equinix.javasdk.internetaccess.model.json.RoutingConfigJson;
import api.equinix.javasdk.internetaccess.model.json.creators.RoutingConfigCreatorJson;
import api.equinix.javasdk.internetaccess.model.wrappers.RoutingConfigWrapper;

public class RoutingConfigClientImpl extends ResourceClientBase<RoutingConfig, RoutingConfigJson> implements RoutingConfigClient<RoutingConfig> {

    public RoutingConfigClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "RoutingConfigs", RoutingConfigJson.class);
    }

    @Override
    protected RoutingConfig wrap(RoutingConfigJson json) {
        return new RoutingConfigWrapper(json, this);
    }

    public Page<RoutingConfig, RoutingConfigJson> list() {
        return listPage("ListRoutingConfigs");
    }

    public RoutingConfigJson getByUuid(String uuid) {
        return getOne("GetRoutingConfig", uuid);
    }

    public RoutingConfigJson create(RoutingConfigCreatorJson routingConfigCreatorJson) {
        return postOne("CreateRoutingConfig", routingConfigCreatorJson);
    }

    public RoutingConfigJson update(String uuid, RoutingConfigCreatorJson routingConfigCreatorJson) {
        return updateOne("UpdateRoutingConfig", uuid, routingConfigCreatorJson);
    }

    public RoutingConfigJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
