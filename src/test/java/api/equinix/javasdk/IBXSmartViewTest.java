package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.model.SensorReading;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IBXSmartViewTest {

    static IBXSmartView ibxSmartView;

    @BeforeAll
    static void obtainTestingData() {
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        ibxSmartView = new IBXSmartView(new BasicEquinixCredentials(accessKey, secretKey));
        ibxSmartView.authenticate();
    }

    @Test
    void environmentals() {
        PaginatedList<SensorReading> sensorReadings = ibxSmartView.environmentals().list("DC2");
        assertTrue(sensorReadings.size() > 0);

        SensorReading sensorReading = ibxSmartView.environmentals().getSensorReading("DC2", sensorReadings.get(0).getSensorId());
        assertNotNull(sensorReading.getSensorId());
    }
}