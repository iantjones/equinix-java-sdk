package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.ValidateConnectionResult;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.MetricQuery;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
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

    private static final String PORT = "8ac8ecfe-c7cb-4def-9f77-5ce93c19d83d";

    @Test
    @DisplayName("search(MetricQuery) POSTs subject, names and range as in the spec example and reads the response example")
    void searchMetrics_specExampleShape() {
        stubPaginatedPost(wireMock, "/fabric/v4/metrics/search", "/json/fabric/metrics_search_response.json");

        PaginatedFilteredList<Metric> metrics = fabric.metrics().search(MetricQuery.forPort(PORT)
                .names("equinix.fabric.port.bandwidth_rx.usage", "equinix.fabric.port.bandwidth_tx.usage")
                .between(LocalDateTime.of(2025, 9, 1, 19, 19, 50), LocalDateTime.of(2025, 9, 3, 19, 19, 50)));

        assertEquals(2, metrics.size());
        assertEquals("equinix.fabric.port.bandwidth_rx.usage", metrics.get(0).getName());
        assertEquals("bit/s", metrics.get(0).getUnit());
        wireMock.verify(postRequestedFor(urlEqualTo("/fabric/v4/metrics/search"))
                .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/subject")))
                .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("=")))
                .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("/fabric/v4/ports/" + PORT)))
                .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/name")))
                .withRequestBody(matchingJsonPath("$.filter.and[1].operator", equalTo("IN")))
                .withRequestBody(matchingJsonPath("$.filter.and[1].values[1]", equalTo("equinix.fabric.port.bandwidth_tx.usage")))
                .withRequestBody(matchingJsonPath("$.filter.and[2].property", equalTo("/dataPoints/endDateTime")))
                .withRequestBody(matchingJsonPath("$.filter.and[2].operator", equalTo("BETWEEN")))
                .withRequestBody(matchingJsonPath("$.filter.and[2].values[0]", equalTo("2025-09-01T19:19:50Z"))));
    }

    @Test
    @DisplayName("forConnection() loads every page of the connection's metrics through /metrics/search")
    void forConnection_usesSearch() {
        stubPaginatedPost(wireMock, "/fabric/v4/metrics/search", "/json/fabric/metrics_search_response.json");

        List<Metric> metrics = fabric.metrics().forConnection("conn-uuid",
                LocalDateTime.of(2026, 11, 10, 0, 0), LocalDateTime.of(2026, 11, 11, 0, 0),
                "equinix.fabric.connection.bandwidth_tx.usage");

        assertEquals(2, metrics.size());
        wireMock.verify(postRequestedFor(urlEqualTo("/fabric/v4/metrics/search"))
                .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("/fabric/v4/connections/conn-uuid"))));
        wireMock.verify(0, getRequestedFor(urlPathMatching("/fabric/v4/connections/.*/metrics")));
    }

    @Test
    @DisplayName("forMetro() targets /fabric/v4/metros/{code} (live-verified subject, 2026-11-11)")
    void forMetro_subject() {
        stubPaginatedPost(wireMock, "/fabric/v4/metrics/search", "/json/fabric/metrics_search_response.json");

        fabric.metrics().forMetro("SV", LocalDateTime.of(2026, 11, 10, 0, 0), LocalDateTime.of(2026, 11, 11, 0, 0),
                "equinix.fabric.metro.sv_dc.latency");

        wireMock.verify(postRequestedFor(urlEqualTo("/fabric/v4/metrics/search"))
                .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("/fabric/v4/metros/SV"))));
    }

    @Test
    @DisplayName("invalid searches are rejected before any request: no name, no subject, two subjects")
    void invalidSearchesRejected() {
        assertThrows(IllegalStateException.class, () -> fabric.metrics().search(MetricQuery.forPort(PORT)));
        assertThrows(IllegalArgumentException.class, () -> fabric.metrics().search(
                Filter.filter().and().equals("/name", "equinix.fabric.port.bandwidth_rx.usage"), null));
        assertThrows(IllegalArgumentException.class, () -> fabric.metrics().search(
                Filter.filter().and().equals("/subject", "/fabric/v4/ports/" + PORT), null));
        assertThrows(IllegalArgumentException.class, () -> fabric.metrics().search(
                Filter.filter().and().in("/subject", List.of((Object) "/fabric/v4/ports/a", "/fabric/v4/ports/b"))
                        .equals("/name", "equinix.fabric.port.bandwidth_rx.usage"), null));
        assertThrows(IllegalArgumentException.class, () -> fabric.metrics().forPort(PORT,
                LocalDateTime.of(2026, 11, 11, 0, 0), LocalDateTime.of(2026, 11, 10, 0, 0), "n"));
        wireMock.verify(0, postRequestedFor(urlEqualTo("/fabric/v4/metrics/search")));
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

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 on /metrics/search throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/metrics/search",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.metrics().search(MetricQuery.forPort(PORT).names("n")));
        }

        @Test
        @DisplayName("500 on /metrics/search throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/metrics/search",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.metrics().search(MetricQuery.forPort(PORT).names("n")));
        }
    }
}
