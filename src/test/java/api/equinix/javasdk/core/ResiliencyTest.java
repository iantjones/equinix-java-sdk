package api.equinix.javasdk.core;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.exception.*;
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
}
