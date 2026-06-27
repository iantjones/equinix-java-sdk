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
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.json.creators.RoutingProtocolOperator;

/**
 * Client interface for managing routing protocols on Equinix Fabric connections.
 * Routing protocols (BGP, Direct) control how routes are exchanged over a connection.
 */
public interface RoutingProtocols {

    /**
     * Lists all routing protocols configured on the specified connection.
     *
     * @param connectionId the unique identifier of the connection
     * @return a paginated list of routing protocols for the connection
     */
    PaginatedList<RoutingProtocol> list(String connectionId);

    /**
     * Retrieves a single routing protocol by its unique identifier on the specified connection.
     *
     * @param connectionId the unique identifier of the connection
     * @param uuid the unique identifier of the routing protocol
     * @return the routing protocol matching the given UUID
     */
    RoutingProtocol getByUuid(String connectionId, String uuid);

    /**
     * Begins the fluent builder for creating a new routing protocol.
     * Call methods on the returned builder to configure the protocol, then call {@code create()}.
     *
     * @return a builder for configuring the new routing protocol
     */
    RoutingProtocolOperator.RoutingProtocolBuilder define();
}
