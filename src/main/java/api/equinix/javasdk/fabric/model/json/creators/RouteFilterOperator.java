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

import api.equinix.javasdk.core.http.request.PatchOperation;
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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class RouteFilterOperator extends ResourceImpl<RouteFilter> {

    @Getter
    private final PageablePost<RouteFilter> serviceClient;

    public RouteFilterOperator(PageablePost<RouteFilter> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public RouteFilterBuilder create() {
        return new RouteFilterBuilder();
    }

    /**
     * <p>Begins a fluent update of an existing route filter, identified by uuid.</p>
     *
     * @param uuid the uuid of the route filter to update
     */
    public RouteFilterUpdater update(String uuid) {
        return new RouteFilterUpdater(uuid);
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

    /**
     * Fluent builder for updating an existing route filter. Each typed setter records a
     * {@code replace} change operation; {@link #save()} sends them as one {@code PATCH}
     * (an op/path/value array, content-type {@code application/json}) and returns the refreshed model.
     *
     * <pre>{@code routeFilter.update().name("New-Name").save();}</pre>
     */
    public class RouteFilterUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected RouteFilterUpdater(String uuid) {
            this.uuid = uuid;
        }

        /**
         * Replaces the route filter name.
         *
         * @param name the new name
         * @return this updater
         */
        public RouteFilterUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        /**
         * Replaces the route filter description.
         *
         * @param description the new description
         * @return this updater
         */
        public RouteFilterUpdater description(String description) {
            operations.add(PatchOperation.replace("/description", description));
            return this;
        }

        /**
         * Adds an arbitrary change operation, for paths not covered by the typed setters above.
         *
         * @param operation the patch operation
         * @return this updater
         */
        public RouteFilterUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Applies the accumulated changes and returns the route filter refreshed from the server.
         *
         * @return the updated {@link api.equinix.javasdk.fabric.model.RouteFilter}
         */
        public RouteFilter save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            RouteFilterJson routeFilterJson = ((RouteFilterClientImpl) RouteFilterOperator.this.getServiceClient()).update(uuid, operations);
            return new RouteFilterWrapper(routeFilterJson, RouteFilterOperator.this.getServiceClient());
        }
    }
}
