package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.core.exception.EquinixNotFoundException;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Fabric domain of the Equinix Java SDK.
 *
 * <p>Three tiers of tests are provided:
 * <ul>
 *     <li><b>integration-readonly</b> - Safe read-only operations (list, get, search).</li>
 *     <li><b>integration-dryrun</b> - Dry-run validation calls; no real mutations.</li>
 *     <li><b>integration-full</b> - Full CRUD lifecycle with automatic cleanup.</li>
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
    static String portUuid;

    static LocalDateTime endDateTime = LocalDateTime.now();
    static LocalDateTime startDateTime = endDateTime.minusDays(1);

    @BeforeAll
    static void setupFabric() {
        fabric = new Fabric(testCredentials());
        fabric.authenticate();

        // Find a DOT1Q port for tests that need a port UUID
        try {
            PaginatedList<Port> ports = timedCall("Fabric", "list", "Port", "GET",
                    () -> fabric.ports().list());
            for (Port port : ports) {
                if (port.getEncapsulation().getType() == EncapsulationType.DOT1Q) {
                    portUuid = port.getUuid();
                    break;
                }
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Unable to list ports during setup: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  READONLY TESTS - Safe GET/list/search operations
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Fabric Read-Only Tests")
    static class ReadonlyTests extends IntegrationTestBase {

        @Test
        @DisplayName("metros_list - List all metros")
        void metros_list() {
            PaginatedList<Metro> metros = timedCall("Fabric", "list", "Metro", "GET",
                    () -> fabric.metros().list());
            assertNotNull(metros);
            assertTrue(metros.size() > 0, "Expected at least one metro");
        }

        @Test
        @DisplayName("metros_getByCode - Get metro by code")
        void metros_getByCode() {
            Metro metro = timedCall("Fabric", "getByCode", "Metro", "GET",
                    MetroCode.LA.name(), () -> fabric.metros().getByMetroCode(MetroCode.LA));
            assertNotNull(metro);
            assertEquals(MetroCode.LA, metro.getCode());
        }

        @Test
        @DisplayName("ports_list - List all ports")
        void ports_list() {
            PaginatedList<Port> ports = timedCall("Fabric", "list", "Port", "GET",
                    () -> fabric.ports().list());
            assertNotNull(ports);
            assertTrue(ports.size() > 0, "Expected at least one port");
        }

        @Test
        @DisplayName("ports_getByUuid - Get port by UUID")
        void ports_getByUuid() {
            Assumptions.assumeTrue(portUuid != null, "No DOT1Q port found; skipping port get test");
            Port port = timedCall("Fabric", "getByUuid", "Port", "GET", portUuid,
                    () -> fabric.ports().getByUuid(portUuid));
            assertNotNull(port);
            assertEquals(portUuid, port.getUuid());
        }

        @Test
        @DisplayName("connections_search - Search connections")
        void connections_search() {
            PaginatedFilteredList<Connection> connections = timedCall("Fabric", "search", "Connection", "POST",
                    () -> fabric.connections().search());
            assertNotNull(connections);
            assertTrue(connections.size() >= 0);
        }

        @Test
        @DisplayName("connections_getByUuid - Get connection by UUID (if any exist)")
        void connections_getByUuid() {
            PaginatedFilteredList<Connection> connections = timedCall("Fabric", "search", "Connection", "POST",
                    () -> fabric.connections().search());
            Assumptions.assumeTrue(connections.size() > 0, "No connections found; skipping get test");

            String uuid = connections.get(0).getUuid();
            Connection connection = timedCall("Fabric", "getByUuid", "Connection", "GET", uuid,
                    () -> fabric.connections().getByUuid(uuid));
            assertNotNull(connection);
            assertEquals(uuid, connection.getUuid());
        }

        @Test
        @DisplayName("cloudRouters_search - Search cloud routers")
        void cloudRouters_search() {
            PaginatedFilteredList<CloudRouter> cloudRouters = timedCall("Fabric", "search", "CloudRouter", "POST",
                    () -> fabric.cloudRouters().search());
            assertNotNull(cloudRouters);
            assertTrue(cloudRouters.size() >= 0);
        }

        @Test
        @DisplayName("cloudRouters_getByUuid - Get cloud router by UUID (if any exist)")
        void cloudRouters_getByUuid() {
            PaginatedFilteredList<CloudRouter> cloudRouters = timedCall("Fabric", "search", "CloudRouter", "POST",
                    () -> fabric.cloudRouters().search());
            Assumptions.assumeTrue(cloudRouters.size() > 0, "No cloud routers found; skipping get test");

            String uuid = cloudRouters.get(0).getUuid();
            CloudRouter cloudRouter = timedCall("Fabric", "getByUuid", "CloudRouter", "GET", uuid,
                    () -> fabric.cloudRouters().getByUuid(uuid));
            assertNotNull(cloudRouter);
            assertEquals(uuid, cloudRouter.getUuid());
        }

        @Test
        @DisplayName("streams_list - List streams")
        void streams_list() {
            PaginatedList<Stream> streams = timedCall("Fabric", "list", "Stream", "GET",
                    () -> fabric.streams().list());
            assertNotNull(streams);
            assertTrue(streams.size() >= 0);
        }

        @Test
        @DisplayName("streams_getByUuid - Get stream by UUID (if any exist)")
        void streams_getByUuid() {
            PaginatedList<Stream> streams = timedCall("Fabric", "list", "Stream", "GET",
                    () -> fabric.streams().list());
            Assumptions.assumeTrue(streams.size() > 0, "No streams found; skipping get test");

            String uuid = streams.get(0).getUuid();
            Stream stream = timedCall("Fabric", "getByUuid", "Stream", "GET", uuid,
                    () -> fabric.streams().getByUuid(uuid));
            assertNotNull(stream);
            assertEquals(uuid, stream.getUuid());
        }

        @Test
        @DisplayName("routeFilters_search - Search route filters")
        void routeFilters_search() {
            PaginatedFilteredList<RouteFilter> routeFilters = timedCall("Fabric", "search", "RouteFilter", "POST",
                    () -> fabric.routeFilters().search());
            assertNotNull(routeFilters);
            assertTrue(routeFilters.size() >= 0);
        }

        @Test
        @DisplayName("routeFilters_getByUuid - Get route filter by UUID (if any exist)")
        void routeFilters_getByUuid() {
            PaginatedFilteredList<RouteFilter> routeFilters = timedCall("Fabric", "search", "RouteFilter", "POST",
                    () -> fabric.routeFilters().search());
            Assumptions.assumeTrue(routeFilters.size() > 0, "No route filters found; skipping get test");

            String uuid = routeFilters.get(0).getUuid();
            RouteFilter routeFilter = timedCall("Fabric", "getByUuid", "RouteFilter", "GET", uuid,
                    () -> fabric.routeFilters().getByUuid(uuid));
            assertNotNull(routeFilter);
            assertEquals(uuid, routeFilter.getUuid());
        }

        @Test
        @DisplayName("networks_search - Search networks")
        void networks_search() {
            PaginatedFilteredList<Network> networks = timedCall("Fabric", "search", "Network", "POST",
                    () -> fabric.networks().search());
            assertNotNull(networks);
            assertTrue(networks.size() >= 0);
        }

        @Test
        @DisplayName("networks_getByUuid - Get network by UUID (if any exist)")
        void networks_getByUuid() {
            PaginatedFilteredList<Network> networks = timedCall("Fabric", "search", "Network", "POST",
                    () -> fabric.networks().search());
            Assumptions.assumeTrue(networks.size() > 0, "No networks found; skipping get test");

            String uuid = networks.get(0).getUuid();
            Network network = timedCall("Fabric", "getByUuid", "Network", "GET", uuid,
                    () -> fabric.networks().getByUuid(uuid));
            assertNotNull(network);
            assertEquals(uuid, network.getUuid());
        }

        @Test
        @DisplayName("serviceProfiles_list - List service profiles")
        void serviceProfiles_list() {
            PaginatedList<ServiceProfile> serviceProfiles = timedCall("Fabric", "list", "ServiceProfile", "GET",
                    () -> fabric.serviceProfiles().list());
            assertNotNull(serviceProfiles);
            assertTrue(serviceProfiles.size() > 0, "Expected at least one service profile");
        }

        @Test
        @DisplayName("health_check - Fabric health endpoint")
        void health_check() {
            HealthStatus healthStatus = timedCall("Fabric", "health", "HealthStatus", "GET",
                    () -> fabric.health());
            assertNotNull(healthStatus);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  DRYRUN TESTS - Dry-run validation; no real mutations
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @Tag("integration-dryrun")
    @DisplayName("Fabric Dry-Run Tests")
    static class DryRunTests extends IntegrationTestBase {

        @Test
        @DisplayName("serviceToken_dryRunCreate_valid - Dry-run create with valid params")
        void serviceToken_dryRunCreate_valid() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");
            Assumptions.assumeTrue(portUuid != null, "No DOT1Q port found; skipping dry-run create test");

            ServiceToken dryRunResult = timedCall("Fabric", "dryRunCreate", "ServiceToken", "POST",
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
    }

    // ════════════════════════════════════════════════════════════════════
    //  FULL CRUD TESTS - Create, read, delete lifecycle with cleanup
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @Tag("integration-full")
    @DisplayName("Fabric Full CRUD Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    static class FullCrudTests extends IntegrationTestBase {

        static String createdServiceTokenUuid;

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
            createdServiceTokenUuid = created.getUuid();

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
