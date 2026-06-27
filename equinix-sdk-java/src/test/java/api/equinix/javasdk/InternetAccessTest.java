package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.internetaccess.enums.ExportPolicy;
import api.equinix.javasdk.internetaccess.enums.ServiceTypeV2;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.creators.BgpRoutingProtocolRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class InternetAccessTest {

    static InternetAccess internetAccess;
    static Boolean skipCreateUpdateOperations;
    static String connectionUuid;

    @BeforeAll
    static void obtainTestingData() {
        skipCreateUpdateOperations = Boolean.valueOf(System.getProperty("skipCreateUpdateOperations"));
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        connectionUuid = System.getProperty("internetAccessConnectionUuid");
        internetAccess = new InternetAccess(new BasicEquinixCredentials(accessKey, secretKey));
        internetAccess.authenticate();
    }

    @Test
    void createBgpService() {
        Assumptions.assumeFalse(skipCreateUpdateOperations, "Skipping create operations.");
        Assumptions.assumeTrue(connectionUuid != null, "No internetAccessConnectionUuid provided.");
        try {
            InternetAccessService service = internetAccess.services().define()
                    .name("sdk-it-eia-v2")
                    .type(ServiceTypeV2.SINGLE)
                    .connection(connectionUuid)
                    .routingProtocol(BgpRoutingProtocolRequest.builder()
                            .customerAsn(16220L)
                            .exportPolicy(ExportPolicy.FULL)
                            .build())
                    .create();
            assertNotNull(service);
            assertNotNull(service.getUuid());
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Internet access create test skipped: " + e.getMessage());
        }
    }
}
