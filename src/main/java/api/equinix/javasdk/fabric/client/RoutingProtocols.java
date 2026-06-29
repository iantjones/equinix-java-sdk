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
import api.equinix.javasdk.fabric.enums.BGPActionType;
import api.equinix.javasdk.fabric.model.BGPAction;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.json.creators.RoutingProtocolOperator;

import java.util.List;

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

    /**
     * Replaces (full update via PUT) an existing routing protocol with the configuration described
     * by the supplied builder. Unlike a PATCH, the supplied configuration replaces the routing
     * protocol in its entirety.
     *
     * @param connectionId the unique identifier of the connection
     * @param uuid the unique identifier of the routing protocol to replace
     * @param builder a configured routing protocol builder (as returned by {@link #define()})
     * @return the replaced routing protocol
     */
    RoutingProtocol replace(String connectionId, String uuid, RoutingProtocolOperator.RoutingProtocolBuilder builder);

    /**
     * Creates multiple routing protocols on a connection in a single bulk request.
     *
     * @param connectionId the unique identifier of the connection
     * @param builders the configured routing protocol builders (each as returned by {@link #define()})
     * @return the list of created routing protocols
     */
    List<RoutingProtocol> createBulk(String connectionId, List<RoutingProtocolOperator.RoutingProtocolBuilder> builders);

    /**
     * Lists the BGP clear/reset actions issued against a routing protocol on a connection.
     *
     * @param connectionId the unique identifier of the connection
     * @param routingProtocolId the unique identifier of the routing protocol
     * @return the list of BGP actions
     */
    List<BGPAction> getBgpActions(String connectionId, String routingProtocolId);

    /**
     * Issues a BGP clear/reset action against a routing protocol on a connection.
     *
     * @param connectionId the unique identifier of the connection
     * @param routingProtocolId the unique identifier of the routing protocol
     * @param type the BGP action type (clear/reset, IPv4/IPv6, inbound)
     * @return the created BGP action
     */
    BGPAction createBgpAction(String connectionId, String routingProtocolId, BGPActionType type);

    /**
     * Retrieves a single BGP clear/reset action by its identifier.
     *
     * @param connectionId the unique identifier of the connection
     * @param routingProtocolId the unique identifier of the routing protocol
     * @param actionId the unique identifier of the BGP action
     * @return the BGP action
     */
    BGPAction getBgpAction(String connectionId, String routingProtocolId, String actionId);

    /**
     * Lists the changes recorded against a routing protocol on a connection.
     *
     * @param connectionId the unique identifier of the connection
     * @param routingProtocolId the unique identifier of the routing protocol
     * @return the list of routing protocol changes
     */
    List<Change> getChanges(String connectionId, String routingProtocolId);

    /**
     * Retrieves a single routing protocol change by its identifier.
     *
     * @param connectionId the unique identifier of the connection
     * @param routingProtocolId the unique identifier of the routing protocol
     * @param changeId the unique identifier of the change
     * @return the routing protocol change
     */
    Change getChange(String connectionId, String routingProtocolId, String changeId);
}
