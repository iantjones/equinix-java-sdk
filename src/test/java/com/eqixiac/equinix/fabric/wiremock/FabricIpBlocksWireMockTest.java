package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.enums.IpBlockProductType;
import com.eqixiac.equinix.fabric.model.IpBlock;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import org.junit.jupiter.api.*;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric IP blocks.
 * Covers the mutation surface: define().create() (POST /fabric/v4/ipBlocks)
 * and update(...).save() (PATCH /fabric/v4/ipBlocks/{uuid}).
 */
class FabricIpBlocksWireMockTest extends WireMockTestBase {

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
        @DisplayName("POSTs the IP block creation body to /fabric/v4/ipBlocks")
        void createPostsBody() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/ipBlocks"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/ip_block_response.json"))));

            IpBlock created = fabric.ipBlocks().define()
                    .ofType(IpBlockProductType.IPV4_IP_BLOCK)
                    .inMetro("SY")
                    .prefixLength(28)
                    .create();

            assertNotNull(created);
            assertEquals("31fbdb3f-8def-410d-868b-ef920878affb", created.getUuid());
            assertEquals(IpBlockProductType.IPV4_IP_BLOCK, created.getType());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/ipBlocks"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("IPV4_IP_BLOCK")))
                    .withRequestBody(matchingJsonPath("$.location.metroCode", equalTo("SY")))
                    .withRequestBody(matchingJsonPath("$.prefixLength", equalTo("28"))));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes a JSON Patch array as application/json-patch+json")
        void savePatchesPrefixLength() {
            stubSingletonGet();
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/ipBlocks/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/ip_block_response.json"))));

            IpBlock ipBlock = fabric.ipBlocks().getByUuid("31fbdb3f-8def-410d-868b-ef920878affb");
            IpBlock updated = ipBlock.update()
                    .patch(PatchOperation.replace("/prefixLength", 27))
                    .save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/fabric/v4/ipBlocks/31fbdb3f-8def-410d-868b-ef920878affb"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/prefixLength\",\"value\":27}]", true, true)));
        }

        @Test
        @DisplayName("save() with no operations throws and makes no request")
        void emptyUpdateThrows() {
            stubSingletonGet();

            IpBlock ipBlock = fabric.ipBlocks().getByUuid("31fbdb3f-8def-410d-868b-ef920878affb");
            assertThrows(IllegalStateException.class, () -> ipBlock.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/ipBlocks/.*")));
        }

        private void stubSingletonGet() {
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/ipBlocks/31fbdb3f-8def-410d-868b-ef920878affb"))
                    .willReturn(okJson(loadFixture("/json/fabric/ip_block_response.json"))));
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("GETs /fabric/v4/ipBlocks/{uuid} and returns the IP block")
        void returnsIpBlock() {
            stubSingleton(wireMock, "/fabric/v4/ipBlocks/.*",
                    "/json/fabric/ip_block_response.json");

            IpBlock ipBlock = fabric.ipBlocks().getByUuid("31fbdb3f-8def-410d-868b-ef920878affb");

            assertNotNull(ipBlock);
            assertEquals("31fbdb3f-8def-410d-868b-ef920878affb", ipBlock.getUuid());
            assertEquals(IpBlockProductType.IPV4_IP_BLOCK, ipBlock.getType());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/ipBlocks/31fbdb3f-8def-410d-868b-ef920878affb")));
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        private static final String SEARCH_URL = "/fabric/v4/ipBlocks/search";

        @Test
        @DisplayName("no-arg search POSTs the default body to /ipBlocks/search and returns a filtered list")
        void searchNoArg() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_ip_blocks.json");

            PaginatedFilteredList<IpBlock> ipBlocks = fabric.ipBlocks().search();

            assertNotNull(ipBlocks);
            assertEquals(2, ipBlocks.size());
            assertEquals("31fbdb3f-8def-410d-868b-ef920878affb", ipBlocks.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.pagination")));
        }

        @Test
        @DisplayName("search(filter) carries the filter predicate in the POST body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_ip_blocks.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", "IPV4_IP_BLOCK")
                    .equals("/state", "ACTIVE");

            PaginatedFilteredList<IpBlock> ipBlocks = fabric.ipBlocks().search(filter);

            assertNotNull(ipBlocks);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("IPV4_IP_BLOCK")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/state")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].values[0]", equalTo("ACTIVE"))));
        }

        @Test
        @DisplayName("search(sort) carries the sort directive in the POST body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_ip_blocks.json");

            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<IpBlock> ipBlocks = fabric.ipBlocks().search(sort);

            assertNotNull(ipBlocks);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) carries both filter and sort in the POST body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_ip_blocks.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", "IPV4_IP_BLOCK");
            SortPropertyList sort = Sort.sort().asc("/prefixLength");

            PaginatedFilteredList<IpBlock> ipBlocks = fabric.ipBlocks().search(filter, sort);

            assertNotNull(ipBlocks);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("IPV4_IP_BLOCK")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/prefixLength")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("Wrapper refresh()")
    class WrapperRefresh {

        private static final String BLOCK_ID = "31fbdb3f-8def-410d-868b-ef920878affb";
        private static final String URL = "/fabric/v4/ipBlocks/" + BLOCK_ID;

        @Test
        @DisplayName("re-GETs /ipBlocks/{uuid} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("ipblock-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/ip_block_response.json")))
                    .willSetStateTo("resized"));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("ipblock-refresh")
                    .whenScenarioStateIs("resized")
                    .willReturn(okJson(loadFixture("/json/fabric/ip_block_response.json")
                            .replace("\"prefixLength\": 28", "\"prefixLength\": 27"))));

            IpBlock ipBlock = fabric.ipBlocks().getByUuid(BLOCK_ID);
            assertEquals(Integer.valueOf(28), ipBlock.getPrefixLength());

            ipBlock.refresh();

            assertEquals(Integer.valueOf(27), ipBlock.getPrefixLength(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Wrapper delete()")
    class WrapperDelete {

        private static final String BLOCK_ID = "31fbdb3f-8def-410d-868b-ef920878affb";
        private static final String URL = "/fabric/v4/ipBlocks/" + BLOCK_ID;

        @Test
        @DisplayName("DELETEs /ipBlocks/{uuid} and returns true")
        void deletesIpBlock() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/ip_block_response.json"))));
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/ip_block_response.json"))));

            IpBlock ipBlock = fabric.ipBlocks().getByUuid(BLOCK_ID);
            Boolean deleted = ipBlock.delete();

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
                  "data": [ { "uuid": "PAGE1_IPBLOCK" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE2_IPBLOCK" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() re-POSTs the search with the body's pagination offset advanced to page 2")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/ipBlocks/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/ipBlocks/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .willReturn(okJson(PAGE_2)));

            PaginatedFilteredList<IpBlock> ipBlocks = fabric.ipBlocks().search();
            assertEquals(1, ipBlocks.size());
            assertTrue(ipBlocks.hasNextPage());

            ipBlocks.loadAll();

            assertEquals(2, ipBlocks.size());
            assertEquals("PAGE1_IPBLOCK", ipBlocks.get(0).getUuid());
            assertEquals("PAGE2_IPBLOCK", ipBlocks.get(1).getUuid());
            assertFalse(ipBlocks.hasNextPage());

            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/ipBlocks/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100"))));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/ipBlocks/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"IP block not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.ipBlocks().getByUuid("invalid-uuid"));
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/ipBlocks/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.ipBlocks().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/ipBlocks/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.ipBlocks().getByUuid("test-uuid"));
        }
    }
}
