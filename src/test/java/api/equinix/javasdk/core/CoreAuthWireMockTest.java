package api.equinix.javasdk.core;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.model.OAuthToken;
import org.junit.jupiter.api.*;

import java.time.Instant;

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
        assertEquals("3600", fabric.getEquinixClient().getOAuthToken().getTokenTimeout());
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

    @Test
    @DisplayName("a call without an explicit authenticate() lazily authenticates and attaches the Bearer token")
    void unauthenticatedCall_lazilyAuthenticates() throws Exception {
        try (Fabric fresh = new Fabric(testCredentials())) {
            redirectToWireMock(fresh);
            wireMock.stubFor(get(urlPathMatching("/fabric/v4/metros"))
                    .willReturn(okJson("{\"pagination\":{\"offset\":0,\"limit\":20,\"total\":0},\"data\":[]}")));

            assertNull(fresh.getEquinixClient().getOAuthToken(), "no token before the first call");
            try {
                fresh.metros().list();
            } catch (Exception ignored) {
                // we only care that the request was signed
            }

            assertNotNull(fresh.getEquinixClient().getOAuthToken(), "lazy auth published a token");
            wireMock.verify(getRequestedFor(urlPathMatching("/fabric/v4/metros"))
                    .withHeader("authorization", containing("Bearer")));
        }
    }

    @Test
    @DisplayName("an expired token is transparently re-authenticated on the next call")
    void expiredToken_isReauthenticated() throws Exception {
        try (Fabric fresh = new Fabric(testCredentials())) {
            redirectToWireMock(fresh);
            wireMock.stubFor(get(urlPathMatching("/fabric/v4/metros"))
                    .willReturn(okJson("{\"pagination\":{\"offset\":0,\"limit\":20,\"total\":0},\"data\":[]}")));

            OAuthToken expired = new OAuthToken(
                    "stale-token", "bearer", "1", Instant.now().minusSeconds(3600));
            fresh.getEquinixClient().setOAuthToken(expired);
            assertFalse(expired.validSession(), "the injected token is already expired");

            try {
                fresh.metros().list();
            } catch (Exception ignored) {
                // we only care that the request was re-signed with a fresh token
            }

            assertEquals("test-token-abc123", fresh.getEquinixClient().getOAuthToken().getSessionToken(),
                    "the stale token was replaced by a fresh one");
            wireMock.verify(getRequestedFor(urlPathMatching("/fabric/v4/metros"))
                    .withHeader("authorization", equalTo("Bearer test-token-abc123")));
        }
    }

    @Test
    @DisplayName("re-authentication after expiry never signs the token request with the stale Bearer")
    void reauthentication_tokenRequestCarriesNoAuthorizationHeader() throws Exception {
        try (Fabric fresh = new Fabric(testCredentials())) {
            redirectToWireMock(fresh);
            // Isolate this test's request journal from the class-level shared-client tests.
            wireMock.resetRequests();
            wireMock.stubFor(get(urlPathMatching("/fabric/v4/metros"))
                    .willReturn(okJson("{\"pagination\":{\"offset\":0,\"limit\":20,\"total\":0},\"data\":[]}")));

            // First (lazy) authentication mints token #1, then the published token is forced
            // to an already-expired one so the next call must RE-authenticate.
            fresh.authenticate();
            OAuthToken expired = new OAuthToken(
                    "stale-token", "bearer", "1", Instant.now().minusSeconds(3600));
            fresh.getEquinixClient().setOAuthToken(expired);
            assertFalse(expired.validSession(), "the injected token is already expired");

            try {
                fresh.metros().list();
            } catch (Exception ignored) {
                // we only care how the re-auth token request was signed
            }

            // The re-auth actually happened: two token mints in total.
            wireMock.verify(2, postRequestedFor(urlPathEqualTo("/oauth2/v1/token")));
            // And NO token-mint request — in particular the second, issued while the expired
            // "stale-token" was still published — carried an authorization header.
            wireMock.verify(0, postRequestedFor(urlPathEqualTo("/oauth2/v1/token"))
                    .withHeader("authorization", matching(".*")));
            wireMock.verify(2, postRequestedFor(urlPathEqualTo("/oauth2/v1/token"))
                    .withoutHeader("authorization"));
        }
    }
}
