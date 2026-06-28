package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.PublicKey;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge Public Keys. The publicKeys resource exposes only
 * list (GET) and create (POST) — the API has no get-by-id or delete endpoint.
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
    @DisplayName("list()")
    class ListKeys {

        @Test
        @DisplayName("returns the list of public keys")
        void returnsPublicKeys() {
            wireMock.stubFor(get(urlPathMatching("/ne/v1/publicKeys/?"))
                    .willReturn(okJson("[{\"uuid\":\"b2c3d4e5-f6a7-8901-bcde-234567890abc\","
                            + "\"keyName\":\"test-public-key\","
                            + "\"keyValue\":\"ssh-rsa AAAA test@example.com\","
                            + "\"custOrgId\":\"org-12345\",\"accountUcmId\":\"ucm-67890\"}]")));

            List<PublicKey> publicKeys = networkEdge.publicKeys().list();
            assertNotNull(publicKeys);
            assertEquals(1, publicKeys.size());
            assertEquals("test-public-key", publicKeys.get(0).getKeyName());
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/publicKeys",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.publicKeys().list());
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the create body and reads the created public key from the response")
        void createsPublicKey() {
            String keyValue = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQTestKeyValue test@example.com";

            // POST /ne/v1/publicKeys -> 201 with the created object in the body.
            wireMock.stubFor(post(urlPathMatching("/ne/v1/publicKeys/?"))
                    .willReturn(okJson("{\"uuid\":\"b2c3d4e5-f6a7-8901-bcde-234567890abc\","
                            + "\"keyName\":\"test-public-key\","
                            + "\"keyValue\":\"" + keyValue + "\","
                            + "\"custOrgId\":\"org-12345\",\"accountUcmId\":\"ucm-67890\"}")));

            PublicKey publicKey = networkEdge.publicKeys()
                    .define("test-public-key", keyValue)
                    .forAccount("ucm-67890")
                    .create();

            assertNotNull(publicKey);
            // getUuid()/getKeyName() reflect the response body returned by the POST.
            assertEquals("b2c3d4e5-f6a7-8901-bcde-234567890abc", publicKey.getUuid());
            assertEquals("test-public-key", publicKey.getKeyName());

            // Verify the outgoing create request body.
            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/publicKeys/?"))
                    .withRequestBody(matchingJsonPath("$.keyName", equalTo("test-public-key")))
                    .withRequestBody(matchingJsonPath("$.keyValue", equalTo(keyValue)))
                    .withRequestBody(matchingJsonPath("$.accountUcmId", equalTo("ucm-67890"))));
        }
    }
}
