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

package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.model.Service;
import api.equinix.javasdk.fabric.client.*;
import api.equinix.javasdk.fabric.client.implementation.*;
import api.equinix.javasdk.fabric.model.HealthStatus;

/**
 * The primary entry point for accessing the Equinix Fabric APIs.
 *
 * <p>Equinix Fabric provides interconnection services that enable direct, private connectivity
 * between infrastructure and applications. This class offers typed access to all Fabric resources
 * including connections, ports, service tokens, cloud routers, routing protocols, route filters,
 * streams, precision time services, networks, and more.</p>
 *
 * <p>All resource accessors use lazy initialization — internal clients are created on first access
 * and reused for subsequent calls.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * Fabric fabric = new Fabric(credentials);
 *
 * // List all connections
 * PaginatedFilteredList<Connection> connections = fabric.connections().search();
 *
 * // Create a connection with dry-run validation
 * Connection validated = fabric.connections()
 *     .define(ConnectionType.EVPL_VC)
 *     .withName("My-Connection")
 *     .withBandwidth(100)
 *     .dryRun()
 *     .create();
 *
 * // Check Fabric API health
 * HealthStatus health = fabric.health();
 * }</pre>
 *
 * <h3>Resource Chaining</h3>
 * <pre>{@code
 * // Port -> Connection -> Statistics pipeline
 * Port port = fabric.ports().list().get(0);
 * ConnectionStatistic stats = fabric.connections().getStatistics(
 *     connectionUuid, startTime, endTime);
 *
 * // Cloud Router -> Routing Protocol -> Route Filter chain
 * PaginatedFilteredList<CloudRouter> routers = fabric.cloudRouters().search();
 * PaginatedList<RoutingProtocol> protocols = fabric.routingProtocols().list(connectionId);
 * PaginatedFilteredList<RouteFilter> filters = fabric.routeFilters().search();
 * }</pre>
 *
 * @author ianjones
 * @version $Id: $Id
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 * @see api.equinix.javasdk.fabric.client.Connections
 * @see api.equinix.javasdk.fabric.client.Ports
 */
public final class Fabric extends EquinixClient implements Service {

    private Metros metros;

    private ServiceTokens serviceTokens;

    private Ports ports;

    private Connections connections;

    private Prices prices;

    private ServiceProfiles serviceProfiles;

    private FabricGateways fabricGateways;

    private CloudRouters cloudRouters;

    private RoutingProtocols routingProtocols;

    private RouteFilters routeFilters;

    private RouteFilterRules routeFilterRules;

    private RouteAggregations routeAggregations;

    private RouteAggregationRules routeAggregationRules;

    private Networks networks;

    private Streams streams;

    private StreamSubscriptions streamSubscriptions;

    private PrecisionTimes precisionTimes;

    private CloudEvents cloudEvents;

    private MarketplaceSubscriptions marketplaceSubscriptions;

    private HealthStatus healthStatus;

    final private FabricConfig fabricConfig;

    /**
     * Creates a new Fabric client using the provided credentials.
     * Authentication occurs automatically on the first API call.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public Fabric(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * Creates a new Fabric client with optional sandbox mode.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public Fabric(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_Fabric.json";
        equinixClient.appendApiParams(paramFile);

        this.fabricConfig = new FabricConfigImpl(equinixClient);
    }

    /**
     * Returns the client for managing Equinix metro locations.
     * Metros represent geographic areas where Equinix data centers are located.
     *
     * @return the {@link Metros} client for listing and querying available metros
     */
    public Metros metros() {
        if (this.metros == null) {
            this.metros = new MetrosImpl(this.fabricConfig.getMetrosClient(), this);
        }
        return metros;
    }

    /**
     * Returns the client for managing Fabric service tokens.
     * Service tokens enable secure, delegated provisioning of connections between parties.
     * Supports dry-run validation via the fluent builder.
     *
     * @return the {@link ServiceTokens} client for creating, listing, and managing service tokens
     */
    public ServiceTokens serviceTokens() {
        if (this.serviceTokens == null) {
            this.serviceTokens = new ServiceTokensImpl(this.fabricConfig.getServiceTokensClient(), this);
        }
        return serviceTokens;
    }

    /**
     * Returns the client for managing Fabric ports and port statistics.
     * Ports represent physical network interfaces at Equinix data centers and serve
     * as access points for Fabric connections.
     *
     * @return the {@link Ports} client for listing ports and retrieving port statistics
     */
    public Ports ports() {
        if (this.ports == null) {
            this.ports = new PortsImpl(this.fabricConfig.getPortsClient(), this.fabricConfig.getPortStatisticsClient(), this);
        }
        return ports;
    }

    /**
     * Returns the client for managing Fabric connections and connection statistics.
     * Connections are the core resource in Fabric, representing virtual circuits between
     * two access points (ports, service profiles, cloud providers, or network endpoints).
     * Supports dry-run validation and fluent builder creation.
     *
     * @return the {@link Connections} client for creating, searching, and managing connections
     */
    public Connections connections() {
        if (this.connections == null) {
            this.connections = new ConnectionsImpl(this.fabricConfig.getConnectionsClient(), this);
        }
        return connections;
    }

    /**
     * Returns the client for querying Fabric pricing information.
     * Provides access to pricing details for various Fabric connection types and configurations.
     *
     * @return the {@link Prices} client for searching and filtering pricing data
     */
    public Prices prices() {
        if (this.prices == null) {
            this.prices = new PricesImpl(this.fabricConfig.getPricingClient(), this);
        }
        return prices;
    }

    /**
     * Returns the client for managing Fabric service profiles.
     * Service profiles define how third-party providers (such as cloud providers)
     * expose their services for Fabric connections.
     *
     * @return the {@link ServiceProfiles} client for searching and managing service profiles
     */
    public ServiceProfiles serviceProfiles() {
        if (this.serviceProfiles == null) {
            this.serviceProfiles = new ServiceProfilesImpl(this.fabricConfig.getServiceProfilesClient(), this);
        }
        return serviceProfiles;
    }

    /**
     * Returns the client for managing Fabric gateways (also known as Fabric Cloud Routers).
     * Gateways provide Layer 3 routing capabilities for Fabric connections,
     * with associated gateway packages defining performance tiers.
     *
     * @return the {@link FabricGateways} client for creating and managing gateways
     */
    public FabricGateways fabricGateways() {
        if (this.fabricGateways == null) {
            this.fabricGateways = new FabricGatewaysImpl(this.fabricConfig.getFabricGatewaysClient(), this.fabricConfig.getGatewayPackageClient(), this);
        }
        return fabricGateways;
    }

    /**
     * Returns the client for managing Fabric Cloud Routers and their associated packages.
     * Cloud Routers enable dynamic routing between Fabric connections using BGP.
     *
     * @return the {@link CloudRouters} client for creating, searching, and managing cloud routers
     */
    public CloudRouters cloudRouters() {
        if (this.cloudRouters == null) {
            this.cloudRouters = new CloudRoutersImpl(this.fabricConfig.getCloudRoutersClient(), this.fabricConfig.getCloudRouterPackagesClient(), this);
        }
        return cloudRouters;
    }

    /**
     * Returns the client for managing routing protocols on Fabric connections.
     * Routing protocols (such as BGP) define how routes are exchanged
     * between connected endpoints through Cloud Routers.
     *
     * @return the {@link RoutingProtocols} client for configuring routing protocols on connections
     */
    public RoutingProtocols routingProtocols() {
        if (this.routingProtocols == null) {
            this.routingProtocols = new RoutingProtocolsImpl(this.fabricConfig.getRoutingProtocolsClient(), this);
        }
        return routingProtocols;
    }

    /**
     * Returns the client for managing Fabric route filters.
     * Route filters control which routes are advertised or accepted through
     * routing protocols, enabling fine-grained traffic engineering.
     *
     * @return the {@link RouteFilters} client for creating, searching, and managing route filters
     */
    public RouteFilters routeFilters() {
        if (this.routeFilters == null) {
            this.routeFilters = new RouteFiltersImpl(this.fabricConfig.getRouteFiltersClient(), this);
        }
        return routeFilters;
    }

    /**
     * Returns the client for managing individual rules within Fabric route filters.
     * Each rule specifies a prefix and action (permit/deny) that determines
     * how matching routes are handled.
     *
     * @return the {@link RouteFilterRules} client for CRUD operations on route filter rules
     */
    public RouteFilterRules routeFilterRules() {
        if (this.routeFilterRules == null) {
            this.routeFilterRules = new RouteFilterRulesImpl(this.fabricConfig.getRouteFilterRulesClient(), this);
        }
        return routeFilterRules;
    }

    /**
     * Returns the client for managing Fabric route aggregations.
     * Route aggregations summarize multiple specific routes into a single aggregated route,
     * reducing the routing table size and simplifying network management.
     *
     * @return the {@link RouteAggregations} client for creating, searching, and managing route aggregations
     */
    public RouteAggregations routeAggregations() {
        if (this.routeAggregations == null) {
            this.routeAggregations = new RouteAggregationsImpl(this.fabricConfig.getRouteAggregationsClient(), this);
        }
        return routeAggregations;
    }

    /**
     * Returns the client for managing individual rules within Fabric route aggregations.
     * Each rule specifies which prefixes are included in the aggregated route.
     *
     * @return the {@link RouteAggregationRules} client for CRUD operations on route aggregation rules
     */
    public RouteAggregationRules routeAggregationRules() {
        if (this.routeAggregationRules == null) {
            this.routeAggregationRules = new RouteAggregationRulesImpl(this.fabricConfig.getRouteAggregationRulesClient(), this);
        }
        return routeAggregationRules;
    }

    /**
     * Returns the client for managing Fabric networks.
     * Networks are logical groupings of connections that enable multi-point connectivity
     * and simplified management of related interconnection resources.
     *
     * @return the {@link Networks} client for creating, searching, and managing networks
     */
    public Networks networks() {
        if (this.networks == null) {
            this.networks = new NetworksImpl(this.fabricConfig.getNetworksClient(), this);
        }
        return networks;
    }

    /**
     * Returns the client for managing Fabric event streams.
     * Streams provide real-time event delivery for Fabric resources,
     * enabling observability and automation workflows.
     *
     * @return the {@link Streams} client for creating and managing event streams
     */
    public Streams streams() {
        if (this.streams == null) {
            this.streams = new StreamsImpl(this.fabricConfig.getStreamsClient(), this);
        }
        return streams;
    }

    /**
     * Returns the client for managing Fabric stream subscriptions.
     * Stream subscriptions define how events from a stream are delivered to consumers,
     * supporting various delivery mechanisms.
     *
     * @return the {@link StreamSubscriptions} client for creating and managing stream subscriptions
     */
    public StreamSubscriptions streamSubscriptions() {
        if (this.streamSubscriptions == null) {
            this.streamSubscriptions = new StreamSubscriptionsImpl(this.fabricConfig.getStreamSubscriptionsClient(), this);
        }
        return streamSubscriptions;
    }

    /**
     * Returns the client for managing Equinix Precision Time services.
     * Precision Time provides accurate time synchronization (NTP/PTP) for
     * network infrastructure connected through Fabric.
     *
     * @return the {@link PrecisionTimes} client for creating and managing precision time services
     */
    public PrecisionTimes precisionTimes() {
        if (this.precisionTimes == null) {
            this.precisionTimes = new PrecisionTimesImpl(this.fabricConfig.getPrecisionTimesClient(), this);
        }
        return precisionTimes;
    }

    /**
     * Returns the client for accessing Fabric cloud events.
     * Cloud events provide audit and operational event data for Fabric resources.
     *
     * @return the {@link CloudEvents} client for listing and searching cloud events
     */
    public CloudEvents cloudEvents() {
        if (this.cloudEvents == null) {
            this.cloudEvents = new CloudEventsImpl(this.fabricConfig.getCloudEventsClient(), this);
        }
        return cloudEvents;
    }

    /**
     * Returns the client for managing Fabric marketplace subscriptions.
     * Marketplace subscriptions represent active subscriptions to services
     * offered through the Equinix Fabric marketplace.
     *
     * @return the {@link MarketplaceSubscriptions} client for managing marketplace subscriptions
     */
    public MarketplaceSubscriptions marketplaceSubscriptions() {
        if (this.marketplaceSubscriptions == null) {
            this.marketplaceSubscriptions = new MarketplaceSubscriptionsImpl(this.fabricConfig.getMarketplaceSubscriptionsClient(), this);
        }
        return marketplaceSubscriptions;
    }

    /**
     * Retrieves the current health status of the Equinix Fabric API.
     * This is a direct call (not a lazy-initialized client) that returns
     * the current API availability and status information.
     *
     * @return the current {@link HealthStatus} of the Fabric API
     */
    public HealthStatus health() {
        return this.fabricConfig.getHealthClient().getHealth();
    }
}
