package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.NetworkEquinixStatus;
import api.equinix.javasdk.fabric.enums.NetworkScope;
import api.equinix.javasdk.fabric.enums.NetworkType;
import api.equinix.javasdk.fabric.model.Network;
import api.equinix.javasdk.fabric.model.implementation.Change;
import org.junit.jupiter.api.*;

import java.util.List;

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
        @DisplayName("deserializes account, operation and links fields")
        void deserializesNewFields() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");

            assertNotNull(network.getAccount());
            assertEquals("Acme Corp", network.getAccount().getAccountName());
            assertNotNull(network.getOperation());
            assertEquals(NetworkEquinixStatus.PROVISIONED, network.getOperation().getEquinixStatus());
            assertNotNull(network.getLinks());
            assertEquals(1, network.getLinks().size());
            assertEquals("getNetworkConnections", network.getLinks().get(0).getRel());
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
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the network body to the collection and returns the created network")
        void createsNetwork() {
            stubCreate(wireMock, "/fabric/v4/networks", "/json/fabric/network_response.json");

            Network network = fabric.networks().define(NetworkType.EVPLAN)
                    .name("Production-EVPLAN-Network")
                    .scope(NetworkScope.REGIONAL)
                    .create();

            assertNotNull(network);
            assertEquals("c3d4e5f6-a7b8-9012-cdef-234567890abc", network.getUuid());
            assertEquals("Production-EVPLAN-Network", network.getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/networks"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("EVPLAN")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Production-EVPLAN-Network")))
                    .withRequestBody(matchingJsonPath("$.scope", equalTo("REGIONAL"))));
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
    @DisplayName("getChanges()")
    class GetChanges {

        @Test
        @DisplayName("GETs {uuid}/changes and returns the list of changes")
        void returnsChanges() {
            stubPaginatedGet(wireMock, "/fabric/v4/networks/.*/changes",
                    "/json/fabric/network_changes_response.json");

            List<Change> changes = fabric.networks().getChanges("c3d4e5f6-a7b8-9012-cdef-234567890abc");

            assertNotNull(changes);
            assertEquals(2, changes.size());
            assertEquals("4b17da68-3d6b-436d-9c8f-2105f3b950d9", changes.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/networks/c3d4e5f6-a7b8-9012-cdef-234567890abc/changes")));
        }
    }

    @Nested
    @DisplayName("getChange()")
    class GetChange {

        @Test
        @DisplayName("GETs {uuid}/changes/{changeId} and returns the single change")
        void returnsChange() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*/changes/.*",
                    "/json/fabric/network_change_response.json");

            Change change = fabric.networks().getChange(
                    "c3d4e5f6-a7b8-9012-cdef-234567890abc", "4b17da68-3d6b-436d-9c8f-2105f3b950d9");

            assertNotNull(change);
            assertEquals("4b17da68-3d6b-436d-9c8f-2105f3b950d9", change.getUuid());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/networks/c3d4e5f6-a7b8-9012-cdef-234567890abc/changes/4b17da68-3d6b-436d-9c8f-2105f3b950d9")));
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
