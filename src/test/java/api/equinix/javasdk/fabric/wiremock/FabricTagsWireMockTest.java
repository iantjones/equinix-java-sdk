package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.Tag;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.stubErrorInline;
import static api.equinix.javasdk.core.ResponseStubs.stubPaginatedGet;
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

    @Nested
    @DisplayName("list()")
    class List {

        @Test
        @DisplayName("GETs /fabric/v4/tags and returns the paginated tags")
        void listGetsAndReturnsTags() {
            stubPaginatedGet(wireMock, "/fabric/v4/tags", "/json/fabric/paginated_tags.json");

            PaginatedList<Tag> tags = fabric.tags().list();

            assertNotNull(tags);
            assertEquals(2, tags.size());

            Tag first = tags.get(0);
            assertEquals("18a127ad-9d0c-46e2-a66d-8ed85d1858b0", first.getUuid());
            assertEquals("RESOURCE_TAG", first.getType());
            assertEquals("environment", first.getName());
            assertEquals("Environment", first.getDisplayName());
            assertEquals(10000, first.getWeight());

            Tag second = tags.get(1);
            assertEquals("2b9f43c1-7a6d-4e11-9c3a-1f2e3d4c5b6a", second.getUuid());
            assertEquals("costCenter", second.getName());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/tags")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 on list() throws EquinixAuthenticationException")
        void unauthorizedList() {
            stubErrorInline(wireMock, "/fabric/v4/tags",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.tags().list());
        }

        @Test
        @DisplayName("500 on create() throws EquinixServerException")
        void serverErrorCreate() {
            stubErrorInline(wireMock, "/fabric/v4/tags",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.tags().create("RESOURCE_TAG", "environment", "Environment"));
        }
    }
}
