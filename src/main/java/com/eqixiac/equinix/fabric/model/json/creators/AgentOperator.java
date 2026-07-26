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

package com.eqixiac.equinix.fabric.model.json.creators;

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.AgentClientImpl;
import com.eqixiac.equinix.fabric.model.Agent;
import com.eqixiac.equinix.fabric.model.Project;
import com.eqixiac.equinix.fabric.model.json.AgentJson;
import com.eqixiac.equinix.fabric.model.wrappers.AgentWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder/updater for Fabric agents.
 *
 * @author ianjones
 */
public class AgentOperator extends ResourceImpl<Agent> {

    @Getter
    private final PageablePost<Agent> serviceClient;

    public AgentOperator(PageablePost<Agent> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public AgentBuilder create(String type) {
        return new AgentBuilder(type);
    }

    public AgentUpdater update(String uuid) {
        return new AgentUpdater(uuid);
    }

    @Getter(AccessLevel.PACKAGE)
    public class AgentBuilder {

        private final String type;
        private String name;
        private String description;
        private Boolean enabled;
        private Project project;
        private String agentTemplateUuid;
        private Map<String, Object> configuration;

        protected AgentBuilder(String type) {
            this.type = type;
        }

        public AgentBuilder name(String name) {
            this.name = name;
            return this;
        }

        public AgentBuilder description(String description) {
            this.description = description;
            return this;
        }

        public AgentBuilder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public AgentBuilder withProject(Project project) {
            this.project = project;
            return this;
        }

        public AgentBuilder agentTemplate(String agentTemplateUuid) {
            this.agentTemplateUuid = agentTemplateUuid;
            return this;
        }

        public AgentBuilder configuration(Map<String, Object> configuration) {
            this.configuration = configuration;
            return this;
        }

        public Agent create() {
            AgentCreatorJson creatorJson = new AgentCreatorJson(this);
            AgentJson agentJson = ((AgentClientImpl) AgentOperator.this.getServiceClient()).create(creatorJson);
            return new AgentWrapper(agentJson, AgentOperator.this.getServiceClient());
        }
    }

    /**
     * Fluent builder for PATCH-updating an existing agent.
     */
    public class AgentUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected AgentUpdater(String uuid) {
            this.uuid = uuid;
        }

        public AgentUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        public AgentUpdater description(String description) {
            operations.add(PatchOperation.replace("/description", description));
            return this;
        }

        public AgentUpdater enabled(Boolean enabled) {
            operations.add(PatchOperation.replace("/enabled", enabled));
            return this;
        }

        public AgentUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        public Agent save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            AgentJson agentJson = ((AgentClientImpl) AgentOperator.this.getServiceClient()).update(uuid, operations);
            return new AgentWrapper(agentJson, AgentOperator.this.getServiceClient());
        }
    }
}
