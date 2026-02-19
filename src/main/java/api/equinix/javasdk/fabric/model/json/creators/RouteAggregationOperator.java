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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.RouteAggregationClientImpl;
import api.equinix.javasdk.fabric.enums.RouteAggregationType;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.RouteAggregation;
import api.equinix.javasdk.fabric.model.json.RouteAggregationJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteAggregationWrapper;
import lombok.Getter;

public class RouteAggregationOperator extends ResourceImpl<RouteAggregation> {

    @Getter
    private final Pageable<RouteAggregation> serviceClient;

    public RouteAggregationOperator(Pageable<RouteAggregation> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public RouteAggregationBuilder create() {
        return new RouteAggregationBuilder();
    }

    @Getter
    public class RouteAggregationBuilder {

        private RouteAggregationType type;
        private String name;
        private String description;
        private String projectId;

        protected RouteAggregationBuilder() {
        }

        public RouteAggregationOperator.RouteAggregationBuilder ofType(RouteAggregationType type) {
            this.type = type;
            return this;
        }

        public RouteAggregationOperator.RouteAggregationBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public RouteAggregationOperator.RouteAggregationBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public RouteAggregationOperator.RouteAggregationBuilder withProjectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public RouteAggregationOperator.RouteAggregationBuilder withProject(Project project) {
            return withProjectId(project.getProjectId());
        }

        public RouteAggregation create() {
            RouteAggregationCreatorJson routeAggregationCreatorJson = new RouteAggregationCreatorJson(this);
            RouteAggregationJson routeAggregationJson = ((RouteAggregationClientImpl) RouteAggregationOperator.this.getServiceClient()).create(routeAggregationCreatorJson);
            return new RouteAggregationWrapper(routeAggregationJson, RouteAggregationOperator.this.getServiceClient());
        }
    }
}
