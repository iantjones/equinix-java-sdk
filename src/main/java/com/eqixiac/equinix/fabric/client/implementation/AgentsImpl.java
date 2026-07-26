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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.client.Agents;
import com.eqixiac.equinix.fabric.client.internal.AgentClient;
import com.eqixiac.equinix.fabric.model.Agent;
import com.eqixiac.equinix.fabric.model.AgentActivity;
import com.eqixiac.equinix.fabric.model.json.AgentJson;
import com.eqixiac.equinix.fabric.model.json.creators.AgentOperator;
import com.eqixiac.equinix.fabric.model.wrappers.AgentWrapper;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AgentsImpl implements Agents {

    private final AgentClient<Agent> serviceClient;

    public PaginatedList<Agent> list() {
        Page<AgentJson> responsePage = this.serviceClient.list();
        PaginatedList<Agent> agentList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, AgentWrapper::new);
        return new PaginatedList<>(agentList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Agent getByUuid(String uuid) {
        AgentJson agentJson = this.serviceClient.getByUuid(uuid);
        return new AgentWrapper(agentJson, this.serviceClient);
    }

    public AgentOperator.AgentBuilder define(String type) {
        return new AgentOperator(this.serviceClient).create(type);
    }

    public List<AgentActivity> activities(String agentId) {
        return this.serviceClient.activities(agentId);
    }
}
