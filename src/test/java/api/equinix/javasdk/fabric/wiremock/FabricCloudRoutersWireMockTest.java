package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.CloudRouterCommandType;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.CloudRouterCommand;
import api.equinix.javasdk.fabric.model.RoutingProtocolValidation;
import api.equinix.javasdk.fabric.model.implementation.CloudRouterCommandRequest;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
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
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the cloud router body to the collection and returns the created router")
        void createsCloudRouter() {
            stubCreate(wireMock, "/fabric/v4/routers", "/json/fabric/cloud_router_response.json");

            CloudRouter router = fabric.cloudRouters().define()
                    .name("My-Cloud-Router-Primary")
                    .inMetro("SV")
                    .withPackage("PREMIUM")
                    .create();

            assertNotNull(router);
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", router.getUuid());
            assertEquals("My-Cloud-Router-Primary", router.getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/routers"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("XF_ROUTER")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("My-Cloud-Router-Primary")))
                    .withRequestBody(matchingJsonPath("$.location.metroCode", equalTo("SV")))
                    .withRequestBody(matchingJsonPath("$.package.code", equalTo("PREMIUM"))));
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
    @DisplayName("defineCommand() / create()")
    class DefineCommand {

        @Test
        @DisplayName("POSTs the diagnostic command to the router's commands collection")
        void createsCommand() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/commands"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/cloud_router_command_response.json"))));

            CloudRouterCommand command = fabric.cloudRouters()
                    .defineCommand("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
                    .ofType(CloudRouterCommandType.PING_COMMAND)
                    .name("ping-to-peer")
                    .description("Ping the remote BGP peer")
                    .withRequest(CloudRouterCommandRequest.builder()
                            .destination("192.168.1.1")
                            .sourceConnection("3a58dd05-f46d-4b1d-a154-2e85c396ea85")
                            .count(5)
                            .build())
                    .create();

            assertNotNull(command);
            assertEquals("9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6", command.getUuid());
            assertEquals(CloudRouterCommandType.PING_COMMAND, command.getType());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/commands"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("PING_COMMAND")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("ping-to-peer")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Ping the remote BGP peer")))
                    .withRequestBody(matchingJsonPath("$.request.destination", equalTo("192.168.1.1")))
                    .withRequestBody(matchingJsonPath("$.request.sourceConnection.uuid", equalTo("3a58dd05-f46d-4b1d-a154-2e85c396ea85")))
                    .withRequestBody(matchingJsonPath("$.request.count", equalTo("5"))));
        }
    }

    @Nested
    @DisplayName("deleteCommand()")
    class DeleteCommand {

        @Test
        @DisplayName("DELETEs the command by id and returns true")
        void deletesCommand() {
            // DeleteCloudRouterCommand goes through deleteOne(), which reads the deleted command from
            // the response body, so the stub returns one (a 204 would make deleteOne fail on a null body).
            wireMock.stubFor(delete(urlPathEqualTo(
                    "/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/commands/9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6"))
                    .willReturn(okJson(loadFixture("/json/fabric/cloud_router_command_response.json"))));

            Boolean deleted = fabric.cloudRouters().deleteCommand(
                    "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6");

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(
                    "/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/commands/9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6")));
        }
    }

    @Nested
    @DisplayName("validateRoutingProtocol()")
    class ValidateRoutingProtocol {

        @Test
        @DisplayName("POSTs the filter to the router's validate endpoint and returns the result")
        void validatesRoutingProtocol() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/validate"))
                    .willReturn(okJson("{\"additionalInfo\":[{\"key\":\"status\",\"value\":\"VALID\"}]}")));

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/directIpv4/equinixICLAdvertisedIP", "10.1.1.1")
                    .equals("/connection/uuid", "3a58dd05-f46d-4b1d-a154-2e85c396ea85");

            RoutingProtocolValidation validation = fabric.cloudRouters()
                    .validateRoutingProtocol("a1b2c3d4-e5f6-7890-abcd-ef1234567890", filter);

            assertNotNull(validation);
            assertNotNull(validation.getAdditionalInfo());
            assertEquals(1, validation.getAdditionalInfo().size());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/validate"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "{\"filter\":{\"and\":["
                            + "{\"property\":\"/directIpv4/equinixICLAdvertisedIP\",\"operator\":\"=\",\"values\":[\"10.1.1.1\"]},"
                            + "{\"property\":\"/connection/uuid\",\"operator\":\"=\",\"values\":[\"3a58dd05-f46d-4b1d-a154-2e85c396ea85\"]}"
                            + "]}}", true, true)));
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
