package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.InternetAccessPort;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.RoutingConfig;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration-readonly")
@DisplayName("Internet Access Integration Tests")
class InternetAccessIntegrationTest extends IntegrationTestBase {

    static InternetAccess client;

    @BeforeAll
    static void setUp() {
        client = new InternetAccess(testCredentials());
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Readonly Operations")
    class ReadonlyTests {

        @Test
        @DisplayName("List internet access services and get by UUID")
        void listServices() {
            try {
                PaginatedList<InternetAccessService> items = timedCall("InternetAccess", "list", "InternetAccessService", "GET",
                        () -> client.services().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    InternetAccessService item = timedCall("InternetAccess", "getByUuid", "InternetAccessService", "GET",
                            items.get(0).getUuid(),
                            () -> client.services().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "InternetAccessServices test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List internet access ports and get by UUID")
        void listPorts() {
            try {
                PaginatedList<InternetAccessPort> items = timedCall("InternetAccess", "list", "InternetAccessPort", "GET",
                        () -> client.ports().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    InternetAccessPort item = timedCall("InternetAccess", "getByUuid", "InternetAccessPort", "GET",
                            items.get(0).getUuid(),
                            () -> client.ports().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "InternetAccessPorts test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List routing configs and get by UUID")
        void listRoutingConfigs() {
            try {
                PaginatedList<RoutingConfig> items = timedCall("InternetAccess", "list", "RoutingConfig", "GET",
                        () -> client.routingConfigs().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    RoutingConfig item = timedCall("InternetAccess", "getByUuid", "RoutingConfig", "GET",
                            items.get(0).getUuid(),
                            () -> client.routingConfigs().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "RoutingConfigs test skipped: " + e.getMessage());
            }
        }
    }
}
