package com.eqixiac.equinix.core;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.client.EquinixClient;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.exception.EquinixAuthenticationException;
import com.eqixiac.equinix.core.http.RequestAssembler;
import com.eqixiac.equinix.core.http.RetryPolicy;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.response.EquinixResponse;
import com.eqixiac.equinix.core.model.OAuthToken;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP 401 recovery in the core client: the rejected token is discarded, ONE new token is minted
 * (single-flight across threads) and the request is replayed ONCE; a second 401 surfaces. No
 * replay for unauthenticated endpoints, the token request itself, or a client without a token
 * source. Also pins single-flight re-authentication of an expired token under concurrency.
 */
class UnauthorizedRecoveryWireMockTest extends WireMockTestBase {

    private static final String TOKEN_PATH = "/oauth2/v1/token";
    private static final String THING_PATH = "/coretest/v1/things/1";
    private static final String FRESH_TOKEN_BODY =
            "{\"access_token\":\"fresh-token\",\"token_timeout\":\"3599\",\"token_type\":\"bearer\"}";

    @BeforeEach
    void stubs() {
        wireMock.resetAll();
        wireMock.stubFor(post(urlPathEqualTo(TOKEN_PATH)).willReturn(okJson(FRESH_TOKEN_BODY)));
        wireMock.stubFor(get(urlPathEqualTo(THING_PATH))
                .withHeader("authorization", equalTo("Bearer fresh-token"))
                .willReturn(okJson("{\"id\":\"1\"}")));
        wireMock.stubFor(get(urlPathEqualTo(THING_PATH))
                .withHeader("authorization", notContaining("fresh-token"))
                .willReturn(aResponse().withStatus(401).withHeader("Content-Type", "application/json")
                        .withBody("{\"fault\":{\"faultstring\":\"Invalid access token\","
                                + "\"detail\":{\"errorcode\":\"oauth.v2.InvalidAccessToken\"}}}")));
    }

    /** A Fabric facade (any facade wires the client-credentials authenticator) pointed at WireMock. */
    private static Fabric facade() {
        Fabric fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.getEquinixClient().appendApiParams("json/core/apiParams_CoreTest.json");
        return fabric;
    }

    /** A token that is valid locally but that the stubbed service rejects (revoked server-side). */
    private static OAuthToken revokedToken() {
        return new OAuthToken("revoked-token", "bearer", "3599", Instant.now());
    }

    static void getThing(EquinixClient core, String parent) throws Exception {
        EquinixRequest<Object> request = RequestAssembler.buildRequest("CoreTest", parent, "Get",
                RequestType.SINGLE, core, Map.of("id", "1"), null, (TypeReference<?>) null);
        EquinixResponse<Object> response = core.invoke(request);
        try (InputStream ignored = response.getContent()) {
            assertEquals(200, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("a 401 clears the token, re-authenticates once and replays the request once")
    void unauthorized_reauthenticatesAndReplaysOnce() throws Exception {
        try (Fabric fabric = facade()) {
            EquinixClient core = fabric.getEquinixClient();
            core.setOAuthToken(revokedToken());

            getThing(core, "Things");

            assertEquals("fresh-token", core.getOAuthToken().getSessionToken());
            wireMock.verify(1, postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
            wireMock.verify(1, getRequestedFor(urlPathEqualTo(THING_PATH)).withHeader("authorization", equalTo("Bearer revoked-token")));
            wireMock.verify(1, getRequestedFor(urlPathEqualTo(THING_PATH)).withHeader("authorization", equalTo("Bearer fresh-token")));
        }
    }

    @Test
    @DisplayName("a second 401 after the replay surfaces: no loop")
    void secondUnauthorized_surfaces() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo(THING_PATH)).atPriority(1)
                .willReturn(aResponse().withStatus(401).withBody("{\"error\":\"invalid_token\"}")));
        try (Fabric fabric = facade()) {
            EquinixClient core = fabric.getEquinixClient();
            core.setOAuthToken(revokedToken());

            EquinixAuthenticationException ex = assertThrows(EquinixAuthenticationException.class, () -> getThing(core, "Things"));

            assertEquals("invalid_token", ex.getExceptionDetails().get(0).getErrorCode());
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(THING_PATH)));
            wireMock.verify(1, postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
        }
    }

    @Test
    @DisplayName("a POST rejected with 401 is replayed once too (401 means it was not processed)")
    void unauthorizedPost_isReplayedOnce() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/coretest/v1/things"))
                .withHeader("authorization", equalTo("Bearer fresh-token")).willReturn(okJson("{}")));
        wireMock.stubFor(post(urlPathEqualTo("/coretest/v1/things"))
                .withHeader("authorization", notContaining("fresh-token")).willReturn(aResponse().withStatus(401)));
        try (Fabric fabric = facade()) {
            EquinixClient core = fabric.getEquinixClient();
            core.setOAuthToken(revokedToken());
            EquinixRequest<Object> request = RequestAssembler.buildRequest("CoreTest", "Things", "Create",
                    RequestType.SINGLE, core, null, null, (TypeReference<?>) null);

            assertEquals(200, core.invoke(request).getStatusCode());
            wireMock.verify(2, postRequestedFor(urlPathEqualTo("/coretest/v1/things")));
        }
    }

    @Test
    @DisplayName("no replay for an endpoint declared unauthenticated")
    void unauthenticatedEndpoint_isNotReplayed() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/coretest/v1/public/1")).willReturn(aResponse().withStatus(401)));
        try (Fabric fabric = facade()) {
            EquinixClient core = fabric.getEquinixClient();

            assertThrows(EquinixAuthenticationException.class, () -> getThing(core, "PublicThings"));

            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/coretest/v1/public/1")).withoutHeader("authorization"));
            wireMock.verify(0, postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
        }
    }

    @Test
    @DisplayName("no replay when no authenticator is wired")
    void noAuthenticator_isNotReplayed() throws Exception {
        try (EquinixClient core = new EquinixClient(testCredentials(), false)) {
            core.setEndPoint(wireMockUrl());
            core.setRetryPolicy(RetryPolicy.none());
            core.appendApiParams("json/core/apiParams_CoreTest.json");
            core.setOAuthToken(revokedToken());

            assertThrows(EquinixAuthenticationException.class, () -> getThing(core, "Things"));

            wireMock.verify(1, getRequestedFor(urlPathEqualTo(THING_PATH)));
            wireMock.verify(0, postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
        }
    }

    @Test
    @DisplayName("a 401 from the token endpoint itself is not retried")
    void tokenEndpoint401_isNotReplayed() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(TOKEN_PATH)).atPriority(1)
                .willReturn(aResponse().withStatus(401).withHeader("Content-Type", "application/json")
                        .withBody("[{\"errorCode\":\"EQ-3000001\",\"errorMessage\":\"Invalid client credentials\"}]")));
        try (Fabric fabric = facade()) {
            EquinixAuthenticationException ex = assertThrows(EquinixAuthenticationException.class, fabric::authenticate);

            assertTrue(ex.getMessage().contains("EQ-3000001: Invalid client credentials"), ex.getMessage());
            wireMock.verify(1, postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
        }
    }

    @Test
    @DisplayName("many threads rejected with the same revoked token cause exactly one token request")
    void concurrentUnauthorized_singleFlightRefresh() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(TOKEN_PATH)).atPriority(1)
                .willReturn(okJson(FRESH_TOKEN_BODY).withFixedDelay(150)));
        try (Fabric fabric = facade()) {
            EquinixClient core = fabric.getEquinixClient();
            core.setOAuthToken(revokedToken());

            runConcurrently(16, () -> getThing(core, "Things"));

            wireMock.verify(1, postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
            wireMock.verify(16, getRequestedFor(urlPathEqualTo(THING_PATH)).withHeader("authorization", equalTo("Bearer fresh-token")));
        }
    }

    @Test
    @DisplayName("many threads hitting an expired token cause exactly one token request")
    void concurrentExpiredToken_singleFlightReauthentication() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(TOKEN_PATH)).atPriority(1)
                .willReturn(okJson(FRESH_TOKEN_BODY).withFixedDelay(150)));
        try (Fabric fabric = facade()) {
            EquinixClient core = fabric.getEquinixClient();
            core.setOAuthToken(new OAuthToken("expired-token", "bearer", "3599", Instant.EPOCH));

            runConcurrently(16, () -> getThing(core, "Things"));

            wireMock.verify(1, postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
            wireMock.verify(0, getRequestedFor(urlPathEqualTo(THING_PATH)).withHeader("authorization", equalTo("Bearer expired-token")));
            wireMock.verify(16, getRequestedFor(urlPathEqualTo(THING_PATH)));
        }
    }

    interface Call {
        void run() throws Exception;
    }

    /** Starts {@code threads} calls behind one gate so they hit the client together; rethrows any failure. */
    static void runConcurrently(int threads, Call call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    call.run();
                    return null;
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            go.countDown();
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
