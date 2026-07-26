package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.auth.BasicEquinixCredentials;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Abstract base class for all WireMock-based API tests.
 * Manages the WireMock server lifecycle and provides common helpers.
 */
@Tag("wiremock")
public abstract class WireMockTestBase {

    protected static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        stubOAuthToken();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    /**
     * Stubs the OAuth2 token endpoint to return a valid test token.
     */
    protected static void stubOAuthToken() {
        wireMock.stubFor(post(urlPathEqualTo("/oauth2/v1/token"))
                .willReturn(okJson(TestFixtures.load("/json/core/oauth_token_response.json"))));
    }

    /**
     * Returns test credentials for creating domain clients.
     */
    protected static BasicEquinixCredentials testCredentials() {
        return new BasicEquinixCredentials("test-client-id", "test-client-secret");
    }

    /**
     * Returns the WireMock base URL (http://localhost:PORT).
     */
    protected static String wireMockUrl() {
        return "http://localhost:" + wireMock.port();
    }

    /**
     * Redirects a domain client (Fabric, CustomerPortal, etc.) to use WireMock.
     */
    protected static void redirectToWireMock(com.eqixiac.equinix.EquinixClient client) {
        client.getEquinixClient().setEndPoint(wireMockUrl());
        // Disable retries by default in tests so error-mapping assertions see exactly one request
        // and don't incur backoff delays. Retry-specific tests re-enable a policy explicitly.
        client.getEquinixClient().setRetryPolicy(com.eqixiac.equinix.core.http.RetryPolicy.none());
    }

    /**
     * Loads a JSON fixture from the classpath.
     */
    protected static String loadFixture(String path) {
        return TestFixtures.load(path);
    }

    /**
     * Resets all WireMock stubs and re-stubs the OAuth token endpoint.
     * Useful for tests that need clean stub state.
     */
    protected static void resetStubs() {
        wireMock.resetAll();
        stubOAuthToken();
    }
}
