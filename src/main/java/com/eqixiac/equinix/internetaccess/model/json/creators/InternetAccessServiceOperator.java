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

package com.eqixiac.equinix.internetaccess.model.json.creators;

import com.eqixiac.equinix.internetaccess.client.internal.InternetAccessServiceClient;
import com.eqixiac.equinix.internetaccess.enums.ServiceTypeV2;
import com.eqixiac.equinix.internetaccess.model.InternetAccessService;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for the nested {@link ServiceRequest} body of an Equinix Internet Access
 * (EIA) v2 service create. The routing protocol, IP blocks, customer routes and peerings are
 * all assembled into the single request body submitted by {@link InternetAccessServiceBuilder#create()}.
 */
public class InternetAccessServiceOperator {

    @Getter
    private final InternetAccessServiceClient serviceClient;

    public InternetAccessServiceOperator(InternetAccessServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    public InternetAccessServiceBuilder create() {
        return new InternetAccessServiceBuilder();
    }

    @Getter
    public class InternetAccessServiceBuilder {
        private String name;
        private String description;
        private ServiceTypeV2 type;
        private List<String> tags;
        private List<String> connections = new ArrayList<>();
        private RoutingProtocolRequest routingProtocol;
        private ServiceOrderRequest order;

        public InternetAccessServiceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public InternetAccessServiceBuilder description(String description) {
            this.description = description;
            return this;
        }

        public InternetAccessServiceBuilder type(ServiceTypeV2 type) {
            this.type = type;
            return this;
        }

        public InternetAccessServiceBuilder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Sets the collection of service connection uuids (1 or 2 entries).
         *
         * @param connections the connection uuids
         * @return this builder
         */
        public InternetAccessServiceBuilder connections(List<String> connections) {
            this.connections = connections;
            return this;
        }

        /**
         * Adds a single service connection uuid (the service supports 1 or 2 connections).
         *
         * @param connectionUuid the connection uuid
         * @return this builder
         */
        public InternetAccessServiceBuilder connection(String connectionUuid) {
            if (this.connections == null) {
                this.connections = new ArrayList<>();
            }
            this.connections.add(connectionUuid);
            return this;
        }

        /**
         * Sets the nested routing protocol — one of {@link DirectRoutingProtocolRequest},
         * {@link StaticRoutingProtocolRequest} or {@link BgpRoutingProtocolRequest}.
         *
         * @param routingProtocol the routing protocol request
         * @return this builder
         */
        public InternetAccessServiceBuilder routingProtocol(RoutingProtocolRequest routingProtocol) {
            this.routingProtocol = routingProtocol;
            return this;
        }

        public InternetAccessServiceBuilder order(ServiceOrderRequest order) {
            this.order = order;
            return this;
        }

        public InternetAccessService create() {
            ServiceRequest serviceRequest = new ServiceRequest(this);
            return InternetAccessServiceOperator.this.getServiceClient().create(serviceRequest);
        }
    }
}
