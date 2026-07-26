package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.enums.GatewayPackageCode;
import com.eqixiac.equinix.fabric.enums.MetroConnectDestinationType;
import com.eqixiac.equinix.fabric.enums.MetroConnectPathType;
import com.eqixiac.equinix.fabric.enums.MetroConnectType;
import com.eqixiac.equinix.fabric.enums.PriceCategory;
import com.eqixiac.equinix.fabric.enums.PriceType;
import com.eqixiac.equinix.fabric.enums.PrecisionTimePackageCode;
import com.eqixiac.equinix.fabric.enums.PrecisionTimeType;
import com.eqixiac.equinix.fabric.model.Pricing;
import com.eqixiac.equinix.fabric.model.implementation.MetroConnectPrice;
import com.eqixiac.equinix.fabric.model.implementation.TimeServicePrice;
import org.junit.jupiter.api.*;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Pricing (POST /prices/search).
 */
class FabricPricesWireMockTest extends WireMockTestBase {

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
    @DisplayName("list(filter)")
    class ListPrices {

        @Test
        @DisplayName("returns a paginated, filtered list of prices")
        void returnsPrices() {
            stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices.json");

            // Simplest read: no filter body (PricesImpl forwards null straight through).
            PaginatedFilteredList<Pricing> prices = fabric.prices().list(null);

            assertNotNull(prices);
            assertEquals(1, prices.size());
            Pricing first = prices.get(0);
            assertEquals(PriceType.VIRTUAL_CONNECTION_PRODUCT, first.getType());
            assertEquals("USD", first.getCurrency());
            assertEquals("EVPL_VC_SV_DC_100", first.getCode());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/prices/search")));
        }

        @Test
        @DisplayName("deserializes termLength, catgory, router, timeService and metroConnect price shapes")
        void deserializesFidelityFields() {
            stubPaginatedPost(wireMock, "/fabric/v4/prices/search",
                    "/json/fabric/paginated_prices_fidelity.json");

            PaginatedFilteredList<Pricing> prices = fabric.prices().list(null);

            assertNotNull(prices);
            assertEquals(4, prices.size());

            // termLength + the spec's own "catgory" wire spelling
            Pricing vc = prices.get(0);
            assertEquals(PriceType.VIRTUAL_CONNECTION_PRODUCT, vc.getType());
            assertEquals(Integer.valueOf(24), vc.getTermLength());
            assertEquals(PriceCategory.COUNTRY, vc.getCategory());

            // "router" -> getGateway() (CLOUD_ROUTER_PRODUCT reads as itself, not UNKNOWN)
            Pricing router = prices.get(1);
            assertEquals(PriceType.CLOUD_ROUTER_PRODUCT, router.getType());
            assertEquals(Integer.valueOf(1), router.getTermLength());
            assertNotNull(router.getGateway());
            assertEquals(GatewayPackageCode.ADVANCED, router.getGateway().getGatewayPackage().getCode());
            assertEquals(MetroCode.CH, router.getGateway().getLocation().getMetroCode());

            // "timeService" (PRECISION_TIME_PRODUCT reads as itself, not UNKNOWN)
            Pricing time = prices.get(2);
            assertEquals(PriceType.PRECISION_TIME_PRODUCT, time.getType());
            TimeServicePrice timeService = time.getTimeService();
            assertNotNull(timeService);
            assertEquals(PrecisionTimeType.NTP, timeService.getType());
            assertEquals(PrecisionTimePackageCode.NTP_STANDARD, timeService.getTimePackage().getCode());
            assertEquals("CH", timeService.getConnection().getASide()
                    .getAccessPoint().getLocation().getMetroCode());
            assertEquals("CH3", timeService.getConnection().getASide()
                    .getAccessPoint().getLocation().getIbx());

            // "metroConnect" (METRO_CONNECT_PRODUCT reads as itself, not UNKNOWN)
            Pricing metroConnectRow = prices.get(3);
            assertEquals(PriceType.METRO_CONNECT_PRODUCT, metroConnectRow.getType());
            assertEquals("MC00007.PROD", metroConnectRow.getCode());
            MetroConnectPrice metroConnect = metroConnectRow.getMetroConnect();
            assertNotNull(metroConnect);
            assertEquals(MetroConnectType.OPTICAL_MC, metroConnect.getType());
            assertEquals(Integer.valueOf(1000), metroConnect.getBandwidth());
            assertEquals(MetroConnectPathType.PROTECTED, metroConnect.getPathType());
            assertEquals(MetroConnectDestinationType.COLO, metroConnect.getConnectionDestinationType());
            assertEquals("CH1", metroConnect.getASide().getLocation().getIbxCode());
            assertEquals("CH3", metroConnect.getZSide().getLocation().getIbxCode());
        }
    }

    @Nested
    @DisplayName("Multi-page search paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "type": "VIRTUAL_CONNECTION_PRODUCT", "currency": "USD", "code": "PAGE1_ITEM" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "type": "VIRTUAL_CONNECTION_PRODUCT", "currency": "USD", "code": "PAGE2_ITEM" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() fetches page 2 by advancing the body's pagination offset (regression: ClassCastException on page 2)")
        void loadAllFetchesSecondPage() {
            // Page 1: request body carries pagination.offset = 0.
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/prices/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            // Page 2: the paging pipeline must re-send the SAME body with offset advanced to 100.
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/prices/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .willReturn(okJson(PAGE_2)));

            PaginatedFilteredList<Pricing> prices = fabric.prices().list(null);
            assertEquals(1, prices.size());
            assertTrue(prices.hasNextPage());

            prices.loadAll();

            assertEquals(2, prices.size());
            assertEquals("PAGE1_ITEM", prices.get(0).getCode());
            assertEquals("PAGE2_ITEM", prices.get(1).getCode());
            assertFalse(prices.hasNextPage());

            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/prices/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100"))));
        }

        @Test
        @DisplayName("a failed page fetch rolls the offset back so a retried next() gets the same page")
        void failedPageFetchIsRetriable() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/prices/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            // Page 2 fails once, then succeeds (scenario state machine).
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/prices/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .inScenario("page2-retry").whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(aResponse().withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"boom\"}]"))
                    .willSetStateTo("recovered"));
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/prices/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .inScenario("page2-retry").whenScenarioStateIs("recovered")
                    .willReturn(okJson(PAGE_2)));

            PaginatedFilteredList<Pricing> prices = fabric.prices().list(null);

            assertThrows(EquinixServerException.class, prices::next);
            assertEquals(1, prices.size(), "failed fetch must not append items");

            prices.next(); // retry must re-request offset 100, not skip to offset 200

            assertEquals(2, prices.size());
            assertEquals("PAGE2_ITEM", prices.get(1).getCode());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/prices/search",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.prices().list(null));
        }
    }
}
