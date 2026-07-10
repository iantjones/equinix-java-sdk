package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.ValidateConnectionResult;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.Sort;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.stubErrorInline;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based tests for the additive Tier-A Fabric features: connection validation
 * ({@code POST /fabric/v4/connections/validate}) and metrics search
 * ({@code POST /fabric/v4/metrics/search}). Verifies both the request path and the
 * serialized {@code {filter:{and:[...]}}} request body, plus that responses deserialize
 * into the new models. Also covers the per-asset {@code {$uuid}/metrics} GET endpoints.
 */
class FabricMetricsWireMockTest extends WireMockTestBase {

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

    @Test
    @DisplayName("validate() POSTs {filter:{and:[...]}} and deserializes ConnectionResponse")
    void validateConnections_postsFilterAndDeserializesResponse() {
        String responseBody = "{"
                + "\"additionalInfo\":[{\"key\":\"status\",\"value\":\"AVAILABLE\"}],"
                + "\"data\":[{"
                + "  \"uuid\":\"3a58dd05-f46d-4b1d-a154-2e85c396ea62\","
                + "  \"bandwidth\":1000,"
                + "  \"redundancy\":{\"group\":\"grp-1\",\"priority\":\"PRIMARY\"},"
                + "  \"aSide\":{\"accessPoint\":{\"type\":\"COLO\"}},"
                + "  \"zSide\":{\"accessPoint\":{\"type\":\"SP\"}}"
                + "}]}";

        wireMock.stubFor(post(urlEqualTo("/fabric/v4/connections/validate"))
                .willReturn(okJson(responseBody)));

        FilterPropertyList filter = Filter.filter().and()
                .equals("/zSide/accessPoint/authenticationKey", "auth-key-123");

        List<ValidateConnectionResult> results = fabric.connections().validate(filter);

        assertNotNull(results);
        assertEquals(1, results.size());
        ValidateConnectionResult result = results.get(0);
        assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea62", result.getUuid());
        assertEquals(1000, result.getBandwidth());
        assertNotNull(result.getRedundancy());
        assertEquals("grp-1", result.getRedundancy().getGroup());
        assertNotNull(result.getASide());
        assertNotNull(result.getZSide());

        wireMock.verify(postRequestedFor(urlEqualTo("/fabric/v4/connections/validate"))
                .withRequestBody(matchingJsonPath("$.filter.and[0].property",
                        equalTo("/zSide/accessPoint/authenticationKey")))
                .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]",
                        equalTo("auth-key-123"))));
    }

    @Test
    @DisplayName("metrics().search() POSTs {filter:{and:[...]}} and deserializes Metric data")
    void searchMetrics_postsFilterAndDeserializesResponse() {
        String responseBody = "{"
                + "\"pagination\":{\"offset\":0,\"limit\":20,\"total\":1},"
                + "\"data\":[{"
                + "  \"type\":\"equinix.fabric.connection\","
                + "  \"name\":\"equinix.fabric.connection.bandwidth_tx.usage\","
                + "  \"unit\":\"bps\","
                + "  \"interval\":\"PT1H\","
                + "  \"resource\":{\"uuid\":\"conn-uuid\",\"type\":\"CONNECTION\",\"name\":\"my-conn\"},"
                + "  \"summary\":\"tx usage\","
                + "  \"datapoints\":[{\"startDateTime\":\"2024-01-01T00:00:00Z\",\"endDateTime\":\"2024-01-01T01:00:00Z\",\"value\":12345.6}]"
                + "}]}";

        wireMock.stubFor(post(urlEqualTo("/fabric/v4/metrics/search"))
                .willReturn(okJson(responseBody)));

        FilterPropertyList filter = Filter.filter().and()
                .equals("/name", "equinix.fabric.connection.bandwidth_tx.usage")
                .equals("/subject", "/connections/conn-uuid");

        PaginatedFilteredList<Metric> metrics = fabric.metrics().search(filter);

        assertNotNull(metrics);
        assertEquals(1, metrics.size());
        Metric metric = metrics.get(0);
        assertEquals("equinix.fabric.connection.bandwidth_tx.usage", metric.getName());
        assertEquals("bps", metric.getUnit());
        assertEquals("PT1H", metric.getInterval());
        assertNotNull(metric.getResource());
        assertEquals("conn-uuid", metric.getResource().getUuid());
        assertNotNull(metric.getDatapoints());
        assertEquals(1, metric.getDatapoints().size());
        assertEquals(12345.6, metric.getDatapoints().get(0).getValue());
        assertNotNull(metric.getDatapoints().get(0).getStartDateTime());

        wireMock.verify(postRequestedFor(urlEqualTo("/fabric/v4/metrics/search"))
                .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/name")))
                .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/subject"))));
    }

    @Test
    @DisplayName("metrics().search() with empty filter still POSTs to search")
    void searchMetrics_emptyFilterStillPostsToSearch() {
        wireMock.stubFor(post(urlEqualTo("/fabric/v4/metrics/search"))
                .willReturn(okJson("{\"pagination\":{\"offset\":0,\"limit\":20,\"total\":0},\"data\":[]}")));

        PaginatedFilteredList<Metric> metrics = fabric.metrics().search();

        assertNotNull(metrics);
        assertEquals(0, metrics.size());
        wireMock.verify(postRequestedFor(urlEqualTo("/fabric/v4/metrics/search")));
    }

    @Test
    @DisplayName("connections().getMetrics() GETs {uuid}/metrics and returns the data list")
    void connectionGetMetrics_returnsDataList() {
        String responseBody = "{"
                + "\"pagination\":{\"offset\":0,\"limit\":20,\"total\":1},"
                + "\"data\":[{"
                + "  \"type\":\"equinix.fabric.connection\","
                + "  \"name\":\"equinix.fabric.connection.bandwidth_rx.usage\","
                + "  \"unit\":\"bps\","
                + "  \"datapoints\":[{\"startDateTime\":\"2024-01-01T00:00:00Z\",\"endDateTime\":\"2024-01-01T01:00:00Z\",\"value\":42.0}]"
                + "}]}";

        wireMock.stubFor(get(urlPathMatching("/fabric/v4/connections/.*/metrics"))
                .willReturn(okJson(responseBody)));

        List<Metric> metrics = fabric.connections().getMetrics(
                "conn-uuid", "equinix.fabric.connection.bandwidth_rx.usage", null, null);

        assertNotNull(metrics);
        assertEquals(1, metrics.size());
        assertEquals("equinix.fabric.connection.bandwidth_rx.usage", metrics.get(0).getName());
        assertEquals(42.0, metrics.get(0).getDatapoints().get(0).getValue());

        wireMock.verify(getRequestedFor(urlPathMatching("/fabric/v4/connections/conn-uuid/metrics"))
                .withQueryParam("name", equalTo("equinix.fabric.connection.bandwidth_rx.usage")));
    }

    @Test
    @DisplayName("ports().getMetrics() GETs {uuid}/metrics and returns the data list")
    void portGetMetrics_returnsDataList() {
        String responseBody = "{"
                + "\"pagination\":{\"offset\":0,\"limit\":20,\"total\":1},"
                + "\"data\":[{"
                + "  \"type\":\"equinix.fabric.port\","
                + "  \"name\":\"equinix.fabric.port.bandwidth_rx.usage\","
                + "  \"unit\":\"bps\","
                + "  \"datapoints\":[{\"startDateTime\":\"2024-01-01T00:00:00Z\",\"endDateTime\":\"2024-01-01T01:00:00Z\",\"value\":7.0}]"
                + "}]}";

        wireMock.stubFor(get(urlPathMatching("/fabric/v4/ports/.*/metrics"))
                .willReturn(okJson(responseBody)));

        List<Metric> metrics = fabric.ports().getMetrics("port-uuid", null, null, null);

        assertNotNull(metrics);
        assertEquals(1, metrics.size());
        assertEquals("equinix.fabric.port.bandwidth_rx.usage", metrics.get(0).getName());

        wireMock.verify(getRequestedFor(urlPathMatching("/fabric/v4/ports/port-uuid/metrics")));
    }

    @Test
    @DisplayName("metrics().getMetricsByName() GETs /metrics with name+value query params")
    void getMetricsByName_returnsDataList() {
        String responseBody = "{"
                + "\"pagination\":{\"offset\":0,\"limit\":20,\"total\":1},"
                + "\"data\":[{"
                + "  \"type\":\"equinix.fabric.metro\","
                + "  \"name\":\"equinix.fabric.metro.sv_dc.latency\","
                + "  \"unit\":\"ms\","
                + "  \"datapoints\":[{\"startDateTime\":\"2024-01-01T00:00:00Z\",\"endDateTime\":\"2024-01-01T01:00:00Z\",\"value\":3.14}]"
                + "}]}";

        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/metrics"))
                .willReturn(okJson(responseBody)));

        List<Metric> metrics = fabric.metrics().getMetricsByName(
                "equinix.fabric.metro.*.latency", "last");

        assertNotNull(metrics);
        assertEquals(1, metrics.size());
        assertEquals("equinix.fabric.metro.sv_dc.latency", metrics.get(0).getName());
        assertEquals(3.14, metrics.get(0).getDatapoints().get(0).getValue());

        // The by-name metrics endpoint defines only name/value/offset/limit query params; it does
        // not support date-range filtering, so fromDateTime/toDateTime must not be sent.
        wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/metrics"))
                .withQueryParam("name", equalTo("equinix.fabric.metro.*.latency"))
                .withQueryParam("value", equalTo("last"))
                .withQueryParam("fromDateTime", absent())
                .withQueryParam("toDateTime", absent()));
    }

    @Test
    @DisplayName("metrics().getMetricsByAssetId() GETs /{asset}/{assetId}/metrics with name query param")
    void getMetricsByAssetId_returnsDataList() {
        String responseBody = "{"
                + "\"pagination\":{\"offset\":0,\"limit\":20,\"total\":1},"
                + "\"data\":[{"
                + "  \"type\":\"equinix.fabric.port\","
                + "  \"name\":\"equinix.fabric.port.bandwidth_rx.usage\","
                + "  \"unit\":\"bps\","
                + "  \"datapoints\":[{\"startDateTime\":\"2024-01-01T00:00:00Z\",\"endDateTime\":\"2024-01-01T01:00:00Z\",\"value\":99.0}]"
                + "}]}";

        wireMock.stubFor(get(urlPathMatching("/fabric/v4/ports/.*/metrics"))
                .willReturn(okJson(responseBody)));

        List<Metric> metrics = fabric.metrics().getMetricsByAssetId(
                "ports", "asset-uuid", "equinix.fabric.port.bandwidth_rx.usage", null, null);

        assertNotNull(metrics);
        assertEquals(1, metrics.size());
        assertEquals("equinix.fabric.port.bandwidth_rx.usage", metrics.get(0).getName());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/ports/asset-uuid/metrics"))
                .withQueryParam("name", equalTo("equinix.fabric.port.bandwidth_rx.usage")));
    }

    /**
     * Coverage for the sort-carrying search variants of the Metrics resource. The Metrics
     * resource is defined with {@code rootUri: "metrics"} and {@code SearchMetrics}
     * {@code requestUri: "search"} (no {@code overrideRootUri}), so every variant targets
     * {@code POST /fabric/v4/metrics/search}. These assert the serialized {@code sort} array
     * carried in the POST body, complementing the existing {@code search()} / {@code search(filter)}
     * cases above.
     */
    @Nested
    @DisplayName("metrics().search(filter, sort) — sort-carrying variants")
    class SearchWithSort {

        private static final String SEARCH_URL = "/fabric/v4/metrics/search";

        private static final String SEARCH_BODY = "{"
                + "\"pagination\":{\"offset\":0,\"limit\":20,\"total\":1},"
                + "\"data\":[{"
                + "  \"type\":\"equinix.fabric.connection\","
                + "  \"name\":\"equinix.fabric.connection.bandwidth_tx.usage\","
                + "  \"unit\":\"bps\","
                + "  \"interval\":\"PT1H\","
                + "  \"resource\":{\"uuid\":\"conn-uuid\",\"type\":\"CONNECTION\",\"name\":\"my-conn\"},"
                + "  \"datapoints\":[{\"startDateTime\":\"2024-01-01T00:00:00Z\",\"endDateTime\":\"2024-01-01T01:00:00Z\",\"value\":12345.6}]"
                + "}]}";

        @Test
        @DisplayName("search(filter, sort) POSTs both the filter and sort arrays in the body")
        void searchWithFilterAndSort_postsBothFilterAndSort() {
            wireMock.stubFor(post(urlPathEqualTo(SEARCH_URL)).willReturn(okJson(SEARCH_BODY)));

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/name", "equinix.fabric.connection.bandwidth_tx.usage")
                    .equals("/subject", "/connections/conn-uuid");
            SortPropertyList sort = Sort.sort().desc("/name");

            PaginatedFilteredList<Metric> metrics = fabric.metrics().search(filter, sort);

            assertNotNull(metrics);
            assertEquals(1, metrics.size());
            assertEquals("equinix.fabric.connection.bandwidth_tx.usage", metrics.get(0).getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]",
                            equalTo("equinix.fabric.connection.bandwidth_tx.usage")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/subject")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(sort) POSTs the sort array in the body without any filter")
        void searchWithSortOnly_postsSortArray() {
            wireMock.stubFor(post(urlPathEqualTo(SEARCH_URL)).willReturn(okJson(SEARCH_BODY)));

            SortPropertyList sort = Sort.sort().asc("/name");

            // No search(sort)-only overload on Metrics; the sort-only shape is expressed as
            // an empty filter plus the sort, which still serializes the sort array in the body.
            PaginatedFilteredList<Metric> metrics =
                    fabric.metrics().search(Filter.filter().empty(), sort);

            assertNotNull(metrics);
            assertEquals(1, metrics.size());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 on /metrics/search throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/metrics/search",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.metrics().search(Filter.filter().empty()));
        }

        @Test
        @DisplayName("500 on /metrics/search throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/metrics/search",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.metrics().search(Filter.filter().empty()));
        }
    }
}
