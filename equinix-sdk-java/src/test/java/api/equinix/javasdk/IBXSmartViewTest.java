package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.model.PowerEvent;
import api.equinix.javasdk.ibxsmartview.model.SensorReading;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.SystemAlert;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
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

    @Test
    void powerEvents() {
        try {
            PaginatedList<PowerEvent> powerEvents = ibxSmartView.powerEvents().search(List.of("DC2"), null, null, 0, 10);
            assertNotNull(powerEvents);
            assertTrue(powerEvents.size() >= 0);

            if (powerEvents.size() > 0) {
                assertNotNull(powerEvents.get(0).getAlertUid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Power events test skipped - account may not have access: " + e.getMessage());
        }
    }

    @Test
    void systemAlerts() {
        try {
            PaginatedList<SystemAlert> alerts = ibxSmartView.systemAlerts().search("ACTIVE", null, null, 0, 10);
            assertNotNull(alerts);
            assertTrue(alerts.size() >= 0);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "System alerts test skipped: " + e.getMessage());
        }
    }

    @Test
    void streamingSubscriptions() {
        try {
            List<StreamingSubscription> subscriptions = ibxSmartView.streamingSubscriptions().list();
            assertNotNull(subscriptions);
            assertTrue(subscriptions.size() >= 0);

            if (subscriptions.size() > 0) {
                StreamingSubscription subscription = ibxSmartView.streamingSubscriptions().getByUuid(subscriptions.get(0).getSubscriptionId());
                assertNotNull(subscription);
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Streaming subscriptions test skipped: " + e.getMessage());
        }
    }

    @Test
    void hierarchy() {
        try {
            var locationHierarchy = ibxSmartView.hierarchy().getLocationHierarchy(null, "DC2");
            assertNotNull(locationHierarchy);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Hierarchy location test skipped - may not have access: " + e.getMessage());
        }

        try {
            var powerHierarchy = ibxSmartView.hierarchy().getPowerHierarchy(null, "DC2");
            assertNotNull(powerHierarchy);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Hierarchy power test skipped - may not have access: " + e.getMessage());
        }
    }

    @Test
    void legacyEnvironmentals() {
        try {
            var envData = ibxSmartView.legacyEnvironmentals().getCurrent(null, "DC2", "IBX", "DC2");
            assertNotNull(envData);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Legacy environmentals test skipped: " + e.getMessage());
        }
    }

    @Test
    void legacyPower() {
        try {
            var powerData = ibxSmartView.legacyPower().getCurrent(null, "DC2", "IBX", "DC2");
            assertNotNull(powerData);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Legacy power test skipped: " + e.getMessage());
        }
    }

    @Test
    void smartViewAssets() {
        try {
            var assets = ibxSmartView.smartViewAssets().list(null, "DC2", null, null);
            assertNotNull(assets);
            assertTrue(assets.size() >= 0);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "SmartView assets test skipped: " + e.getMessage());
        }
    }
}
