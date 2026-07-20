package api.equinix.javasdk.mcp.auth;

import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.mcp.McpException;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock tests for {@link DeviceCodeAuthenticator}: public-client form-encoded
 * refresh-token exchange, access-token caching, 401-style invalidation, refresh-token
 * rotation with callback, and fail-loud error surfaces.
 */
@Tag("wiremock")
class DeviceCodeAuthenticatorWireMockTest extends WireMockTestBase {

    @BeforeEach
    void cleanStubs() {
        resetStubs();
    }

    @Test
    @DisplayName("Refresh grant is a form-encoded public-client request (client_id in body, no secret) and the access token is cached")
    void refreshGrantFormEncodedAndCached() throws Exception {
        String path = "/as/token-basic";
        wireMock.stubFor(post(urlPathEqualTo(path))
                .willReturn(okJson("{\"access_token\":\"at-1\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

        try (DeviceCodeAuthenticator auth =
                     new DeviceCodeAuthenticator("cid-1", "rt-1", wireMockUrl() + path, null)) {
            assertEquals("at-1", auth.bearerToken());
            assertEquals("at-1", auth.bearerToken());

            wireMock.verify(1, postRequestedFor(urlPathEqualTo(path))
                    .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                    .withRequestBody(containing("grant_type=refresh_token"))
                    .withRequestBody(containing("refresh_token=rt-1"))
                    .withRequestBody(containing("client_id=cid-1"))
                    .withRequestBody(notMatching(".*client_secret.*")));
        }
    }

    @Test
    @DisplayName("invalidate() discards the cached access token and forces a fresh exchange")
    void invalidateForcesFreshExchange() throws Exception {
        String path = "/as/token-invalidate";
        wireMock.stubFor(post(urlPathEqualTo(path))
                .willReturn(okJson("{\"access_token\":\"at-1\",\"expires_in\":3600}")));

        try (DeviceCodeAuthenticator auth =
                     new DeviceCodeAuthenticator("cid-1", "rt-1", wireMockUrl() + path, null)) {
            auth.bearerToken();
            auth.invalidate();
            auth.bearerToken();

            wireMock.verify(2, postRequestedFor(urlPathEqualTo(path)));
        }
    }

    @Test
    @DisplayName("A rotated refresh_token is adopted, reported to the callback, and used for the next exchange")
    void refreshTokenRotation() throws Exception {
        String path = "/as/token-rotate";
        wireMock.stubFor(post(urlPathEqualTo(path)).inScenario("rotation")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(okJson("{\"access_token\":\"at-1\",\"expires_in\":3600,\"refresh_token\":\"rt-2\"}"))
                .willSetStateTo("rotated"));
        wireMock.stubFor(post(urlPathEqualTo(path)).inScenario("rotation")
                .whenScenarioStateIs("rotated")
                .willReturn(okJson("{\"access_token\":\"at-2\",\"expires_in\":3600}")));

        List<String> rotations = new ArrayList<>();
        try (DeviceCodeAuthenticator auth =
                     new DeviceCodeAuthenticator("cid-1", "rt-1", wireMockUrl() + path, rotations::add)) {
            assertEquals("at-1", auth.bearerToken());
            assertEquals(List.of("rt-2"), rotations);
            assertEquals("rt-2", auth.getCurrentRefreshToken());

            auth.invalidate();
            assertEquals("at-2", auth.bearerToken());

            wireMock.verify(1, postRequestedFor(urlPathEqualTo(path))
                    .withRequestBody(containing("refresh_token=rt-1")));
            wireMock.verify(1, postRequestedFor(urlPathEqualTo(path))
                    .withRequestBody(containing("refresh_token=rt-2")));
        }
    }

    @Test
    @DisplayName("An unchanged refresh_token in the response does not fire the rotation callback")
    void unchangedRefreshTokenDoesNotFireCallback() throws Exception {
        String path = "/as/token-same";
        wireMock.stubFor(post(urlPathEqualTo(path))
                .willReturn(okJson("{\"access_token\":\"at-1\",\"expires_in\":3600,\"refresh_token\":\"rt-1\"}")));

        List<String> rotations = new ArrayList<>();
        try (DeviceCodeAuthenticator auth =
                     new DeviceCodeAuthenticator("cid-1", "rt-1", wireMockUrl() + path, rotations::add)) {
            auth.bearerToken();
            assertTrue(rotations.isEmpty());
            assertEquals("rt-1", auth.getCurrentRefreshToken());
        }
    }

    @Test
    @DisplayName("A rejected refresh grant fails loud with the AS body and points at McpLogin")
    void rejectedRefreshGrantFailsLoud() throws Exception {
        String path = "/as/token-reject";
        wireMock.stubFor(post(urlPathEqualTo(path))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid_grant\",\"error_description\":\"refresh token revoked\"}")));

        try (DeviceCodeAuthenticator auth =
                     new DeviceCodeAuthenticator("cid-1", "rt-dead", wireMockUrl() + path, null)) {
            McpException ex = assertThrows(McpException.class, auth::bearerToken);
            assertTrue(ex.getMessage().contains("invalid_grant"), "message should carry the AS error body");
            assertTrue(ex.getMessage().contains("McpLogin"), "message should point at the re-login remedy");
        }
    }

    @Test
    @DisplayName("A 200 without an access_token fails loud")
    void missingAccessTokenFailsLoud() throws Exception {
        String path = "/as/token-empty";
        wireMock.stubFor(post(urlPathEqualTo(path))
                .willReturn(okJson("{\"token_type\":\"Bearer\"}")));

        try (DeviceCodeAuthenticator auth =
                     new DeviceCodeAuthenticator("cid-1", "rt-1", wireMockUrl() + path, null)) {
            McpException ex = assertThrows(McpException.class, auth::bearerToken);
            assertTrue(ex.getMessage().contains("no access_token"));
        }
    }
}
