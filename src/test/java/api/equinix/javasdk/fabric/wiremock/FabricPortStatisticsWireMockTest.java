package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.fabric.model.PortStatistic;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Port statistics
 * (fabric.ports().getStatistics(...)).
 */
class FabricPortStatisticsWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    static final LocalDateTime START = LocalDateTime.of(2026, 6, 1, 0, 0);
    static final LocalDateTime END = LocalDateTime.of(2026, 6, 2, 0, 0);
    static final String UUID = "p1234567-89ab-cdef-0123-456789abcdef";

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
    @DisplayName("getStatistics() returns the spec Statistics shape (top-level interval, viewPoint and utilization)")
    void returnsStatistics() {
        stubSingleton(wireMock, "/fabric/v4/ports/.*/stats",
                "/json/fabric/port_statistic_response.json");

        PortStatistic stats = fabric.ports().getStatistics(UUID, START, END);

        assertNotNull(stats);
        // The spec's Statistics schema is top-level, not nested under "stats"
        assertEquals(START, stats.getStartDateTime());
        assertEquals(END, stats.getEndDateTime());
        assertEquals(Side.A_Side, stats.getViewPoint());
        assertNotNull(stats.getBandwidthUtilization());
        assertEquals(BandwidthUnit.MBPS, stats.getBandwidthUtilization().getUnit());
        assertEquals("PT1H", stats.getBandwidthUtilization().getMetricInterval());
        assertEquals(8200.0f, stats.getBandwidthUtilization().getInbound().getMax());
        assertEquals(3100.5f, stats.getBandwidthUtilization().getInbound().getMean());
        assertEquals(7600.25f, stats.getBandwidthUtilization().getOutbound().getMax());
        assertEquals(2850.75f, stats.getBandwidthUtilization().getOutbound().getMean());
    }

    @Test
    @DisplayName("getStatistics() sends startDateTime and endDateTime query params")
    void sendsQueryParams() {
        stubSingleton(wireMock, "/fabric/v4/ports/.*/stats",
                "/json/fabric/port_statistic_response.json");

        fabric.ports().getStatistics(UUID, START, END);

        wireMock.verify(getRequestedFor(urlPathMatching("/fabric/v4/ports/.*/stats"))
                .withQueryParam("startDateTime", matching(".+"))
                .withQueryParam("endDateTime", matching(".+")));
    }

    @Test
    @DisplayName("500 throws EquinixServerException")
    void serverError() {
        stubErrorInline(wireMock, "/fabric/v4/ports/.*/stats",
                500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

        assertThrows(EquinixServerException.class,
                () -> fabric.ports().getStatistics(UUID, START, END));
    }

    @Test
    @DisplayName("wrapper refresh() re-GETs the same stats window and updates the wrapper in place")
    void refreshReloadsInPlace() {
        String url = "/fabric/v4/ports/" + UUID + "/stats";
        // The wrapper's refresh() targets /ports/{this.getUuid()}/stats, so the payload must
        // carry the (legacy top-level) uuid — the shipped fixture omits it, hence inline bodies.
        String first = "{"
                + "\"uuid\":\"" + UUID + "\","
                + "\"startDateTime\":\"2026-06-01T00:00:00.000Z\","
                + "\"endDateTime\":\"2026-06-02T00:00:00.000Z\","
                + "\"viewPoint\":\"aSide\","
                + "\"bandwidthUtilization\":{\"unit\":\"Mbps\",\"inbound\":{\"max\":8200.0},\"outbound\":{\"max\":7600.25}}"
                + "}";
        String second = first.replace("\"max\":8200.0", "\"max\":9100.0");

        wireMock.stubFor(get(urlPathEqualTo(url))
                .inScenario("port-stats-refresh")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(okJson(first))
                .willSetStateTo("changed"));
        wireMock.stubFor(get(urlPathEqualTo(url))
                .inScenario("port-stats-refresh")
                .whenScenarioStateIs("changed")
                .willReturn(okJson(second)));

        PortStatistic stats = fabric.ports().getStatistics(UUID, START, END);
        assertEquals(8200.0f, stats.getBandwidthUtilization().getInbound().getMax());

        // refresh() lives on the wrapper only — the PortStatistic interface does not declare it.
        PortStatistic refreshed = stats.refresh();

        assertSame(stats, refreshed, "refresh() returns the same live wrapper");
        assertEquals(9100.0f, stats.getBandwidthUtilization().getInbound().getMax(),
                "refresh() must swap the wrapper's backing state in place");
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(url)));
        wireMock.verify(getRequestedFor(urlPathEqualTo(url))
                .withQueryParam("startDateTime", matching(".+"))
                .withQueryParam("endDateTime", matching(".+")));
    }
}
