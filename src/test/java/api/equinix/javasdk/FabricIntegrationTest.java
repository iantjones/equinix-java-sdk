package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.exception.EquinixNotFoundException;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.*;
import api.equinix.javasdk.fabric.model.implementation.AccessPoint;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.implementation.ConnectionSide;
import api.equinix.javasdk.fabric.model.implementation.LinkProtocol;
import api.equinix.javasdk.fabric.model.implementation.ServiceMetro;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live integration tests for the Fabric domain of the Equinix Java SDK.
 *
 * <p>The live tier's purpose is spec-vs-reality: prove the real Fabric v4 API matches what the
 * SDK (built from the OpenAPI catalog specs) expects. Coverage is catalog-complete for every
 * safe operation the SDK exposes:
 * <ul>
 *     <li><b>integration-readonly</b> - every GET/list and POST-search operation. Calls go
 *         through {@code requireEntitled}, which skips ONLY on 401/403 (credential not entitled)
 *         and fails on everything else (deserialization crash, 5xx, unmapped enum, 404 on a
 *         collection URL). Item-GETs discover a live uuid from the corresponding list/search
 *         first and skip via {@link Assumptions} when the account has none.</li>
 *     <li><b>integration-dryrun</b> - the documented dry-run creates and updates
 *         ({@code dryRun=true}) and dedicated validate endpoints. Still zero real mutations,
 *         and every live payload is harmless-by-construction (no-op renames of live-discovered
 *         resources, cleanup-registered creates) so that even a regression that silently
 *         dropped the {@code dryRun} parameter could not do damage.</li>
 *     <li><b>integration-full</b> - full CRUD lifecycle with automatic cleanup. Irreversible;
 *         double opt-in required.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * mvn test -Pintegration-readonly -DaccessKey=ID -DsecretKey=SECRET
 * mvn test -Pintegration-dryrun   -DaccessKey=ID -DsecretKey=SECRET -DtestMode=dryrun
 * mvn test -Pintegration-full     -DaccessKey=ID -DsecretKey=SECRET -DtestMode=full -DconfirmDestructive=true
 * </pre>
 */
@Tag("integration-readonly")
class FabricIntegrationTest extends IntegrationTestBase {

    static Fabric fabric;

    /** First DOT1Q port discovered during setup (or {@code null} when the account has none). */
    static String portUuid;

    /** A second, distinct DOT1Q port for port-to-port dry-run creates (or {@code null}). */
    static String secondPortUuid;

    static final LocalDateTime endDateTime = LocalDateTime.now(ZoneOffset.UTC);
    static final LocalDateTime startDateTime = endDateTime.minusDays(1);

    // ── Discovery caches (one live call each, shared across data-dependent tests) ──

    private static PaginatedList<Port> cachedPorts;
    private static PaginatedFilteredList<Connection> cachedConnections;
    private static PaginatedFilteredList<CloudRouter> cachedCloudRouters;
    private static PaginatedFilteredList<RouteFilter> cachedRouteFilters;
    private static PaginatedFilteredList<RouteAggregation> cachedRouteAggregations;
    private static PaginatedFilteredList<Network> cachedNetworks;
    private static PaginatedList<Stream> cachedStreams;
    private static PaginatedList<ServiceProfile> cachedServiceProfiles;
    private static PaginatedFilteredList<CompanyProfile> cachedCompanyProfiles;
    private static PaginatedList<PrecisionTime> cachedTimeServices;
    private static PaginatedList<Agent> cachedAgents;

    private static boolean routingProtocolDiscoveryDone;
    private static String routingProtocolConnectionId;
    private static String routingProtocolUuid;

    @BeforeAll
    static void setupFabric() {
        fabric = new Fabric(testCredentials());
        fabric.authenticate();

        // Best-effort DOT1Q port discovery for tests that need a port uuid. Failures here are
        // deliberately swallowed rather than skipping the whole class: the dedicated ports_list
        // test surfaces the real error through requireEntitled, while port-dependent tests skip
        // individually via Assumptions when no port was found.
        try {
            for (Port port : fabric.ports().list()) {
                if (port.getEncapsulation() != null && port.getEncapsulation().getType() == EncapsulationType.DOT1Q) {
                    if (portUuid == null) {
                        portUuid = port.getUuid();
                    } else if (!port.getUuid().equals(portUuid)) {
                        secondPortUuid = port.getUuid();
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            System.err.println("[SETUP] Port discovery failed (the ports_list test will report it): " + e.getMessage());
        }
    }

    // ── Discovery helpers ──────────────────────────────────────────────────
    //
    // Each helper runs its underlying read through requireEntitled, so a missing entitlement
    // aborts (skips) the calling test while any real API defect fails it.

    static <T> T firstOrNull(Iterable<T> items) {
        if (items == null) return null;
        var it = items.iterator();
        return it.hasNext() ? it.next() : null;
    }

    static PaginatedList<Port> discoverPorts() {
        if (cachedPorts == null) {
            cachedPorts = requireEntitled("Fabric", "list", "Port", "GET", () -> fabric.ports().list());
        }
        return cachedPorts;
    }

    static PaginatedFilteredList<Connection> discoverConnections() {
        if (cachedConnections == null) {
            cachedConnections = requireEntitled("Fabric", "search", "Connection", "POST",
                    () -> fabric.connections().search());
        }
        return cachedConnections;
    }

    static PaginatedFilteredList<CloudRouter> discoverCloudRouters() {
        if (cachedCloudRouters == null) {
            cachedCloudRouters = requireEntitled("Fabric", "search", "CloudRouter", "POST",
                    () -> fabric.cloudRouters().search());
        }
        return cachedCloudRouters;
    }

    static PaginatedFilteredList<RouteFilter> discoverRouteFilters() {
        if (cachedRouteFilters == null) {
            cachedRouteFilters = requireEntitled("Fabric", "search", "RouteFilter", "POST",
                    () -> fabric.routeFilters().search());
        }
        return cachedRouteFilters;
    }

    static PaginatedFilteredList<RouteAggregation> discoverRouteAggregations() {
        if (cachedRouteAggregations == null) {
            cachedRouteAggregations = requireEntitled("Fabric", "search", "RouteAggregation", "POST",
                    () -> fabric.routeAggregations().search());
        }
        return cachedRouteAggregations;
    }

    static PaginatedFilteredList<Network> discoverNetworks() {
        if (cachedNetworks == null) {
            cachedNetworks = requireEntitled("Fabric", "search", "Network", "POST",
                    () -> fabric.networks().search());
        }
        return cachedNetworks;
    }

    static PaginatedList<Stream> discoverStreams() {
        if (cachedStreams == null) {
            cachedStreams = requireEntitled("Fabric", "list", "Stream", "GET", () -> fabric.streams().list());
        }
        return cachedStreams;
    }

    static PaginatedList<ServiceProfile> discoverServiceProfiles() {
        if (cachedServiceProfiles == null) {
            cachedServiceProfiles = requireEntitled("Fabric", "list", "ServiceProfile", "GET",
                    () -> fabric.serviceProfiles().list());
        }
        return cachedServiceProfiles;
    }

    static PaginatedFilteredList<CompanyProfile> discoverCompanyProfiles() {
        if (cachedCompanyProfiles == null) {
            cachedCompanyProfiles = requireEntitled("Fabric", "search", "CompanyProfile", "POST",
                    () -> fabric.companyProfiles().search());
        }
        return cachedCompanyProfiles;
    }

    static PaginatedList<PrecisionTime> discoverTimeServices() {
        if (cachedTimeServices == null) {
            cachedTimeServices = requireEntitled("Fabric", "list", "PrecisionTime", "GET",
                    () -> fabric.precisionTimes().list());
        }
        return cachedTimeServices;
    }

    static PaginatedList<Agent> discoverAgents() {
        if (cachedAgents == null) {
            cachedAgents = requireEntitled("Fabric", "list", "Agent", "GET", () -> fabric.agents().list());
        }
        return cachedAgents;
    }

    static boolean sideIsCloudRouter(ConnectionSide side) {
        AccessPoint accessPoint = side == null ? null : side.getAccessPoint();
        return accessPoint != null && accessPoint.getType() == AccessPointType.CLOUD_ROUTER;
    }

    static boolean hasCloudRouterSide(Connection connection) {
        return sideIsCloudRouter(connection.getASide()) || sideIsCloudRouter(connection.getZSide());
    }

    /** First connection with a Cloud Router on either side (route/attachment endpoints need one). */
    static Connection findCloudRouterConnection() {
        for (Connection connection : discoverConnections()) {
            if (hasCloudRouterSide(connection)) {
                return connection;
            }
        }
        return null;
    }

    /**
     * Finds a (connectionId, routingProtocolId) pair by probing the first few Cloud Router
     * connections. Returns {@code true} when one was found; results are cached.
     */
    static boolean discoverRoutingProtocol() {
        if (!routingProtocolDiscoveryDone) {
            routingProtocolDiscoveryDone = true;
            int examined = 0;
            for (Connection connection : discoverConnections()) {
                if (!hasCloudRouterSide(connection)) {
                    continue;
                }
                if (++examined > 10) {
                    break;
                }
                String connectionId = connection.getUuid();
                PaginatedList<RoutingProtocol> protocols = requireEntitled("Fabric", "list", "RoutingProtocol", "GET",
                        () -> fabric.routingProtocols().list(connectionId));
                if (!protocols.isEmpty()) {
                    routingProtocolConnectionId = connectionId;
                    routingProtocolUuid = protocols.get(0).getUuid();
                    break;
                }
            }
        }
        return routingProtocolUuid != null;
    }

    /** Picks a VLAN tag that is not currently configured on the given port. */
    static Integer findFreeVlan(String portId) {
        List<PortVlan> vlans = requireEntitled("Fabric", "getVlans", "PortVlan", "GET",
                () -> fabric.ports().getVlans(portId));
        Set<Integer> used = new HashSet<>();
        for (PortVlan vlan : vlans) {
            if (vlan.getVlanTag() != null) {
                used.add(vlan.getVlanTag());
            }
            if (vlan.getVlanSTag() != null) {
                used.add(vlan.getVlanSTag());
            }
        }
        for (int candidate = 2; candidate < 4090; candidate++) {
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════
    //  READONLY TESTS - every safe GET/list/search operation, per resource
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Metros")
    class MetroTests {

        @Test
        @DisplayName("metros_list - List all metros (getMetros)")
        void metros_list() {
            PaginatedList<Metro> metros = requireEntitled("Fabric", "list", "Metro", "GET",
                    () -> fabric.metros().list());
            assertNotNull(metros);
            assertTrue(metros.size() > 0, "Expected at least one metro");
            Metro first = metros.get(0);
            assertNotNull(first.getCode());
            first.getName();
            first.getRegion();
        }

        @Test
        @DisplayName("metros_listMyPorts - List metros filtered by presence (getMetros?presence=MY_PORTS)")
        void metros_listMyPorts() {
            PaginatedList<Metro> myMetros = requireEntitled("Fabric", "list", "Metro", "GET",
                    () -> fabric.metros().list(MetroPresence.MY_PORTS));
            assertNotNull(myMetros);
            if (!myMetros.isEmpty()) {
                assertNotNull(myMetros.get(0).getCode());
            }
        }

        @Test
        @DisplayName("metros_getByCode - Get metro by code (getMetroByCode)")
        void metros_getByCode() {
            Metro metro = requireEntitled("Fabric", "getByCode", "Metro", "GET",
                    () -> fabric.metros().getByMetroCode(MetroCode.LA));
            assertNotNull(metro);
            assertEquals(MetroCode.LA, metro.getCode());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Health")
    class HealthTests {

        @Test
        @DisplayName("health_check - Fabric health endpoint (getStatus)")
        void health_check() {
            HealthStatus healthStatus = requireEntitled("Fabric", "health", "HealthStatus", "GET",
                    () -> fabric.health());
            assertNotNull(healthStatus);
            healthStatus.getState();
            healthStatus.getApiServices();
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Ports")
    class PortTests {

        @Test
        @DisplayName("ports_list - List all ports (getPorts)")
        void ports_list() {
            PaginatedList<Port> ports = requireEntitled("Fabric", "list", "Port", "GET",
                    () -> fabric.ports().list());
            assertNotNull(ports);
            assertTrue(ports.size() > 0, "Expected at least one port");
            Port first = ports.get(0);
            assertNotNull(first.getUuid());
            first.getName();
            first.getState();
        }

        @Test
        @DisplayName("ports_search - Search ports (searchPorts)")
        void ports_search() {
            PaginatedFilteredList<Port> ports = requireEntitled("Fabric", "search", "Port", "POST",
                    () -> fabric.ports().search());
            assertNotNull(ports);
            if (!ports.isEmpty()) {
                assertNotNull(ports.get(0).getUuid());
            }
        }

        @Test
        @DisplayName("ports_getByUuid - Get port by UUID (getPortByUuid)")
        void ports_getByUuid() {
            Port anyPort = firstOrNull(discoverPorts());
            Assumptions.assumeTrue(anyPort != null, "No ports found; skipping port get test");

            Port port = requireEntitled("Fabric", "getByUuid", "Port", "GET",
                    () -> fabric.ports().getByUuid(anyPort.getUuid()));
            assertNotNull(port);
            assertEquals(anyPort.getUuid(), port.getUuid());
        }

        @Test
        @DisplayName("ports_getVlans - List a port's VLANs / link protocols (getVlans)")
        void ports_getVlans() {
            Assumptions.assumeTrue(portUuid != null, "No DOT1Q port found; skipping VLAN list test");

            List<PortVlan> vlans = requireEntitled("Fabric", "getVlans", "PortVlan", "GET",
                    () -> fabric.ports().getVlans(portUuid));
            assertNotNull(vlans);
            if (!vlans.isEmpty()) {
                PortVlan first = vlans.get(0);
                first.getType();
                first.getVlanTag();
            }
        }

        @Test
        @DisplayName("ports_getMetrics - Port metrics by asset id (getMetricByAssetId)")
        void ports_getMetrics() {
            Port anyPort = firstOrNull(discoverPorts());
            Assumptions.assumeTrue(anyPort != null, "No ports found; skipping port metrics test");

            List<Metric> metrics = requireEntitled("Fabric", "getMetrics", "Metric", "GET",
                    () -> fabric.ports().getMetrics(anyPort.getUuid(),
                            "equinix.fabric.port.bandwidth_rx.usage", startDateTime, endDateTime));
            assertNotNull(metrics);
            if (!metrics.isEmpty()) {
                Metric first = metrics.get(0);
                assertNotNull(first.getName());
                first.getDatapoints();
            }
        }

        @Test
        @SuppressWarnings("deprecation")
        @DisplayName("ports_getStatistics - Deprecated port stats endpoint still honours the contract (getPortStatsByPortUuid)")
        void ports_getStatistics_deprecated() {
            Port anyPort = firstOrNull(discoverPorts());
            Assumptions.assumeTrue(anyPort != null, "No ports found; skipping port statistics test");

            PortStatistic statistic = requireEntitled("Fabric", "getStatistics", "PortStatistic", "GET",
                    () -> fabric.ports().getStatistics(anyPort.getUuid(), startDateTime, endDateTime));
            assertNotNull(statistic);
            statistic.getStats();
        }

        @Test
        @DisplayName("portPackages_list - List port packages (getPortPackages)")
        void portPackages_list() {
            List<PortPackage> packages = requireEntitled("Fabric", "list", "PortPackage", "GET",
                    () -> fabric.portPackages().list());
            assertNotNull(packages);
            if (!packages.isEmpty()) {
                PortPackage first = packages.get(0);
                assertNotNull(first.getCode());
                first.getType();
            }
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Connections")
    class ConnectionTests {

        @Test
        @DisplayName("connections_search - Search connections (searchConnections)")
        void connections_search() {
            PaginatedFilteredList<Connection> connections = requireEntitled("Fabric", "search", "Connection", "POST",
                    () -> fabric.connections().search());
            assertNotNull(connections);
            if (!connections.isEmpty()) {
                Connection first = connections.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("connections_getByUuid - Get connection by UUID (getConnectionByUuid)")
        void connections_getByUuid() {
            Connection any = firstOrNull(discoverConnections());
            Assumptions.assumeTrue(any != null, "No connections found; skipping get test");

            Connection connection = requireEntitled("Fabric", "getByUuid", "Connection", "GET",
                    () -> fabric.connections().getByUuid(any.getUuid()));
            assertNotNull(connection);
            assertEquals(any.getUuid(), connection.getUuid());
        }

        @Test
        @DisplayName("connections_getMetrics - Connection metrics by asset id (getMetricByAssetId)")
        void connections_getMetrics() {
            Connection any = firstOrNull(discoverConnections());
            Assumptions.assumeTrue(any != null, "No connections found; skipping connection metrics test");

            List<Metric> metrics = requireEntitled("Fabric", "getMetrics", "Metric", "GET",
                    () -> fabric.connections().getMetrics(any.getUuid(),
                            "equinix.fabric.connection.bandwidth_tx.usage", startDateTime, endDateTime));
            assertNotNull(metrics);
        }

        @Test
        @SuppressWarnings("deprecation")
        @DisplayName("connections_getStatistics - Deprecated connection stats endpoint (getConnectionStatsByPortUuid)")
        void connections_getStatistics_deprecated() {
            Connection any = firstOrNull(discoverConnections());
            Assumptions.assumeTrue(any != null, "No connections found; skipping connection statistics test");

            ConnectionStatistic statistic = requireEntitled("Fabric", "getStatistics", "ConnectionStatistic", "GET",
                    () -> fabric.connections().getStatistics(any.getUuid(), startDateTime, endDateTime));
            assertNotNull(statistic);
            statistic.getStats();
        }

        @Test
        @DisplayName("connections_searchAdvertisedRoutes - Advertised routes of an FCR connection (searchConnectionAdvertisedRoutes)")
        void connections_searchAdvertisedRoutes() {
            Assumptions.assumeTrue(discoverRoutingProtocol(),
                    "No Cloud Router connection with a routing protocol found; skipping advertised-routes search");

            PaginatedFilteredList<RouteTableEntry> routes = requireEntitled("Fabric", "searchAdvertisedRoutes", "RouteTableEntry", "POST",
                    () -> fabric.connections().searchAdvertisedRoutes(routingProtocolConnectionId));
            assertNotNull(routes);
            if (!routes.isEmpty()) {
                RouteTableEntry first = routes.get(0);
                first.getPrefix();
                first.getType();
            }
        }

        @Test
        @DisplayName("connections_searchReceivedRoutes - Received routes of an FCR connection (searchConnectionReceivedRoutes)")
        void connections_searchReceivedRoutes() {
            Assumptions.assumeTrue(discoverRoutingProtocol(),
                    "No Cloud Router connection with a routing protocol found; skipping received-routes search");

            PaginatedFilteredList<RouteTableEntry> routes = requireEntitled("Fabric", "searchReceivedRoutes", "RouteTableEntry", "POST",
                    () -> fabric.connections().searchReceivedRoutes(routingProtocolConnectionId));
            assertNotNull(routes);
        }

        @Test
        @DisplayName("connections_routeAggregations_list - Route Aggregations attached to a connection (getConnectionRouteAggregations)")
        void connections_routeAggregations_list() {
            Connection fcrConnection = findCloudRouterConnection();
            Assumptions.assumeTrue(fcrConnection != null, "No Cloud Router connection found; skipping attachment list");

            List<RouteAggregationAttachment> attachments = requireEntitled("Fabric", "getRouteAggregations", "RouteAggregationAttachment", "GET",
                    () -> fabric.connections().getRouteAggregations(fcrConnection.getUuid()));
            assertNotNull(attachments);
        }

        @Test
        @DisplayName("connections_routeAggregations_getByUuid - A single attached Route Aggregation (getConnectionRouteAggregationByUuid)")
        void connections_routeAggregations_getByUuid() {
            Connection fcrConnection = findCloudRouterConnection();
            Assumptions.assumeTrue(fcrConnection != null, "No Cloud Router connection found; skipping attachment get");

            List<RouteAggregationAttachment> attachments = requireEntitled("Fabric", "getRouteAggregations", "RouteAggregationAttachment", "GET",
                    () -> fabric.connections().getRouteAggregations(fcrConnection.getUuid()));
            RouteAggregationAttachment any = firstOrNull(attachments);
            Assumptions.assumeTrue(any != null, "No Route Aggregations attached to the connection; skipping get");

            RouteAggregationAttachment attachment = requireEntitled("Fabric", "getRouteAggregation", "RouteAggregationAttachment", "GET",
                    () -> fabric.connections().getRouteAggregation(fcrConnection.getUuid(), any.getUuid()));
            assertNotNull(attachment);
            assertEquals(any.getUuid(), attachment.getUuid());
        }

        @Test
        @DisplayName("connections_routeFilters_list - Route Filters attached to a connection (getConnectionRouteFilters)")
        void connections_routeFilters_list() {
            Connection fcrConnection = findCloudRouterConnection();
            Assumptions.assumeTrue(fcrConnection != null, "No Cloud Router connection found; skipping attachment list");

            List<RouteFilterAttachment> attachments = requireEntitled("Fabric", "getRouteFilters", "RouteFilterAttachment", "GET",
                    () -> fabric.connections().getRouteFilters(fcrConnection.getUuid()));
            assertNotNull(attachments);
        }

        @Test
        @DisplayName("connections_routeFilters_getByUuid - A single attached Route Filter (getConnectionRouteFilterByUuid)")
        void connections_routeFilters_getByUuid() {
            Connection fcrConnection = findCloudRouterConnection();
            Assumptions.assumeTrue(fcrConnection != null, "No Cloud Router connection found; skipping attachment get");

            List<RouteFilterAttachment> attachments = requireEntitled("Fabric", "getRouteFilters", "RouteFilterAttachment", "GET",
                    () -> fabric.connections().getRouteFilters(fcrConnection.getUuid()));
            RouteFilterAttachment any = firstOrNull(attachments);
            Assumptions.assumeTrue(any != null, "No Route Filters attached to the connection; skipping get");

            RouteFilterAttachment attachment = requireEntitled("Fabric", "getRouteFilter", "RouteFilterAttachment", "GET",
                    () -> fabric.connections().getRouteFilter(fcrConnection.getUuid(), any.getUuid()));
            assertNotNull(attachment);
            assertEquals(any.getUuid(), attachment.getUuid());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Routing Protocols")
    class RoutingProtocolTests {

        @Test
        @DisplayName("routingProtocols_list - Routing protocols on a connection (getConnectionRoutingProtocols)")
        void routingProtocols_list() {
            Connection fcrConnection = findCloudRouterConnection();
            Assumptions.assumeTrue(fcrConnection != null, "No Cloud Router connection found; skipping routing protocol list");

            PaginatedList<RoutingProtocol> protocols = requireEntitled("Fabric", "list", "RoutingProtocol", "GET",
                    () -> fabric.routingProtocols().list(fcrConnection.getUuid()));
            assertNotNull(protocols);
            if (!protocols.isEmpty()) {
                RoutingProtocol first = protocols.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("routingProtocols_getByUuid - Get a routing protocol (getConnectionRoutingProtocolByUuid)")
        void routingProtocols_getByUuid() {
            Assumptions.assumeTrue(discoverRoutingProtocol(), "No routing protocol found; skipping get");

            RoutingProtocol protocol = requireEntitled("Fabric", "getByUuid", "RoutingProtocol", "GET",
                    () -> fabric.routingProtocols().getByUuid(routingProtocolConnectionId, routingProtocolUuid));
            assertNotNull(protocol);
            assertEquals(routingProtocolUuid, protocol.getUuid());
        }

        @Test
        @DisplayName("routingProtocols_bgpActions_list - All BGP actions (getConnectionRoutingProtocolAllBgpActions)")
        void routingProtocols_bgpActions_list() {
            Assumptions.assumeTrue(discoverRoutingProtocol(), "No routing protocol found; skipping BGP actions list");

            List<BGPAction> actions = requireEntitled("Fabric", "getBgpActions", "BGPAction", "GET",
                    () -> fabric.routingProtocols().getBgpActions(routingProtocolConnectionId, routingProtocolUuid));
            assertNotNull(actions);
            if (!actions.isEmpty()) {
                BGPAction first = actions.get(0);
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("routingProtocols_bgpActions_getByUuid - A single BGP action (getConnectionRoutingProtocolsBgpActionByUuid)")
        void routingProtocols_bgpActions_getByUuid() {
            Assumptions.assumeTrue(discoverRoutingProtocol(), "No routing protocol found; skipping BGP action get");

            List<BGPAction> actions = requireEntitled("Fabric", "getBgpActions", "BGPAction", "GET",
                    () -> fabric.routingProtocols().getBgpActions(routingProtocolConnectionId, routingProtocolUuid));
            BGPAction any = firstOrNull(actions);
            Assumptions.assumeTrue(any != null, "No BGP actions recorded; skipping get");

            BGPAction action = requireEntitled("Fabric", "getBgpAction", "BGPAction", "GET",
                    () -> fabric.routingProtocols().getBgpAction(routingProtocolConnectionId, routingProtocolUuid, any.getUuid()));
            assertNotNull(action);
            assertEquals(any.getUuid(), action.getUuid());
        }

        @Test
        @DisplayName("routingProtocols_changes_list - Routing protocol changes (getConnectionRoutingProtocolsChanges)")
        void routingProtocols_changes_list() {
            Assumptions.assumeTrue(discoverRoutingProtocol(), "No routing protocol found; skipping changes list");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.routingProtocols().getChanges(routingProtocolConnectionId, routingProtocolUuid));
            assertNotNull(changes);
            if (!changes.isEmpty()) {
                Change first = changes.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getStatus();
            }
        }

        @Test
        @DisplayName("routingProtocols_changes_getByUuid - A single routing protocol change (getConnectionRoutingProtocolsChangeByUuid)")
        void routingProtocols_changes_getByUuid() {
            Assumptions.assumeTrue(discoverRoutingProtocol(), "No routing protocol found; skipping change get");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.routingProtocols().getChanges(routingProtocolConnectionId, routingProtocolUuid));
            Change any = firstOrNull(changes);
            Assumptions.assumeTrue(any != null, "No routing protocol changes recorded; skipping get");

            Change change = requireEntitled("Fabric", "getChange", "Change", "GET",
                    () -> fabric.routingProtocols().getChange(routingProtocolConnectionId, routingProtocolUuid, any.getUuid()));
            assertNotNull(change);
            assertEquals(any.getUuid(), change.getUuid());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Service Tokens")
    class ServiceTokenTests {

        @Test
        @DisplayName("serviceTokens_list - List service tokens (getServiceTokens)")
        void serviceTokens_list() {
            PaginatedList<ServiceToken> tokens = requireEntitled("Fabric", "list", "ServiceToken", "GET",
                    () -> fabric.serviceTokens().list());
            assertNotNull(tokens);
            if (!tokens.isEmpty()) {
                ServiceToken first = tokens.get(0);
                assertNotNull(first.getUuid());
                first.getState();
            }
        }

        @Test
        @DisplayName("serviceTokens_search - Search service tokens (searchServiceTokens)")
        void serviceTokens_search() {
            PaginatedFilteredList<ServiceToken> tokens = requireEntitled("Fabric", "search", "ServiceToken", "POST",
                    () -> fabric.serviceTokens().search());
            assertNotNull(tokens);
        }

        @Test
        @DisplayName("serviceTokens_getByUuid - Get service token by UUID (getServiceTokenByUuid)")
        void serviceTokens_getByUuid() {
            PaginatedList<ServiceToken> tokens = requireEntitled("Fabric", "list", "ServiceToken", "GET",
                    () -> fabric.serviceTokens().list());
            ServiceToken any = firstOrNull(tokens);
            Assumptions.assumeTrue(any != null, "No service tokens found; skipping get");

            ServiceToken token = requireEntitled("Fabric", "getByUuid", "ServiceToken", "GET",
                    () -> fabric.serviceTokens().getByUuid(any.getUuid()));
            assertNotNull(token);
            assertEquals(any.getUuid(), token.getUuid());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Service Profiles")
    class ServiceProfileTests {

        @Test
        @DisplayName("serviceProfiles_list - List service profiles (getServiceProfiles)")
        void serviceProfiles_list() {
            PaginatedList<ServiceProfile> serviceProfiles = requireEntitled("Fabric", "list", "ServiceProfile", "GET",
                    () -> fabric.serviceProfiles().list());
            assertNotNull(serviceProfiles);
            assertTrue(serviceProfiles.size() > 0, "Expected at least one service profile");
            ServiceProfile first = serviceProfiles.get(0);
            assertNotNull(first.getUuid());
            first.getType();
            first.getVisibility();
        }

        @Test
        @DisplayName("serviceProfiles_search - Search service profiles (searchServiceProfiles)")
        void serviceProfiles_search() {
            PaginatedFilteredList<ServiceProfile> serviceProfiles = requireEntitled("Fabric", "search", "ServiceProfile", "POST",
                    () -> fabric.serviceProfiles().search());
            assertNotNull(serviceProfiles);
        }

        @Test
        @DisplayName("serviceProfiles_getByUuid - Get service profile by UUID (getServiceProfileByUuid)")
        void serviceProfiles_getByUuid() {
            ServiceProfile any = firstOrNull(discoverServiceProfiles());
            Assumptions.assumeTrue(any != null, "No service profiles found; skipping get");

            ServiceProfile profile = requireEntitled("Fabric", "getByUuid", "ServiceProfile", "GET",
                    () -> fabric.serviceProfiles().getByUuid(any.getUuid()));
            assertNotNull(profile);
            assertEquals(any.getUuid(), profile.getUuid());
        }

        @Test
        @DisplayName("serviceProfiles_getMetros - Service profile metros (getServiceProfileMetrosByUuid)")
        void serviceProfiles_getMetros() {
            ServiceProfile any = firstOrNull(discoverServiceProfiles());
            Assumptions.assumeTrue(any != null, "No service profiles found; skipping metros get");

            List<ServiceMetro> metros = requireEntitled("Fabric", "getMetros", "ServiceMetro", "GET",
                    () -> fabric.serviceProfiles().getMetros(any.getUuid()));
            assertNotNull(metros);
            if (!metros.isEmpty()) {
                metros.get(0).getName();
            }
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Cloud Routers")
    class CloudRouterTests {

        @Test
        @DisplayName("cloudRouters_search - Search cloud routers (searchCloudRouters)")
        void cloudRouters_search() {
            PaginatedFilteredList<CloudRouter> cloudRouters = requireEntitled("Fabric", "search", "CloudRouter", "POST",
                    () -> fabric.cloudRouters().search());
            assertNotNull(cloudRouters);
            if (!cloudRouters.isEmpty()) {
                CloudRouter first = cloudRouters.get(0);
                assertNotNull(first.getUuid());
                first.getState();
            }
        }

        @Test
        @DisplayName("cloudRouters_getByUuid - Get cloud router by UUID (getCloudRouterByUuid)")
        void cloudRouters_getByUuid() {
            CloudRouter any = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(any != null, "No cloud routers found; skipping get");

            CloudRouter cloudRouter = requireEntitled("Fabric", "getByUuid", "CloudRouter", "GET",
                    () -> fabric.cloudRouters().getByUuid(any.getUuid()));
            assertNotNull(cloudRouter);
            assertEquals(any.getUuid(), cloudRouter.getUuid());
        }

        @Test
        @DisplayName("routerPackages_list - Cloud router packages (getCloudRouterPackages)")
        void routerPackages_list() {
            PaginatedList<CloudRouterPackage> packages = requireEntitled("Fabric", "list", "CloudRouterPackage", "GET",
                    () -> fabric.cloudRouters().routerPackages());
            assertNotNull(packages);
            assertTrue(packages.size() > 0, "Expected at least one router package");
            CloudRouterPackage first = packages.get(0);
            assertNotNull(first.getCode());
            first.getType();
        }

        @Test
        @DisplayName("routerPackages_getByCode - Cloud router package by code (getCloudRouterPackageByCode)")
        void routerPackages_getByCode() {
            PaginatedList<CloudRouterPackage> packages = requireEntitled("Fabric", "list", "CloudRouterPackage", "GET",
                    () -> fabric.cloudRouters().routerPackages());
            CloudRouterPackage any = firstOrNull(packages);
            Assumptions.assumeTrue(any != null && any.getCode() != null, "No router packages found; skipping get by code");

            CloudRouterPackage routerPackage = requireEntitled("Fabric", "getByCode", "CloudRouterPackage", "GET",
                    () -> fabric.cloudRouters().routerPackageByCode(any.getCode()));
            assertNotNull(routerPackage);
            assertEquals(any.getCode(), routerPackage.getCode());
        }

        @Test
        @DisplayName("cloudRouters_searchRoutes - Route table entries of a router (searchCloudRouterRoutes)")
        void cloudRouters_searchRoutes() {
            CloudRouter router = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(router != null, "No cloud routers found; skipping routes search");

            PaginatedFilteredList<RouteTableEntry> routes = requireEntitled("Fabric", "searchRoutes", "RouteTableEntry", "POST",
                    () -> fabric.cloudRouters().searchRoutes(router.getUuid()));
            assertNotNull(routes);
            if (!routes.isEmpty()) {
                routes.get(0).getPrefix();
            }
        }

        @Test
        @DisplayName("cloudRouters_actions_list - Router actions (getCloudRouterActions)")
        void cloudRouters_actions_list() {
            CloudRouter router = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(router != null, "No cloud routers found; skipping actions list");

            List<CloudRouterAction> actions = requireEntitled("Fabric", "getActions", "CloudRouterAction", "GET",
                    () -> fabric.cloudRouters().getActions(router.getUuid()));
            assertNotNull(actions);
            if (!actions.isEmpty()) {
                CloudRouterAction first = actions.get(0);
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("cloudRouters_actions_getByUuid - A single router action (getCloudRouterActionsByUuid)")
        void cloudRouters_actions_getByUuid() {
            CloudRouter router = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(router != null, "No cloud routers found; skipping action get");

            List<CloudRouterAction> actions = requireEntitled("Fabric", "getActions", "CloudRouterAction", "GET",
                    () -> fabric.cloudRouters().getActions(router.getUuid()));
            CloudRouterAction any = firstOrNull(actions);
            Assumptions.assumeTrue(any != null, "No router actions recorded; skipping get");

            CloudRouterAction action = requireEntitled("Fabric", "getAction", "CloudRouterAction", "GET",
                    () -> fabric.cloudRouters().getAction(router.getUuid(), any.getUuid()));
            assertNotNull(action);
            assertEquals(any.getUuid(), action.getUuid());
        }

        @Test
        @DisplayName("cloudRouters_actions_search - Search router actions (searchRouterActions)")
        void cloudRouters_actions_search() {
            CloudRouter router = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(router != null, "No cloud routers found; skipping actions search");

            PaginatedFilteredList<CloudRouterAction> actions = requireEntitled("Fabric", "searchActions", "CloudRouterAction", "POST",
                    () -> fabric.cloudRouters().searchActions(router.getUuid(), null, null));
            assertNotNull(actions);
        }

        @Test
        @DisplayName("cloudRouters_commands_list - Router commands (getAllCloudRouterCommands)")
        void cloudRouters_commands_list() {
            CloudRouter router = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(router != null, "No cloud routers found; skipping commands list");

            PaginatedList<CloudRouterCommand> commands = requireEntitled("Fabric", "commands", "CloudRouterCommand", "GET",
                    () -> fabric.cloudRouters().commands(router.getUuid()));
            assertNotNull(commands);
            if (!commands.isEmpty()) {
                CloudRouterCommand first = commands.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("cloudRouters_commands_getByUuid - A single router command (getCloudRouterCommand)")
        void cloudRouters_commands_getByUuid() {
            CloudRouter router = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(router != null, "No cloud routers found; skipping command get");

            PaginatedList<CloudRouterCommand> commands = requireEntitled("Fabric", "commands", "CloudRouterCommand", "GET",
                    () -> fabric.cloudRouters().commands(router.getUuid()));
            CloudRouterCommand any = firstOrNull(commands);
            Assumptions.assumeTrue(any != null, "No router commands recorded; skipping get");

            CloudRouterCommand command = requireEntitled("Fabric", "getCommand", "CloudRouterCommand", "GET",
                    () -> fabric.cloudRouters().getCommand(router.getUuid(), any.getUuid()));
            assertNotNull(command);
            assertEquals(any.getUuid(), command.getUuid());
        }

        @Test
        @DisplayName("cloudRouters_commands_search - Search router commands (searchCloudRouterCommands)")
        void cloudRouters_commands_search() {
            CloudRouter router = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(router != null, "No cloud routers found; skipping commands search");

            PaginatedFilteredList<CloudRouterCommand> commands = requireEntitled("Fabric", "searchCommands", "CloudRouterCommand", "POST",
                    () -> fabric.cloudRouters().searchCommands(router.getUuid(), null, null));
            assertNotNull(commands);
        }

        @Test
        @DisplayName("cloudRouters_routeFilterAttachments_search - Route filter attachments (searchCloudRouterRouteFilterAttachments)")
        void cloudRouters_routeFilterAttachments_search() {
            CloudRouter router = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(router != null, "No cloud routers found; skipping attachment search");

            PaginatedFilteredList<RouteFilterAttachment> attachments = requireEntitled("Fabric", "searchRouteFilterAttachments", "RouteFilterAttachment", "POST",
                    () -> fabric.cloudRouters().searchRouteFilterAttachments(router.getUuid(), null, null));
            assertNotNull(attachments);
        }

        @Test
        @DisplayName("cloudRouters_routeAggregationAttachments_search - Route aggregation attachments (searchCloudRouterRouteAggregationAttachments)")
        void cloudRouters_routeAggregationAttachments_search() {
            CloudRouter router = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(router != null, "No cloud routers found; skipping attachment search");

            PaginatedFilteredList<RouteAggregationAttachment> attachments = requireEntitled("Fabric", "searchRouteAggregationAttachments", "RouteAggregationAttachment", "POST",
                    () -> fabric.cloudRouters().searchRouteAggregationAttachments(router.getUuid(), null, null));
            assertNotNull(attachments);
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Route Filters")
    class RouteFilterTests {

        @Test
        @DisplayName("routeFilters_search - Search route filters (searchRouteFilters)")
        void routeFilters_search() {
            PaginatedFilteredList<RouteFilter> routeFilters = requireEntitled("Fabric", "search", "RouteFilter", "POST",
                    () -> fabric.routeFilters().search());
            assertNotNull(routeFilters);
            if (!routeFilters.isEmpty()) {
                assertNotNull(routeFilters.get(0).getUuid());
            }
        }

        @Test
        @DisplayName("routeFilters_getByUuid - Get route filter by UUID (getRouteFilterByUuid)")
        void routeFilters_getByUuid() {
            RouteFilter any = firstOrNull(discoverRouteFilters());
            Assumptions.assumeTrue(any != null, "No route filters found; skipping get");

            RouteFilter routeFilter = requireEntitled("Fabric", "getByUuid", "RouteFilter", "GET",
                    () -> fabric.routeFilters().getByUuid(any.getUuid()));
            assertNotNull(routeFilter);
            assertEquals(any.getUuid(), routeFilter.getUuid());
        }

        @Test
        @DisplayName("routeFilters_getChanges - Route filter changes (getRouteFilterChanges)")
        void routeFilters_getChanges() {
            RouteFilter any = firstOrNull(discoverRouteFilters());
            Assumptions.assumeTrue(any != null, "No route filters found; skipping changes list");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.routeFilters().getChanges(any.getUuid()));
            assertNotNull(changes);
        }

        @Test
        @DisplayName("routeFilters_getChange - A single route filter change (getRouteFilterChangeByUuid)")
        void routeFilters_getChange() {
            RouteFilter any = firstOrNull(discoverRouteFilters());
            Assumptions.assumeTrue(any != null, "No route filters found; skipping change get");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.routeFilters().getChanges(any.getUuid()));
            Change change = firstOrNull(changes);
            Assumptions.assumeTrue(change != null, "No route filter changes recorded; skipping get");

            Change fetched = requireEntitled("Fabric", "getChange", "Change", "GET",
                    () -> fabric.routeFilters().getChange(any.getUuid(), change.getUuid()));
            assertNotNull(fetched);
            assertEquals(change.getUuid(), fetched.getUuid());
        }

        @Test
        @DisplayName("routeFilters_getConnections - Connections using a route filter (getRouteFilterConnections)")
        void routeFilters_getConnections() {
            RouteFilter any = firstOrNull(discoverRouteFilters());
            Assumptions.assumeTrue(any != null, "No route filters found; skipping connections list");

            List<Connection> connections = requireEntitled("Fabric", "getConnections", "Connection", "GET",
                    () -> fabric.routeFilters().getConnections(any.getUuid()));
            assertNotNull(connections);
        }

        @Test
        @DisplayName("routeFilterRules_list - Rules of a route filter (getRouteFilterRules)")
        void routeFilterRules_list() {
            RouteFilter any = firstOrNull(discoverRouteFilters());
            Assumptions.assumeTrue(any != null, "No route filters found; skipping rules list");

            PaginatedList<RouteFilterRule> rules = requireEntitled("Fabric", "list", "RouteFilterRule", "GET",
                    () -> fabric.routeFilterRules().list(any.getUuid()));
            assertNotNull(rules);
            if (!rules.isEmpty()) {
                RouteFilterRule first = rules.get(0);
                assertNotNull(first.getUuid());
                first.getPrefix();
                first.getAction();
            }
        }

        @Test
        @DisplayName("routeFilterRules_search - Search rules of a route filter (searchRouteFilterRules)")
        void routeFilterRules_search() {
            RouteFilter any = firstOrNull(discoverRouteFilters());
            Assumptions.assumeTrue(any != null, "No route filters found; skipping rules search");

            PaginatedFilteredList<RouteFilterRule> rules = requireEntitled("Fabric", "search", "RouteFilterRule", "POST",
                    () -> fabric.routeFilterRules().search(any.getUuid(), null, null));
            assertNotNull(rules);
        }

        @Test
        @DisplayName("routeFilterRules_getByUuid - A single route filter rule (getRouteFilterRuleByUuid)")
        void routeFilterRules_getByUuid() {
            RouteFilter any = firstOrNull(discoverRouteFilters());
            Assumptions.assumeTrue(any != null, "No route filters found; skipping rule get");

            PaginatedList<RouteFilterRule> rules = requireEntitled("Fabric", "list", "RouteFilterRule", "GET",
                    () -> fabric.routeFilterRules().list(any.getUuid()));
            RouteFilterRule rule = firstOrNull(rules);
            Assumptions.assumeTrue(rule != null, "No route filter rules found; skipping get");

            RouteFilterRule fetched = requireEntitled("Fabric", "getByUuid", "RouteFilterRule", "GET",
                    () -> fabric.routeFilterRules().getByUuid(any.getUuid(), rule.getUuid()));
            assertNotNull(fetched);
            assertEquals(rule.getUuid(), fetched.getUuid());
        }

        @Test
        @DisplayName("routeFilterRules_getChanges - Route filter rule changes (getRouteFilterRuleChanges)")
        void routeFilterRules_getChanges() {
            RouteFilter any = firstOrNull(discoverRouteFilters());
            Assumptions.assumeTrue(any != null, "No route filters found; skipping rule changes");

            PaginatedList<RouteFilterRule> rules = requireEntitled("Fabric", "list", "RouteFilterRule", "GET",
                    () -> fabric.routeFilterRules().list(any.getUuid()));
            RouteFilterRule rule = firstOrNull(rules);
            Assumptions.assumeTrue(rule != null, "No route filter rules found; skipping changes list");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.routeFilterRules().getChanges(any.getUuid(), rule.getUuid()));
            assertNotNull(changes);
        }

        @Test
        @DisplayName("routeFilterRules_getChange - A single rule change (getRouteFilterRuleChangeByUuid)")
        void routeFilterRules_getChange() {
            RouteFilter any = firstOrNull(discoverRouteFilters());
            Assumptions.assumeTrue(any != null, "No route filters found; skipping rule change get");

            PaginatedList<RouteFilterRule> rules = requireEntitled("Fabric", "list", "RouteFilterRule", "GET",
                    () -> fabric.routeFilterRules().list(any.getUuid()));
            RouteFilterRule rule = firstOrNull(rules);
            Assumptions.assumeTrue(rule != null, "No route filter rules found; skipping change get");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.routeFilterRules().getChanges(any.getUuid(), rule.getUuid()));
            Change change = firstOrNull(changes);
            Assumptions.assumeTrue(change != null, "No rule changes recorded; skipping get");

            Change fetched = requireEntitled("Fabric", "getChange", "Change", "GET",
                    () -> fabric.routeFilterRules().getChange(any.getUuid(), rule.getUuid(), change.getUuid()));
            assertNotNull(fetched);
            assertEquals(change.getUuid(), fetched.getUuid());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Route Aggregations")
    class RouteAggregationTests {

        @Test
        @DisplayName("routeAggregations_search - Search route aggregations (searchRouteAggregations)")
        void routeAggregations_search() {
            PaginatedFilteredList<RouteAggregation> routeAggregations = requireEntitled("Fabric", "search", "RouteAggregation", "POST",
                    () -> fabric.routeAggregations().search());
            assertNotNull(routeAggregations);
            if (!routeAggregations.isEmpty()) {
                RouteAggregation first = routeAggregations.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("routeAggregations_getByUuid - Get route aggregation by UUID (getRouteAggregationByUuid)")
        void routeAggregations_getByUuid() {
            RouteAggregation any = firstOrNull(discoverRouteAggregations());
            Assumptions.assumeTrue(any != null, "No route aggregations found; skipping get");

            RouteAggregation routeAggregation = requireEntitled("Fabric", "getByUuid", "RouteAggregation", "GET",
                    () -> fabric.routeAggregations().getByUuid(any.getUuid()));
            assertNotNull(routeAggregation);
            assertEquals(any.getUuid(), routeAggregation.getUuid());
        }

        @Test
        @DisplayName("routeAggregations_getChanges - Route aggregation changes (getRouteAggregationChanges)")
        void routeAggregations_getChanges() {
            RouteAggregation any = firstOrNull(discoverRouteAggregations());
            Assumptions.assumeTrue(any != null, "No route aggregations found; skipping changes list");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.routeAggregations().getChanges(any.getUuid()));
            assertNotNull(changes);
        }

        @Test
        @DisplayName("routeAggregations_getChange - A single route aggregation change (getRouteAggregationChangeByUuid)")
        void routeAggregations_getChange() {
            RouteAggregation any = firstOrNull(discoverRouteAggregations());
            Assumptions.assumeTrue(any != null, "No route aggregations found; skipping change get");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.routeAggregations().getChanges(any.getUuid()));
            Change change = firstOrNull(changes);
            Assumptions.assumeTrue(change != null, "No route aggregation changes recorded; skipping get");

            Change fetched = requireEntitled("Fabric", "getChange", "Change", "GET",
                    () -> fabric.routeAggregations().getChange(any.getUuid(), change.getUuid()));
            assertNotNull(fetched);
            assertEquals(change.getUuid(), fetched.getUuid());
        }

        @Test
        @DisplayName("routeAggregations_getConnections - Connections using a route aggregation (getRouteAggregationConnections)")
        void routeAggregations_getConnections() {
            RouteAggregation any = firstOrNull(discoverRouteAggregations());
            Assumptions.assumeTrue(any != null, "No route aggregations found; skipping connections list");

            List<Connection> connections = requireEntitled("Fabric", "getConnections", "Connection", "GET",
                    () -> fabric.routeAggregations().getConnections(any.getUuid()));
            assertNotNull(connections);
        }

        @Test
        @DisplayName("routeAggregationRules_list - Rules of a route aggregation (getRouteAggregationRules)")
        void routeAggregationRules_list() {
            RouteAggregation any = firstOrNull(discoverRouteAggregations());
            Assumptions.assumeTrue(any != null, "No route aggregations found; skipping rules list");

            PaginatedList<RouteAggregationRule> rules = requireEntitled("Fabric", "list", "RouteAggregationRule", "GET",
                    () -> fabric.routeAggregationRules().list(any.getUuid()));
            assertNotNull(rules);
            if (!rules.isEmpty()) {
                RouteAggregationRule first = rules.get(0);
                assertNotNull(first.getUuid());
                first.getPrefix();
            }
        }

        @Test
        @DisplayName("routeAggregationRules_search - Search rules of a route aggregation (searchRouteAggregationRules)")
        void routeAggregationRules_search() {
            RouteAggregation any = firstOrNull(discoverRouteAggregations());
            Assumptions.assumeTrue(any != null, "No route aggregations found; skipping rules search");

            PaginatedFilteredList<RouteAggregationRule> rules = requireEntitled("Fabric", "search", "RouteAggregationRule", "POST",
                    () -> fabric.routeAggregationRules().search(any.getUuid(), null, null));
            assertNotNull(rules);
        }

        @Test
        @DisplayName("routeAggregationRules_getByUuid - A single route aggregation rule (getRouteAggregationRuleByUuid)")
        void routeAggregationRules_getByUuid() {
            RouteAggregation any = firstOrNull(discoverRouteAggregations());
            Assumptions.assumeTrue(any != null, "No route aggregations found; skipping rule get");

            PaginatedList<RouteAggregationRule> rules = requireEntitled("Fabric", "list", "RouteAggregationRule", "GET",
                    () -> fabric.routeAggregationRules().list(any.getUuid()));
            RouteAggregationRule rule = firstOrNull(rules);
            Assumptions.assumeTrue(rule != null, "No route aggregation rules found; skipping get");

            RouteAggregationRule fetched = requireEntitled("Fabric", "getByUuid", "RouteAggregationRule", "GET",
                    () -> fabric.routeAggregationRules().getByUuid(any.getUuid(), rule.getUuid()));
            assertNotNull(fetched);
            assertEquals(rule.getUuid(), fetched.getUuid());
        }

        @Test
        @DisplayName("routeAggregationRules_getChanges - Route aggregation rule changes (getRouteAggregationRuleChanges)")
        void routeAggregationRules_getChanges() {
            RouteAggregation any = firstOrNull(discoverRouteAggregations());
            Assumptions.assumeTrue(any != null, "No route aggregations found; skipping rule changes");

            PaginatedList<RouteAggregationRule> rules = requireEntitled("Fabric", "list", "RouteAggregationRule", "GET",
                    () -> fabric.routeAggregationRules().list(any.getUuid()));
            RouteAggregationRule rule = firstOrNull(rules);
            Assumptions.assumeTrue(rule != null, "No route aggregation rules found; skipping changes list");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.routeAggregationRules().getChanges(any.getUuid(), rule.getUuid()));
            assertNotNull(changes);
        }

        @Test
        @DisplayName("routeAggregationRules_getChange - A single rule change (getRouteAggregationRuleChangeByUuid)")
        void routeAggregationRules_getChange() {
            RouteAggregation any = firstOrNull(discoverRouteAggregations());
            Assumptions.assumeTrue(any != null, "No route aggregations found; skipping rule change get");

            PaginatedList<RouteAggregationRule> rules = requireEntitled("Fabric", "list", "RouteAggregationRule", "GET",
                    () -> fabric.routeAggregationRules().list(any.getUuid()));
            RouteAggregationRule rule = firstOrNull(rules);
            Assumptions.assumeTrue(rule != null, "No route aggregation rules found; skipping change get");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.routeAggregationRules().getChanges(any.getUuid(), rule.getUuid()));
            Change change = firstOrNull(changes);
            Assumptions.assumeTrue(change != null, "No rule changes recorded; skipping get");

            Change fetched = requireEntitled("Fabric", "getChange", "Change", "GET",
                    () -> fabric.routeAggregationRules().getChange(any.getUuid(), rule.getUuid(), change.getUuid()));
            assertNotNull(fetched);
            assertEquals(change.getUuid(), fetched.getUuid());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Networks")
    class NetworkTests {

        @Test
        @DisplayName("networks_search - Search networks (searchNetworks)")
        void networks_search() {
            PaginatedFilteredList<Network> networks = requireEntitled("Fabric", "search", "Network", "POST",
                    () -> fabric.networks().search());
            assertNotNull(networks);
            if (!networks.isEmpty()) {
                Network first = networks.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("networks_getByUuid - Get network by UUID (getNetworkByUuid)")
        void networks_getByUuid() {
            Network any = firstOrNull(discoverNetworks());
            Assumptions.assumeTrue(any != null, "No networks found; skipping get");

            Network network = requireEntitled("Fabric", "getByUuid", "Network", "GET",
                    () -> fabric.networks().getByUuid(any.getUuid()));
            assertNotNull(network);
            assertEquals(any.getUuid(), network.getUuid());
        }

        @Test
        @DisplayName("networks_getConnections - Connections in a network (getConnectionsByNetworkUuid)")
        void networks_getConnections() {
            Network any = firstOrNull(discoverNetworks());
            Assumptions.assumeTrue(any != null, "No networks found; skipping connections list");

            PaginatedList<Connection> connections = requireEntitled("Fabric", "getConnections", "Connection", "GET",
                    () -> fabric.networks().getConnections(any.getUuid()));
            assertNotNull(connections);
        }

        @Test
        @DisplayName("networks_getChanges - Network changes (getNetworkChanges)")
        void networks_getChanges() {
            Network any = firstOrNull(discoverNetworks());
            Assumptions.assumeTrue(any != null, "No networks found; skipping changes list");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.networks().getChanges(any.getUuid()));
            assertNotNull(changes);
        }

        @Test
        @DisplayName("networks_getChange - A single network change (getNetworkChangeByUuid)")
        void networks_getChange() {
            Network any = firstOrNull(discoverNetworks());
            Assumptions.assumeTrue(any != null, "No networks found; skipping change get");

            List<Change> changes = requireEntitled("Fabric", "getChanges", "Change", "GET",
                    () -> fabric.networks().getChanges(any.getUuid()));
            Change change = firstOrNull(changes);
            Assumptions.assumeTrue(change != null, "No network changes recorded; skipping get");

            Change fetched = requireEntitled("Fabric", "getChange", "Change", "GET",
                    () -> fabric.networks().getChange(any.getUuid(), change.getUuid()));
            assertNotNull(fetched);
            assertEquals(change.getUuid(), fetched.getUuid());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Streams")
    class StreamTests {

        @Test
        @DisplayName("streams_list - List streams (getStreams)")
        void streams_list() {
            PaginatedList<Stream> streams = requireEntitled("Fabric", "list", "Stream", "GET",
                    () -> fabric.streams().list());
            assertNotNull(streams);
            if (!streams.isEmpty()) {
                Stream first = streams.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("streams_getByUuid - Get stream by UUID (getStreamByUuid)")
        void streams_getByUuid() {
            Stream any = firstOrNull(discoverStreams());
            Assumptions.assumeTrue(any != null, "No streams found; skipping get");

            Stream stream = requireEntitled("Fabric", "getByUuid", "Stream", "GET",
                    () -> fabric.streams().getByUuid(any.getUuid()));
            assertNotNull(stream);
            assertEquals(any.getUuid(), stream.getUuid());
        }

        @Test
        @DisplayName("streamSubscriptions_list - Subscriptions of a stream (getStreamSubscriptions)")
        void streamSubscriptions_list() {
            Stream any = firstOrNull(discoverStreams());
            Assumptions.assumeTrue(any != null, "No streams found; skipping subscription list");

            PaginatedList<StreamSubscription> subscriptions = requireEntitled("Fabric", "list", "StreamSubscription", "GET",
                    () -> fabric.streamSubscriptions().list(any.getUuid()));
            assertNotNull(subscriptions);
            if (!subscriptions.isEmpty()) {
                StreamSubscription first = subscriptions.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("streamSubscriptions_getByUuid - A single stream subscription (getStreamSubscriptionByUuid)")
        void streamSubscriptions_getByUuid() {
            Stream any = firstOrNull(discoverStreams());
            Assumptions.assumeTrue(any != null, "No streams found; skipping subscription get");

            PaginatedList<StreamSubscription> subscriptions = requireEntitled("Fabric", "list", "StreamSubscription", "GET",
                    () -> fabric.streamSubscriptions().list(any.getUuid()));
            StreamSubscription subscription = firstOrNull(subscriptions);
            Assumptions.assumeTrue(subscription != null, "No stream subscriptions found; skipping get");

            StreamSubscription fetched = requireEntitled("Fabric", "getByUuid", "StreamSubscription", "GET",
                    () -> fabric.streamSubscriptions().getByUuid(any.getUuid(), subscription.getUuid()));
            assertNotNull(fetched);
            assertEquals(subscription.getUuid(), fetched.getUuid());
        }

        @Test
        @DisplayName("streamAlertRules_list - Alert rules of a stream (getStreamAlertRules)")
        void streamAlertRules_list() {
            Stream any = firstOrNull(discoverStreams());
            Assumptions.assumeTrue(any != null, "No streams found; skipping alert rule list");

            PaginatedList<StreamAlertRule> alertRules = requireEntitled("Fabric", "list", "StreamAlertRule", "GET",
                    () -> fabric.streamAlertRules().list(any.getUuid()));
            assertNotNull(alertRules);
            if (!alertRules.isEmpty()) {
                StreamAlertRule first = alertRules.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("streamAlertRules_getByUuid - A single stream alert rule (getStreamAlertRuleByUuid)")
        void streamAlertRules_getByUuid() {
            Stream any = firstOrNull(discoverStreams());
            Assumptions.assumeTrue(any != null, "No streams found; skipping alert rule get");

            PaginatedList<StreamAlertRule> alertRules = requireEntitled("Fabric", "list", "StreamAlertRule", "GET",
                    () -> fabric.streamAlertRules().list(any.getUuid()));
            StreamAlertRule alertRule = firstOrNull(alertRules);
            Assumptions.assumeTrue(alertRule != null, "No stream alert rules found; skipping get");

            StreamAlertRule fetched = requireEntitled("Fabric", "getByUuid", "StreamAlertRule", "GET",
                    () -> fabric.streamAlertRules().getByUuid(any.getUuid(), alertRule.getUuid()));
            assertNotNull(fetched);
            assertEquals(alertRule.getUuid(), fetched.getUuid());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Precision Time")
    class PrecisionTimeTests {

        @Test
        @DisplayName("timeServices_getByUuid - Get a precision time service (getTimeServicesById)")
        void timeServices_getByUuid() {
            PrecisionTime any = firstOrNull(discoverTimeServices());
            Assumptions.assumeTrue(any != null, "No precision time services found; skipping get");

            PrecisionTime service = requireEntitled("Fabric", "getByUuid", "PrecisionTime", "GET",
                    () -> fabric.precisionTimes().getByUuid(any.getUuid()));
            assertNotNull(service);
            assertEquals(any.getUuid(), service.getUuid());
            service.getType();
            service.getState();
        }

        @Test
        @DisplayName("timeServices_getConnections - Connections of a time service (getTimeServicesConnectionsByServiceId)")
        void timeServices_getConnections() {
            PrecisionTime any = firstOrNull(discoverTimeServices());
            Assumptions.assumeTrue(any != null, "No precision time services found; skipping connections list");

            List<TimeServiceConnection> connections = requireEntitled("Fabric", "getConnections", "TimeServiceConnection", "GET",
                    () -> fabric.precisionTimes().getConnections(any.getUuid()));
            assertNotNull(connections);
        }

        @Test
        @DisplayName("timeServicePackages_list - Time service packages (getTimeServicesPackages)")
        void timeServicePackages_list() {
            List<TimeServicePackage> packages = requireEntitled("Fabric", "packages", "TimeServicePackage", "GET",
                    () -> fabric.precisionTimes().packages());
            assertNotNull(packages);
            assertTrue(packages.size() > 0, "Expected at least one time service package");
            TimeServicePackage first = packages.get(0);
            assertNotNull(first.getCode());
            first.getType();
        }

        @Test
        @DisplayName("timeServicePackages_getByCode - Time service package by code (getTimeServicesPackageByCode)")
        void timeServicePackages_getByCode() {
            List<TimeServicePackage> packages = requireEntitled("Fabric", "packages", "TimeServicePackage", "GET",
                    () -> fabric.precisionTimes().packages());
            TimeServicePackage any = firstOrNull(packages);
            Assumptions.assumeTrue(any != null && any.getCode() != null, "No time service packages found; skipping get by code");

            TimeServicePackage timeServicePackage = requireEntitled("Fabric", "packageByCode", "TimeServicePackage", "GET",
                    () -> fabric.precisionTimes().packageByCode(any.getCode()));
            assertNotNull(timeServicePackage);
            assertEquals(any.getCode(), timeServicePackage.getCode());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Cloud Events")
    class CloudEventTests {

        @Test
        @DisplayName("cloudEvents_search - Search cloud events (searchCloudEvents)")
        void cloudEvents_search() {
            PaginatedFilteredList<CloudEvent> events = requireEntitled("Fabric", "search", "CloudEvent", "POST",
                    () -> fabric.cloudEvents().search());
            assertNotNull(events);
            if (!events.isEmpty()) {
                CloudEvent first = events.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getSource();
            }
        }

        @Test
        @DisplayName("cloudEvents_getByUuid - Get a cloud event by UUID (getCloudEvent)")
        void cloudEvents_getByUuid() {
            PaginatedFilteredList<CloudEvent> events = requireEntitled("Fabric", "search", "CloudEvent", "POST",
                    () -> fabric.cloudEvents().search());
            CloudEvent any = firstOrNull(events);
            Assumptions.assumeTrue(any != null, "No cloud events found; skipping get");

            CloudEvent event = requireEntitled("Fabric", "getByUuid", "CloudEvent", "GET",
                    () -> fabric.cloudEvents().getByUuid(any.getUuid()));
            assertNotNull(event);
            assertEquals(any.getUuid(), event.getUuid());
        }

        @Test
        @DisplayName("cloudEvents_getByAssetId - Cloud events for an asset (getCloudEventByAssetId)")
        void cloudEvents_getByAssetId() {
            Connection any = firstOrNull(discoverConnections());
            Assumptions.assumeTrue(any != null, "No connections found; skipping asset cloud events");

            List<CloudEvent> events = requireEntitled("Fabric", "getByAssetId", "CloudEvent", "GET",
                    () -> fabric.cloudEvents().getByAssetId("connections", any.getUuid()));
            assertNotNull(events);
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Metrics")
    class MetricTests {

        @Test
        @DisplayName("metrics_search - Search metrics (searchMetrics)")
        void metrics_search() {
            PaginatedFilteredList<Metric> metrics = requireEntitled("Fabric", "search", "Metric", "POST",
                    () -> fabric.metrics().search());
            assertNotNull(metrics);
            if (!metrics.isEmpty()) {
                Metric first = metrics.get(0);
                assertNotNull(first.getName());
                first.getResource();
            }
        }

        @Test
        @DisplayName("metrics_getByName - Wildcard metro metrics (getMetricByName)")
        void metrics_getByName() {
            List<Metric> metrics = requireEntitled("Fabric", "getByName", "Metric", "GET",
                    () -> fabric.metrics().getMetricsByName("equinix.fabric.metro.*.latency", "last"));
            assertNotNull(metrics);
            if (!metrics.isEmpty()) {
                Metric first = metrics.get(0);
                assertNotNull(first.getName());
                first.getDatapoints();
            }
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Internet Access (EIA over Fabric)")
    class EiaServiceTests {

        @Test
        @DisplayName("eiaServices_search - Search Internet Access services (searchEiaServices)")
        void eiaServices_search() {
            PaginatedFilteredList<EiaService> services = requireEntitled("Fabric", "search", "EiaService", "POST",
                    () -> fabric.eiaServices().search());
            assertNotNull(services);
            if (!services.isEmpty()) {
                EiaService first = services.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("eiaServices_getByUuid - Get an Internet Access service (getEiaService)")
        void eiaServices_getByUuid() {
            PaginatedFilteredList<EiaService> services = requireEntitled("Fabric", "search", "EiaService", "POST",
                    () -> fabric.eiaServices().search());
            EiaService any = firstOrNull(services);
            Assumptions.assumeTrue(any != null, "No Internet Access services found; skipping get");

            EiaService service = requireEntitled("Fabric", "getByUuid", "EiaService", "GET",
                    () -> fabric.eiaServices().getByUuid(any.getUuid()));
            assertNotNull(service);
            assertEquals(any.getUuid(), service.getUuid());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("IP Blocks")
    class IpBlockTests {

        @Test
        @DisplayName("ipBlocks_search - Search IP blocks (searchIpBlocks)")
        void ipBlocks_search() {
            PaginatedFilteredList<IpBlock> ipBlocks = requireEntitled("Fabric", "search", "IpBlock", "POST",
                    () -> fabric.ipBlocks().search());
            assertNotNull(ipBlocks);
            if (!ipBlocks.isEmpty()) {
                IpBlock first = ipBlocks.get(0);
                assertNotNull(first.getUuid());
                first.getType();
                first.getState();
            }
        }

        @Test
        @DisplayName("ipBlocks_getByUuid - Get an IP block (getIpBlock)")
        void ipBlocks_getByUuid() {
            PaginatedFilteredList<IpBlock> ipBlocks = requireEntitled("Fabric", "search", "IpBlock", "POST",
                    () -> fabric.ipBlocks().search());
            IpBlock any = firstOrNull(ipBlocks);
            Assumptions.assumeTrue(any != null, "No IP blocks found; skipping get");

            IpBlock ipBlock = requireEntitled("Fabric", "getByUuid", "IpBlock", "GET",
                    () -> fabric.ipBlocks().getByUuid(any.getUuid()));
            assertNotNull(ipBlock);
            assertEquals(any.getUuid(), ipBlock.getUuid());
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Prices")
    class PriceTests {

        @Test
        @DisplayName("prices_search - Search catalog prices (searchPrices)")
        void prices_search() {
            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", PriceType.VIRTUAL_CONNECTION_PRODUCT.name());

            PaginatedFilteredList<Pricing> prices = requireEntitled("Fabric", "search", "Pricing", "POST",
                    () -> fabric.prices().list(filter));
            assertNotNull(prices);
            if (!prices.isEmpty()) {
                Pricing first = prices.get(0);
                assertNotNull(first.getType());
                first.getCurrency();
                first.getConnection();
            }
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Company Profiles")
    class CompanyProfileTests {

        @Test
        @DisplayName("companyProfiles_search - Search company profiles (searchCompanyProfile)")
        void companyProfiles_search() {
            PaginatedFilteredList<CompanyProfile> profiles = requireEntitled("Fabric", "search", "CompanyProfile", "POST",
                    () -> fabric.companyProfiles().search());
            assertNotNull(profiles);
            if (!profiles.isEmpty()) {
                CompanyProfile first = profiles.get(0);
                assertNotNull(first.getUuid());
                first.getName();
                first.getState();
            }
        }

        @Test
        @DisplayName("companyProfiles_getByUuid - Get company profile by UUID (getCompanyProfile)")
        void companyProfiles_getByUuid() {
            CompanyProfile any = firstOrNull(discoverCompanyProfiles());
            Assumptions.assumeTrue(any != null, "No company profiles found; skipping get");

            CompanyProfile profile = requireEntitled("Fabric", "getByUuid", "CompanyProfile", "GET",
                    () -> fabric.companyProfiles().getByUuid(any.getUuid()));
            assertNotNull(profile);
            assertEquals(any.getUuid(), profile.getUuid());
        }

        @Test
        @DisplayName("companyProfiles_getServiceProfiles - Attached service profiles (getCompanyProfileServiceProfiles)")
        void companyProfiles_getServiceProfiles() {
            CompanyProfile any = firstOrNull(discoverCompanyProfiles());
            Assumptions.assumeTrue(any != null, "No company profiles found; skipping service profile list");

            List<CompanyServiceProfile> serviceProfiles = requireEntitled("Fabric", "getServiceProfiles", "CompanyServiceProfile", "GET",
                    () -> fabric.companyProfiles().getServiceProfiles(any.getUuid()));
            assertNotNull(serviceProfiles);
        }

        @Test
        @DisplayName("companyProfiles_getTags - Attached tags (getTags)")
        void companyProfiles_getTags() {
            CompanyProfile any = firstOrNull(discoverCompanyProfiles());
            Assumptions.assumeTrue(any != null, "No company profiles found; skipping tag list");

            List<api.equinix.javasdk.fabric.model.Tag> tags = requireEntitled("Fabric", "getTags", "Tag", "GET",
                    () -> fabric.companyProfiles().getTags(any.getUuid()));
            assertNotNull(tags);
        }

        @Test
        @DisplayName("companyProfiles_getPrivateServices - Attached private services (getCompanyProfilePrivateServices)")
        void companyProfiles_getPrivateServices() {
            CompanyProfile any = firstOrNull(discoverCompanyProfiles());
            Assumptions.assumeTrue(any != null, "No company profiles found; skipping private service list");

            List<PrivateService> privateServices = requireEntitled("Fabric", "getPrivateServices", "PrivateService", "GET",
                    () -> fabric.companyProfiles().getPrivateServices(any.getUuid()));
            assertNotNull(privateServices);
        }

        @Test
        @DisplayName("companyProfiles_getLogo - Company logo bytes (getLogoByUuid)")
        void companyProfiles_getLogo() {
            String logoUuid = null;
            for (CompanyProfile profile : discoverCompanyProfiles()) {
                if (profile.getLogo() != null && profile.getLogo().getUuid() != null) {
                    logoUuid = profile.getLogo().getUuid();
                    break;
                }
            }
            Assumptions.assumeTrue(logoUuid != null, "No company profile with a logo found; skipping logo get");

            String finalLogoUuid = logoUuid;
            byte[] logo = requireEntitled("Fabric", "getLogo", "Logo", "GET",
                    () -> fabric.companyProfiles().getLogo(finalLogoUuid));
            assertNotNull(logo);
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Tags")
    class TagTests {

        @Test
        @DisplayName("tags_list - List tags (listTags)")
        void tags_list() {
            PaginatedList<api.equinix.javasdk.fabric.model.Tag> tags = requireEntitled("Fabric", "list", "Tag", "GET",
                    () -> fabric.tags().list());
            assertNotNull(tags);
            if (!tags.isEmpty()) {
                api.equinix.javasdk.fabric.model.Tag first = tags.get(0);
                assertNotNull(first.getUuid());
                first.getName();
                first.getType();
            }
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Agents")
    class AgentTests {

        @Test
        @DisplayName("agents_list - List agents (getAgents)")
        void agents_list() {
            PaginatedList<Agent> agents = requireEntitled("Fabric", "list", "Agent", "GET",
                    () -> fabric.agents().list());
            assertNotNull(agents);
            if (!agents.isEmpty()) {
                Agent first = agents.get(0);
                assertNotNull(first.getUuid());
                first.getState();
            }
        }

        @Test
        @DisplayName("agents_getByUuid - Get agent by UUID (getAgentByUuid)")
        void agents_getByUuid() {
            Agent any = firstOrNull(discoverAgents());
            Assumptions.assumeTrue(any != null, "No agents found; skipping get");

            Agent agent = requireEntitled("Fabric", "getByUuid", "Agent", "GET",
                    () -> fabric.agents().getByUuid(any.getUuid()));
            assertNotNull(agent);
            assertEquals(any.getUuid(), agent.getUuid());
        }

        @Test
        @DisplayName("agents_activities - Agent activities (getAgentActivities)")
        void agents_activities() {
            Agent any = firstOrNull(discoverAgents());
            Assumptions.assumeTrue(any != null, "No agents found; skipping activities list");

            List<AgentActivity> activities = requireEntitled("Fabric", "activities", "AgentActivity", "GET",
                    () -> fabric.agents().activities(any.getUuid()));
            assertNotNull(activities);
        }

        @Test
        @DisplayName("agentTemplates_list - List agent templates (getAgentTemplates)")
        void agentTemplates_list() {
            PaginatedList<AgentTemplate> templates = requireEntitled("Fabric", "list", "AgentTemplate", "GET",
                    () -> fabric.agentTemplates().list());
            assertNotNull(templates);
            if (!templates.isEmpty()) {
                AgentTemplate first = templates.get(0);
                assertNotNull(first.getUuid());
                first.getName();
            }
        }

        @Test
        @DisplayName("agentTemplates_getByUuid - Get agent template by UUID (getAgentTemplateByUuid)")
        void agentTemplates_getByUuid() {
            PaginatedList<AgentTemplate> templates = requireEntitled("Fabric", "list", "AgentTemplate", "GET",
                    () -> fabric.agentTemplates().list());
            AgentTemplate any = firstOrNull(templates);
            Assumptions.assumeTrue(any != null, "No agent templates found; skipping get");

            AgentTemplate template = requireEntitled("Fabric", "getByUuid", "AgentTemplate", "GET",
                    () -> fabric.agentTemplates().getByUuid(any.getUuid()));
            assertNotNull(template);
            assertEquals(any.getUuid(), template.getUuid());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  DRYRUN TESTS - dry-run creates and dedicated validate endpoints
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @Tag("integration-dryrun")
    @DisplayName("Fabric Dry-Run and Validate Tests")
    class DryRunTests {

        @Test
        @DisplayName("serviceToken_dryRunCreate_valid - Dry-run create with valid params (createServiceToken?dryRun=true)")
        void serviceToken_dryRunCreate_valid() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");
            Assumptions.assumeTrue(portUuid != null, "No DOT1Q port found; skipping dry-run create test");

            ServiceToken dryRunResult = requireEntitled("Fabric", "dryRunCreate", "ServiceToken", "POST",
                    () -> fabric.serviceTokens()
                            .define(Side.A_Side)
                            .ofType(ServiceTokenType.VC_TOKEN)
                            .withExpiry(30)
                            .forConnectionType(ConnectionType.EVPL_VC)
                            .allowRemoteConnection(true)
                            .allowCustomBandwidth(true)
                            .withBandwidthLimit(20)
                            .forAccessPointType(AccessPointType.COLO)
                            .onPortUuid(portUuid)
                            .usingProtocolDot1q(1527)
                            .withNotificationEmail("test@example.com")
                            .dryRun()
                            .create());

            assertNotNull(dryRunResult, "Dry-run create should return a non-null result");
        }

        @Test
        @DisplayName("serviceToken_dryRunCreate_invalid - Dry-run create with invalid params triggers validation error")
        void serviceToken_dryRunCreate_invalid() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");
            Assumptions.assumeTrue(portUuid != null, "No DOT1Q port found; skipping dry-run validation test");

            // Use an invalid VLAN tag (0) to trigger a validation error
            timedExpectedFailure("Fabric", "dryRunCreate_invalid", "ServiceToken", "POST", null,
                    () -> fabric.serviceTokens()
                            .define(Side.A_Side)
                            .ofType(ServiceTokenType.VC_TOKEN)
                            .withExpiry(30)
                            .forConnectionType(ConnectionType.EVPL_VC)
                            .allowRemoteConnection(true)
                            .allowCustomBandwidth(true)
                            .withBandwidthLimit(20)
                            .forAccessPointType(AccessPointType.COLO)
                            .onPortUuid(portUuid)
                            .usingProtocolDot1q(0)
                            .withNotificationEmail("test@example.com")
                            .dryRun()
                            .create());
        }

        @Test
        @DisplayName("connections_dryRunCreate - Dry-run create a port-to-port EVPL connection (createConnection?dryRun=true)")
        void connections_dryRunCreate() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");
            Assumptions.assumeTrue(portUuid != null && secondPortUuid != null,
                    "Need two DOT1Q ports for a port-to-port dry-run create; skipping");

            Integer aSideVlan = findFreeVlan(portUuid);
            Integer zSideVlan = findFreeVlan(secondPortUuid);
            Assumptions.assumeTrue(aSideVlan != null && zSideVlan != null,
                    "No free VLAN tag found on the discovered ports; skipping");

            Connection dryRunResult = requireEntitled("Fabric", "dryRunCreate", "Connection", "POST",
                    () -> fabric.connections()
                            .define(ConnectionType.EVPL_VC)
                            .name(testResourceName("dryrun-evpl"))
                            .bandwidth(50)
                            .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(aSideVlan).create())
                            .zSideAccessPointPort(secondPortUuid, LinkProtocol.dot1q().vlanTag(zSideVlan).create())
                            .notification("test@example.com")
                            .dryRun()
                            .create());

            assertNotNull(dryRunResult, "Dry-run create should echo the validated connection payload");
        }

        @Test
        @DisplayName("connections_validate - Validate VLAN availability on a port (validateConnections)")
        void connections_validate_vlanAvailability() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");
            Assumptions.assumeTrue(portUuid != null, "No DOT1Q port found; skipping connection validate test");

            Integer vlanTag = findFreeVlan(portUuid);
            Assumptions.assumeTrue(vlanTag != null, "No free VLAN tag found on the discovered port; skipping");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/aSide/accessPoint/port/uuid", portUuid)
                    .equals("/aSide/accessPoint/linkProtocol/vlanTag", String.valueOf(vlanTag));

            List<ValidateConnectionResult> results = requireEntitled("Fabric", "validate", "ValidateConnectionResult", "POST",
                    () -> fabric.connections().validate(filter));
            assertNotNull(results, "Validate should return a (possibly empty) result list");
        }

        @Test
        @DisplayName("cloudRouters_validateRoutingProtocol - Validate a subnet against a Cloud Router (validateRoutingProtocol)")
        void cloudRouters_validateRoutingProtocol() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");

            CloudRouter router = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(router != null, "No cloud routers found; skipping validate test");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/directIpv4/equinixICLAdvertisedIP", "190.1.1.1/30");

            RoutingProtocolValidation validation = requireEntitled("Fabric", "validateRoutingProtocol", "RoutingProtocolValidation", "POST",
                    () -> fabric.cloudRouters().validateRoutingProtocol(router.getUuid(), filter));
            assertNotNull(validation, "Validate should return a validation result");
        }

        // ── Newly exposed dryRun=true surfaces ─────────────────────────────
        //
        // PARAM-DROP SAFETY DOCTRINE: every live dry-run below must be harmless even if a
        // future regression silently dropped the dryRun parameter and the call executed FOR
        // REAL. Update dry-runs therefore send a NO-OP payload (replace /name with the CURRENT
        // name of a live-discovered resource), and create dry-runs register a delete-by-uuid
        // cleanup BEFORE asserting anything. Two surfaces are deliberately NOT live-tested:
        //   - POST /fabric/v4/ports?dryRun=true (PortOperator.PortBuilder.dryRun()): an
        //     accidental real create is a REAL port procurement order — billable and NOT
        //     recoverable by an API delete. The FabricPortsWireMockTest wire-proof (asserts
        //     the dryRun=true query parameter actually goes on the wire) is the lock for
        //     this surface; no live call is ever made.
        //   - DELETE /fabric/v4/ports/{portId}?dryRun=true (Ports.delete(uuid, true)): the
        //     payload is nothing but the uuid of a live port, so a dropped parameter would
        //     REALLY decommission that port — catastrophic and irreversible. WireMock-proofed
        //     only (FabricPortsWireMockTest); no live call is ever made.

        @Test
        @DisplayName("cloudRouters_dryRunCreate - Dry-run create a cloud router (createCloudRouter?dryRun=true)")
        void cloudRouters_dryRunCreate() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");

            CloudRouter template = firstOrNull(discoverCloudRouters());
            Assumptions.assumeTrue(template != null,
                    "No cloud routers found to source a known-valid payload; skipping dry-run create");
            Assumptions.assumeTrue(template.getLocation() != null && template.getLocation().getMetroCode() != null
                            && template.getRouterPackage() != null && template.getRouterPackage().getCode() != null
                            && template.getProject() != null && template.getProject().getProjectId() != null
                            && template.getAccount() != null && template.getAccount().getAccountNumber() != null,
                    "Discovered cloud router lacks location/package/project/account context; skipping dry-run create");

            CloudRouter dryRunResult = requireEntitled("Fabric", "dryRunCreate", "CloudRouter", "POST",
                    () -> fabric.cloudRouters().define()
                            .name(testResourceName("dryrun-fcr"))
                            .inMetro(template.getLocation().getMetroCode())
                            .withPackage(template.getRouterPackage().getCode())
                            .projectId(template.getProject().getProjectId())
                            .accountNumber(template.getAccount().getAccountNumber())
                            .notification(NotificationType.ALL, List.of("test@example.com"))
                            .dryRun()
                            .create());

            // PARAM-DROP SAFETY: the dry-run echo carries no uuid (spec example
            // CloudRouterResponseExampleDryRun). A uuid in the response means the dryRun
            // parameter was dropped and a REAL router was provisioned — register its
            // destruction BEFORE any assertion so it is destroyed even if this test fails.
            if (dryRunResult != null && dryRunResult.getUuid() != null) {
                registerCleanup("CloudRouter", dryRunResult.getUuid(), id -> {
                    try {
                        fabric.cloudRouters().getByUuid(id).delete();
                    } catch (EquinixNotFoundException ignored) {
                        // Already gone; nothing to clean up
                    }
                });
            }

            assertNotNull(dryRunResult, "Dry-run create should echo the validated cloud router payload");
            assertNull(dryRunResult.getUuid(),
                    "Dry-run echo must carry no uuid — one means a REAL router was created (cleanup registered)");
        }

        @Test
        @DisplayName("networks_dryRunCreate - Dry-run create a network (createNetwork?dryRun=true)")
        void networks_dryRunCreate() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");

            Network template = firstOrNull(discoverNetworks());
            Assumptions.assumeTrue(template != null,
                    "No networks found to source a known-valid payload; skipping dry-run create");
            Assumptions.assumeTrue(template.getType() != null && template.getScope() != null
                            && template.getProject() != null && template.getProject().getProjectId() != null,
                    "Discovered network lacks type/scope/project context; skipping dry-run create");

            Network dryRunResult = requireEntitled("Fabric", "dryRunCreate", "Network", "POST",
                    () -> {
                        var builder = fabric.networks().define(template.getType())
                                .name(testResourceName("dryrun-net"))
                                .scope(template.getScope())
                                .withProject(template.getProject())
                                .notification(NotificationType.ALL, "test@example.com")
                                .dryRun();
                        if (template.getLocation() != null) {
                            builder = builder.withLocation(template.getLocation());
                        }
                        return builder.create();
                    });

            // PARAM-DROP SAFETY: same contract as the cloud router dry-run — the echo (spec
            // example CreateNetworkDryRunResponse) carries no uuid; if one came back a REAL
            // network was created, so register its destruction BEFORE asserting.
            if (dryRunResult != null && dryRunResult.getUuid() != null) {
                registerCleanup("Network", dryRunResult.getUuid(), id -> {
                    try {
                        fabric.networks().getByUuid(id).delete();
                    } catch (EquinixNotFoundException ignored) {
                        // Already gone; nothing to clean up
                    }
                });
            }

            assertNotNull(dryRunResult, "Dry-run create should echo the validated network payload");
            assertNull(dryRunResult.getUuid(),
                    "Dry-run echo must carry no uuid — one means a REAL network was created (cleanup registered)");
        }

        @Test
        @DisplayName("connections_dryRunUpdate - Dry-run a NO-OP connection rename (updateConnectionByUuid?dryRun=true)")
        void connections_dryRunUpdate() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");

            Connection target = null;
            for (Connection candidate : discoverConnections()) {
                if (candidate.getState() == ConnectionState.ACTIVE && candidate.getName() != null) {
                    target = candidate;
                    break;
                }
            }
            Assumptions.assumeTrue(target != null, "No ACTIVE named connection found; skipping dry-run update");
            final Connection connection = target;

            // PARAM-DROP SAFETY: the JSON Patch replaces /name with the connection's CURRENT
            // name, so even if a regression dropped dryRun=true and the PATCH executed for
            // real, the connection would be "renamed" to what it is already called.
            Connection dryRunResult = requireEntitled("Fabric", "dryRunUpdate", "Connection", "PATCH",
                    () -> connection.update()
                            .name(connection.getName())
                            .dryRun()
                            .save());

            assertNotNull(dryRunResult, "Dry-run update should return the simulated post-update connection");
            assertEquals(connection.getName(), dryRunResult.getName(),
                    "Simulated connection should carry the (unchanged) name from the no-op patch");
        }

        @Test
        @DisplayName("serviceTokens_dryRunUpdate - Dry-run a NO-OP service token rename (updateServiceTokenByUuid?dryRun=true)")
        void serviceTokens_dryRunUpdate() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");

            PaginatedList<ServiceToken> tokens = requireEntitled("Fabric", "list", "ServiceToken", "GET",
                    () -> fabric.serviceTokens().list());
            ServiceToken target = null;
            for (ServiceToken candidate : tokens) {
                if (candidate.getName() != null
                        && (candidate.getState() == ServiceTokenState.ACTIVE
                                || candidate.getState() == ServiceTokenState.INACTIVE)) {
                    target = candidate;
                    break;
                }
            }
            Assumptions.assumeTrue(target != null,
                    "No ACTIVE/INACTIVE named service token found; skipping dry-run update");
            final ServiceToken token = target;

            // PARAM-DROP SAFETY: replaces /name with the token's CURRENT name — a no-op even
            // if the dryRun parameter were ever dropped and the PATCH executed for real.
            ServiceToken dryRunResult = requireEntitled("Fabric", "dryRunUpdate", "ServiceToken", "PATCH",
                    () -> fabric.serviceTokens().update(token.getUuid())
                            .name(token.getName())
                            .dryRun()
                            .save());

            assertNotNull(dryRunResult, "Dry-run update should return the validated/simulated token");
            assertEquals(token.getName(), dryRunResult.getName(),
                    "Simulated token should carry the (unchanged) name from the no-op patch");
        }

        @Test
        @DisplayName("ports_dryRunUpdate - Dry-run a NO-OP port rename (updatePortByUuid?dryRun=true)")
        void ports_dryRunUpdate() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");

            Port target = null;
            for (Port candidate : discoverPorts()) {
                if (candidate.getState() == PortState.ACTIVE && candidate.getName() != null) {
                    target = candidate;
                    break;
                }
            }
            Assumptions.assumeTrue(target != null, "No ACTIVE named port found; skipping dry-run update");
            final Port port = target;

            // PARAM-DROP SAFETY: replaces /name with the port's CURRENT name — a no-op even if
            // the dryRun parameter were ever dropped and the PATCH executed for real.
            //
            // Wire-shape caveat (javadoc'd on PortUpdater.dryRun()): unlike the real update's
            // bare Port body, the dry-run 200 responds with an AllPortsResponse paginated
            // envelope ({pagination, data:[Port]}); the SDK deserializes that envelope and
            // unwraps data[0] into the Port returned here — this test live-proves that spec
            // oddity holds in reality.
            Port dryRunResult = requireEntitled("Fabric", "dryRunUpdate", "Port", "PATCH",
                    () -> fabric.ports().update(port.getUuid())
                            .name(port.getName())
                            .dryRun()
                            .save());

            assertNotNull(dryRunResult,
                    "Dry-run update should return the simulated port unwrapped from the paginated envelope");
            assertEquals(port.getName(), dryRunResult.getName(),
                    "Simulated port should carry the (unchanged) name from the no-op patch");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  FULL CRUD TESTS - Create, read, delete lifecycle with cleanup
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @Tag("integration-full")
    @DisplayName("Fabric Full CRUD Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FullCrudTests {

        @Test
        @Order(1)
        @DisplayName("serviceToken_fullLifecycle - Create, get, list, delete, verify 404")
        void serviceToken_fullLifecycle() {
            Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD tests disabled; requires -DtestMode=full -DconfirmDestructive=true");
            Assumptions.assumeTrue(portUuid != null, "No DOT1Q port found; skipping full CRUD test");

            // ── CREATE ──────────────────────────────────────────────
            ServiceToken created = timedCall("Fabric", "create", "ServiceToken", "POST",
                    () -> fabric.serviceTokens()
                            .define(Side.A_Side)
                            .ofType(ServiceTokenType.VC_TOKEN)
                            .withExpiry(30)
                            .forConnectionType(ConnectionType.EVPL_VC)
                            .allowRemoteConnection(true)
                            .allowCustomBandwidth(true)
                            .withBandwidthLimit(20)
                            .forAccessPointType(AccessPointType.COLO)
                            .onPortUuid(portUuid)
                            .usingProtocolDot1q(1527)
                            .withNotificationEmail("test@example.com")
                            .create());

            assertNotNull(created, "Created service token should not be null");
            assertNotNull(created.getUuid(), "Created service token UUID should not be null");
            String createdServiceTokenUuid = created.getUuid();

            // Register cleanup immediately so the token is deleted even if later assertions fail
            registerCleanup("ServiceToken", createdServiceTokenUuid, id -> {
                try {
                    ServiceToken toDelete = fabric.serviceTokens().getByUuid(id);
                    toDelete.delete();
                } catch (EquinixNotFoundException ignored) {
                    // Already deleted; nothing to clean up
                }
            });

            // ── GET ─────────────────────────────────────────────────
            ServiceToken fetched = timedCall("Fabric", "getByUuid", "ServiceToken", "GET", createdServiceTokenUuid,
                    () -> fabric.serviceTokens().getByUuid(createdServiceTokenUuid));
            assertNotNull(fetched);
            assertEquals(createdServiceTokenUuid, fetched.getUuid());

            // ── LIST (verify it appears) ────────────────────────────
            PaginatedList<ServiceToken> tokens = timedCall("Fabric", "list", "ServiceToken", "GET",
                    () -> fabric.serviceTokens().list());
            assertNotNull(tokens);
            boolean found = false;
            for (ServiceToken token : tokens) {
                if (createdServiceTokenUuid.equals(token.getUuid())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Newly created service token should appear in the list");

            // ── DELETE ──────────────────────────────────────────────
            boolean deleted = timedCall("Fabric", "delete", "ServiceToken", "DELETE", createdServiceTokenUuid,
                    () -> fetched.delete());
            assertTrue(deleted, "Delete should return true");

            // ── VERIFY 404 ──────────────────────────────────────────
            timedExpectedFailure("Fabric", "getByUuid_afterDelete", "ServiceToken", "GET", createdServiceTokenUuid,
                    () -> fabric.serviceTokens().getByUuid(createdServiceTokenUuid));
        }
    }
}
