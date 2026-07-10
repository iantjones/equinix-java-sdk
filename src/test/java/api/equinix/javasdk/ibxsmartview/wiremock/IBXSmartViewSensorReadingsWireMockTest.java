package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.ibxsmartview.enums.SensorUnit;
import api.equinix.javasdk.ibxsmartview.model.SensorReading;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for IBX SmartView environmental sensor readings
 * (smartView.environmentals().list(ibx) / getSensorReading(ibx, sensorId)).
 */
class IBXSmartViewSensorReadingsWireMockTest extends WireMockTestBase {

    static IBXSmartView ibxSmartView;

    static final String IBX = "SV5";

    @BeforeAll
    static void setUp() {
        ibxSmartView = new IBXSmartView(testCredentials());
        redirectToWireMock(ibxSmartView);
        ibxSmartView.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (ibxSmartView != null) ibxSmartView.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("list() returns paginated sensor readings for an IBX")
    void listReturnsReadings() {
        stubPaginatedGet(wireMock, "/smartview/v2/environmental/ibxs/.*/sensors/readings",
                "/json/ibxsmartview/paginated_sensor_readings.json");

        PaginatedList<SensorReading> readings = ibxSmartView.environmentals().list(IBX);

        assertNotNull(readings);
        assertEquals(2, readings.size());
        SensorReading first = readings.get(0);
        assertEquals("SENSOR-SV5-001", first.getSensorId());
        assertEquals(IBX, first.getIbx());
        assertNotNull(first.getTemperature());
        assertEquals(22.5, first.getTemperature().getValue());
        assertEquals(SensorUnit.CELSIUS, first.getTemperature().getUnit());
        assertEquals(SensorUnit.PERCENT, first.getHumidity().getUnit());

        // A FAHRENHEIT temperature unit must be preserved (not silently deserialized to null).
        SensorReading second = readings.get(1);
        assertEquals(73.6, second.getTemperature().getValue());
        assertEquals(SensorUnit.FAHRENHEIT, second.getTemperature().getUnit());
    }

    @Test
    @DisplayName("getSensorReading() returns a single sensor reading")
    void getSingleReading() {
        stubSingleton(wireMock, "/smartview/v2/environmental/ibxs/.*/sensors/.*/readings",
                "/json/ibxsmartview/sensor_reading_response.json");

        SensorReading reading = ibxSmartView.environmentals().getSensorReading(IBX, "SENSOR-SV5-001");

        assertNotNull(reading);
        assertEquals("SENSOR-SV5-001", reading.getSensorId());
        assertEquals("ZONE-A", reading.getZoneId());
        assertEquals(45.0, reading.getHumidity().getValue());
    }

    @Test
    @DisplayName("list().loadAll() pages sensor readings across two pages via the server-reported window")
    void loadAllPagesReadings() {
        wireMock.stubFor(get(urlPathEqualTo("/smartview/v2/environmental/ibxs/SV5/sensors/readings"))
                .withQueryParam("offset", equalTo("0"))
                .willReturn(okJson(loadFixture("/json/ibxsmartview/sensor_readings_page1.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/smartview/v2/environmental/ibxs/SV5/sensors/readings"))
                .withQueryParam("offset", equalTo("2"))
                .willReturn(okJson(loadFixture("/json/ibxsmartview/sensor_readings_page2.json"))));

        PaginatedList<SensorReading> readings = ibxSmartView.environmentals().list(IBX, null, null, 0, 2);
        assertTrue(readings.hasNextPage());
        assertEquals(2, readings.size());

        readings.loadAll();

        assertFalse(readings.hasNextPage());
        assertEquals(3, readings.size());
        assertEquals("SENSOR-SV5-001", readings.get(0).getSensorId());
        assertEquals("SENSOR-SV5-002", readings.get(1).getSensorId());
        assertEquals("SENSOR-SV5-003", readings.get(2).getSensorId());

        // The page-2 request must advance from the SERVER-reported window (offset 0 + limit 2),
        // not from anything caller-side.
        wireMock.verify(getRequestedFor(
                urlPathEqualTo("/smartview/v2/environmental/ibxs/SV5/sensors/readings"))
                .withQueryParam("offset", equalTo("2"))
                .withQueryParam("limit", equalTo("2")));
    }

    @Test
    @DisplayName("500 on list throws EquinixServerException")
    void serverError() {
        stubErrorInline(wireMock, "/smartview/v2/environmental/ibxs/.*/sensors/readings",
                500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

        assertThrows(EquinixServerException.class,
                () -> ibxSmartView.environmentals().list(IBX));
    }

    /**
     * Request-shape coverage for the filtered list overload
     * {@code list(ibx, type, zone, offset, limit)}: verifies the exact path, verb,
     * embedded IBX path segment, and query parameters (or their absence).
     */
    @Nested
    @DisplayName("list(ibx, type, zone, offset, limit) request shape")
    class FilteredListRequestShape {

        static final String EXPECTED_PATH = "/smartview/v2/environmental/ibxs/SV5/sensors/readings";

        @Test
        @DisplayName("all filters present are sent as query params on a GET to the exact path")
        void allFiltersSent() {
            stubPaginatedGet(wireMock, "/smartview/v2/environmental/ibxs/.*/sensors/readings",
                    "/json/ibxsmartview/paginated_sensor_readings.json");

            PaginatedList<SensorReading> readings =
                    ibxSmartView.environmentals().list(IBX, "HUMIDITY", "ZONE-A", 40, 10);

            assertNotNull(readings);
            assertEquals(2, readings.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo(EXPECTED_PATH))
                    .withQueryParam("type", equalTo("HUMIDITY"))
                    .withQueryParam("zone", equalTo("ZONE-A"))
                    .withQueryParam("offset", equalTo("40"))
                    .withQueryParam("limit", equalTo("10")));
        }

        @Test
        @DisplayName("null filters are omitted from the query string")
        void nullFiltersOmitted() {
            stubPaginatedGet(wireMock, "/smartview/v2/environmental/ibxs/.*/sensors/readings",
                    "/json/ibxsmartview/paginated_sensor_readings.json");

            ibxSmartView.environmentals().list(IBX, null, null, null, null);

            // null type/zone are omitted, but offset/limit fall back to the SDK's pagination defaults.
            wireMock.verify(getRequestedFor(urlPathEqualTo(EXPECTED_PATH))
                    .withoutQueryParam("type")
                    .withoutQueryParam("zone")
                    .withQueryParam("offset", equalTo("0"))
                    .withQueryParam("limit", equalTo("100")));
        }

        @Test
        @DisplayName("partial filters send only the supplied params")
        void partialFiltersSent() {
            stubPaginatedGet(wireMock, "/smartview/v2/environmental/ibxs/.*/sensors/readings",
                    "/json/ibxsmartview/paginated_sensor_readings.json");

            ibxSmartView.environmentals().list(IBX, "TEMPERATURE", null, null, 5);

            wireMock.verify(getRequestedFor(urlPathEqualTo(EXPECTED_PATH))
                    .withQueryParam("type", equalTo("TEMPERATURE"))
                    .withQueryParam("limit", equalTo("5"))
                    .withoutQueryParam("zone")
                    .withQueryParam("offset", equalTo("0")));
        }

        @Test
        @DisplayName("the IBX code is embedded in the path, not sent as a query param")
        void ibxInPath() {
            stubPaginatedGet(wireMock, "/smartview/v2/environmental/ibxs/.*/sensors/readings",
                    "/json/ibxsmartview/paginated_sensor_readings.json");

            ibxSmartView.environmentals().list("DC11", "HUMIDITY", null, null, null);

            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/smartview/v2/environmental/ibxs/DC11/sensors/readings"))
                    .withQueryParam("type", equalTo("HUMIDITY"))
                    .withoutQueryParam("ibx"));
        }
    }
}
