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

package api.equinix.javasdk.internetaccess.client.implementation;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.client.RoutingConfigs;
import api.equinix.javasdk.internetaccess.client.internal.RoutingConfigClient;
import api.equinix.javasdk.internetaccess.model.RoutingConfig;
import api.equinix.javasdk.internetaccess.model.json.RoutingConfigJson;
import api.equinix.javasdk.internetaccess.model.json.creators.RoutingConfigOperator;
import api.equinix.javasdk.internetaccess.model.wrappers.RoutingConfigWrapper;

public class RoutingConfigsImpl implements RoutingConfigs {

    private final InternetAccess serviceManager;

    private final RoutingConfigClient<RoutingConfig> serviceClient;

    public RoutingConfigsImpl(RoutingConfigClient<RoutingConfig> serviceClient, InternetAccess serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<RoutingConfig> list() {
        Page<RoutingConfig, RoutingConfigJson> responsePage = this.serviceClient.list();
        PaginatedList<RoutingConfig> routingConfigList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, RoutingConfigWrapper::new);
        return new PaginatedList<>(routingConfigList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RoutingConfig getByUuid(String uuid) {
        RoutingConfigJson routingConfigJson = this.serviceClient.getByUuid(uuid);
        return new RoutingConfigWrapper(routingConfigJson, this.serviceClient);
    }

    public RoutingConfigOperator.RoutingConfigBuilder define() {
        return new RoutingConfigOperator(this.serviceClient).create();
    }
}
