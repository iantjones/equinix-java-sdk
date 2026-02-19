package api.equinix.javasdk.ibxsmartview;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.ibxsmartview.model.json.EnvironmentDataJson;
import api.equinix.javasdk.ibxsmartview.model.json.PowerReadingJson;
import api.equinix.javasdk.ibxsmartview.model.json.SensorReadingJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for IBX SmartView JSON models:
 * {@link SensorReadingJson}, {@link PowerReadingJson}, and {@link EnvironmentDataJson}.
 */
class SmartViewDeserializationTest {

    private static ObjectMapper objectMapper;
    private static SensorReadingJson sensorReading;
    private static PowerReadingJson powerReading;
    private static EnvironmentDataJson environmentData;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;

        InputStream sensorIs = SmartViewDeserializationTest.class.getResourceAsStream("/json/ibxsmartview/sensor_reading_response.json");
        assertNotNull(sensorIs, "sensor_reading_response.json fixture not found on classpath");
        sensorReading = objectMapper.readValue(sensorIs, SensorReadingJson.class);

        InputStream powerIs = SmartViewDeserializationTest.class.getResourceAsStream("/json/ibxsmartview/power_reading_response.json");
        assertNotNull(powerIs, "power_reading_response.json fixture not found on classpath");
        powerReading = objectMapper.readValue(powerIs, PowerReadingJson.class);

        InputStream envIs = SmartViewDeserializationTest.class.getResourceAsStream("/json/ibxsmartview/environment_data_response.json");
        assertNotNull(envIs, "environment_data_response.json fixture not found on classpath");
        environmentData = objectMapper.readValue(envIs, EnvironmentDataJson.class);
    }

    // --- SensorReadingJson tests ---

    @Test
    void sensor_sensorId_isDeserialized() {
        assertEquals("SENSOR-SV5-001", sensorReading.getSensorId());
    }

    @Test
    void sensor_ibx_isDeserialized() {
        assertEquals("SV5", sensorReading.getIbx());
    }

    @Test
    void sensor_zoneId_isDeserialized() {
        assertEquals("ZONE-A", sensorReading.getZoneId());
    }

    @Test
    void sensor_temperature_isDeserialized() {
        assertNotNull(sensorReading.getTemperature());
        assertEquals(22.5, sensorReading.getTemperature().getValue());
    }

    @Test
    void sensor_humidity_isDeserialized() {
        assertNotNull(sensorReading.getHumidity());
        assertEquals(45.0, sensorReading.getHumidity().getValue());
    }

    // --- PowerReadingJson tests ---

    @Test
    void power_ibx_isDeserialized() {
        assertEquals("SV5", powerReading.getIbx());
    }

    @Test
    void power_cageId_isDeserialized() {
        assertEquals("SV5:01:001100", powerReading.getCageId());
    }

    @Test
    void power_cabinetId_isDeserialized() {
        assertEquals("SV5:01:001100:0101", powerReading.getCabinetId());
    }

    @Test
    void power_phase_isDeserialized() {
        assertEquals("A", powerReading.getPhase());
    }

    @Test
    void power_currentAmps_isDeserialized() {
        assertEquals(12.5, powerReading.getCurrentAmps());
    }

    @Test
    void power_apparentPower_isDeserialized() {
        assertEquals(2.75, powerReading.getApparentPower());
    }

    @Test
    void power_activePower_isDeserialized() {
        assertEquals(2.50, powerReading.getActivePower());
    }

    @Test
    void power_voltage_isDeserialized() {
        assertEquals(220.0, powerReading.getVoltage());
    }

    @Test
    void power_timestamp_isDeserialized() {
        assertEquals("2024-01-15T14:30:00Z", powerReading.getTimestamp());
    }

    @Test
    void power_status_isDeserialized() {
        assertEquals("OK", powerReading.getStatus());
    }

    // --- EnvironmentDataJson tests ---

    @Test
    void env_ibx_isDeserialized() {
        assertEquals("SV5", environmentData.getIbx());
    }

    @Test
    void env_levelType_isDeserialized() {
        assertEquals("CAGE", environmentData.getLevelType());
    }

    @Test
    void env_levelValue_isDeserialized() {
        assertEquals("SV5:01:001100", environmentData.getLevelValue());
    }

    @Test
    void env_temperature_isDeserialized() {
        assertEquals(21.8, environmentData.getTemperature());
    }

    @Test
    void env_humidity_isDeserialized() {
        assertEquals(42.3, environmentData.getHumidity());
    }

    @Test
    void env_dewPoint_isDeserialized() {
        assertEquals(8.5, environmentData.getDewPoint());
    }

    @Test
    void env_sensorId_isDeserialized() {
        assertEquals("ENV-SV5-002", environmentData.getSensorId());
    }

    @Test
    void env_timestamp_isDeserialized() {
        assertEquals("2024-01-15T14:30:00Z", environmentData.getTimestamp());
    }
}
