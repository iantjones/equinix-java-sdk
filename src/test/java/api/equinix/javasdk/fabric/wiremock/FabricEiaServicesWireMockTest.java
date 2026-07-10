package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.EiaBillingType;
import api.equinix.javasdk.fabric.enums.EiaRoutingProtocolType;
import api.equinix.javasdk.fabric.enums.EiaServiceType;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.model.EiaService;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.Sort;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.EiaRoutingProtocolRequest;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Equinix Internet Access (EIA) services.
 * Covers define()/create() request-body serialization against the
 * {@code /fabric/v4/internetAccessServices} collection endpoint.
 */
class FabricEiaServicesWireMockTest extends WireMockTestBase {

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
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs a full EIA service body to the collection and returns the created service")
        void createsEiaService() {
            stubCreate(wireMock, "/fabric/v4/internetAccessServices", "/json/fabric/eia_service_response.json");

            EiaService service = fabric.eiaServices().define()
                    .ofType(EiaServiceType.SINGLE_IA)
                    .name("My-EIA-Service")
                    .bandwidth(1000)
                    .bandwidthCommit(500)
                    .withRoutingProtocol(new EiaRoutingProtocolRequest(EiaRoutingProtocolType.DIRECT))
                    .withProject(new Project("proj-abc-123"))
                    .withAccountNumber("123456")
                    .withBillingType(EiaBillingType.FIXED)
                    .purchaseOrderNumber("PO-98765")
                    .create();

            assertNotNull(service);
            assertEquals("f1e2d3c4-b5a6-7890-abcd-ef0123456789", service.getUuid());
            assertEquals("My-EIA-Service", service.getName());
            assertEquals(EiaServiceType.SINGLE_IA, service.getType());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/internetAccessServices"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("SINGLE_IA")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("My-EIA-Service")))
                    .withRequestBody(matchingJsonPath("$.bandwidth", equalTo("1000")))
                    .withRequestBody(matchingJsonPath("$.bandwidthCommit", equalTo("500")))
                    .withRequestBody(matchingJsonPath("$.routingProtocol.type", equalTo("DIRECT")))
                    .withRequestBody(matchingJsonPath("$.project.projectId", equalTo("proj-abc-123")))
                    .withRequestBody(matchingJsonPath("$.account.accountNumber", equalTo("123456")))
                    .withRequestBody(matchingJsonPath("$.billing.type", equalTo("FIXED")))
                    .withRequestBody(matchingJsonPath("$.order.purchaseOrderNumber", equalTo("PO-98765"))));
        }

        @Test
        @DisplayName("omits null optional fields (account/billing/order) from the create body")
        void createsMinimalEiaService() {
            stubCreate(wireMock, "/fabric/v4/internetAccessServices", "/json/fabric/eia_service_response.json");

            EiaService service = fabric.eiaServices().define()
                    .ofType(EiaServiceType.DUAL_IA)
                    .name("Minimal-EIA")
                    .bandwidth(200)
                    .withRoutingProtocol(new EiaRoutingProtocolRequest(EiaRoutingProtocolType.BGP))
                    .withProject(new Project("proj-min-1"))
                    .create();

            assertNotNull(service);

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/internetAccessServices"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("DUAL_IA")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Minimal-EIA")))
                    .withRequestBody(matchingJsonPath("$.routingProtocol.type", equalTo("BGP")))
                    .withRequestBody(matchingJsonPath("$.project.projectId", equalTo("proj-min-1")))
                    .withRequestBody(notMatching("(?s).*\"account\".*"))
                    .withRequestBody(notMatching("(?s).*\"billing\".*"))
                    .withRequestBody(notMatching("(?s).*\"order\".*")));
        }
    }

    @Nested
    @DisplayName("create() error handling")
    class Errors {

        @Test
        @DisplayName("400 throws EquinixServiceException")
        void badRequest() {
            stubErrorInline(wireMock, "/fabric/v4/internetAccessServices",
                    400, "[{\"errorCode\":\"ERR-400\",\"errorMessage\":\"Invalid EIA service request\"}]");

            assertThrows(EquinixServiceException.class,
                    () -> fabric.eiaServices().define()
                            .ofType(EiaServiceType.SINGLE_IA)
                            .name("Bad-EIA")
                            .withRoutingProtocol(new EiaRoutingProtocolRequest(EiaRoutingProtocolType.DIRECT))
                            .withProject(new Project("proj-bad"))
                            .create());
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/internetAccessServices",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.eiaServices().define()
                            .ofType(EiaServiceType.SINGLE_IA)
                            .name("Unauth-EIA")
                            .withRoutingProtocol(new EiaRoutingProtocolRequest(EiaRoutingProtocolType.DIRECT))
                            .withProject(new Project("proj-unauth"))
                            .create());
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("search() POSTs an empty search body to /internetAccessServices/search")
        void searchNoArgs() {
            stubPaginatedPost(wireMock, "/fabric/v4/internetAccessServices/search",
                    "/json/fabric/paginated_eia_services.json");

            PaginatedFilteredList<EiaService> results = fabric.eiaServices().search();

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("f1e2d3c4-b5a6-7890-abcd-ef0123456789", results.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/internetAccessServices/search")));
        }

        @Test
        @DisplayName("search(filter) POSTs the AND/equals filter in the body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, "/fabric/v4/internetAccessServices/search",
                    "/json/fabric/paginated_eia_services.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/name", "My-EIA-Service");

            PaginatedFilteredList<EiaService> results = fabric.eiaServices().search(filter);

            assertNotNull(results);
            assertEquals(2, results.size());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/internetAccessServices/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("=")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("My-EIA-Service"))));
        }

        @Test
        @DisplayName("search(sort) POSTs the sort array in the body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, "/fabric/v4/internetAccessServices/search",
                    "/json/fabric/paginated_eia_services.json");

            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<EiaService> results = fabric.eiaServices().search(sort);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/internetAccessServices/search"))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) POSTs both filter and sort in the body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, "/fabric/v4/internetAccessServices/search",
                    "/json/fabric/paginated_eia_services.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/state", "PROVISIONED");
            SortPropertyList sort = Sort.sort().asc("/name");

            PaginatedFilteredList<EiaService> results = fabric.eiaServices().search(filter, sort);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/internetAccessServices/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/state")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("PROVISIONED")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("GETs /internetAccessServices/{uuid} and returns the service")
        void returnsService() {
            stubSingleton(wireMock, "/fabric/v4/internetAccessServices/.*",
                    "/json/fabric/eia_service_response.json");

            EiaService service = fabric.eiaServices().getByUuid("f1e2d3c4-b5a6-7890-abcd-ef0123456789");

            assertNotNull(service);
            assertEquals("f1e2d3c4-b5a6-7890-abcd-ef0123456789", service.getUuid());
            assertEquals("My-EIA-Service", service.getName());
            assertEquals(EiaServiceType.SINGLE_IA, service.getType());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/internetAccessServices/f1e2d3c4-b5a6-7890-abcd-ef0123456789")));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/internetAccessServices/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"EIA service not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.eiaServices().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        private static final String SERVICE_ID = "f1e2d3c4-b5a6-7890-abcd-ef0123456789";
        private static final String URL = "/fabric/v4/internetAccessServices/" + SERVICE_ID;

        @Test
        @DisplayName("PATCHes an op/path/value array as application/json")
        void savePatchesBandwidth() {
            stubSingleton(wireMock, URL, "/json/fabric/eia_service_response.json");
            wireMock.stubFor(patch(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/eia_service_response.json"))));

            EiaService service = fabric.eiaServices().getByUuid(SERVICE_ID);
            EiaService updated = service.update().bandwidth(2000).bandwidthCommit(1000).save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathEqualTo(URL))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/bandwidth\",\"value\":2000},"
                            + "{\"op\":\"replace\",\"path\":\"/bandwidthCommit\",\"value\":1000}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, URL, "/json/fabric/eia_service_response.json");

            EiaService service = fabric.eiaServices().getByUuid(SERVICE_ID);
            assertThrows(IllegalStateException.class, () -> service.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/internetAccessServices/.*")));
        }
    }

    @Nested
    @DisplayName("Wrapper refresh()")
    class WrapperRefresh {

        private static final String SERVICE_ID = "f1e2d3c4-b5a6-7890-abcd-ef0123456789";
        private static final String URL = "/fabric/v4/internetAccessServices/" + SERVICE_ID;

        @Test
        @DisplayName("re-GETs /internetAccessServices/{uuid} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("eia-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/eia_service_response.json")))
                    .willSetStateTo("renamed"));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("eia-refresh")
                    .whenScenarioStateIs("renamed")
                    .willReturn(okJson(loadFixture("/json/fabric/eia_service_response.json")
                            .replace("My-EIA-Service", "My-EIA-Service-Renamed"))));

            EiaService service = fabric.eiaServices().getByUuid(SERVICE_ID);
            assertEquals("My-EIA-Service", service.getName());

            service.refresh();

            assertEquals("My-EIA-Service-Renamed", service.getName(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Wrapper delete()")
    class WrapperDelete {

        private static final String SERVICE_ID = "f1e2d3c4-b5a6-7890-abcd-ef0123456789";
        private static final String URL = "/fabric/v4/internetAccessServices/" + SERVICE_ID;

        @Test
        @DisplayName("DELETEs /internetAccessServices/{uuid} and returns true")
        void deletesEiaService() {
            stubSingleton(wireMock, URL, "/json/fabric/eia_service_response.json");
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/eia_service_response.json"))));

            EiaService service = fabric.eiaServices().getByUuid(SERVICE_ID);
            Boolean deleted = service.delete();

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Multi-page search paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE1_EIA" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE2_EIA" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() re-POSTs the search with the body's pagination offset advanced to page 2")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/internetAccessServices/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/internetAccessServices/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .willReturn(okJson(PAGE_2)));

            PaginatedFilteredList<EiaService> services = fabric.eiaServices().search();
            assertEquals(1, services.size());
            assertTrue(services.hasNextPage());

            services.loadAll();

            assertEquals(2, services.size());
            assertEquals("PAGE1_EIA", services.get(0).getUuid());
            assertEquals("PAGE2_EIA", services.get(1).getUuid());
            assertFalse(services.hasNextPage());

            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/internetAccessServices/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100"))));
        }
    }
}
