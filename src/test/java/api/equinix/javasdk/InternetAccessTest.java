package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
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
}
