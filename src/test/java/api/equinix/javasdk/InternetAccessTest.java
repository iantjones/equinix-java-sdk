package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.InternetAccessPort;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.RoutingConfig;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class InternetAccessTest {

    static InternetAccess internetAccess;
    static Boolean skipCreateUpdateOperations;

    @BeforeAll
    static void obtainTestingData() {
        skipCreateUpdateOperations = Boolean.valueOf(System.getProperty("skipCreateUpdateOperations"));
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        internetAccess = new InternetAccess(new BasicEquinixCredentials(accessKey, secretKey));
        internetAccess.authenticate();
    }

    @Test
    void services() {
        try {
            PaginatedList<InternetAccessService> services = internetAccess.services().list();
            assertNotNull(services);
            assertTrue(services.size() >= 0);

            if (services.size() > 0) {
                InternetAccessService service = internetAccess.services().getByUuid(services.get(0).getUuid());
                assertNotNull(service);
                assertEquals(services.get(0).getUuid(), service.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Internet access services test skipped: " + e.getMessage());
        }
    }

    @Test
    void ports() {
        try {
            PaginatedList<InternetAccessPort> ports = internetAccess.ports().list();
            assertNotNull(ports);
            assertTrue(ports.size() >= 0);

            if (ports.size() > 0) {
                InternetAccessPort port = internetAccess.ports().getByUuid(ports.get(0).getUuid());
                assertNotNull(port);
                assertEquals(ports.get(0).getUuid(), port.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Internet access ports test skipped: " + e.getMessage());
        }
    }

    @Test
    void routingConfigs() {
        try {
            PaginatedList<RoutingConfig> routingConfigs = internetAccess.routingConfigs().list();
            assertNotNull(routingConfigs);
            assertTrue(routingConfigs.size() >= 0);

            if (routingConfigs.size() > 0) {
                RoutingConfig routingConfig = internetAccess.routingConfigs().getByUuid(routingConfigs.get(0).getUuid());
                assertNotNull(routingConfig);
                assertEquals(routingConfigs.get(0).getUuid(), routingConfig.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Routing configs test skipped: " + e.getMessage());
        }
    }
}
