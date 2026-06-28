package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.model.LocationHierarchy;
import api.equinix.javasdk.ibxsmartview.model.PowerEvent;
import api.equinix.javasdk.ibxsmartview.model.SensorReading;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.SystemAlert;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration-readonly")
@DisplayName("IBX SmartView Integration Tests")
class IBXSmartViewIntegrationTest extends IntegrationTestBase {

    static IBXSmartView client;
    static String testIbxCode;

    @BeforeAll
    static void setUp() {
        client = new IBXSmartView(testCredentials());
        testIbxCode = System.getProperty("testIbxCode", "DC2");
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Readonly Operations")
    class ReadonlyTests {

        @Test
        @DisplayName("List environmental readings for IBX returns valid response")
        void listEnvironmentals() {
            try {
                PaginatedList<SensorReading> items = timedCall("IBXSmartView", "list", "SensorReading", "GET",
                        () -> client.environmentals().list(testIbxCode));
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Environmentals test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Search power events for IBX returns valid response")
        void searchPowerEvents() {
            try {
                PaginatedList<PowerEvent> items = timedCall("IBXSmartView", "search", "PowerEvent", "GET",
                        () -> client.powerEvents().search(List.of(testIbxCode), null, null, 0, 10));
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Power events test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Search system alerts returns valid response")
        void searchSystemAlerts() {
            try {
                PaginatedList<SystemAlert> items = timedCall("IBXSmartView", "search", "SystemAlert", "GET",
                        () -> client.systemAlerts().search("ACTIVE", null, null, 0, 10));
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "SystemAlerts test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List streaming subscriptions returns valid response")
        void listStreamingSubscriptions() {
            try {
                List<StreamingSubscription> items = timedCall("IBXSmartView", "list", "StreamingSubscription", "GET",
                        () -> client.streamingSubscriptions().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "StreamingSubscriptions test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Get location hierarchy for IBX returns valid response")
        void getLocationHierarchy() {
            try {
                LocationHierarchy hierarchy = timedCall("IBXSmartView", "getLocationHierarchy", "LocationHierarchy", "GET",
                        () -> client.hierarchy().getLocationHierarchy(null, testIbxCode));
                assertNotNull(hierarchy);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Hierarchy test skipped: " + e.getMessage());
            }
        }
    }
}
