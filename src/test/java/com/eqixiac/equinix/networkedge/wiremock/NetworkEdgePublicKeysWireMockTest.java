package com.eqixiac.equinix.networkedge.wiremock;

import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.networkedge.enums.KeyType;
import com.eqixiac.equinix.networkedge.model.PublicKey;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge Public Keys: list (GET), create (POST) and delete
 * (DELETE {uuid}; live-only, not in the published spec). Create and delete shapes are the ones
 * observed live on 2026-11-11: 201 with an empty body and the id in {@code Location}; 204.
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
            // getPublicKeys returns a GetPublicKeysResponse object: {pagination, data:[...]}.
            wireMock.stubFor(get(urlPathMatching("/ne/v1/publicKeys/?"))
                    .willReturn(okJson("{\"pagination\":{\"offset\":0,\"limit\":20,\"total\":1},"
                            + "\"data\":[{\"uuid\":\"b2c3d4e5-f6a7-8901-bcde-234567890abc\","
                            + "\"keyName\":\"test-public-key\","
                            + "\"keyValue\":\"ssh-rsa AAAA test@example.com\","
                            + "\"keyType\":\"RSA\"}]}")));

            List<PublicKey> publicKeys = networkEdge.publicKeys().list();
            assertNotNull(publicKeys);
            assertEquals(1, publicKeys.size());
            assertEquals("test-public-key", publicKeys.get(0).getKeyName());
            assertEquals(KeyType.RSA, publicKeys.get(0).getKeyType());
        }

        @Test
        @DisplayName("list(accountUcmId) sends accountUcmId as a query param")
        void filtersByAccountUcmId() {
            wireMock.stubFor(get(urlPathMatching("/ne/v1/publicKeys/?"))
                    .willReturn(okJson("{\"pagination\":{\"offset\":0,\"limit\":20,\"total\":1},"
                            + "\"data\":[{\"uuid\":\"b2c3d4e5-f6a7-8901-bcde-234567890abc\","
                            + "\"keyName\":\"acct-scoped-key\","
                            + "\"keyValue\":\"ssh-rsa AAAA acct@example.com\","
                            + "\"keyType\":\"RSA\"}]}")));

            List<PublicKey> publicKeys = networkEdge.publicKeys().list("ucm-67890");

            assertNotNull(publicKeys);
            assertEquals(1, publicKeys.size());
            assertEquals("acct-scoped-key", publicKeys.get(0).getKeyName());

            // GET /ne/v1/publicKeys?accountUcmId=ucm-67890
            wireMock.verify(getRequestedFor(urlPathMatching("/ne/v1/publicKeys/?"))
                    .withQueryParam("accountUcmId", equalTo("ucm-67890")));
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
    @DisplayName("define() / create() / delete()")
    class Create {

        private static final String UUID = "0f1bde3d-7b7a-4ade-8619-62f486bdbe5f";

        @Test
        @DisplayName("reads the new id from Location on the live 201-with-empty-body, then the key from the list")
        void createsPublicKey() {
            String keyValue = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQTestKeyValue test@example.com";

            wireMock.stubFor(post(urlPathMatching("/ne/v1/publicKeys/?"))
                    .willReturn(aResponse().withStatus(201).withHeader("Location", "/v1/publicKeys/" + UUID)));
            // Live list shape: a bare array (2026-11-11).
            wireMock.stubFor(get(urlPathMatching("/ne/v1/publicKeys/?"))
                    .willReturn(okJson("[{\"uuid\":\"" + UUID + "\",\"keyName\":\"test-public-key\","
                            + "\"keyValue\":\"" + keyValue + "\",\"keyType\":\"RSA\",\"projectId\":\"p-1\"}]")));

            PublicKey publicKey = networkEdge.publicKeys()
                    .define("test-public-key", keyValue)
                    .forAccount("ucm-67890")
                    .create();

            assertEquals(UUID, publicKey.getUuid());
            assertEquals("test-public-key", publicKey.getKeyName());
            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/publicKeys/?"))
                    .withRequestBody(matchingJsonPath("$.keyName", equalTo("test-public-key")))
                    .withRequestBody(matchingJsonPath("$.keyValue", equalTo(keyValue)))
                    .withRequestBody(matchingJsonPath("$.accountUcmId", equalTo("ucm-67890"))));
            wireMock.verify(getRequestedFor(urlPathMatching("/ne/v1/publicKeys/?"))
                    .withQueryParam("accountUcmId", equalTo("ucm-67890")));
        }

        @Test
        @DisplayName("falls back to the request values when the new key is not yet listed")
        void createsPublicKeyNotYetListed() {
            wireMock.stubFor(post(urlPathMatching("/ne/v1/publicKeys/?"))
                    .willReturn(aResponse().withStatus(201).withHeader("Location", "/v1/publicKeys/" + UUID)));
            wireMock.stubFor(get(urlPathMatching("/ne/v1/publicKeys/?")).willReturn(okJson("[]")));

            PublicKey publicKey = networkEdge.publicKeys().define("k", "ssh-rsa AAAA x").create();

            assertEquals(UUID, publicKey.getUuid());
            assertEquals("k", publicKey.getKeyName());
        }

        @Test
        @DisplayName("delete() sends DELETE /ne/v1/publicKeys/{uuid} and the wrapper stays readable")
        void deletesPublicKey() {
            wireMock.stubFor(post(urlPathMatching("/ne/v1/publicKeys/?"))
                    .willReturn(aResponse().withStatus(201).withHeader("Location", "/v1/publicKeys/" + UUID)));
            wireMock.stubFor(get(urlPathMatching("/ne/v1/publicKeys/?")).willReturn(okJson("[]")));
            wireMock.stubFor(delete(urlPathEqualTo("/ne/v1/publicKeys/" + UUID))
                    .willReturn(aResponse().withStatus(204)));

            PublicKey publicKey = networkEdge.publicKeys().define("k", "ssh-rsa AAAA x").create();
            assertTrue(publicKey.delete());
            assertTrue(networkEdge.publicKeys().delete(UUID));

            wireMock.verify(2, deleteRequestedFor(urlPathEqualTo("/ne/v1/publicKeys/" + UUID)));
            assertEquals(UUID, publicKey.getUuid());
        }
    }
}
