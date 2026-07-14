package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Live smoke test for the OAuth authentication handshake itself — the one
 * call every other integration test depends on. Runs in the readonly tier;
 * skips (like all integration tests) when {@code -DaccessKey/-DsecretKey}
 * are not supplied.
 */
@Tag("integration-readonly")
class EquinixClientTest extends IntegrationTestBase {

    @Test
    void authenticate() {
        EquinixClient equinixClient = new EquinixClient(testCredentials());
        timedCall("Core", "authenticate", "OAuthToken", "POST", () -> {
            equinixClient.authenticate();
            return null;
        });
        Assertions.assertNotNull(equinixClient.getEquinixClient().getOAuthToken(),
                "authenticate() must yield a live OAuth token");
    }
}
