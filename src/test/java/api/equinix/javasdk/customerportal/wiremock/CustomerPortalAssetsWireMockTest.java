package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.enums.AssetProductType;
import api.equinix.javasdk.customerportal.enums.AssetStatus;
import api.equinix.javasdk.customerportal.model.Asset;
import api.equinix.javasdk.customerportal.model.json.creators.AssetSearchDateRange;
import api.equinix.javasdk.customerportal.model.json.creators.AssetSearchFilter;
import api.equinix.javasdk.customerportal.model.json.creators.AssetSearchRequest;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal Assets (assets v1) client.
 *
 * <p>Covers the two read ops declared in {@code apiParams_CustomerPortal.json} under
 * {@code Assets} (rootUri {@code assets}, defaultVersion 1, default format
 * {@code v{version}/{rootUri}/{requestUri}}):</p>
 * <ul>
 *   <li>{@code search(AssetSearchRequest)} &rarr; {@code POST /v1/assets/search}
 *       (the typed {@code filter} is serialized in the body; {@code q}/{@code exactMatch}/{@code sorts}
 *       are carried as query parameters)</li>
 *   <li>{@code getByUuid(assetId)} &rarr; {@code GET /v1/assets/{assetId}}</li>
 * </ul>
 */
class CustomerPortalAssetsWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

    private static final String SEARCH_PATH = "/v1/assets/search";
    private static final String GET_PATH_REGEX = "/v1/assets/[^/]+";

    @BeforeAll
    static void setUp() {
        customerPortal = new CustomerPortal(testCredentials());
        redirectToWireMock(customerPortal);
        customerPortal.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (customerPortal != null) customerPortal.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("POSTs the typed filter body to /v1/assets/search and returns the content records")
        void returnsContent() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/customerportal/paginated_assets.json");

            AssetSearchFilter filter = new AssetSearchFilter()
                    .ibxs(List.of("SV5", "DC11"))
                    .cages(List.of("SV5:01:000ABC"))
                    .productTypes(List.of(AssetProductType.CROSS_CONNECT, AssetProductType.CABINET))
                    .dateRange(new AssetSearchDateRange(
                            "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z"));
            AssetSearchRequest request = new AssetSearchRequest(filter);

            PaginatedList<Asset> results = customerPortal.assets().search(request);

            assertNotNull(results);
            assertEquals(2, results.size());
            Asset first = results.get(0);
            assertEquals("AST-100045", first.getAssetNumber());
            assertEquals("SN-998877", first.getSerialNumber());
            assertEquals("CROSS_CONNECT", first.getProductName());
            assertEquals("SV5", first.getIbx());
            assertEquals("SV5:01:000ABC", first.getCage());
            assertEquals(AssetStatus.ACTIVE, first.getStatus());
            assertEquals("Media Type", first.getProductDetails().get(0).getKey());
            assertEquals("C-14", first.getAdditionalDetails().getCabinetNumber());
            assertEquals(AssetStatus.INACTIVE, results.get(1).getStatus());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.filter.ibxs[0]", equalTo("SV5")))
                    .withRequestBody(matchingJsonPath("$.filter.ibxs[1]", equalTo("DC11")))
                    .withRequestBody(matchingJsonPath("$.filter.cages[0]", equalTo("SV5:01:000ABC")))
                    .withRequestBody(matchingJsonPath("$.filter.productTypes[0]", equalTo("CROSS_CONNECT")))
                    .withRequestBody(matchingJsonPath("$.filter.productTypes[1]", equalTo("CABINET")))
                    .withRequestBody(matchingJsonPath("$.filter.dateRange.fromDate", equalTo("2025-01-01T00:00:00Z")))
                    .withRequestBody(matchingJsonPath("$.filter.dateRange.toDate", equalTo("2025-12-31T23:59:59Z"))));
        }

        @Test
        @DisplayName("carries q/exactMatch/sorts as query parameters, not in the body")
        void carriesSearchQueryParams() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/customerportal/paginated_assets.json");

            AssetSearchRequest request = new AssetSearchRequest(
                    new AssetSearchFilter().ibxs(List.of("SV5")))
                    .q("AST-100045")
                    .exactMatch(true)
                    .sorts(List.of("installationDate:DESC"));

            customerPortal.assets().search(request);

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("q", equalTo("AST-100045"))
                    .withQueryParam("exactMatch", equalTo("true"))
                    .withQueryParam("sorts", equalTo("installationDate:DESC"))
                    .withRequestBody(matchingJsonPath("$.filter.ibxs[0]", equalTo("SV5")))
                    .withRequestBody(notMatching("(?s).*\"q\".*"))
                    .withRequestBody(notMatching("(?s).*\"exactMatch\".*"))
                    .withRequestBody(notMatching("(?s).*\"sorts\".*")));
        }

        @Test
        @DisplayName("POSTs without query params when only a filter is supplied")
        void noQueryParamsWhenFilterOnly() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/customerportal/paginated_assets.json");

            AssetSearchRequest request = new AssetSearchRequest(
                    new AssetSearchFilter().productTypes(List.of(AssetProductType.POWER)));

            customerPortal.assets().search(request);

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withoutQueryParam("q")
                    .withoutQueryParam("exactMatch")
                    .withoutQueryParam("sorts")
                    .withRequestBody(matchingJsonPath("$.filter.productTypes[0]", equalTo("POWER"))));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, SEARCH_PATH,
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}]");

            AssetSearchRequest request = new AssetSearchRequest(new AssetSearchFilter());

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.assets().search(request));
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("GETs /v1/assets/{assetId} and returns the asset")
        void returnsAsset() {
            stubSingleton(wireMock, GET_PATH_REGEX,
                    "/json/customerportal/asset_response.json");

            Asset asset = customerPortal.assets().getByUuid("AST-100045");

            assertNotNull(asset);
            assertEquals("AST-100045", asset.getAssetNumber());
            assertEquals("SN-998877", asset.getSerialNumber());
            assertEquals("1-204050607", asset.getOrderNumber());
            assertEquals("CROSS_CONNECT", asset.getProductName());
            assertEquals("SV5", asset.getIbx());
            assertEquals("SV5:01:000ABC", asset.getCage());
            assertEquals("128745", asset.getAccountNumber());
            assertEquals("Acme Corp", asset.getAccountName());
            assertEquals(AssetStatus.ACTIVE, asset.getStatus());
            assertEquals(2, asset.getProductDetails().size());
            assertEquals("A-SIDE", asset.getProductDetails().get(0).getTag());
            assertEquals("CIRCUIT-7788", asset.getAdditionalDetails().getCustomerOrCarrierCircuitID());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/assets/AST-100045")));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, GET_PATH_REGEX,
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.assets().getByUuid("AST-100045"));
        }
    }
}
