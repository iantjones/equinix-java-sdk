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
import api.equinix.javasdk.fabric.client.internal.CloudRouterCommandClient;
import api.equinix.javasdk.fabric.model.CloudRouterCommand;
import api.equinix.javasdk.fabric.model.json.CloudRouterCommandJson;
import api.equinix.javasdk.fabric.model.json.creators.CloudRouterCommandCreatorJson;

import java.util.Map;

/**
 * Internal client for Fabric Cloud Router diagnostic commands (ping / traceroute). The JSON model
 * implements the public interface directly, so {@link #wrap(CloudRouterCommandJson)} is the identity.
 *
 * @author ianjones
 */
public class CloudRouterCommandClientImpl extends ResourceClientBase<CloudRouterCommand, CloudRouterCommandJson> implements CloudRouterCommandClient<CloudRouterCommand> {

    public CloudRouterCommandClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "CloudRouters", CloudRouterCommandJson.class);
    }

    @Override
    protected CloudRouterCommand wrap(CloudRouterCommandJson json) {
        return json;
    }

    public Page<CloudRouterCommand, CloudRouterCommandJson> list(String routerId) {
        return listPagePath("GetCloudRouterCommands", Map.of("routerId", routerId));
    }

    public CloudRouterCommandJson getByUuid(String routerId, String uuid) {
        return getOne("GetCloudRouterCommand", Map.of("routerId", routerId, "uuid", uuid));
    }

    public CloudRouterCommandJson create(String routerId, CloudRouterCommandCreatorJson creatorJson) {
        return postOne("PostCloudRouterCommand", Map.of("routerId", routerId), creatorJson);
    }

    public CloudRouterCommandJson delete(String routerId, String uuid) {
        return deleteOne("DeleteCloudRouterCommand", Map.of("routerId", routerId, "uuid", uuid));
    }

    public CloudRouterCommandJson refresh(String routerId, String uuid) {
        return getByUuid(routerId, uuid);
    }
}
