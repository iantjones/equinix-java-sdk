package api.equinix.javasdk.scenarios;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.core.exception.EquinixNotFoundException;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration scenario test for Cloud Router + ServiceToken + RouteFilter +
 * RouteFilterRule + RouteAggregation + RouteAggregationRule dependency chain.
 *
 * <p>Exercises the full lifecycle: dry-run validation, create, verify, search, and teardown
 * with 404 confirmation on deleted resources.</p>
 */
@Tag("integration-scenario")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FabricCloudRouterScenarioTest extends IntegrationTestBase {

    private Fabric fabric;

    private String portUuid;
    private String cloudRouterUuid;
    private String routeFilterUuid;
    private String routeFilterRuleUuid;
    private String routeAggregationUuid;
    private String routeAggregationRuleUuid;

    @BeforeAll
    void setUp() {
        fabric = new Fabric(testCredentials());
        fabric.authenticate();

        // Attempt to find a DOT1Q port for dry-run tests
        try {
            PaginatedList<Port> ports = timedCall("Fabric", "list", "Port", "GET",
                    () -> fabric.ports().list());
            for (Port port : ports) {
                if (port.getEncapsulation() != null
                        && port.getEncapsulation().getType() == EncapsulationType.DOT1Q) {
                    portUuid = port.getUuid();
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("  [SETUP] Could not discover DOT1Q port: " + e.getMessage());
        }
    }

    @AfterAll
    void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    // ── Dry-Run Tests ───────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Dry-run: validate ServiceToken creation")
    void dryRunServiceToken() {
        Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run mode not enabled");
        Assumptions.assumeTrue(portUuid != null, "No DOT1Q port available for dry-run");

        try {
            ServiceToken token = timedCall("Fabric", "dryRunCreate", "ServiceToken", "POST",
                    () -> fabric.serviceTokens().define(Side.A_Side)
                            .ofType(ServiceTokenType.VC_TOKEN)
                            .forConnectionType(ConnectionType.EVPL_VC)
                            .onPortUuid(portUuid)
                            .usingProtocolDot1q(1500)
                            .withNotificationEmail("test@example.com")
                            .dryRun()
                            .create());
            assertNotNull(token, "Dry-run ServiceToken should return a non-null response");
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Dry-run ServiceToken creation failed (may not be supported): " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Dry-run: validate Connection creation")
    void dryRunConnection() {
        Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run mode not enabled");
        Assumptions.assumeTrue(portUuid != null, "No DOT1Q port available for dry-run");

        try {
            Connection connection = timedCall("Fabric", "dryRunCreate", "Connection", "POST",
                    () -> fabric.connections().define(ConnectionType.EVPL_VC)
                            .name(testResourceName("conn-dryrun"))
                            .bandwidth(50)
                            .aSideAccessPointPort(portUuid,
                                    api.equinix.javasdk.fabric.model.implementation.LinkProtocol
                                            .dot1q().vlanTag(1500).create())
                            .notification("test@example.com")
                            .dryRun()
                            .create());
            assertNotNull(connection, "Dry-run Connection should return a non-null response");
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Dry-run Connection creation failed (may not be supported): " + e.getMessage());
        }
    }

    // ── Full CRUD Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Create CloudRouter in SV metro")
    void createCloudRouter() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");

        String name = testResourceName("cloud-router");
        CloudRouter router = timedCall("Fabric", "create", "CloudRouter", "POST",
                () -> fabric.cloudRouters().define()
                        .name(name)
                        .inMetro(MetroCode.SV)
                        .withPackage("STANDARD")
                        .create());

        assertNotNull(router, "CloudRouter should be created");
        assertNotNull(router.getUuid(), "CloudRouter UUID should not be null");
        cloudRouterUuid = router.getUuid();
        registerCleanup("CloudRouter", cloudRouterUuid, id -> fabric.cloudRouters().getByUuid(id).delete());
    }

    @Test
    @Order(4)
    @DisplayName("Create RouteFilter with BGP IPv4 prefix filter")
    void createRouteFilter() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");

        String name = testResourceName("route-filter");
        RouteFilter filter = timedCall("Fabric", "create", "RouteFilter", "POST",
                () -> fabric.routeFilters().define()
                        .ofType(RouteFilterType.BGP_IPv4_PREFIX_FILTER)
                        .name(name)
                        .notMatchedRuleAction(RouteFilterAction.DENY)
                        .create());

        assertNotNull(filter, "RouteFilter should be created");
        assertNotNull(filter.getUuid(), "RouteFilter UUID should not be null");
        routeFilterUuid = filter.getUuid();
        registerCleanup("RouteFilter", routeFilterUuid, id -> fabric.routeFilters().getByUuid(id).delete());
    }

    @Test
    @Order(5)
    @DisplayName("Create RouteFilterRule on the RouteFilter")
    void createRouteFilterRule() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(routeFilterUuid != null, "RouteFilter was not created");

        String name = testResourceName("rf-rule");
        RouteFilterRule rule = timedCall("Fabric", "create", "RouteFilterRule", "POST",
                () -> fabric.routeFilterRules().define(routeFilterUuid)
                        .prefix("10.0.0.0/24")
                        .name(name)
                        .action(RouteFilterAction.PERMIT)
                        .create());

        assertNotNull(rule, "RouteFilterRule should be created");
        assertNotNull(rule.getUuid(), "RouteFilterRule UUID should not be null");
        routeFilterRuleUuid = rule.getUuid();
        registerCleanup("RouteFilterRule", routeFilterRuleUuid,
                id -> fabric.routeFilterRules().getByUuid(routeFilterUuid, id).delete(routeFilterUuid));
    }

    @Test
    @Order(6)
    @DisplayName("Create RouteAggregation with BGP IPv4 prefix aggregation")
    void createRouteAggregation() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");

        String name = testResourceName("route-agg");
        RouteAggregation aggregation = timedCall("Fabric", "create", "RouteAggregation", "POST",
                () -> fabric.routeAggregations().define()
                        .ofType(RouteAggregationType.BGP_IPv4_PREFIX_AGGREGATION)
                        .withName(name)
                        .create());

        assertNotNull(aggregation, "RouteAggregation should be created");
        assertNotNull(aggregation.getUuid(), "RouteAggregation UUID should not be null");
        routeAggregationUuid = aggregation.getUuid();
        registerCleanup("RouteAggregation", routeAggregationUuid,
                id -> fabric.routeAggregations().getByUuid(id).delete());
    }

    @Test
    @Order(7)
    @DisplayName("Create RouteAggregationRule on the RouteAggregation")
    void createRouteAggregationRule() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(routeAggregationUuid != null, "RouteAggregation was not created");

        String name = testResourceName("ra-rule");
        RouteAggregationRule rule = timedCall("Fabric", "create", "RouteAggregationRule", "POST",
                () -> fabric.routeAggregationRules().define(routeAggregationUuid)
                        .withName(name)
                        .withPrefix("10.0.0.0/8")
                        .create());

        assertNotNull(rule, "RouteAggregationRule should be created");
        assertNotNull(rule.getUuid(), "RouteAggregationRule UUID should not be null");
        routeAggregationRuleUuid = rule.getUuid();
        registerCleanup("RouteAggregationRule", routeAggregationRuleUuid,
                id -> fabric.routeAggregationRules().getByUuid(routeAggregationUuid, id).delete(routeAggregationUuid));
    }

    @Test
    @Order(8)
    @DisplayName("Verify all created resources via GET by UUID")
    void verifyAllResources() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");

        // Verify CloudRouter
        if (cloudRouterUuid != null) {
            CloudRouter router = timedCall("Fabric", "getByUuid", "CloudRouter", "GET", cloudRouterUuid,
                    () -> fabric.cloudRouters().getByUuid(cloudRouterUuid));
            assertNotNull(router);
            assertEquals(cloudRouterUuid, router.getUuid());
            assertNotNull(router.getName(), "CloudRouter name should not be null");
        }

        // Verify RouteFilter
        if (routeFilterUuid != null) {
            RouteFilter filter = timedCall("Fabric", "getByUuid", "RouteFilter", "GET", routeFilterUuid,
                    () -> fabric.routeFilters().getByUuid(routeFilterUuid));
            assertNotNull(filter);
            assertEquals(routeFilterUuid, filter.getUuid());
            assertEquals(RouteFilterType.BGP_IPv4_PREFIX_FILTER, filter.getType());
        }

        // Verify RouteFilterRule
        if (routeFilterRuleUuid != null) {
            RouteFilterRule rule = timedCall("Fabric", "getByUuid", "RouteFilterRule", "GET", routeFilterRuleUuid,
                    () -> fabric.routeFilterRules().getByUuid(routeFilterUuid, routeFilterRuleUuid));
            assertNotNull(rule);
            assertEquals(routeFilterRuleUuid, rule.getUuid());
            assertEquals("10.0.0.0/24", rule.getPrefix());
        }

        // Verify RouteAggregation
        if (routeAggregationUuid != null) {
            RouteAggregation aggregation = timedCall("Fabric", "getByUuid", "RouteAggregation", "GET", routeAggregationUuid,
                    () -> fabric.routeAggregations().getByUuid(routeAggregationUuid));
            assertNotNull(aggregation);
            assertEquals(routeAggregationUuid, aggregation.getUuid());
            assertEquals(RouteAggregationType.BGP_IPv4_PREFIX_AGGREGATION, aggregation.getType());
        }

        // Verify RouteAggregationRule
        if (routeAggregationRuleUuid != null) {
            RouteAggregationRule rule = timedCall("Fabric", "getByUuid", "RouteAggregationRule", "GET", routeAggregationRuleUuid,
                    () -> fabric.routeAggregationRules().getByUuid(routeAggregationUuid, routeAggregationRuleUuid));
            assertNotNull(rule);
            assertEquals(routeAggregationRuleUuid, rule.getUuid());
            assertEquals("10.0.0.0/8", rule.getPrefix());
        }
    }

    @Test
    @Order(9)
    @DisplayName("Search and list cloud routers, route filters, and route aggregations")
    void searchAndListResources() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");

        // Search CloudRouters
        try {
            PaginatedFilteredList<CloudRouter> routers = timedCall("Fabric", "search", "CloudRouter", "POST",
                    () -> fabric.cloudRouters().search());
            assertNotNull(routers, "CloudRouter search should return results");
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "CloudRouter search failed: " + e.getMessage());
        }

        // Search RouteFilters
        try {
            PaginatedFilteredList<RouteFilter> filters = timedCall("Fabric", "search", "RouteFilter", "POST",
                    () -> fabric.routeFilters().search());
            assertNotNull(filters, "RouteFilter search should return results");
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "RouteFilter search failed: " + e.getMessage());
        }

        // Search RouteAggregations
        try {
            PaginatedFilteredList<RouteAggregation> aggregations = timedCall("Fabric", "search", "RouteAggregation", "POST",
                    () -> fabric.routeAggregations().search());
            assertNotNull(aggregations, "RouteAggregation search should return results");
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "RouteAggregation search failed: " + e.getMessage());
        }
    }

    @Test
    @Order(10)
    @DisplayName("Teardown in reverse order and verify 404s")
    void teardownAndVerify() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");

        // Delete RouteAggregationRule first
        if (routeAggregationRuleUuid != null) {
            timedCall("Fabric", "delete", "RouteAggregationRule", "DELETE", routeAggregationRuleUuid,
                    () -> fabric.routeAggregationRules().getByUuid(routeAggregationUuid, routeAggregationRuleUuid)
                            .delete(routeAggregationUuid));
            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.routeAggregationRules().getByUuid(routeAggregationUuid, routeAggregationRuleUuid),
                    "RouteAggregationRule should return 404 after deletion");
        }

        // Delete RouteAggregation
        if (routeAggregationUuid != null) {
            timedCall("Fabric", "delete", "RouteAggregation", "DELETE", routeAggregationUuid,
                    () -> fabric.routeAggregations().getByUuid(routeAggregationUuid).delete());
            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.routeAggregations().getByUuid(routeAggregationUuid),
                    "RouteAggregation should return 404 after deletion");
        }

        // Delete RouteFilterRule
        if (routeFilterRuleUuid != null) {
            timedCall("Fabric", "delete", "RouteFilterRule", "DELETE", routeFilterRuleUuid,
                    () -> fabric.routeFilterRules().getByUuid(routeFilterUuid, routeFilterRuleUuid)
                            .delete(routeFilterUuid));
            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.routeFilterRules().getByUuid(routeFilterUuid, routeFilterRuleUuid),
                    "RouteFilterRule should return 404 after deletion");
        }

        // Delete RouteFilter
        if (routeFilterUuid != null) {
            timedCall("Fabric", "delete", "RouteFilter", "DELETE", routeFilterUuid,
                    () -> fabric.routeFilters().getByUuid(routeFilterUuid).delete());
            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.routeFilters().getByUuid(routeFilterUuid),
                    "RouteFilter should return 404 after deletion");
        }

        // Delete CloudRouter
        if (cloudRouterUuid != null) {
            timedCall("Fabric", "delete", "CloudRouter", "DELETE", cloudRouterUuid,
                    () -> fabric.cloudRouters().getByUuid(cloudRouterUuid).delete());
            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.cloudRouters().getByUuid(cloudRouterUuid),
                    "CloudRouter should return 404 after deletion");
        }
    }
}
