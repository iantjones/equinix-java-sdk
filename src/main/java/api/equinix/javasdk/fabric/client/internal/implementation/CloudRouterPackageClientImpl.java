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
import api.equinix.javasdk.fabric.client.internal.CloudRouterPackageClient;
import api.equinix.javasdk.fabric.enums.CloudRouterPackageCode;
import api.equinix.javasdk.fabric.model.CloudRouterPackage;
import api.equinix.javasdk.fabric.model.json.CloudRouterPackageJson;

import java.util.Map;

/**
 * Internal client for Fabric Cloud Router packages (read-only). The JSON model implements the
 * public interface directly, so {@link #wrap(CloudRouterPackageJson)} is the identity.
 *
 * @author ianjones
 */
public class CloudRouterPackageClientImpl extends ResourceClientBase<CloudRouterPackage, CloudRouterPackageJson> implements CloudRouterPackageClient<CloudRouterPackage> {

    public CloudRouterPackageClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "CloudRouters", CloudRouterPackageJson.class);
    }

    @Override
    protected CloudRouterPackage wrap(CloudRouterPackageJson json) {
        return json;
    }

    public Page<CloudRouterPackageJson> list() {
        return listPage("GetCloudRouterPackages");
    }

    public CloudRouterPackageJson getByPackageCode(CloudRouterPackageCode packageCode) {
        return getOne("GetCloudRouterPackage", Map.of("routerPackageCode", packageCode.toString()));
    }
}
