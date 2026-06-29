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

package api.equinix.javasdk.fabric.model.json.creators;

import api.equinix.javasdk.fabric.client.internal.CloudRouterCommandClient;
import api.equinix.javasdk.fabric.enums.CloudRouterCommandType;
import api.equinix.javasdk.fabric.model.CloudRouterCommand;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.CloudRouterCommandRequest;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Fluent builder for issuing a Fabric Cloud Router diagnostic command (ping / traceroute).
 *
 * @author ianjones
 */
public class CloudRouterCommandOperator {

    @Getter
    private final CloudRouterCommandClient<CloudRouterCommand> serviceClient;

    private final String routerId;

    public CloudRouterCommandOperator(CloudRouterCommandClient<CloudRouterCommand> serviceClient, String routerId) {
        this.serviceClient = serviceClient;
        this.routerId = routerId;
    }

    public CloudRouterCommandBuilder create() {
        return new CloudRouterCommandBuilder();
    }

    @Getter(AccessLevel.PUBLIC)
    public class CloudRouterCommandBuilder {

        private CloudRouterCommandType type;
        private String name;
        private String description;
        private Project project;
        private CloudRouterCommandRequest request;

        protected CloudRouterCommandBuilder() {
        }

        public CloudRouterCommandBuilder ofType(CloudRouterCommandType type) {
            this.type = type;
            return this;
        }

        public CloudRouterCommandBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CloudRouterCommandBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CloudRouterCommandBuilder withProject(Project project) {
            this.project = project;
            return this;
        }

        public CloudRouterCommandBuilder withRequest(CloudRouterCommandRequest request) {
            this.request = request;
            return this;
        }

        public CloudRouterCommand create() {
            CloudRouterCommandCreatorJson creatorJson = new CloudRouterCommandCreatorJson(this);
            return CloudRouterCommandOperator.this.serviceClient.create(CloudRouterCommandOperator.this.routerId, creatorJson);
        }
    }
}
