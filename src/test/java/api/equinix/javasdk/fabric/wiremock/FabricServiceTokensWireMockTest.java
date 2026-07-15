package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.AccessPointType;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.ServiceTokenAction;
import api.equinix.javasdk.fabric.enums.ServiceTokenType;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.Sort;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

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
    @DisplayName("list()")
    class List {

        @Test
        @DisplayName("GETs /fabric/v4/serviceTokens and returns a paginated list")
        void listsServiceTokens() {
            stubPaginatedGet(wireMock, "/fabric/v4/serviceTokens",
                    "/json/fabric/paginated_service_tokens.json");

            PaginatedList<ServiceToken> tokens = fabric.serviceTokens().list();

            assertNotNull(tokens);
            assertEquals(2, tokens.size());
            assertEquals("ab7f685-41b0-1b07-6de0-3a7c54b08b8f", tokens.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/serviceTokens")));
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        private static final String SEARCH_URL = "/fabric/v4/serviceTokens/search";

        @Test
        @DisplayName("no-arg search POSTs the default body to /serviceTokens/search and returns a filtered list")
        void searchNoArg() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_service_tokens.json");

            PaginatedFilteredList<ServiceToken> tokens = fabric.serviceTokens().search();

            assertNotNull(tokens);
            assertEquals(2, tokens.size());
            assertEquals("ab7f685-41b0-1b07-6de0-3a7c54b08b8f", tokens.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.pagination")));
        }

        @Test
        @DisplayName("search(filter) carries the filter predicate in the POST body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_service_tokens.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/name", "Token-Primary")
                    .equals("/state", "ACTIVE");

            PaginatedFilteredList<ServiceToken> tokens = fabric.serviceTokens().search(filter);

            assertNotNull(tokens);
            assertEquals(2, tokens.size());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("Token-Primary")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/state")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].values[0]", equalTo("ACTIVE"))));
        }

        @Test
        @DisplayName("search(sort) carries the sort directive in the POST body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_service_tokens.json");

            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<ServiceToken> tokens = fabric.serviceTokens().search(sort);

            assertNotNull(tokens);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) carries both filter and sort in the POST body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_service_tokens.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/state", "ACTIVE");
            SortPropertyList sort = Sort.sort().asc("/name");

            PaginatedFilteredList<ServiceToken> tokens = fabric.serviceTokens().search(filter, sort);

            assertNotNull(tokens);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/state")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("ACTIVE")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
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

        @Test
        @DisplayName("POSTs name/description/expirationDateTime/project and the virtual-device selector")
        void createsVirtualDeviceTokenWithMetadata() {
            stubCreate(wireMock, "/fabric/v4/serviceTokens",
                    "/json/fabric/service_token_response.json");

            ServiceToken created = fabric.serviceTokens().define(Side.Z_Side)
                    .ofType(ServiceTokenType.VC_TOKEN)
                    .withName("Zside-VD-Token")
                    .withDescription("Token targeting a Network Edge device")
                    .withExpirationDateTime(LocalDateTime.of(2025, 12, 31, 23, 59, 59))
                    .inProject("44f4c4f8-2f39-494a-838c-5350c32f0a2e")
                    .forConnectionType(ConnectionType.EVPL_VC)
                    .forAccessPointType(AccessPointType.VD)
                    .onVirtualDeviceUuid("3c7687dc-3b3d-4d22-a0a3-9a64116cae83")
                    .withNetworkInterfaceId(5)
                    .create();

            assertNotNull(created);

            wireMock.verify(postRequestedFor(urlPathMatching("/fabric/v4/serviceTokens"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("VC_TOKEN")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Zside-VD-Token")))
                    .withRequestBody(matchingJsonPath("$.description",
                            equalTo("Token targeting a Network Edge device")))
                    .withRequestBody(matchingJsonPath("$.expirationDateTime",
                            equalTo("2025-12-31T23:59:59Z")))
                    .withRequestBody(matchingJsonPath("$.project.projectId",
                            equalTo("44f4c4f8-2f39-494a-838c-5350c32f0a2e")))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.zSide.accessPointSelectors[0].type", equalTo("VD")))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.zSide.accessPointSelectors[0].virtualDevice.type", equalTo("EDGE")))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.zSide.accessPointSelectors[0].virtualDevice.uuid",
                            equalTo("3c7687dc-3b3d-4d22-a0a3-9a64116cae83")))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.zSide.accessPointSelectors[0].interface.type", equalTo("NETWORK")))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.zSide.accessPointSelectors[0].interface.id", equalTo("5"))));
        }

        @Test
        @DisplayName("POSTs the network access-point selector for NETWORK tokens")
        void createsNetworkTokenSelector() {
            stubCreate(wireMock, "/fabric/v4/serviceTokens",
                    "/json/fabric/service_token_response.json");

            ServiceToken created = fabric.serviceTokens().define(Side.A_Side)
                    .ofType(ServiceTokenType.VC_TOKEN)
                    .withExpiry(30)
                    .forConnectionType(ConnectionType.EVPLAN_VC)
                    .forAccessPointType(AccessPointType.NETWORK)
                    .onNetworkUuid("94a494a4-f4a4-44b4-b4b4-c4c4c4c4c4c4")
                    .create();

            assertNotNull(created);

            wireMock.verify(postRequestedFor(urlPathMatching("/fabric/v4/serviceTokens"))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.aSide.accessPointSelectors[0].type", equalTo("NETWORK")))
                    .withRequestBody(matchingJsonPath(
                            "$.connection.aSide.accessPointSelectors[0].network.uuid",
                            equalTo("94a494a4-f4a4-44b4-b4b4-c4c4c4c4c4c4"))));
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

        @Test
        @DisplayName("dryRun().save() PATCHes with dryRun=true and deserializes the simulated token")
        void dryRunSendsQueryParamAndDeserializesSimulation() {
            // Dry-run responds 200 with the validated/simulated token entity (spec example
            // getServiceToken-DryRun: the token with uuid, state INACTIVE); nothing persisted.
            wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/serviceTokens/ab7f685-41b0-1b07-6de0-3a7c54b08b8f"))
                    .withQueryParam("dryRun", equalTo("true"))
                    .willReturn(okJson(loadFixture("/json/fabric/service_token_response.json")
                            .replace("\"state\": \"ACTIVE\"", "\"state\": \"INACTIVE\"")
                            .replace("Az-Token-Primary", "Renamed-Token"))));

            ServiceToken simulated = fabric.serviceTokens()
                    .update("ab7f685-41b0-1b07-6de0-3a7c54b08b8f")
                    .name("Renamed-Token")
                    .dryRun()
                    .save();

            assertNotNull(simulated);
            assertEquals("ab7f685-41b0-1b07-6de0-3a7c54b08b8f", simulated.getUuid());
            assertEquals("Renamed-Token", simulated.getName());

            // Regression lock: the dry run MUST carry dryRun=true on the wire — if a future
            // change drops the parameter, this "verification" becomes a REAL mutation.
            wireMock.verify(patchRequestedFor(
                    urlPathEqualTo("/fabric/v4/serviceTokens/ab7f685-41b0-1b07-6de0-3a7c54b08b8f"))
                    .withQueryParam("dryRun", equalTo("true"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Token\"}]", true, true)));
        }

        @Test
        @DisplayName("save() without dryRun() sends no dryRun query parameter")
        void defaultSaveOmitsDryRunQueryParam() {
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/serviceTokens/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/service_token_response.json"))));

            fabric.serviceTokens()
                    .update("ab7f685-41b0-1b07-6de0-3a7c54b08b8f")
                    .name("Renamed-Token")
                    .save();

            wireMock.verify(patchRequestedFor(
                    urlPathEqualTo("/fabric/v4/serviceTokens/ab7f685-41b0-1b07-6de0-3a7c54b08b8f"))
                    .withQueryParam("dryRun", absent()));
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

    @Nested
    @DisplayName("Wrapper refresh()")
    class WrapperRefresh {

        private static final String TOKEN_ID = "ab7f685-41b0-1b07-6de0-3a7c54b08b8f";
        private static final String URL = "/fabric/v4/serviceTokens/" + TOKEN_ID;

        @Test
        @DisplayName("re-GETs /serviceTokens/{uuid} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("token-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/service_token_response.json")))
                    .willSetStateTo("renamed"));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("token-refresh")
                    .whenScenarioStateIs("renamed")
                    .willReturn(okJson(loadFixture("/json/fabric/service_token_response.json")
                            .replace("Az-Token-Primary", "Az-Token-Renamed"))));

            ServiceToken token = fabric.serviceTokens().getByUuid(TOKEN_ID);
            assertEquals("Az-Token-Primary", token.getName());

            token.refresh();

            assertEquals("Az-Token-Renamed", token.getName(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Wrapper delete()")
    class WrapperDelete {

        private static final String TOKEN_ID = "ab7f685-41b0-1b07-6de0-3a7c54b08b8f";
        private static final String URL = "/fabric/v4/serviceTokens/" + TOKEN_ID;

        @Test
        @DisplayName("DELETEs /serviceTokens/{uuid} and returns true")
        void deletesServiceToken() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/service_token_response.json"))));
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/service_token_response.json"))));

            ServiceToken token = fabric.serviceTokens().getByUuid(TOKEN_ID);
            Boolean deleted = token.delete();

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Multi-page list paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE1_TOKEN" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE2_TOKEN" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() re-GETs /serviceTokens with the offset query param advanced to page 2")
        void loadAllFetchesSecondPage() {
            // Page 1: catch-all, registered first (WireMock: the later, more specific stub wins).
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/serviceTokens"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/serviceTokens"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<ServiceToken> tokens = fabric.serviceTokens().list();
            assertEquals(1, tokens.size());
            assertTrue(tokens.hasNextPage());

            tokens.loadAll();

            assertEquals(2, tokens.size());
            assertEquals("PAGE1_TOKEN", tokens.get(0).getUuid());
            assertEquals("PAGE2_TOKEN", tokens.get(1).getUuid());
            assertFalse(tokens.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/serviceTokens"))
                    .withQueryParam("offset", equalTo("100")));
        }
    }
}
