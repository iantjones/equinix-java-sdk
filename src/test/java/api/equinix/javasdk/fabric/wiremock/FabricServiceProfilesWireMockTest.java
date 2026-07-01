package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.enums.ServiceProfileVisibility;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.ServiceProfileAction;
import api.equinix.javasdk.fabric.model.implementation.ServiceMetro;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Service Profiles.
 */
class FabricServiceProfilesWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns service profile for valid UUID")
        void returnsServiceProfile() {
            stubSingleton(wireMock, "/fabric/v4/serviceProfiles/.*",
                    "/json/fabric/service_profile_response.json");

            ServiceProfile profile = fabric.serviceProfiles().getByUuid("f6a7b8c9-d0e1-2345-fabc-567890123def");
            assertNotNull(profile);
            assertEquals("f6a7b8c9-d0e1-2345-fabc-567890123def", profile.getUuid());
            assertEquals("AWS Direct Connect - Production", profile.getName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/serviceProfiles/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Service profile not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.serviceProfiles().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("getMetros()")
    class GetMetros {

        @Test
        @DisplayName("GETs {uuid}/metros and returns the list of service metros")
        void returnsMetros() {
            stubPaginatedGet(wireMock, "/fabric/v4/serviceProfiles/.*/metros",
                    "/json/fabric/service_profile_metros_response.json");

            List<ServiceMetro> metros = fabric.serviceProfiles().getMetros("f6a7b8c9-d0e1-2345-fabc-567890123def");

            assertNotNull(metros);
            assertEquals(2, metros.size());
            assertEquals("Silicon Valley (SV)", metros.get(0).getDisplayName());
            assertEquals(10000, metros.get(0).getVcBandwidthMax());
            assertTrue(metros.get(0).getInTrail());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/serviceProfiles/f6a7b8c9-d0e1-2345-fabc-567890123def/metros")));
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs a new service profile with the configured body")
        void createsServiceProfile() {
            stubCreate(wireMock, "/fabric/v4/serviceProfiles",
                    "/json/fabric/service_profile_response.json");

            ServiceProfile created = fabric.serviceProfiles()
                    .define(ServiceProfileType.L2_PROFILE)
                    .name("AWS Direct Connect - Production")
                    .description("AWS Direct Connect service profile for production workloads with low-latency connectivity")
                    .visibility(ServiceProfileVisibility.PUBLIC)
                    .allowedEmail("partner-onboard@example.com")
                    .tag("cloud")
                    .create();

            assertNotNull(created);
            assertEquals("f6a7b8c9-d0e1-2345-fabc-567890123def", created.getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/serviceProfiles"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("L2_PROFILE")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("AWS Direct Connect - Production")))
                    .withRequestBody(matchingJsonPath("$.visibility", equalTo("PUBLIC")))
                    .withRequestBody(matchingJsonPath("$.allowedEmails[0]", equalTo("partner-onboard@example.com")))
                    .withRequestBody(matchingJsonPath("$.tags[0]", equalTo("cloud"))));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes a JSON Patch array as application/json-patch+json")
        void savePatchesNameAndDescription() {
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/serviceProfiles/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_response.json"))));

            ServiceProfile updated = fabric.serviceProfiles()
                    .update("f6a7b8c9-d0e1-2345-fabc-567890123def")
                    .name("Renamed-Profile")
                    .description("Updated description")
                    .save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/fabric/v4/serviceProfiles/f6a7b8c9-d0e1-2345-fabc-567890123def"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Profile\"},"
                            + "{\"op\":\"replace\",\"path\":\"/description\",\"value\":\"Updated description\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            assertThrows(IllegalStateException.class,
                    () -> fabric.serviceProfiles().update("f6a7b8c9-d0e1-2345-fabc-567890123def").save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/serviceProfiles/.*")));
        }
    }

    @Nested
    @DisplayName("createAction()")
    class CreateAction {

        @Test
        @DisplayName("POSTs {uuid}/actions with the action type and description")
        void postsAction() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/serviceProfiles/.*/actions"))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_action_response.json"))));

            ServiceProfileAction action = fabric.serviceProfiles().createAction(
                    "f6a7b8c9-d0e1-2345-fabc-567890123def",
                    "PROFILE_UPDATE_ACCEPTANCE",
                    "Approved by network team");

            assertNotNull(action);
            assertEquals("PROFILE_UPDATE_ACCEPTANCE", action.getType());

            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/serviceProfiles/f6a7b8c9-d0e1-2345-fabc-567890123def/actions"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("PROFILE_UPDATE_ACCEPTANCE")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Approved by network team"))));
        }

        @Test
        @DisplayName("omits null description from the request body")
        void omitsNullDescription() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/serviceProfiles/.*/actions"))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_action_response.json"))));

            fabric.serviceProfiles().createAction(
                    "f6a7b8c9-d0e1-2345-fabc-567890123def",
                    "PROFILE_UPDATE_REJECTION",
                    null);

            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/serviceProfiles/f6a7b8c9-d0e1-2345-fabc-567890123def/actions"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("PROFILE_UPDATE_REJECTION")))
                    .withRequestBody(notContaining("description")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/serviceProfiles/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.serviceProfiles().getByUuid("test-uuid"));
        }
    }
}
