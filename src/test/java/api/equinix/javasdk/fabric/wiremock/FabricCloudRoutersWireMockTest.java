package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.CloudRouter;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Cloud Routers.
 */
class FabricCloudRoutersWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns cloud router for valid UUID")
        void returnsCloudRouter() {
            stubSingleton(wireMock, "/fabric/v4/routers/.*",
                    "/json/fabric/cloud_router_response.json");

            CloudRouter router = fabric.cloudRouters().getByUuid("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            assertNotNull(router);
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", router.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/routers/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Cloud router not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.cloudRouters().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("sends an RFC 6902 JSON Patch with json-patch content-type")
        void savePatchesName() {
            stubSingleton(wireMock, "/fabric/v4/routers/.*",
                    "/json/fabric/cloud_router_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/routers/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/cloud_router_response.json"))));

            CloudRouter router = fabric.cloudRouters().getByUuid("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            CloudRouter updated = router.update().name("Renamed-Router").save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Router\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/routers/.*",
                    "/json/fabric/cloud_router_response.json");

            CloudRouter router = fabric.cloudRouters().getByUuid("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            assertThrows(IllegalStateException.class, () -> router.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/routers/.*")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/routers/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.cloudRouters().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/routers/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.cloudRouters().getByUuid("test-uuid"));
        }
    }
}
