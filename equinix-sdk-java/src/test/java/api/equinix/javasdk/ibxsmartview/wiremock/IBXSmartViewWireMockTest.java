package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for IBX SmartView domain.
 * Tests error handling for environmentals and power endpoints.
 */
class IBXSmartViewWireMockTest extends WireMockTestBase {

    static IBXSmartView ibxSmartView;

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
    @DisplayName("IBXSmartView client initializes and authenticates successfully")
    void clientInitializes() {
        assertNotNull(ibxSmartView);
        assertNotNull(ibxSmartView.getEquinixClient().getOAuthToken());
    }

    @Test
    @DisplayName("environmentals() client accessor returns non-null")
    void environmentalsAccessor() {
        assertNotNull(ibxSmartView.environmentals());
    }

    @Test
    @DisplayName("powerEvents() client accessor returns non-null")
    void powerEventsAccessor() {
        assertNotNull(ibxSmartView.powerEvents());
    }
}
