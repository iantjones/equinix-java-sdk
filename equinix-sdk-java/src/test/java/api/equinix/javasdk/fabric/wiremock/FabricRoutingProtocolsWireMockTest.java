package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Routing Protocols.
 *
 * <p>Routing protocols are parent-keyed on a connection, so the fluent {@code update(connectionId)}
 * shares the same shape as {@code delete(connectionId)} / {@code refresh(connectionId)}.</p>
 */
class FabricRoutingProtocolsWireMockTest extends WireMockTestBase {

    static final String CONNECTION_ID = "c0ffee00-1111-2222-3333-444455556666";
    static final String PROTOCOL_UUID = "f1e2d3c4-b5a6-7890-abcd-1234567890ef";
    static final String PROTOCOL_PATH =
            "/fabric/v4/connections/" + CONNECTION_ID + "/routingProtocols/" + PROTOCOL_UUID;

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
        @DisplayName("returns routing protocol for valid UUID")
        void returnsRoutingProtocol() {
            stubSingleton(wireMock, "/fabric/v4/connections/.*/routingProtocols/.*",
                    "/json/fabric/routing_protocol_response.json");

            RoutingProtocol protocol = fabric.routingProtocols().getByUuid(CONNECTION_ID, PROTOCOL_UUID);
            assertNotNull(protocol);
            assertEquals(PROTOCOL_UUID, protocol.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*/routingProtocols/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Routing protocol not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.routingProtocols().getByUuid(CONNECTION_ID, "invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("update(connectionId) / save()")
    class Update {

        @Test
        @DisplayName("PATCHes an op/path/value array as application/json")
        void savePatchesName() {
            stubSingleton(wireMock, "/fabric/v4/connections/.*/routingProtocols/.*",
                    "/json/fabric/routing_protocol_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/connections/.*/routingProtocols/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/routing_protocol_response.json"))));

            RoutingProtocol protocol = fabric.routingProtocols().getByUuid(CONNECTION_ID, PROTOCOL_UUID);
            RoutingProtocol updated = protocol.update(CONNECTION_ID)
                    .name("Renamed-Protocol")
                    .bgpIpv4Enabled(false)
                    .save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching(PROTOCOL_PATH))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Protocol\"},"
                                    + "{\"op\":\"replace\",\"path\":\"/bgpIpv4/enabled\",\"value\":false}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/connections/.*/routingProtocols/.*",
                    "/json/fabric/routing_protocol_response.json");

            RoutingProtocol protocol = fabric.routingProtocols().getByUuid(CONNECTION_ID, PROTOCOL_UUID);
            assertThrows(IllegalStateException.class, () -> protocol.update(CONNECTION_ID).save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/connections/.*/routingProtocols/.*")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*/routingProtocols/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.routingProtocols().getByUuid(CONNECTION_ID, PROTOCOL_UUID));
        }
    }
}
