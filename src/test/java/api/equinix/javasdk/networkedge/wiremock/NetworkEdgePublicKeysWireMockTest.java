package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.PublicKey;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge Public Keys.
 */
class NetworkEdgePublicKeysWireMockTest extends WireMockTestBase {

    static NetworkEdge networkEdge;

    @BeforeAll
    static void setUp() {
        networkEdge = new NetworkEdge(testCredentials());
        redirectToWireMock(networkEdge);
        networkEdge.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (networkEdge != null) networkEdge.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns public key for valid UUID")
        void returnsPublicKey() {
            stubSingleton(wireMock, "/ne/v1/publicKeys/.*",
                    "/json/networkedge/publickey_response.json");

            PublicKey publicKey = networkEdge.publicKeys().getByUuid("b2c3d4e5-f6a7-8901-bcde-234567890abc");
            assertNotNull(publicKey);
            assertEquals("b2c3d4e5-f6a7-8901-bcde-234567890abc", publicKey.getUuid());
            assertEquals("test-public-key", publicKey.getKeyName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/ne/v1/publicKeys/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Public key not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> networkEdge.publicKeys().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/publicKeys/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.publicKeys().getByUuid("test-uuid"));
        }
    }
}
