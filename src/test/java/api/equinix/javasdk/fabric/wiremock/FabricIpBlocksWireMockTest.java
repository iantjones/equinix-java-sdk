package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.fabric.enums.IpBlockProductType;
import api.equinix.javasdk.fabric.model.IpBlock;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric IP blocks.
 * Covers the mutation surface: define().create() (POST /fabric/v4/ipBlocks)
 * and update(...).save() (PATCH /fabric/v4/ipBlocks/{uuid}).
 */
class FabricIpBlocksWireMockTest extends WireMockTestBase {

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
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the IP block creation body to /fabric/v4/ipBlocks")
        void createPostsBody() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/ipBlocks"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/ip_block_response.json"))));

            IpBlock created = fabric.ipBlocks().define()
                    .ofType(IpBlockProductType.IPV4_IP_BLOCK)
                    .inMetro("SY")
                    .prefixLength(28)
                    .create();

            assertNotNull(created);
            assertEquals("31fbdb3f-8def-410d-868b-ef920878affb", created.getUuid());
            assertEquals(IpBlockProductType.IPV4_IP_BLOCK, created.getType());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/ipBlocks"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("IPV4_IP_BLOCK")))
                    .withRequestBody(matchingJsonPath("$.location.metroCode", equalTo("SY")))
                    .withRequestBody(matchingJsonPath("$.prefixLength", equalTo("28"))));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes a JSON Patch array as application/json-patch+json")
        void savePatchesPrefixLength() {
            stubSingletonGet();
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/ipBlocks/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/ip_block_response.json"))));

            IpBlock ipBlock = fabric.ipBlocks().getByUuid("31fbdb3f-8def-410d-868b-ef920878affb");
            IpBlock updated = ipBlock.update()
                    .patch(PatchOperation.replace("/prefixLength", 27))
                    .save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/fabric/v4/ipBlocks/31fbdb3f-8def-410d-868b-ef920878affb"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/prefixLength\",\"value\":27}]", true, true)));
        }

        @Test
        @DisplayName("save() with no operations throws and makes no request")
        void emptyUpdateThrows() {
            stubSingletonGet();

            IpBlock ipBlock = fabric.ipBlocks().getByUuid("31fbdb3f-8def-410d-868b-ef920878affb");
            assertThrows(IllegalStateException.class, () -> ipBlock.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/ipBlocks/.*")));
        }

        private void stubSingletonGet() {
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/ipBlocks/31fbdb3f-8def-410d-868b-ef920878affb"))
                    .willReturn(okJson(loadFixture("/json/fabric/ip_block_response.json"))));
        }
    }
}
