package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.fabric.model.Tag;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Tags.
 * Covers the create(type, name, displayName) operation and asserts the request body.
 */
class FabricTagsWireMockTest extends WireMockTestBase {

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
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("POSTs type/name/displayName to /fabric/v4/tags and returns the created tag")
        void createPostsBodyAndReturnsTag() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/tags"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/tag_response.json"))));

            Tag tag = fabric.tags().create("RESOURCE_TAG", "environment", "Environment");

            assertNotNull(tag);
            assertEquals("18a127ad-9d0c-46e2-a66d-8ed85d1858b0", tag.getUuid());
            assertEquals("RESOURCE_TAG", tag.getType());
            assertEquals("environment", tag.getName());
            assertEquals("Environment", tag.getDisplayName());
            assertEquals(10000, tag.getWeight());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/tags"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "{\"type\":\"RESOURCE_TAG\",\"name\":\"environment\",\"displayName\":\"Environment\"}",
                            true, true)));
        }
    }
}
