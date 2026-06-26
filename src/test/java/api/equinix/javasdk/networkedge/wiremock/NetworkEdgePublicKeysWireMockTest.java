package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.PublicKey;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
    @DisplayName("define() / create()")
    class Create {

        // Valid UUID for the 201 Location header (Constants.UUID_PATTERN = 8-4-4-4-12 hex).
        private static final String NEW_UUID = "b2c3d4e5-f6a7-8901-bcde-234567890abc";

        @Test
        @DisplayName("POSTs the create body, follows the 201 Location header, and GETs the new public key")
        void createsPublicKey() {
            String keyValue = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQTestKeyValue test@example.com";

            // POST /ne/v1/publicKeys -> 201 with Location header carrying the new uuid.
            wireMock.stubFor(post(urlPathMatching("/ne/v1/publicKeys/?"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Location", "https://localhost/ne/v1/publicKeys/" + NEW_UUID)));
            // GET /ne/v1/publicKeys/{uuid} -> returns the created object body.
            stubSingleton(wireMock, "/ne/v1/publicKeys/.*", "/json/networkedge/publickey_response.json");

            PublicKey publicKey = networkEdge.publicKeys()
                    .define("test-public-key", keyValue)
                    .forAccount("ucm-67890")
                    .create();

            assertNotNull(publicKey);
            // getUuid()/getKeyName() reflect the fixture body returned by the follow-up GET.
            assertEquals("b2c3d4e5-f6a7-8901-bcde-234567890abc", publicKey.getUuid());
            assertEquals("test-public-key", publicKey.getKeyName());

            // Verify the outgoing create request body.
            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/publicKeys/?"))
                    .withRequestBody(matchingJsonPath("$.keyName", equalTo("test-public-key")))
                    .withRequestBody(matchingJsonPath("$.keyValue", equalTo(keyValue)))
                    .withRequestBody(matchingJsonPath("$.accountUcmId", equalTo("ucm-67890"))));
            // Verify the follow-up GET for the uuid parsed from the Location header.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/publicKeys/" + NEW_UUID)));
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
