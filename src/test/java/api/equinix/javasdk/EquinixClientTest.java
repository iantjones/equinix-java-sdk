package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EquinixClientTest {

    static EquinixClient equinixClient;

    @BeforeAll
    static void obtainTestingData() {
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        equinixClient = new EquinixClient(new BasicEquinixCredentials(accessKey, secretKey));
    }

    @Test
    void authenticate() {
        equinixClient.authenticate();
        Assertions.assertNotNull(equinixClient.getEquinixClient().getOAuthToken());
    }
}