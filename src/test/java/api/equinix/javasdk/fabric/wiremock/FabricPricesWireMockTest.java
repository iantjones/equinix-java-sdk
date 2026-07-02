package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.enums.PriceCategory;
import api.equinix.javasdk.fabric.enums.PriceType;
import api.equinix.javasdk.fabric.enums.PrecisionTimePackageCode;
import api.equinix.javasdk.fabric.enums.PrecisionTimeType;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.implementation.TimeServicePrice;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
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
        @DisplayName("deserializes termLength, catgory, router and timeService price shapes")
        void deserializesFidelityFields() {
            stubPaginatedPost(wireMock, "/fabric/v4/prices/search",
                    "/json/fabric/paginated_prices_fidelity.json");

            PaginatedFilteredList<Pricing> prices = fabric.prices().list(null);

            assertNotNull(prices);
            assertEquals(3, prices.size());

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
