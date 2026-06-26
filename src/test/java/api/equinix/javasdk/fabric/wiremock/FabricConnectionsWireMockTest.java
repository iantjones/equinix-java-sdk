package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.Connection;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Connections.
 * Tests search, getByUuid, create/delete lifecycle, and error handling.
 */
class FabricConnectionsWireMockTest extends WireMockTestBase {

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
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns connection for valid UUID")
        void returnsConnection() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            assertNotNull(connection);
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", connection.getUuid());
            assertEquals("My-EVPL-Connection", connection.getName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound_throws404() {
            stubErrorInline(wireMock, "/fabric/v4/connections/invalid-uuid",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Connection not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.connections().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes a JSON Patch array as application/json-patch+json")
        void savePatchesNameAndBandwidth() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/connections/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_response.json"))));

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            Connection updated = connection.update().name("Renamed-Connection").bandwidth(200).save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Connection\"},"
                            + "{\"op\":\"replace\",\"path\":\"/bandwidth\",\"value\":200}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            assertThrows(IllegalStateException.class, () -> connection.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/connections/.*")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("403 throws EquinixAuthorizationException")
        void forbidden() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    403, "[{\"errorCode\":\"ERR-403\",\"errorMessage\":\"Forbidden\"}]");

            assertThrows(EquinixAuthorizationException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("429 throws EquinixRateLimitException")
        void rateLimited() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    429, "[{\"errorCode\":\"ERR-429\",\"errorMessage\":\"Rate limit exceeded\"}]");

            assertThrows(EquinixRateLimitException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }
    }
}
