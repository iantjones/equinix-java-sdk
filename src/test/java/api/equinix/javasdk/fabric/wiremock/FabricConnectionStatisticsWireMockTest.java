package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.fabric.model.ConnectionStatistic;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.TimeZone;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Connection statistics
 * (fabric.connections().getStatistics(...)).
 */
class FabricConnectionStatisticsWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    static final LocalDateTime START = LocalDateTime.of(2026, 6, 1, 0, 0);
    static final LocalDateTime END = LocalDateTime.of(2026, 6, 2, 0, 0);
    static final String UUID = "c3d4e5f6-a7b8-9012-cdef-234567890abc";

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
    @DisplayName("getStatistics() returns connection statistics with bandwidth utilization")
    void returnsStatistics() {
        stubSingleton(wireMock, "/fabric/v4/connections/.*/stats",
                "/json/fabric/connection_statistic_response.json");

        ConnectionStatistic stats = fabric.connections().getStatistics(UUID, START, END);

        assertNotNull(stats);
        assertEquals(UUID, stats.getUuid());
        assertEquals(ConnectionType.EVPL_VC, stats.getType());
        assertEquals("test-connection-stats", stats.getName());
        assertNotNull(stats.getStats());
        assertEquals(Side.A_Side, stats.getStats().getViewPoint());
        assertNotNull(stats.getStats().getBandwidthUtilization());
        assertEquals(950.5f, stats.getStats().getBandwidthUtilization().getInbound().getMax());
    }

    @Test
    @DisplayName("getStatistics() sends startDateTime, endDateTime and viewPoint query params")
    void sendsQueryParams() {
        stubSingleton(wireMock, "/fabric/v4/connections/.*/stats",
                "/json/fabric/connection_statistic_response.json");

        fabric.connections().getStatistics(UUID, START, END, Side.Z_Side);

        wireMock.verify(getRequestedFor(urlPathMatching("/fabric/v4/connections/.*/stats"))
                .withQueryParam("viewPoint", equalTo("zSide"))
                .withQueryParam("startDateTime", matching(".+"))
                .withQueryParam("endDateTime", matching(".+")));
    }

    @Test
    @DisplayName("getStatistics() sends LocalDateTime inputs verbatim as UTC (no JVM-zone shift)")
    void sendsDateTimesVerbatimRegardlessOfJvmZone() {
        // SDK-wide UTC policy regression: LocalDateTime inputs are UTC wall clock — the same
        // convention as every timestamp the SDK deserializes — so the query must carry the
        // input digits verbatim with a literal 'Z'. A briefly-shipped variant converted
        // systemDefault -> UTC, shifting every time window by the host's UTC offset.
        stubSingleton(wireMock, "/fabric/v4/connections/.*/stats",
                "/json/fabric/connection_statistic_response.json");

        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT-5")); // fixed non-UTC zone, no DST
            fabric.connections().getStatistics(UUID, START, END, Side.Z_Side);
        }
        finally {
            TimeZone.setDefault(original);
        }

        wireMock.verify(getRequestedFor(urlPathMatching("/fabric/v4/connections/.*/stats"))
                .withQueryParam("startDateTime", equalTo("2026-06-01T00:00:00Z"))
                .withQueryParam("endDateTime", equalTo("2026-06-02T00:00:00Z")));
    }

    @Test
    @DisplayName("500 throws EquinixServerException")
    void serverError() {
        stubErrorInline(wireMock, "/fabric/v4/connections/.*/stats",
                500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

        assertThrows(EquinixServerException.class,
                () -> fabric.connections().getStatistics(UUID, START, END));
    }
}
