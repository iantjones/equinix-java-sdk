package api.equinix.javasdk.ibxsmartview;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.ibxsmartview.enums.AlertStatus;
import api.equinix.javasdk.ibxsmartview.model.json.EnvironmentDataJson;
import api.equinix.javasdk.ibxsmartview.model.json.PowerEventJson;
import api.equinix.javasdk.ibxsmartview.model.json.SensorReadingJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for IBX SmartView JSON models:
 * {@link SensorReadingJson}, {@link PowerEventJson}, and {@link EnvironmentDataJson}.
 */
class SmartViewDeserializationTest {

    private static ObjectMapper objectMapper;
    private static SensorReadingJson sensorReading;
    private static PowerEventJson powerEvent;
    private static EnvironmentDataJson environmentData;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;

        InputStream sensorIs = SmartViewDeserializationTest.class.getResourceAsStream("/json/ibxsmartview/sensor_reading_response.json");
        assertNotNull(sensorIs, "sensor_reading_response.json fixture not found on classpath");
        sensorReading = objectMapper.readValue(sensorIs, SensorReadingJson.class);

        InputStream powerIs = SmartViewDeserializationTest.class.getResourceAsStream("/json/ibxsmartview/power_event_response.json");
        assertNotNull(powerIs, "power_event_response.json fixture not found on classpath");
        powerEvent = objectMapper.readValue(powerIs, PowerEventJson.class);

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

    // --- PowerEventJson tests ---

    @Test
    void powerEvent_alertUid_isDeserialized() {
        assertEquals("SV5.CAGE-DRAW#EXCEEDS:95", powerEvent.getAlertUid());
    }

    @Test
    void powerEvent_status_isDeserialized() {
        assertEquals(AlertStatus.ACTIVE, powerEvent.getStatus());
    }

    @Test
    void powerEvent_eventType_isDeserialized() {
        assertEquals("CAGE_DRAW", powerEvent.getEventType());
    }

    @Test
    void powerEvent_conditionType_isDeserialized() {
        assertEquals("EXCEEDS", powerEvent.getConditionType());
    }

    @Test
    void powerEvent_triggerValue_isDeserialized() {
        assertEquals("95", powerEvent.getTriggerValue());
    }

    @Test
    void powerEvent_currentValue_isDeserialized() {
        assertEquals("97.3", powerEvent.getCurrentValue());
    }

    @Test
    void powerEvent_asset_isDeserialized() {
        assertNotNull(powerEvent.getAsset());
        assertEquals("SV5", powerEvent.getAsset().getIbx());
        assertEquals("SV5:01:A1234", powerEvent.getAsset().getAssetUid());
    }

    @Test
    void powerEvent_activeProcessing_isDeserialized() {
        assertNotNull(powerEvent.getActiveProcessing());
        assertEquals("2024-01-15T08:30:00.000Z", powerEvent.getActiveProcessing().getEdgeCollectedOn());
    }

    @Test
    void powerEvent_accountNo_isDeserialized() {
        assertEquals("123456", powerEvent.getAccountNo());
    }

    // --- EnvironmentDataJson tests (payLoad/status envelope) ---

    @Test
    void env_payLoad_isDeserialized() {
        assertNotNull(environmentData.getPayLoad());
    }

    @Test
    void env_ibx_isDeserialized() {
        assertEquals("SV5", environmentData.getPayLoad().getIbx());
    }

    @Test
    void env_accountNo_isDeserialized() {
        assertEquals("123456", environmentData.getPayLoad().getAccountNo());
    }

    @Test
    void env_cage_isDeserialized() {
        assertEquals("SV5:01:001100", environmentData.getPayLoad().getCage());
    }

    @Test
    void env_temperature_isDeserialized() {
        assertEquals("21.8", environmentData.getPayLoad().getTemperature());
    }

    @Test
    void env_humidity_isDeserialized() {
        assertEquals("42.3", environmentData.getPayLoad().getHumidity());
    }

    @Test
    void env_temperatureUom_isDeserialized() {
        assertEquals("°C", environmentData.getPayLoad().getTemperatureUom());
    }

    @Test
    void env_humidityUom_isDeserialized() {
        assertEquals("%", environmentData.getPayLoad().getHumidityUom());
    }

    @Test
    void env_sensor_isDeserialized() {
        assertEquals("ENV-SV5-002", environmentData.getPayLoad().getSensor());
    }

    @Test
    void env_timestamp_isDeserialized() {
        assertEquals("2024-01-15T14:30:00Z", environmentData.getPayLoad().getTimestamp());
    }

    @Test
    void env_status_isDeserialized() {
        assertNotNull(environmentData.getStatus());
        assertEquals("OK", environmentData.getStatus().getMsg());
        assertEquals(1000, environmentData.getStatus().getStatuscode());
    }
}
