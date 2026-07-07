package api.equinix.javasdk.core;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.CircuitBreaker;
import api.equinix.javasdk.core.http.RetryPolicy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-cutting resiliency tests: error handling, status code mapping,
 * malformed responses, and authentication edge cases.
 */
class ResiliencyTest extends WireMockTestBase {

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
        // Default every test to no-retry and no breaker; the Retry / CircuitBreaking nested
        // classes opt in explicitly.
        fabric.getEquinixClient().setRetryPolicy(RetryPolicy.none());
        fabric.getEquinixClient().setCircuitBreaker(null);
    }

    @Nested
    @DisplayName("Status code → Exception mapping")
    class StatusCodeMapping {

        @Test
        @DisplayName("401 → EquinixAuthenticationException")
        void status401_throwsAuthenticationException() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("403 → EquinixAuthorizationException")
        void status403_throwsAuthorizationException() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    403, "[{\"errorCode\":\"ERR-403\",\"errorMessage\":\"Forbidden\"}]");

            assertThrows(EquinixAuthorizationException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("404 → EquinixNotFoundException")
        void status404_throwsNotFoundException() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("409 → EquinixConflictException")
        void status409_throwsConflictException() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    409, "[{\"errorCode\":\"ERR-409\",\"errorMessage\":\"Conflict\"}]");

            assertThrows(EquinixConflictException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("429 → EquinixRateLimitException")
        void status429_throwsRateLimitException() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    429, "[{\"errorCode\":\"ERR-429\",\"errorMessage\":\"Rate limit exceeded\"}]");

            assertThrows(EquinixRateLimitException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 → EquinixServerException")
        void status500_throwsServerException() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("502 → EquinixServerException")
        void status502_throwsServerException() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    502, "[{\"errorCode\":\"ERR-502\",\"errorMessage\":\"Bad gateway\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("503 → EquinixServerException")
        void status503_throwsServerException() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    503, "[{\"errorCode\":\"ERR-503\",\"errorMessage\":\"Service unavailable\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }
    }

    @Nested
    @DisplayName("Error body parsing")
    class ErrorBodyParsing {

        @Test
        @DisplayName("array format error body is parsed into ExceptionDetails")
        void arrayFormat_parsesExceptionDetails() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Resource not found\",\"correlationId\":\"abc-123\"}]");

            EquinixNotFoundException ex = assertThrows(EquinixNotFoundException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));

            assertNotNull(ex.getExceptionDetails());
            assertFalse(ex.getExceptionDetails().isEmpty());
            assertEquals("ERR-404", ex.getExceptionDetails().get(0).getErrorCode());
            assertEquals("Resource not found", ex.getExceptionDetails().get(0).getErrorMessage());
            assertEquals("abc-123", ex.getExceptionDetails().get(0).getCorrelationId());
        }

        @Test
        @DisplayName("single object error body is parsed into ExceptionDetails")
        void singleObjectFormat_parsesExceptionDetail() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    404, "{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}");

            EquinixNotFoundException ex = assertThrows(EquinixNotFoundException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));

            assertNotNull(ex.getExceptionDetails());
            assertEquals(1, ex.getExceptionDetails().size());
        }

        @Test
        @DisplayName("unparseable error body still throws with correct status code")
        void unparseableBody_stillThrowsWithStatusCode() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    500, "This is not JSON at all");

            EquinixServerException ex = assertThrows(EquinixServerException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("HTML error page still throws with correct status code")
        void htmlErrorPage_stillThrowsWithStatusCode() {
            wireMock.stubFor(get(urlPathMatching("/fabric/v4/connections/.*"))
                    .willReturn(aResponse()
                            .withStatus(502)
                            .withHeader("Content-Type", "text/html")
                            .withBody("<html><body><h1>502 Bad Gateway</h1></body></html>")));

            EquinixServerException ex = assertThrows(EquinixServerException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));

            assertEquals(502, ex.getStatusCode());
        }

        @Test
        @DisplayName("exception includes request path")
        void exception_includesRequestPath() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}]");

            EquinixNotFoundException ex = assertThrows(EquinixNotFoundException.class,
                    () -> fabric.connections().getByUuid("my-test-uuid"));

            assertNotNull(ex.getPath());
            assertTrue(ex.getPath().contains("my-test-uuid"));
        }
    }

    @Nested
    @DisplayName("Authentication edge cases")
    class AuthenticationEdgeCases {

        @Test
        @DisplayName("invalid credentials returns 401 on token endpoint")
        void invalidCredentials_throwsOnAuthenticate() {
            // Override the default OAuth stub with a 401
            wireMock.stubFor(post(urlPathEqualTo("/oauth2/v1/token"))
                    .willReturn(aResponse()
                            .withStatus(401)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Invalid client credentials\"}]")));

            Fabric badFabric = new Fabric(testCredentials());
            redirectToWireMock(badFabric);

            assertThrows(EquinixAuthenticationException.class, badFabric::authenticate);

            try { badFabric.close(); } catch (Exception ignored) {}
        }
    }

    @Nested
    @DisplayName("Timeout handling")
    class TimeoutHandling {

        @Test
        @DisplayName("slow response eventually completes or throws")
        void slowResponse_handledGracefully() {
            wireMock.stubFor(get(urlPathMatching("/fabric/v4/connections/.*"))
                    .willReturn(okJson(TestFixtures.load("/json/fabric/connection_response.json"))
                            .withFixedDelay(2000)));

            // Should either succeed (if timeout > 2s) or throw a client exception
            // Either outcome is acceptable — the key is no unhandled exception
            try {
                fabric.connections().getByUuid("test-uuid");
            } catch (EquinixClientException e) {
                // Timeout-based client exception is acceptable
                assertNotNull(e);
            }
        }
    }

    @Nested
    @DisplayName("Retry/backoff")
    class Retry {

        // Fast policy: tiny backoff so tests don't sleep meaningfully; full jitter keeps waits in [0,1]ms.
        private void enableFastRetry(int maxRetries) {
            fabric.getEquinixClient().setRetryPolicy(
                    new RetryPolicy(maxRetries, 1, 5, java.util.Set.of(429, 500, 502, 503, 504), true, true));
        }

        @Test
        @DisplayName("retries a transient 503 then succeeds")
        void retriesTransient503ThenSucceeds() {
            enableFastRetry(3);
            wireMock.stubFor(get(urlPathMatching("/fabric/v4/networks/.*")).inScenario("retry503")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-503\",\"errorMessage\":\"Service Unavailable\"}]"))
                    .willSetStateTo("recovered"));
            wireMock.stubFor(get(urlPathMatching("/fabric/v4/networks/.*")).inScenario("retry503")
                    .whenScenarioStateIs("recovered")
                    .willReturn(okJson(loadFixture("/json/fabric/network_response.json"))));

            var network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            assertNotNull(network);
            // 1 failed (503) + 1 successful = 2 requests
            wireMock.verify(2, getRequestedFor(urlPathMatching("/fabric/v4/networks/.*")));
        }

        @Test
        @DisplayName("exhausts retries on persistent 503 then throws, making maxRetries+1 attempts")
        void exhaustsRetriesThenThrows() {
            enableFastRetry(2);
            stubErrorInline(wireMock, "/fabric/v4/networks/.*",
                    503, "[{\"errorCode\":\"ERR-503\",\"errorMessage\":\"Service Unavailable\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.networks().getByUuid("test-uuid"));
            wireMock.verify(3, getRequestedFor(urlPathMatching("/fabric/v4/networks/.*")));
        }

        @Test
        @DisplayName("does not retry a non-retryable status (404)")
        void doesNotRetryNonRetryable() {
            enableFastRetry(3);
            stubErrorInline(wireMock, "/fabric/v4/networks/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not Found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.networks().getByUuid("test-uuid"));
            wireMock.verify(1, getRequestedFor(urlPathMatching("/fabric/v4/networks/.*")));
        }

        @Test
        @DisplayName("does not retry a POST by default, even on a retryable 503 (no duplicate side effects)")
        void doesNotRetryPostByDefault() {
            // Default-method gating: retryNonIdempotentMethods=false, so POST is never retried.
            enableFastRetry(3);
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/connections/search"))
                    .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-503\",\"errorMessage\":\"Service Unavailable\"}]")));

            assertThrows(EquinixServerException.class, () -> fabric.connections().search());
            // Exactly one POST despite the 503 being a retryable status — POST is non-idempotent.
            wireMock.verify(1, postRequestedFor(urlPathMatching("/fabric/v4/connections/search")));
        }

        @Test
        @DisplayName("retries a POST when retryNonIdempotentMethods is explicitly enabled")
        void retriesPostWhenNonIdempotentRetryEnabled() {
            // Opt-in: the 7-arg policy enables retrying non-idempotent methods.
            fabric.getEquinixClient().setRetryPolicy(new RetryPolicy(
                    2, 1, 5, java.util.Set.of(429, 500, 502, 503, 504), true, true, true));
            stubErrorInline(wireMock, "/fabric/v4/connections/search",
                    503, "[{\"errorCode\":\"ERR-503\",\"errorMessage\":\"Service Unavailable\"}]");

            assertThrows(EquinixServerException.class, () -> fabric.connections().search());
            // 1 initial + 2 retries = 3 POSTs once non-idempotent retry is opted in.
            wireMock.verify(3, postRequestedFor(urlPathMatching("/fabric/v4/connections/search")));
        }
    }

    @Nested
    @DisplayName("Correlation id")
    class CorrelationId {

        private static final String UUID_REGEX =
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

        @Test
        @DisplayName("every request carries a generated X-Correlation-Id header")
        void requestCarriesCorrelationIdHeader() {
            wireMock.stubFor(get(urlPathMatching("/fabric/v4/connections/.*"))
                    .willReturn(okJson(TestFixtures.load("/json/fabric/connection_response.json"))));

            fabric.connections().getByUuid("test-uuid");

            wireMock.verify(getRequestedFor(urlPathMatching("/fabric/v4/connections/.*"))
                    .withHeader("X-Correlation-Id", matching(UUID_REGEX)));
        }

        @Test
        @DisplayName("service exceptions carry the correlation id (field and message)")
        void serviceExceptionCarriesCorrelationId() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}]");

            EquinixNotFoundException ex = assertThrows(EquinixNotFoundException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));

            assertNotNull(ex.getCorrelationId());
            assertTrue(ex.getCorrelationId().matches(UUID_REGEX));
            assertTrue(ex.getMessage().contains("Correlation Id: " + ex.getCorrelationId()));
            // The id the exception reports is the id that was actually sent to the API.
            wireMock.verify(getRequestedFor(urlPathMatching("/fabric/v4/connections/.*"))
                    .withHeader("X-Correlation-Id", equalTo(ex.getCorrelationId())));
        }

        @Test
        @DisplayName("all retry attempts of one logical request share a single correlation id")
        void retriesShareOneCorrelationId() {
            fabric.getEquinixClient().setRetryPolicy(
                    new RetryPolicy(2, 1, 5, java.util.Set.of(503), true, true));
            stubErrorInline(wireMock, "/fabric/v4/networks/.*",
                    503, "[{\"errorCode\":\"ERR-503\",\"errorMessage\":\"Service Unavailable\"}]");

            EquinixServerException ex = assertThrows(EquinixServerException.class,
                    () -> fabric.networks().getByUuid("test-uuid"));

            // 3 attempts, all sent with the exception's correlation id.
            wireMock.verify(3, getRequestedFor(urlPathMatching("/fabric/v4/networks/.*"))
                    .withHeader("X-Correlation-Id", equalTo(ex.getCorrelationId())));
        }
    }

    @Nested
    @DisplayName("Circuit breaker")
    class CircuitBreaking {

        @Test
        @DisplayName("opens after N consecutive 5xx failures and fails fast without touching the network")
        void opensAfterConsecutiveServerErrorsAndFailsFast() {
            fabric.getEquinixClient().setCircuitBreaker(new CircuitBreaker(2, 60_000));
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    503, "[{\"errorCode\":\"ERR-503\",\"errorMessage\":\"Service Unavailable\"}]");

            // Two real failures trip the breaker...
            assertThrows(EquinixServerException.class, () -> fabric.connections().getByUuid("test-uuid"));
            assertThrows(EquinixServerException.class, () -> fabric.connections().getByUuid("test-uuid"));

            // ...the third call is rejected client-side with CircuitOpenException.
            CircuitOpenException rejection = assertThrows(CircuitOpenException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
            assertTrue(rejection.getRemainingCooldownMillis() > 0);

            // Only the two real attempts reached the wire.
            wireMock.verify(2, getRequestedFor(urlPathMatching("/fabric/v4/connections/.*")));
        }

        @Test
        @DisplayName("4xx responses do not trip the breaker (the service is answering)")
        void clientErrorsDoNotTripBreaker() {
            fabric.getEquinixClient().setCircuitBreaker(new CircuitBreaker(2, 60_000));
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}]");

            for (int i = 0; i < 4; i++) {
                assertThrows(EquinixNotFoundException.class,
                        () -> fabric.connections().getByUuid("test-uuid"));
            }

            // All four calls reached the wire — the breaker never opened.
            wireMock.verify(4, getRequestedFor(urlPathMatching("/fabric/v4/connections/.*")));
        }

        @Test
        @DisplayName("after the cooldown a successful half-open probe closes the breaker")
        void recoversViaHalfOpenProbe() throws InterruptedException {
            fabric.getEquinixClient().setCircuitBreaker(new CircuitBreaker(1, 100));
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    503, "[{\"errorCode\":\"ERR-503\",\"errorMessage\":\"Service Unavailable\"}]");

            // One failure opens the breaker (threshold 1); while open, calls are rejected.
            assertThrows(EquinixServerException.class, () -> fabric.connections().getByUuid("test-uuid"));
            assertThrows(CircuitOpenException.class, () -> fabric.connections().getByUuid("test-uuid"));

            // Service recovers; after the cooldown the probe goes through and closes the circuit.
            resetStubs();
            wireMock.stubFor(get(urlPathMatching("/fabric/v4/connections/.*"))
                    .willReturn(okJson(TestFixtures.load("/json/fabric/connection_response.json"))));
            Thread.sleep(150);

            assertNotNull(fabric.connections().getByUuid("test-uuid")); // the probe
            assertNotNull(fabric.connections().getByUuid("test-uuid")); // circuit closed again
        }
    }
}
