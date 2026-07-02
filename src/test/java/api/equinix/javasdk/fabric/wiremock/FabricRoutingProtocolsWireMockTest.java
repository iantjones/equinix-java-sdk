package api.equinix.javasdk.fabric.wiremock;
import api.equinix.javasdk.fabric.enums.BGPConnectionOperationalStatus;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.BGPActionType;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.fabric.model.BGPAction;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.implementation.Change;
import org.junit.jupiter.api.*;

import java.util.List;

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

            // BGPConnectionIpv4/Ipv6 fidelity: routesMax + nested operation block.
            assertEquals(1000L, protocol.getBgpIpv4().getRoutesMax());
            assertNotNull(protocol.getBgpIpv4().getOperation());
            assertEquals(BGPConnectionOperationalStatus.UP, protocol.getBgpIpv4().getOperation().getOperationalStatus());
            assertNotNull(protocol.getBgpIpv4().getOperation().getOpStatusChangedAt());
            assertEquals(500L, protocol.getBgpIpv6().getRoutesMax());
            assertNotNull(protocol.getBgpIpv6().getOperation());
            assertEquals(BGPConnectionOperationalStatus.DOWN, protocol.getBgpIpv6().getOperation().getOperationalStatus());

            // Top-level spec fields added by the fidelity wave.
            assertEquals("testAuthKey", protocol.getBgpAuthKey());
            assertEquals(Boolean.TRUE, protocol.getAsOverrideEnabled());
            assertNotNull(protocol.getOperation());
            assertEquals(1, protocol.getOperation().getErrors().size());
            assertEquals("EQ-3044019", protocol.getOperation().getErrors().get(0).getErrorCode());
            assertNotNull(protocol.getProject());
            assertEquals("d7b0a4b8-1c2d-4e5f-a6b7-c8d9e0f12345", protocol.getProject().getProjectId());
            assertNotNull(protocol.getConnection());
            assertEquals(CONNECTION_ID, protocol.getConnection().getUuid());
            assertEquals("9a8b7c6d-5e4f-3210-abcd-ef9876543210", protocol.getConnection().getPlatformUuid());

            // The spec names this wire property "changelog" (lowercase), unlike most resources.
            assertNotNull(protocol.getChangeLog());
            assertEquals("user1234", protocol.getChangeLog().getCreatedBy());
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
    @DisplayName("replace(connectionId, uuid, builder)")
    class Replace {

        @Test
        @DisplayName("PUTs a full routing protocol body and returns the replaced model")
        void replacesRoutingProtocol() {
            wireMock.stubFor(put(urlPathMatching("/fabric/v4/connections/.*/routingProtocols/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/routing_protocol_response.json"))));

            RoutingProtocol replaced = fabric.routingProtocols().replace(CONNECTION_ID, PROTOCOL_UUID,
                    fabric.routingProtocols().define()
                            .ofType(RoutingProtocolType.BGP)
                            .withName("Replaced-Protocol")
                            .withBGPIpv4("192.168.100.1", "192.168.100.2", true)
                            .withBgpAuthKey("secret-key")
                            .withAsOverrideEnabled(true)
                            .withCustomerAsn(65001L));

            assertNotNull(replaced);
            assertEquals(PROTOCOL_UUID, replaced.getUuid());
            wireMock.verify(putRequestedFor(urlPathMatching(PROTOCOL_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Replaced-Protocol")))
                    .withRequestBody(matchingJsonPath("$.bgpAuthKey", equalTo("secret-key")))
                    .withRequestBody(matchingJsonPath("$.asOverrideEnabled", equalTo("true"))));
        }
    }

    @Nested
    @DisplayName("createBulk(connectionId, builders)")
    class CreateBulk {

        @Test
        @DisplayName("POSTs a {\"data\":[...]} body and returns the created protocols")
        void createsBulk() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/connections/.*/routingProtocols/bulk"))
                    .willReturn(okJson(loadFixture("/json/fabric/routing_protocols_bulk_response.json"))));

            List<RoutingProtocol> created = fabric.routingProtocols().createBulk(CONNECTION_ID, List.of(
                    fabric.routingProtocols().define()
                            .ofType(RoutingProtocolType.BGP)
                            .withName("Bulk-BGP-RoutingProtocol")
                            .withBGPIpv4("192.168.100.1", "192.168.100.2", true),
                    fabric.routingProtocols().define()
                            .ofType(RoutingProtocolType.DIRECT)
                            .withName("Bulk-Direct-RoutingProtocol")
                            .withDirectIpv4("192.168.200.2")));

            assertNotNull(created);
            assertEquals(2, created.size());
            assertEquals("Bulk-BGP-RoutingProtocol", created.get(0).getName());
            wireMock.verify(postRequestedFor(urlPathMatching(
                    "/fabric/v4/connections/" + CONNECTION_ID + "/routingProtocols/bulk"))
                    .withRequestBody(matchingJsonPath("$.data[0].name", equalTo("Bulk-BGP-RoutingProtocol")))
                    .withRequestBody(matchingJsonPath("$.data[1].type", equalTo("DIRECT"))));
        }
    }

    @Nested
    @DisplayName("getBgpAction(connectionId, routingProtocolId, actionId)")
    class GetBgpAction {

        @Test
        @DisplayName("GETs a single BGP action by id")
        void returnsBgpAction() {
            stubSingleton(wireMock, "/fabric/v4/connections/.*/routingProtocols/.*/actions/.*",
                    "/json/fabric/bgp_action_response.json");

            BGPAction action = fabric.routingProtocols().getBgpAction(CONNECTION_ID, PROTOCOL_UUID,
                    "b9a8c7d6-e5f4-3210-abcd-fedcba112233");

            assertNotNull(action);
            assertEquals("b9a8c7d6-e5f4-3210-abcd-fedcba112233", action.getUuid());
            assertEquals(BGPActionType.RESET_BGPIPV4, action.getType());
        }
    }

    @Nested
    @DisplayName("getChanges() / getChange()")
    class Changes {

        @Test
        @DisplayName("getChanges() returns the paginated list of changes")
        void returnsChanges() {
            stubPaginatedGet(wireMock, "/fabric/v4/connections/.*/routingProtocols/.*/changes",
                    "/json/fabric/routing_protocol_changes_response.json");

            List<Change> changes = fabric.routingProtocols().getChanges(CONNECTION_ID, PROTOCOL_UUID);

            assertNotNull(changes);
            assertEquals(2, changes.size());
            assertEquals("a9b8c7d6-e5f4-3210-abcd-fedcba987654", changes.get(0).getUuid());
        }

        @Test
        @DisplayName("getChange() returns a single change by id")
        void returnsChange() {
            stubSingleton(wireMock, "/fabric/v4/connections/.*/routingProtocols/.*/changes/.*",
                    "/json/fabric/routing_protocol_change_response.json");

            Change change = fabric.routingProtocols().getChange(CONNECTION_ID, PROTOCOL_UUID,
                    "a9b8c7d6-e5f4-3210-abcd-fedcba987654");

            assertNotNull(change);
            assertEquals("a9b8c7d6-e5f4-3210-abcd-fedcba987654", change.getUuid());
        }
    }

    @Nested
    @DisplayName("define().create(...)")
    class Create {

        @Test
        @DisplayName("POSTs the routing protocol body to the connection and returns the created model (create(connectionId))")
        void createsRoutingProtocolByConnectionId() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/connections/.*/routingProtocols"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/routing_protocol_response.json"))));

            // R2025.5 removed equinixASN from the BGP create request payload; the builder's
            // withEquinixAsn(...) is deprecated and intentionally not used here.
            RoutingProtocol created = fabric.routingProtocols().define()
                    .ofType(RoutingProtocolType.BGP)
                    .withName("New-BGP-RoutingProtocol")
                    .withBGPIpv4("192.168.100.1", "192.168.100.2", true)
                    .withBgpAuthKey("secret-key")
                    .withCustomerAsn(65001L)
                    .create(CONNECTION_ID);

            assertNotNull(created);
            assertEquals(PROTOCOL_UUID, created.getUuid());
            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/" + CONNECTION_ID + "/routingProtocols"))
                    .withRequestBody(equalToJson(
                            "{\"type\":\"BGP\",\"name\":\"New-BGP-RoutingProtocol\","
                                    + "\"bgpIpv4\":{\"customerPeerIp\":\"192.168.100.1\","
                                    + "\"equinixPeerIp\":\"192.168.100.2\",\"enabled\":true},"
                                    + "\"bgpAuthKey\":\"secret-key\","
                                    + "\"customerAsn\":65001}",
                            true, true)));
            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/" + CONNECTION_ID + "/routingProtocols"))
                    .withRequestBody(notMatching(".*equinixAsn.*")));
        }

        @Test
        @DisplayName("create(Connection) delegates to the connection's uuid")
        void createsRoutingProtocolByConnection() {
            // Fetch a Connection so we can drive the create(Connection) overload with a real model.
            stubSingleton(wireMock, "/fabric/v4/connections/" + CONNECTION_ID,
                    "/json/fabric/connection_response.json");
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/connections/.*/routingProtocols"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/routing_protocol_response.json"))));

            Connection connection = fabric.connections().getByUuid(CONNECTION_ID);

            RoutingProtocol created = fabric.routingProtocols().define()
                    .ofType(RoutingProtocolType.DIRECT)
                    .withName("New-Direct-RoutingProtocol")
                    .withDirectIpv4("192.168.200.2")
                    .create(connection);

            assertNotNull(created);
            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/" + connection.getUuid() + "/routingProtocols"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("DIRECT")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("New-Direct-RoutingProtocol")))
                    .withRequestBody(matchingJsonPath("$.directIpv4.equinixIfaceIp", equalTo("192.168.200.2"))));
        }
    }

    @Nested
    @DisplayName("createBgpAction(connectionId, routingProtocolId, type)")
    class CreateBgpAction {

        @Test
        @DisplayName("POSTs a {\"type\":...} body to the actions sub-resource and returns the created action")
        void createsBgpAction() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/connections/.*/routingProtocols/.*/actions"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/bgp_action_response.json"))));

            BGPAction action = fabric.routingProtocols()
                    .createBgpAction(CONNECTION_ID, PROTOCOL_UUID, BGPActionType.RESET_BGPIPV4);

            assertNotNull(action);
            assertEquals("b9a8c7d6-e5f4-3210-abcd-fedcba112233", action.getUuid());
            assertEquals(BGPActionType.RESET_BGPIPV4, action.getType());
            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/" + CONNECTION_ID + "/routingProtocols/" + PROTOCOL_UUID + "/actions"))
                    .withRequestBody(equalToJson("{\"type\":\"RESET_BGPIPV4\"}", true, true)));
        }
    }

    @Nested
    @DisplayName("list(connectionId)")
    class ListByConnection {

        @Test
        @DisplayName("GETs the connection's routingProtocols collection and maps the paginated body")
        void listsRoutingProtocols() {
            stubPaginatedGet(wireMock, "/fabric/v4/connections/.*/routingProtocols",
                    "/json/fabric/paginated_routing_protocols.json");

            PaginatedList<RoutingProtocol> protocols = fabric.routingProtocols().list(CONNECTION_ID);

            assertNotNull(protocols);
            assertEquals(2, protocols.size());
            assertEquals("f1e2d3c4-b5a6-7890-abcd-1234567890ef", protocols.get(0).getUuid());
            assertEquals(RoutingProtocolType.BGP, protocols.get(0).getType());
            assertEquals(RoutingProtocolType.DIRECT, protocols.get(1).getType());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/" + CONNECTION_ID + "/routingProtocols")));
        }
    }

    @Nested
    @DisplayName("getBgpActions(connectionId, routingProtocolId)")
    class GetBgpActions {

        @Test
        @DisplayName("GETs the routing protocol's actions collection and returns the list")
        void listsBgpActions() {
            stubPaginatedGet(wireMock, "/fabric/v4/connections/.*/routingProtocols/.*/actions",
                    "/json/fabric/paginated_bgp_actions.json");

            List<BGPAction> actions = fabric.routingProtocols().getBgpActions(CONNECTION_ID, PROTOCOL_UUID);

            assertNotNull(actions);
            assertEquals(2, actions.size());
            assertEquals("b9a8c7d6-e5f4-3210-abcd-fedcba112233", actions.get(0).getUuid());
            assertEquals(BGPActionType.RESET_BGPIPV4, actions.get(0).getType());
            assertEquals(BGPActionType.RESET_BGPIPV6, actions.get(1).getType());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/" + CONNECTION_ID + "/routingProtocols/" + PROTOCOL_UUID + "/actions")));
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
