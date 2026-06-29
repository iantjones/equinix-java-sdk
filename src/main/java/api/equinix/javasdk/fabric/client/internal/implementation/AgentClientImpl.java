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
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.AgentClient;
import api.equinix.javasdk.fabric.model.Agent;
import api.equinix.javasdk.fabric.model.AgentActivity;
import api.equinix.javasdk.fabric.model.json.AgentActivityJson;
import api.equinix.javasdk.fabric.model.json.AgentJson;
import api.equinix.javasdk.fabric.model.json.creators.AgentCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.AgentWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AgentClientImpl extends ResourceClientBase<Agent, AgentJson> implements AgentClient<Agent> {

    public AgentClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Agents", AgentJson.class);
    }

    @Override
    protected Agent wrap(AgentJson json) {
        return new AgentWrapper(json, this);
    }

    public Page<Agent, AgentJson> list() {
        return listPage("GetAgents");
    }

    public AgentJson getByUuid(String uuid) {
        return getOne("GetAgent", uuid);
    }

    public AgentJson create(AgentCreatorJson agentCreatorJson) {
        return postOne("PostAgent", agentCreatorJson);
    }

    public AgentJson update(String uuid, List<PatchOperation> operations) {
        return patchOne("PatchAgent", uuid, operations);
    }

    public AgentJson delete(String uuid) {
        return deleteOne("DeleteAgent", uuid);
    }

    public List<AgentActivity> activities(String agentId) {
        EquinixRequest<AgentActivity> request = buildRequestWithPathParams("GetAgentActivities", RequestType.PAGINATED,
                Map.of("agentId", agentId), AgentActivityJson.class);
        Page<AgentActivity, AgentActivityJson> page = Utils.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public AgentJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
