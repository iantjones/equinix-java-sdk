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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.Agent;
import api.equinix.javasdk.fabric.model.AgentActivity;
import api.equinix.javasdk.fabric.model.json.creators.AgentOperator;

import java.util.List;

/**
 * Client interface for managing Equinix Fabric agents.
 */
public interface Agents {

    /**
     * Lists all agents accessible to the current account.
     *
     * @return a paginated list of agents
     */
    PaginatedList<Agent> list();

    /**
     * Retrieves a single agent by its unique identifier.
     *
     * @param uuid the unique identifier of the agent
     * @return the agent matching the given UUID
     */
    Agent getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new agent.
     *
     * @param type the agent type (for example {@code ANO_AGENT})
     * @return a builder for configuring the new agent
     */
    AgentOperator.AgentBuilder define(String type);

    /**
     * Lists the activities (operations) performed by an agent.
     *
     * @param agentId the unique identifier of the agent
     * @return the list of agent activities
     */
    List<AgentActivity> activities(String agentId);
}
