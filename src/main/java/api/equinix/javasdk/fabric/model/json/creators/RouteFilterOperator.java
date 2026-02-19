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

import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.RouteFilterClientImpl;
import api.equinix.javasdk.fabric.enums.RouteFilterAction;
import api.equinix.javasdk.fabric.enums.RouteFilterType;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.RouteFilter;
import api.equinix.javasdk.fabric.model.json.RouteFilterJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteFilterWrapper;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * <p>RouteFilterOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class RouteFilterOperator extends ResourceImpl<RouteFilter> {

    @Getter
    private final PageablePost<RouteFilter> serviceClient;

    /**
     * <p>Constructor for RouteFilterOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.PageablePost} object.
     */
    public RouteFilterOperator(PageablePost<RouteFilter> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.RouteFilterOperator.RouteFilterBuilder} object.
     */
    public RouteFilterBuilder create() {
        return new RouteFilterBuilder();
    }

    @Getter(AccessLevel.PACKAGE)
    public class RouteFilterBuilder {

        private RouteFilterType type;
        private String name;
        private String description;
        private Project project;
        private RouteFilterAction notMatchedRuleAction;

        protected RouteFilterBuilder() {
        }

        public RouteFilterOperator.RouteFilterBuilder ofType(RouteFilterType type) {
            this.type = type;
            return this;
        }

        public RouteFilterOperator.RouteFilterBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RouteFilterOperator.RouteFilterBuilder description(String description) {
            this.description = description;
            return this;
        }

        public RouteFilterOperator.RouteFilterBuilder withProject(Project project) {
            this.project = project;
            return this;
        }

        public RouteFilterOperator.RouteFilterBuilder notMatchedRuleAction(RouteFilterAction notMatchedRuleAction) {
            this.notMatchedRuleAction = notMatchedRuleAction;
            return this;
        }

        public RouteFilter create() {
            RouteFilterCreatorJson routeFilterCreatorJson = new RouteFilterCreatorJson(this);
            RouteFilterJson routeFilterJson = ((RouteFilterClientImpl) RouteFilterOperator.this.getServiceClient()).create(routeFilterCreatorJson);
            return new RouteFilterWrapper(routeFilterJson, RouteFilterOperator.this.getServiceClient());
        }
    }
}
