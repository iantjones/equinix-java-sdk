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
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.IPAddress;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.RouteAggregationRuleClientImpl;
import api.equinix.javasdk.fabric.model.RouteAggregationRule;
import api.equinix.javasdk.fabric.model.json.RouteAggregationRuleJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteAggregationRuleWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class RouteAggregationRuleOperator extends ResourceImpl<RouteAggregationRule> {

    @Getter
    private final Pageable<RouteAggregationRule> serviceClient;

    private final String routeAggregationId;

    public RouteAggregationRuleOperator(Pageable<RouteAggregationRule> serviceClient, String routeAggregationId) {
        this.serviceClient = serviceClient;
        this.routeAggregationId = routeAggregationId;
    }

    public RouteAggregationRuleBuilder create() {
        return new RouteAggregationRuleBuilder();
    }

    /**
     * <p>Begins a fluent update of an existing route aggregation rule, identified by uuid.</p>
     *
     * @param uuid the uuid of the route aggregation rule to update
     */
    public RouteAggregationRuleUpdater update(String uuid) {
        return new RouteAggregationRuleUpdater(uuid);
    }

    @Getter
    public class RouteAggregationRuleBuilder {

        private String name;
        private String prefix;
        private String description;

        protected RouteAggregationRuleBuilder() {
        }

        public RouteAggregationRuleOperator.RouteAggregationRuleBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public RouteAggregationRuleOperator.RouteAggregationRuleBuilder withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Typed variant of {@code withPrefix(String)}. The prefix is a network in CIDR form, so
         * the {@link IPAddress} should carry its subnet length (e.g. {@code IPAddress.parse("10.0.0.0/8")});
         * it is serialized via {@link IPAddress#toCidr()}, producing the identical wire value to the
         * String setter.
         */
        public RouteAggregationRuleOperator.RouteAggregationRuleBuilder withPrefix(IPAddress prefix) {
            return withPrefix(prefix == null ? null : prefix.toCidr());
        }

        public RouteAggregationRuleOperator.RouteAggregationRuleBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public RouteAggregationRule create() {
            RouteAggregationRuleCreatorJson routeAggregationRuleCreatorJson = new RouteAggregationRuleCreatorJson(this);
            RouteAggregationRuleJson routeAggregationRuleJson = ((RouteAggregationRuleClientImpl) RouteAggregationRuleOperator.this.getServiceClient()).create(RouteAggregationRuleOperator.this.routeAggregationId, routeAggregationRuleCreatorJson);
            return new RouteAggregationRuleWrapper(routeAggregationRuleJson, RouteAggregationRuleOperator.this.getServiceClient());
        }
    }

    /**
     * Fluent builder for updating an existing route aggregation rule. Each typed setter records a
     * {@code replace} change operation; {@link #save()} sends them as one {@code PATCH}
     * (an op/path/value array, content-type {@code application/json}) and returns the refreshed model.
     *
     * <pre>{@code rule.update(routeAggregationId).name("New-Name").save();}</pre>
     */
    public class RouteAggregationRuleUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected RouteAggregationRuleUpdater(String uuid) {
            this.uuid = uuid;
        }

        /**
         * Replaces the route aggregation rule name.
         *
         * @param name the new name
         * @return this updater
         */
        public RouteAggregationRuleUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        /**
         * Replaces the route aggregation rule prefix.
         *
         * @param prefix the new prefix
         * @return this updater
         */
        public RouteAggregationRuleUpdater prefix(String prefix) {
            operations.add(PatchOperation.replace("/prefix", prefix));
            return this;
        }

        /**
         * Typed variant of {@code prefix(String)}. The prefix is a network in CIDR form, so the
         * {@link IPAddress} should carry its subnet length (e.g. {@code IPAddress.parse("10.0.0.0/8")});
         * it is serialized via {@link IPAddress#toCidr()}, producing the identical wire value to the
         * String setter.
         *
         * @param prefix the new prefix
         * @return this updater
         */
        public RouteAggregationRuleUpdater prefix(IPAddress prefix) {
            return prefix(prefix == null ? null : prefix.toCidr());
        }

        /**
         * Replaces the route aggregation rule description.
         *
         * @param description the new description
         * @return this updater
         */
        public RouteAggregationRuleUpdater description(String description) {
            operations.add(PatchOperation.replace("/description", description));
            return this;
        }

        /**
         * Adds an arbitrary change operation, for paths not covered by the typed setters above.
         *
         * @param operation the patch operation
         * @return this updater
         */
        public RouteAggregationRuleUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Applies the accumulated changes and returns the rule refreshed from the server.
         *
         * @return the updated {@link api.equinix.javasdk.fabric.model.RouteAggregationRule}
         */
        public RouteAggregationRule save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            RouteAggregationRuleJson routeAggregationRuleJson = ((RouteAggregationRuleClientImpl) RouteAggregationRuleOperator.this.getServiceClient())
                    .update(RouteAggregationRuleOperator.this.routeAggregationId, uuid, operations);
            return new RouteAggregationRuleWrapper(routeAggregationRuleJson, RouteAggregationRuleOperator.this.getServiceClient());
        }
    }
}
