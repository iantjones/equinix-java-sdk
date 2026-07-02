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
}
