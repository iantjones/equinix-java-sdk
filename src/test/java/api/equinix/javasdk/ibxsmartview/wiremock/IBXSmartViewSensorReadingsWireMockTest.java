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
    @DisplayName("500 on list throws EquinixServerException")
    void serverError() {
        stubErrorInline(wireMock, "/smartview/v2/environmental/ibxs/.*/sensors/readings",
                500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

        assertThrows(EquinixServerException.class,
                () -> ibxSmartView.environmentals().list(IBX));
    }
}
