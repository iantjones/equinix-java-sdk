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
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.RouteFilterRuleClientImpl;
import com.eqixiac.equinix.fabric.enums.RouteFilterAction;
import com.eqixiac.equinix.fabric.model.RouteFilterRule;
import com.eqixiac.equinix.fabric.model.json.RouteFilterRuleJson;
import com.eqixiac.equinix.fabric.model.wrappers.RouteFilterRuleWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class RouteFilterRuleOperator extends ResourceImpl<RouteFilterRule> {

    @Getter
    private final Pageable<RouteFilterRule> serviceClient;

    private final String routeFilterId;

    public RouteFilterRuleOperator(Pageable<RouteFilterRule> serviceClient, String routeFilterId) {
        this.serviceClient = serviceClient;
        this.routeFilterId = routeFilterId;
    }

    public RouteFilterRuleBuilder create() {
        return new RouteFilterRuleBuilder();
    }

    /**
     * <p>Begins a fluent update of an existing route filter rule, identified by uuid.</p>
     *
     * @param uuid the uuid of the route filter rule to update
     */
    public RouteFilterRuleUpdater update(String uuid) {
        return new RouteFilterRuleUpdater(uuid);
    }

    @Getter(AccessLevel.PACKAGE)
    public class RouteFilterRuleBuilder {

        private String prefix;
        private String name;
        private String description;
        private RouteFilterAction action;
        private String prefixMatch;

        protected RouteFilterRuleBuilder() {
        }

        public RouteFilterRuleOperator.RouteFilterRuleBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public RouteFilterRuleOperator.RouteFilterRuleBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RouteFilterRuleOperator.RouteFilterRuleBuilder description(String description) {
            this.description = description;
            return this;
        }

        public RouteFilterRuleOperator.RouteFilterRuleBuilder action(RouteFilterAction action) {
            this.action = action;
            return this;
        }

        public RouteFilterRuleOperator.RouteFilterRuleBuilder prefixMatch(String prefixMatch) {
            this.prefixMatch = prefixMatch;
            return this;
        }

        public RouteFilterRule create() {
            RouteFilterRuleCreatorJson routeFilterRuleCreatorJson = new RouteFilterRuleCreatorJson(this);
            RouteFilterRuleJson routeFilterRuleJson = ((RouteFilterRuleClientImpl) RouteFilterRuleOperator.this.getServiceClient()).create(RouteFilterRuleOperator.this.routeFilterId, routeFilterRuleCreatorJson);
            return new RouteFilterRuleWrapper(routeFilterRuleJson, RouteFilterRuleOperator.this.getServiceClient());
        }
    }

    /**
     * Fluent builder for updating an existing route filter rule. Each typed setter records a
     * {@code replace} change operation; {@link #save()} sends them as one {@code PATCH}
     * (an op/path/value array, content-type {@code application/json}) and returns the refreshed model.
     *
     * <pre>{@code rule.update(routeFilterId).name("New-Name").save();}</pre>
     */
    public class RouteFilterRuleUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected RouteFilterRuleUpdater(String uuid) {
            this.uuid = uuid;
        }

        /**
         * Replaces the route filter rule name.
         *
         * @param name the new name
         * @return this updater
         */
        public RouteFilterRuleUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        /**
         * Replaces the route filter rule action.
         *
         * @param action the new action
         * @return this updater
         */
        public RouteFilterRuleUpdater action(RouteFilterAction action) {
            operations.add(PatchOperation.replace("/action", action));
            return this;
        }

        /**
         * Replaces the route filter rule description.
         *
         * @param description the new description
         * @return this updater
         */
        public RouteFilterRuleUpdater description(String description) {
            operations.add(PatchOperation.replace("/description", description));
            return this;
        }

        /**
         * Adds an arbitrary change operation, for paths not covered by the typed setters above.
         *
         * @param operation the patch operation
         * @return this updater
         */
        public RouteFilterRuleUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Applies the accumulated changes and returns the route filter rule refreshed from the server.
         *
         * @return the updated {@link com.eqixiac.equinix.fabric.model.RouteFilterRule}
         */
        public RouteFilterRule save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            RouteFilterRuleJson routeFilterRuleJson = ((RouteFilterRuleClientImpl) RouteFilterRuleOperator.this.getServiceClient())
                    .update(RouteFilterRuleOperator.this.routeFilterId, uuid, operations);
            return new RouteFilterRuleWrapper(routeFilterRuleJson, RouteFilterRuleOperator.this.getServiceClient());
        }
    }
}
