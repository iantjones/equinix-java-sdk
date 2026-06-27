package api.equinix.javasdk.core;

import api.equinix.javasdk.Fabric;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test verifying OAuth2 authentication works through WireMock.
 */
class CoreAuthWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @Test
    @DisplayName("authenticate() obtains and stores OAuth token")
    void authenticate_storesToken() {
        fabric.authenticate();

        assertNotNull(fabric.getEquinixClient().getOAuthToken());
        assertEquals("test-token-abc123", fabric.getEquinixClient().getOAuthToken().getSessionToken());
        assertEquals("bearer", fabric.getEquinixClient().getOAuthToken().getTokenType());
        assertEquals(3600, fabric.getEquinixClient().getOAuthToken().getTokenTimeout());
    }

    @Test
    @DisplayName("authenticate() sends POST to /oauth2/v1/token")
    void authenticate_sendsCorrectRequest() {
        fabric.authenticate();

        wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth2/v1/token"))
                .withHeader("Content-Type", containing("application/json")));
    }

    @Test
    @DisplayName("authenticated request includes Bearer token header")
    void authenticatedRequest_includesBearerHeader() {
        fabric.authenticate();

        // Stub a simple GET endpoint to verify headers
        wireMock.stubFor(get(urlPathMatching("/fabric/v4/metros"))
                .willReturn(okJson("{\"pagination\":{\"offset\":0,\"limit\":20,\"total\":0},\"data\":[]}")));

        try {
            fabric.metros().list();
        } catch (Exception ignored) {
            // Response parsing may fail, we only care about the request headers
        }

        wireMock.verify(getRequestedFor(urlPathMatching("/fabric/v4/metros"))
                .withHeader("authorization", equalTo("Bearer test-token-abc123")));
    }

    @Test
    @DisplayName("token validity check passes for fresh token")
    void freshToken_isValid() {
        fabric.authenticate();
        assertTrue(fabric.getEquinixClient().getOAuthToken().validSession());
    }
}
