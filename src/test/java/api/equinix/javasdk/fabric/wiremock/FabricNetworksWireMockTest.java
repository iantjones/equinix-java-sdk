package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.Network;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Networks.
 */
class FabricNetworksWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns network for valid UUID")
        void returnsNetwork() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            assertNotNull(network);
            assertEquals("c3d4e5f6-a7b8-9012-cdef-234567890abc", network.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/networks/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Network not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.networks().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("sends an RFC 6902 JSON Patch with json-patch content-type")
        void savePatchesName() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/networks/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/network_response.json"))));

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            Network updated = network.update().name("New-Name").save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/networks/c3d4e5f6-a7b8-9012-cdef-234567890abc"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"New-Name\"}]")));
        }

        @Test
        @DisplayName("accumulates multiple field changes into one patch document")
        void saveMultipleFields() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/networks/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/network_response.json"))));

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            network.update()
                    .name("Renamed")
                    .patch(api.equinix.javasdk.core.http.request.PatchOperation.replace("/scope", "GLOBAL"))
                    .save();

            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/networks/.*"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed\"},"
                                    + "{\"op\":\"replace\",\"path\":\"/scope\",\"value\":\"GLOBAL\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            assertThrows(IllegalStateException.class, () -> network.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/networks/.*")));
        }

        @Test
        @DisplayName("404 on update throws EquinixNotFoundException")
        void updateNotFound() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/networks/.*"))
                    .willReturn(aResponse().withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Network not found\"}]")));

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            assertThrows(EquinixNotFoundException.class, () -> network.update().name("x").save());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/networks/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.networks().getByUuid("test-uuid"));
        }
    }
}
