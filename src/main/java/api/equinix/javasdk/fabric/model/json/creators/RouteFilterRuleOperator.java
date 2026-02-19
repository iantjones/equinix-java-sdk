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
import api.equinix.javasdk.fabric.client.internal.implementation.RouteFilterRuleClientImpl;
import api.equinix.javasdk.fabric.enums.RouteFilterAction;
import api.equinix.javasdk.fabric.model.RouteFilterRule;
import api.equinix.javasdk.fabric.model.json.RouteFilterRuleJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteFilterRuleWrapper;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * <p>RouteFilterRuleOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class RouteFilterRuleOperator extends ResourceImpl<RouteFilterRule> {

    @Getter
    private final Pageable<RouteFilterRule> serviceClient;

    private final String routeFilterId;

    /**
     * <p>Constructor for RouteFilterRuleOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     * @param routeFilterId a {@link java.lang.String} object.
     */
    public RouteFilterRuleOperator(Pageable<RouteFilterRule> serviceClient, String routeFilterId) {
        this.serviceClient = serviceClient;
        this.routeFilterId = routeFilterId;
    }

    /**
     * <p>create.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.RouteFilterRuleOperator.RouteFilterRuleBuilder} object.
     */
    public RouteFilterRuleBuilder create() {
        return new RouteFilterRuleBuilder();
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
}
