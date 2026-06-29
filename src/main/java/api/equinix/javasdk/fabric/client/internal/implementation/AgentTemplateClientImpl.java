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
import api.equinix.javasdk.fabric.client.internal.AgentTemplateClient;
import api.equinix.javasdk.fabric.model.AgentTemplate;
import api.equinix.javasdk.fabric.model.json.AgentTemplateJson;

/**
 * Internal client for Fabric Agent Templates (read-only). The JSON model implements the public
 * interface directly, so {@link #wrap(AgentTemplateJson)} is the identity.
 *
 * @author ianjones
 */
public class AgentTemplateClientImpl extends ResourceClientBase<AgentTemplate, AgentTemplateJson> implements AgentTemplateClient<AgentTemplate> {

    public AgentTemplateClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "AgentTemplates", AgentTemplateJson.class);
    }

    @Override
    protected AgentTemplate wrap(AgentTemplateJson json) {
        return json;
    }

    public Page<AgentTemplate, AgentTemplateJson> list() {
        return listPage("GetAgentTemplates");
    }

    public AgentTemplateJson getByUuid(String uuid) {
        return getOne("GetAgentTemplate", uuid);
    }

    public AgentTemplateJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
