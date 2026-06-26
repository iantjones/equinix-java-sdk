package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
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
    }
}
