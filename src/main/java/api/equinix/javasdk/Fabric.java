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
import api.equinix.javasdk.core.auth.EquinixCredentialsProvider;
import api.equinix.javasdk.core.auth.EquinixStaticCredentialsProvider;
import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.fabric.client.*;
import api.equinix.javasdk.fabric.client.implementation.*;
import api.equinix.javasdk.fabric.model.HealthStatus;
import api.equinix.javasdk.fabric.model.MetroRegistry;
import api.equinix.javasdk.internetaccess.enums.ConnectionType;
import api.equinix.javasdk.internetaccess.model.Ibx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import api.equinix.javasdk.mcp.McpClientConfig;
import api.equinix.javasdk.mcp.bridge.McpBridge;
import api.equinix.javasdk.design.optimizer.MetroOptimizer;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard;
import api.equinix.javasdk.design.peering.PeeringIntelligence;
import api.equinix.javasdk.design.value.savings.SavingsCalculator;
import api.equinix.javasdk.design.value.tco.TcoCalculator;

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
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 * @see api.equinix.javasdk.fabric.client.Connections
 * @see api.equinix.javasdk.fabric.client.Ports
 */
public final class Fabric extends EquinixClient implements FabricGateway {

    private static final Logger log = LoggerFactory.getLogger(Fabric.class);

    private Metros metros;

    private MetroRegistry metroRegistry;

    /**
     * Lazily-built EIA facade over this client's own core (shared token + pool), used only when
     * {@code EquinixConfig.enrichMetroRegistry} is on to pull per-IBX detail into the registry.
     */
    private InternetAccess enrichmentInternetAccess;

    private ServiceTokens serviceTokens;

    private Ports ports;

    private Connections connections;

    private Metrics metrics;

    private Prices prices;

    private ServiceProfiles serviceProfiles;

    private CloudRouters cloudRouters;

    private RoutingProtocols routingProtocols;

    private RouteFilters routeFilters;

    private EiaServices eiaServices;

    private RouteFilterRules routeFilterRules;

    private RouteAggregations routeAggregations;

    private RouteAggregationRules routeAggregationRules;

    private Networks networks;

    private Streams streams;

    private StreamSubscriptions streamSubscriptions;

    private PrecisionTimes precisionTimes;

    private CloudEvents cloudEvents;

    private MarketplaceSubscriptions marketplaceSubscriptions;

    private IpBlocks ipBlocks;

    private PortPackages portPackages;

    private StreamAlertRules streamAlertRules;

    private StreamAssets streamAssets;

    private Agents agents;

    private AgentTemplates agentTemplates;

    private CompanyProfiles companyProfiles;

    private Tags tags;

    private HealthStatus healthStatus;

    private McpBridge mcpBridge;

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
        this(new EquinixStaticCredentialsProvider(equinixCredentials), isSandBoxed);
    }

    /**
     * Creates a new Fabric client with explicit {@link EquinixConfig} options (sandbox, retry,
     * metro auto-loading).
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public Fabric(EquinixCredentials equinixCredentials, EquinixConfig config) {
        this(new EquinixStaticCredentialsProvider(equinixCredentials), config);
    }

    /**
     * Creates a new Fabric client whose credentials are resolved through the given provider.
     * Authentication occurs automatically on the first API call.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     */
    public Fabric(EquinixCredentialsProvider credentialsProvider) {
        this(credentialsProvider, EquinixConfig.defaults());
    }

    /**
     * Creates a new Fabric client over a custom credentials provider, with optional sandbox mode.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public Fabric(EquinixCredentialsProvider credentialsProvider, boolean isSandBoxed) {
        this(credentialsProvider, EquinixConfig.builder().sandbox(isSandBoxed).build());
    }

    /**
     * Creates a new Fabric client over a custom credentials provider with explicit
     * {@link EquinixConfig} options.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public Fabric(EquinixCredentialsProvider credentialsProvider, EquinixConfig config) {
        super(credentialsProvider, config);

        String paramFile = "json/apiParams_Fabric.json";
        equinixClient.appendApiParams(paramFile);

        this.fabricConfig = new FabricConfigImpl(equinixClient);
    }

    /**
     * Package-private constructor for {@link Equinix} sessions: builds this domain client over a
     * shared core client (one OAuth token + connection pool across domains), carrying the
     * session-configured options this domain consumes (PeeringDB API key, metro-registry
     * enrichment).
     */
    Fabric(api.equinix.javasdk.core.client.EquinixClient sharedCore, EquinixConfig sessionConfig) {
        super(sharedCore, sessionConfig);
        equinixClient.appendApiParams("json/apiParams_Fabric.json");
        this.fabricConfig = new FabricConfigImpl(equinixClient);
    }

    /**
     * Performs OAuth2 authentication and, when metro auto-loading is enabled (the default — see
     * {@code EquinixConfig.isAutoLoadMetros()} on {@link EquinixConfig}), eagerly loads the {@link #metroRegistry()} so the
     * full metro catalogue (metros, their IBXs, coordinates, region, and inter-metro latencies) is
     * resolved up front rather than lazily on first access. The catalogue load is best-effort: a
     * failure there does not fail authentication, and the registry stays lazily loadable on demand.
     *
     * @throws EquinixClientException if authentication itself fails
     */
    @Override
    public void authenticate() throws EquinixClientException {
        super.authenticate();
        if (autoLoadMetros) {
            try {
                metroRegistry();
            }
            catch (RuntimeException ignored) {
                // best-effort eager load; metroRegistry() remains available lazily
            }
        }
    }

    /**
     * Returns the client for managing Equinix metro locations.
     * Metros represent geographic areas where Equinix data centers are located.
     *
     * @return the {@link Metros} client for listing and querying available metros
     */
    public Metros metros() {
        if (this.metros == null) {
            this.metros = new MetrosImpl(this.fabricConfig.getMetrosClient());
        }
        return metros;
    }

    /**
     * Returns a cached, in-memory registry of every metro the Metros API reports (and the IBX data
     * centers within each), keyed by {@link api.equinix.javasdk.core.model.MetroId} so it covers
     * metros the {@link api.equinix.javasdk.core.enums.MetroCode} enum does not list. Loaded lazily
     * on first access from {@link #metros()}; call {@link #reloadMetroRegistry()} to refresh it.
     *
     * <p>When {@code EquinixConfig.enrichMetroRegistry} is enabled, the load also pulls the EIA
     * per-IBX catalogue (the only Equinix API with per-data-center coordinates) over this client's
     * own transport and exposes it via {@code MetroRegistry.ibx(String)} — see
     * {@link EquinixConfig#isEnrichMetroRegistry()}.</p>
     *
     * @return the metro registry
     */
    public MetroRegistry metroRegistry() {
        if (this.metroRegistry == null) {
            this.metroRegistry = loadMetroRegistry();
        }
        return metroRegistry;
    }

    /**
     * Rebuilds the {@link #metroRegistry()} from a fresh Metros API call (re-running EIA enrichment
     * when configured), picking up any metros added since it was last loaded.
     *
     * @return the refreshed metro registry
     */
    public MetroRegistry reloadMetroRegistry() {
        this.metroRegistry = loadMetroRegistry();
        return metroRegistry;
    }

    private MetroRegistry loadMetroRegistry() {
        return MetroRegistry.load(metros(), enrichMetroRegistry ? fetchIbxDetails() : java.util.List.of());
    }

    /**
     * Best-effort fetch of the EIA per-IBX catalogue for registry enrichment, unioned across both
     * EIA connection types (the {@code /internetAccess/v2/ibxs} listing is scoped per connection
     * type). A failure never fails the registry load — it just leaves the registry un-enriched.
     */
    private java.util.List<Ibx> fetchIbxDetails() {
        java.util.List<Ibx> details = new java.util.ArrayList<>();
        try {
            if (enrichmentInternetAccess == null) {
                enrichmentInternetAccess = new InternetAccess(equinixClient);
            }
            for (ConnectionType connectionType : ConnectionType.values()) {
                try {
                    for (Ibx ibx : enrichmentInternetAccess.ibxs().availability(connectionType).loadAll()) {
                        details.add(ibx);
                    }
                } catch (RuntimeException e) {
                    log.warn("Metro-registry enrichment: EIA ibxs fetch failed for connection type {} ({})",
                            connectionType, e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Metro-registry enrichment unavailable: {}", e.getMessage());
        }
        return details;
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
            this.serviceTokens = new ServiceTokensImpl(this.fabricConfig.getServiceTokensClient());
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
            this.ports = new PortsImpl(this.fabricConfig.getPortsClient(), this.fabricConfig.getPortStatisticsClient());
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
            this.connections = new ConnectionsImpl(this.fabricConfig.getConnectionsClient(), this.fabricConfig.getConnectionRoutesClient());
        }
        return connections;
    }

    /**
     * Returns the client for querying Equinix Fabric metrics.
     * The Metrics API provides time-series measurements (such as bandwidth usage) for Fabric
     * assets and supersedes the deprecated per-asset {@code /stats} statistics endpoints.
     *
     * @return the {@link Metrics} client for searching Fabric metrics
     */
    public Metrics metrics() {
        if (this.metrics == null) {
            this.metrics = new MetricsImpl(this.fabricConfig.getMetricsClient());
        }
        return metrics;
    }

    /**
     * Returns the client for querying Fabric pricing information.
     * Provides access to pricing details for various Fabric connection types and configurations.
     *
     * @return the {@link Prices} client for searching and filtering pricing data
     */
    public Prices prices() {
        if (this.prices == null) {
            this.prices = new PricesImpl(this.fabricConfig.getPricingClient());
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
            this.serviceProfiles = new ServiceProfilesImpl(this.fabricConfig.getServiceProfilesClient());
        }
        return serviceProfiles;
    }

    /**
     * Returns the client for managing Fabric Cloud Routers and their associated packages.
     * Cloud Routers enable dynamic routing between Fabric connections using BGP.
     *
     * @return the {@link CloudRouters} client for creating, searching, and managing cloud routers
     */
    public CloudRouters cloudRouters() {
        if (this.cloudRouters == null) {
            this.cloudRouters = new CloudRoutersImpl(this.fabricConfig.getCloudRoutersClient(), this.fabricConfig.getCloudRouterPackagesClient(),
                    this.fabricConfig.getCloudRouterRoutesClient(), this.fabricConfig.getCloudRouterCommandsClient());
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
            this.routingProtocols = new RoutingProtocolsImpl(this.fabricConfig.getRoutingProtocolsClient());
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
            this.routeFilters = new RouteFiltersImpl(this.fabricConfig.getRouteFiltersClient());
        }
        return routeFilters;
    }

    /**
     * Returns the client for managing Equinix Internet Access (EIA) services.
     * EIA services provide dedicated internet connectivity (single or dual) over Equinix Fabric,
     * with configurable bandwidth, billing, and routing protocol (BGP/DIRECT/STATIC).
     *
     * @return the {@link EiaServices} client for creating, searching, and managing EIA services
     */
    public EiaServices eiaServices() {
        if (this.eiaServices == null) {
            this.eiaServices = new EiaServicesImpl(this.fabricConfig.getEiaServicesClient());
        }
        return eiaServices;
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
            this.routeFilterRules = new RouteFilterRulesImpl(this.fabricConfig.getRouteFilterRulesClient());
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
            this.routeAggregations = new RouteAggregationsImpl(this.fabricConfig.getRouteAggregationsClient());
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
            this.routeAggregationRules = new RouteAggregationRulesImpl(this.fabricConfig.getRouteAggregationRulesClient());
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
            this.networks = new NetworksImpl(this.fabricConfig.getNetworksClient(), this.fabricConfig.getConnectionsClient());
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
            this.streams = new StreamsImpl(this.fabricConfig.getStreamsClient());
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
            this.streamSubscriptions = new StreamSubscriptionsImpl(this.fabricConfig.getStreamSubscriptionsClient());
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
            this.precisionTimes = new PrecisionTimesImpl(this.fabricConfig.getPrecisionTimesClient());
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
            this.cloudEvents = new CloudEventsImpl(this.fabricConfig.getCloudEventsClient());
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
            this.marketplaceSubscriptions = new MarketplaceSubscriptionsImpl(this.fabricConfig.getMarketplaceSubscriptionsClient());
        }
        return marketplaceSubscriptions;
    }

    /**
     * Returns the client for managing Fabric IP blocks (BYOIP / Equinix-owned IPv4 and IPv6 prefixes).
     *
     * @return the {@link IpBlocks} client for submitting, searching, and managing IP blocks
     */
    public IpBlocks ipBlocks() {
        if (this.ipBlocks == null) {
            this.ipBlocks = new IpBlocksImpl(this.fabricConfig.getIpBlocksClient());
        }
        return ipBlocks;
    }

    /**
     * Returns the client for querying Fabric port packages.
     *
     * @return the {@link PortPackages} client for listing port packages
     */
    public PortPackages portPackages() {
        if (this.portPackages == null) {
            this.portPackages = new PortPackagesImpl(this.fabricConfig.getPortPackagesClient());
        }
        return portPackages;
    }

    /**
     * Returns the client for managing alert rules attached to Fabric streams.
     *
     * @return the {@link StreamAlertRules} client for creating and managing stream alert rules
     */
    public StreamAlertRules streamAlertRules() {
        if (this.streamAlertRules == null) {
            this.streamAlertRules = new StreamAlertRulesImpl(this.fabricConfig.getStreamAlertRulesClient());
        }
        return streamAlertRules;
    }

    /**
     * Returns the client for attaching and detaching assets to and from Fabric streams.
     *
     * @return the {@link StreamAssets} client for managing stream asset attachments
     */
    public StreamAssets streamAssets() {
        if (this.streamAssets == null) {
            this.streamAssets = new StreamAssetsImpl(this.fabricConfig.getStreamAssetsClient());
        }
        return streamAssets;
    }

    /**
     * Returns the client for managing Fabric agents (for example Autonomous Network Operations agents).
     *
     * @return the {@link Agents} client for creating, listing, and managing agents
     */
    public Agents agents() {
        if (this.agents == null) {
            this.agents = new AgentsImpl(this.fabricConfig.getAgentsClient());
        }
        return agents;
    }

    /**
     * Returns the client for querying Fabric agent templates.
     *
     * @return the {@link AgentTemplates} client for listing agent templates
     */
    public AgentTemplates agentTemplates() {
        if (this.agentTemplates == null) {
            this.agentTemplates = new AgentTemplatesImpl(this.fabricConfig.getAgentTemplatesClient());
        }
        return agentTemplates;
    }

    /**
     * Returns the client for managing Fabric company profiles and their service-profile / tag attachments.
     *
     * @return the {@link CompanyProfiles} client for creating, searching, and managing company profiles
     */
    public CompanyProfiles companyProfiles() {
        if (this.companyProfiles == null) {
            this.companyProfiles = new CompanyProfilesImpl(this.fabricConfig.getCompanyProfilesClient());
        }
        return companyProfiles;
    }

    /**
     * Returns the client for managing Fabric resource tags.
     *
     * @return the {@link Tags} client for listing and creating tags
     */
    public Tags tags() {
        if (this.tags == null) {
            this.tags = new TagsImpl(this.fabricConfig.getTagsClient());
        }
        return tags;
    }

    /**
     * Begins a metro optimization session. Returns a fluent builder for defining
     * workforce locations, provider requirements, workloads, and constraints, then
     * computes optimal Equinix metro placements with ranked recommendations.
     *
     * <pre>{@code
     * OptimizationResult result = fabric.optimizeMetros()
     *     .addSite("NYC HQ").nearestMetro(MetroCode.NY).role(SiteRole.HEADQUARTERS).headcount(500).done()
     *     .requireProvider(CloudProviderType.AWS).done()
     *     .addWorkload("ML Training").type(WorkloadType.AI_ML_TRAINING).bandwidthMbps(10_000).done()
     *     .strategy(OptimizationStrategy.BALANCED)
     *     .optimize();
     *
     * System.out.println(result.toMarkdown());
     * }</pre>
     *
     * @return a {@link MetroOptimizer.Builder} for configuring the optimization request
     */
    public MetroOptimizer.Builder optimizeMetros() {
        return MetroOptimizer.builder(this);
    }

    /**
     * Creates a Deployment Wizard from a completed optimization result. The wizard
     * generates an executable deployment plan with Cloud Routers, provider connections,
     * inter-metro backbone links, and routing protocol configurations — all with
     * bandwidth sizing that drives accurate pricing.
     *
     * <pre>{@code
     * DeploymentPlan plan = fabric.deploymentWizard(optimizationResult)
     *     .routerPackage("STANDARD")
     *     .routerNamePrefix("FCR")
     *     .backboneBandwidthMbps(10_000)
     *     .backboneTopology(BackboneTopology.FULL_MESH)
     *     .bandwidthStrategy(BandwidthStrategy.PER_WORKLOAD)
     *     .customerAsn(65100L)
     *     .withBFD(true, 300)
     *     .plan();
     *
     * System.out.println(plan.toMarkdown());
     * DeploymentOutcome outcome = plan.execute();
     * }</pre>
     *
     * @param optimizationResult the completed optimization result to convert into a deployment plan
     * @return a {@link DeploymentWizard.Builder} for configuring the deployment plan
     */
    public DeploymentWizard.Builder deploymentWizard(OptimizationResult optimizationResult) {
        return DeploymentWizard.builder(this, optimizationResult);
    }

    /**
     * Begins a Peering Intelligence analysis session with PeeringDB API key authentication.
     * Returns a fluent builder for specifying target ASNs, customer metro locations,
     * and analysis options, then queries PeeringDB and Equinix Fabric to produce
     * presence matrices, resiliency assessments, and peering opportunity discovery.
     *
     * <pre>{@code
     * PeeringIntelligenceResult result = fabric.peeringIntelligence("your-peeringdb-api-key")
     *     .addAsn(16509, "AWS")
     *     .addAsn(8075, "Microsoft")
     *     .addAsn(15169, "Google")
     *     .customerMetros(MetroCode.DC, MetroCode.DA, MetroCode.SG)
     *     .customerAsn(65100L)
     *     .includeResiliency(true)
     *     .analyze();
     *
     * System.out.println(result.presenceMatrix().toTableString());
     * System.out.println(result.toMarkdown());
     * }</pre>
     *
     * @param peeringDbApiKey the PeeringDB API key for authenticated access
     * @return a {@link PeeringIntelligence.Builder} for configuring the analysis
     * @see <a href="https://docs.peeringdb.com/howto/api_keys/">PeeringDB API Keys</a>
     */
    public PeeringIntelligence.Builder peeringIntelligence(String peeringDbApiKey) {
        return PeeringIntelligence.builder(this, peeringDbApiKey);
    }

    /**
     * Begins a Peering Intelligence analysis session, resolving the PeeringDB credential from the
     * client's configuration: the {@code EquinixConfig.peeringDbApiKey} option if one was set, else
     * the {@code PEERINGDB_API_KEY} environment variable, else anonymous access. Anonymous access is
     * rate-limited by PeeringDB to approximately 20 requests per minute.
     *
     * @return a {@link PeeringIntelligence.Builder} for configuring the analysis
     * @see #peeringIntelligence(String)
     * @see EquinixConfig#getPeeringDbApiKey()
     */
    public PeeringIntelligence.Builder peeringIntelligence() {
        return PeeringIntelligence.builder(this, peeringDbApiKey);
    }

    /**
     * Begins a value-realization savings analysis: how much routing cloud egress
     * over an Equinix private interconnect saves versus the public internet.
     * Equinix interconnect costs default to live Fabric pricing; egress rates come
     * from the supplied rate card (a {@code ReferenceRateCard} or a
     * {@link api.equinix.javasdk.design.value.ratecard.CustomRateCard}).
     *
     * <pre>{@code
     * SavingsEstimate s = fabric.savingsCalculator()
     *     .egress(50, DataUnit.TERABYTE)
     *     .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
     *     .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
     *     .calculate();
     *
     * System.out.println(s.toMarkdown());
     * }</pre>
     *
     * @return a {@link SavingsCalculator.Builder} for configuring the analysis
     */
    public SavingsCalculator.Builder savingsCalculator() {
        return SavingsCalculator.builder(this);
    }

    /**
     * Begins a total-cost-of-ownership comparison across the three deployment
     * archetypes — public cloud over the internet, on-prem, and Equinix-
     * interconnected — for a given egress/bandwidth workload.
     *
     * <pre>{@code
     * TcoComparison tco = fabric.tcoComparison()
     *     .egress(100, DataUnit.TERABYTE)
     *     .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
     *     .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
     *     .compare();
     *
     * System.out.println(tco.toMarkdown());
     * }</pre>
     *
     * @return a {@link TcoCalculator.Builder} for configuring the comparison
     */
    public TcoCalculator.Builder tcoComparison() {
        return TcoCalculator.builder(this);
    }

    /**
     * Returns the MCP (Model Context Protocol) bridge for interacting with Equinix MCP servers.
     *
     * <p>The MCP bridge provides access to the Equinix Fabric MCP server's tools via the
     * JSON-RPC 2.0 protocol (metro lookup, connection search/validation, cloud-router and
     * observability helpers; the exact set is discovered at runtime via {@code listTools()}),
     * enabling real-time validation, enrichment, and observability for the SDK's optimization
     * and deployment modules. The MCP client is automatically initialized on first access.</p>
     *
     * <pre>{@code
     * McpBridge mcp = fabric.mcp();
     *
     * // List available metros via MCP
     * List<McpMetroBridge.McpMetro> metros = mcp.metros().listMetros();
     *
     * // Validate a connection before deployment
     * McpConnectionBridge.McpConnectionValidation result =
     *     mcp.connections().validateConnection(connectionSpec);
     * }</pre>
     *
     * @return the {@link McpBridge} for MCP server interactions
     */
    public McpBridge mcp() {
        if (this.mcpBridge == null) {
            Mcp mcpClient = new Mcp(
                    this.equinixClient.getEquinixCredentialsProvider().getCredentials());
            mcpClient.initialize();
            this.mcpBridge = new McpBridge(mcpClient);
        }
        return mcpBridge;
    }

    /**
     * Returns the MCP bridge with custom configuration.
     *
     * @param config the MCP client configuration (endpoint URLs, timeouts, etc.)
     * @return the {@link McpBridge} for MCP server interactions
     */
    public McpBridge mcp(McpClientConfig config) {
        if (this.mcpBridge == null) {
            Mcp mcpClient = new Mcp(
                    this.equinixClient.getEquinixCredentialsProvider().getCredentials(), config);
            mcpClient.initialize();
            this.mcpBridge = new McpBridge(mcpClient);
        }
        return mcpBridge;
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
