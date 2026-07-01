package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.Direction;
import api.equinix.javasdk.fabric.model.implementation.LinkProtocol;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.RouteAggregationAttachment;
import api.equinix.javasdk.fabric.model.RouteFilterAttachment;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Connections.
 * Tests search, getByUuid, create/delete lifecycle, and error handling.
 */
class FabricConnectionsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns connection for valid UUID")
        void returnsConnection() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            assertNotNull(connection);
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", connection.getUuid());
            assertEquals("My-EVPL-Connection", connection.getName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound_throws404() {
            stubErrorInline(wireMock, "/fabric/v4/connections/invalid-uuid",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Connection not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.connections().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes a JSON Patch array as application/json-patch+json")
        void savePatchesNameAndBandwidth() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/connections/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_response.json"))));

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            Connection updated = connection.update().name("Renamed-Connection").bandwidth(200).save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Connection\"},"
                            + "{\"op\":\"replace\",\"path\":\"/bandwidth\",\"value\":200}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            assertThrows(IllegalStateException.class, () -> connection.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/connections/.*")));
        }
    }

    @Nested
    @DisplayName("define(...).create()")
    class Create {

        @Test
        @DisplayName("POSTs a connection to /connections with the built request body")
        void createPostsConnection() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/connection_response.json"))));

            Connection created = fabric.connections()
                    .define(ConnectionType.EVPL_VC)
                    .name("My-EVPL-Connection")
                    .bandwidth(100)
                    .aSideAccessPointPort("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee",
                            LinkProtocol.dot1q().vlanTag(1001).create())
                    .zSideAccessPointServiceProfile("20d32a80-0d61-4333-bc03-4b2d446794a0",
                            LinkProtocol.dot1q().vlanTag(1002).create())
                    .notification("ops@example.com")
                    .create();

            assertNotNull(created);
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", created.getUuid());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("EVPL_VC")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("My-EVPL-Connection")))
                    .withRequestBody(matchingJsonPath("$.bandwidth", equalTo("100")))
                    .withRequestBody(matchingJsonPath("$.aSide.accessPoint.port.uuid",
                            equalTo("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee")))
                    .withRequestBody(matchingJsonPath("$.aSide.accessPoint.linkProtocol.vlanTag",
                            equalTo("1001")))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.profile.uuid",
                            equalTo("20d32a80-0d61-4333-bc03-4b2d446794a0"))));
        }
    }

    @Nested
    @DisplayName("startBatch().createBatch()")
    class Batch {

        @Test
        @DisplayName("POSTs an array of connections to /connections/bulk")
        void createBatchPostsToBulk() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/bulk"))
                    .willReturn(okJson(loadFixture("/json/fabric/connections_bulk_response.json"))));

            var operator = fabric.connections();
            var batch = operator.startBatch();
            batch.addConnection(operator.define(ConnectionType.EVPL_VC)
                    .name("Batch-Connection-1")
                    .bandwidth(100)
                    .aSideAccessPointPort("port-a-1", LinkProtocol.dot1q().vlanTag(101).create())
                    .zSideAccessPointServiceProfile("sp-1", LinkProtocol.dot1q().vlanTag(102).create()));
            batch.addConnection(operator.define(ConnectionType.EVPL_VC)
                    .name("Batch-Connection-2")
                    .bandwidth(200)
                    .aSideAccessPointPort("port-a-2", LinkProtocol.dot1q().vlanTag(201).create())
                    .zSideAccessPointServiceProfile("sp-2", LinkProtocol.dot1q().vlanTag(202).create()));

            List<Connection> created = batch.createBatch();

            assertNotNull(created);
            assertEquals(2, created.size());
            assertEquals("Batch-Connection-1", created.get(0).getName());
            assertEquals("Batch-Connection-2", created.get(1).getName());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections/bulk"))
                    .withRequestBody(matchingJsonPath("$[0].name", equalTo("Batch-Connection-1")))
                    .withRequestBody(matchingJsonPath("$[0].bandwidth", equalTo("100")))
                    .withRequestBody(matchingJsonPath("$[1].name", equalTo("Batch-Connection-2")))
                    .withRequestBody(matchingJsonPath("$[1].bandwidth", equalTo("200"))));
        }
    }

    @Nested
    @DisplayName("Route Aggregation attach / detach")
    class RouteAggregationActions {

        @Test
        @DisplayName("attachRouteAggregation PUTs to /connections/{id}/routeAggregations/{raId}")
        void attach() {
            wireMock.stubFor(put(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations/695a8471-6595-4ac6-a2f4-b3d96ed3a59d"))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_route_aggregation_attachment_response.json"))));

            RouteAggregationAttachment attachment = fabric.connections().attachRouteAggregation(
                    "81331c52-04c0-4656-a4a7-18c52669348f", "695a8471-6595-4ac6-a2f4-b3d96ed3a59d");

            assertNotNull(attachment);
            assertEquals("695a8471-6595-4ac6-a2f4-b3d96ed3a59d", attachment.getUuid());
            wireMock.verify(putRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations/695a8471-6595-4ac6-a2f4-b3d96ed3a59d")));
        }

        @Test
        @DisplayName("detachRouteAggregation DELETEs to /connections/{id}/routeAggregations/{raId}")
        void detach() {
            wireMock.stubFor(delete(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations/695a8471-6595-4ac6-a2f4-b3d96ed3a59d"))
                    .willReturn(noContent()));

            Boolean result = fabric.connections().detachRouteAggregation(
                    "81331c52-04c0-4656-a4a7-18c52669348f", "695a8471-6595-4ac6-a2f4-b3d96ed3a59d");

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations/695a8471-6595-4ac6-a2f4-b3d96ed3a59d")));
        }
    }

    @Nested
    @DisplayName("Route Filter attach / detach")
    class RouteFilterActions {

        @Test
        @DisplayName("attachRouteFilter PUTs the direction body to /connections/{id}/routeFilters/{rfId}")
        void attach() {
            wireMock.stubFor(put(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters/695a8471-6595-4ac6-a2f4-b3d96ed3a59d"))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_route_filter_attachment_response.json"))));

            RouteFilterAttachment attachment = fabric.connections().attachRouteFilter(
                    "81331c52-04c0-4656-a4a7-18c52669348f", "695a8471-6595-4ac6-a2f4-b3d96ed3a59d",
                    Direction.INBOUND);

            assertNotNull(attachment);
            assertEquals("695a8471-6595-4ac6-a2f4-b3d96ed3a59d", attachment.getUuid());
            wireMock.verify(putRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters/695a8471-6595-4ac6-a2f4-b3d96ed3a59d"))
                    .withRequestBody(equalToJson("{\"direction\":\"INBOUND\"}", true, true)));
        }

        @Test
        @DisplayName("detachRouteFilter DELETEs to /connections/{id}/routeFilters/{rfId}")
        void detach() {
            wireMock.stubFor(delete(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters/695a8471-6595-4ac6-a2f4-b3d96ed3a59d"))
                    .willReturn(noContent()));

            Boolean result = fabric.connections().detachRouteFilter(
                    "81331c52-04c0-4656-a4a7-18c52669348f", "695a8471-6595-4ac6-a2f4-b3d96ed3a59d");

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters/695a8471-6595-4ac6-a2f4-b3d96ed3a59d")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("403 throws EquinixAuthorizationException")
        void forbidden() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    403, "[{\"errorCode\":\"ERR-403\",\"errorMessage\":\"Forbidden\"}]");

            assertThrows(EquinixAuthorizationException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("429 throws EquinixRateLimitException")
        void rateLimited() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    429, "[{\"errorCode\":\"ERR-429\",\"errorMessage\":\"Rate limit exceeded\"}]");

            assertThrows(EquinixRateLimitException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }
    }
}
