package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.enums.PriceType;
import api.equinix.javasdk.fabric.model.Pricing;
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
