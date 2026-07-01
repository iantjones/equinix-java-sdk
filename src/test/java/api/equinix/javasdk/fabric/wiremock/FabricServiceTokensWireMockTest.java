package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.AccessPointType;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.ServiceTokenAction;
import api.equinix.javasdk.fabric.enums.ServiceTokenType;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.fabric.model.ServiceToken;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Service Tokens.
 */
class FabricServiceTokensWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns service token for valid UUID")
        void returnsServiceToken() {
            stubSingleton(wireMock, "/fabric/v4/serviceTokens/.*",
                    "/json/fabric/service_token_response.json");

            ServiceToken token = fabric.serviceTokens().getByUuid("ab7f685-41b0-1b07-6de0-3a7c54b08b8f");
            assertNotNull(token);
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/serviceTokens/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Service token not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.serviceTokens().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the configured A-side service token and returns the created object")
        void createsServiceToken() {
            // POST returns the created object body directly.
            stubCreate(wireMock, "/fabric/v4/serviceTokens",
                    "/json/fabric/service_token_response.json");

            ServiceToken created = fabric.serviceTokens().define(Side.A_Side)
                    .ofType(ServiceTokenType.VC_TOKEN)
                    .withExpiry(30)
                    .forConnectionType(ConnectionType.EVPL_VC)
                    .forAccessPointType(AccessPointType.COLO)
                    .onPortUuid("c791f8cb-5cc9-4a9f-8b8a-1f2e3d4c5b6a")
                    .usingProtocolDot1q(1001)
                    .create();

            assertNotNull(created);

            // issuerSide A_Side routes the access point selector into connection.aSide.
            wireMock.verify(postRequestedFor(urlPathMatching("/fabric/v4/serviceTokens"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("VC_TOKEN")))
                    .withRequestBody(matchingJsonPath("$.expiry", equalTo("30")))
                    .withRequestBody(matchingJsonPath("$.connection.type", equalTo("EVPL_VC")))
                    .withRequestBody(matchingJsonPath("$.connection.issuerSide", equalTo("A_Side")))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.aSide.accessPointSelectors[0].type", equalTo("COLO")))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.aSide.accessPointSelectors[0].port.uuid",
                            equalTo("c791f8cb-5cc9-4a9f-8b8a-1f2e3d4c5b6a")))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.aSide.accessPointSelectors[0].linkProtocol.type", equalTo("DOT1Q")))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.aSide.accessPointSelectors[0].linkProtocol.vlanTag", equalTo("1001"))));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes a JSON Patch array of the changed fields")
        void savePatchesFields() {
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/serviceTokens/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/service_token_response.json"))));

            ServiceToken updated = fabric.serviceTokens()
                    .update("ab7f685-41b0-1b07-6de0-3a7c54b08b8f")
                    .name("Renamed-Token")
                    .description("Updated description")
                    .expiry(45)
                    .save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(
                    urlPathEqualTo("/fabric/v4/serviceTokens/ab7f685-41b0-1b07-6de0-3a7c54b08b8f"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Token\"},"
                            + "{\"op\":\"replace\",\"path\":\"/description\",\"value\":\"Updated description\"},"
                            + "{\"op\":\"replace\",\"path\":\"/expiry\",\"value\":45}]", true, true)));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            assertThrows(IllegalStateException.class,
                    () -> fabric.serviceTokens().update("ab7f685-41b0-1b07-6de0-3a7c54b08b8f").save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/serviceTokens/.*")));
        }
    }

    @Nested
    @DisplayName("createAction()")
    class CreateAction {

        @Test
        @DisplayName("POSTs the action type to /{uuid}/actions and returns the token")
        void postsAction() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/serviceTokens/.*/actions"))
                    .willReturn(okJson(loadFixture("/json/fabric/service_token_response.json"))));

            ServiceToken result = fabric.serviceTokens().createAction(
                    "ab7f685-41b0-1b07-6de0-3a7c54b08b8f",
                    ServiceTokenAction.RESEND_EMAIL_NOTIFICATION);

            assertNotNull(result);
            wireMock.verify(postRequestedFor(
                    urlPathEqualTo("/fabric/v4/serviceTokens/ab7f685-41b0-1b07-6de0-3a7c54b08b8f/actions"))
                    .withRequestBody(equalToJson("{\"type\":\"RESEND_EMAIL_NOTIFICATION\"}", true, true)));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/serviceTokens/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.serviceTokens().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("429 throws EquinixRateLimitException")
        void rateLimited() {
            stubErrorInline(wireMock, "/fabric/v4/serviceTokens/.*",
                    429, "[{\"errorCode\":\"ERR-429\",\"errorMessage\":\"Rate limit exceeded\"}]");

            assertThrows(EquinixRateLimitException.class,
                    () -> fabric.serviceTokens().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/serviceTokens/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.serviceTokens().getByUuid("test-uuid"));
        }
    }
}
