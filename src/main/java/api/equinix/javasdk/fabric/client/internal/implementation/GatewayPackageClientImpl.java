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
import api.equinix.javasdk.fabric.client.internal.GatewayPackageClient;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.model.GatewayPackage;
import api.equinix.javasdk.fabric.model.json.GatewayPackageJson;
import api.equinix.javasdk.fabric.model.wrappers.GatewayPackageWrapper;

import java.util.Map;

public class GatewayPackageClientImpl extends ResourceClientBase<GatewayPackage, GatewayPackageJson> implements GatewayPackageClient<GatewayPackage> {

    public GatewayPackageClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "FabricGateways", GatewayPackageJson.class);
    }

    @Override
    protected GatewayPackage wrap(GatewayPackageJson json) {
        return new GatewayPackageWrapper(json, this);
    }

    public Page<GatewayPackage, GatewayPackageJson> list() {
        return listPage("GetGatewayPackages");
    }

    public GatewayPackageJson getByPackageCode(GatewayPackageCode gatewayPackageCode) {
        return getOne("GetGatewayPackage", Map.of("gatewayPackageCode", gatewayPackageCode.toString()));
    }
}
