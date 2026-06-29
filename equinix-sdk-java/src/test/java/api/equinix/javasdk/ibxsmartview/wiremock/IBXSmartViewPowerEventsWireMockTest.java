package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.model.PowerAlertConfiguration;
import api.equinix.javasdk.ibxsmartview.model.PowerEvent;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.TestFixtures.load;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based paging tests for the IBX SmartView power-events API.
 *
 * <p>Power events page off the flat {@code PowerEventsPaginatedResponse} (top-level
 * items/limit/offset/totalCount), and alert configurations page off the nested
 * {@code AlertPaginatedResponse} (data/pagination). These tests exercise {@code loadAll()} across
 * two pages to cover the subsequent-page handling, which previously NPE-d (flat power events) and
 * ClassCastException-ed (alert configurations).</p>
 */
class IBXSmartViewPowerEventsWireMockTest extends WireMockTestBase {

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
    @DisplayName("search().loadAll() pages the flat power-events response across two pages")
    void powerEventsLoadAll() {
        wireMock.stubFor(get(urlPathEqualTo("/dcim/v3/powerEvents/search"))
                .withQueryParam("offset", equalTo("0"))
                .willReturn(okJson(load("/json/ibxsmartview/power_events_page1.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/dcim/v3/powerEvents/search"))
                .withQueryParam("offset", equalTo("100"))
                .willReturn(okJson(load("/json/ibxsmartview/power_events_page2.json"))));

        PaginatedList<PowerEvent> events = ibxSmartView.powerEvents().search(null, null, null, 0, 100);
        assertTrue(events.hasNextPage());
        assertEquals(1, events.size());

        events.loadAll();

        assertFalse(events.hasNextPage());
        assertEquals(2, events.size());
        assertEquals(1042L, events.get(0).getId());
        assertEquals(1043L, events.get(1).getId());
    }

    @Test
    @DisplayName("searchAlertConfigurations().loadAll() pages the nested response without ClassCastException")
    void alertConfigurationsLoadAll() {
        wireMock.stubFor(get(urlPathEqualTo("/dcim/v3/powerEvents/configurations/search"))
                .withQueryParam("offset", equalTo("0"))
                .willReturn(okJson(load("/json/ibxsmartview/alert_configurations_page1.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/dcim/v3/powerEvents/configurations/search"))
                .withQueryParam("offset", equalTo("100"))
                .willReturn(okJson(load("/json/ibxsmartview/alert_configurations_page2.json"))));

        PaginatedList<PowerAlertConfiguration> configs =
                ibxSmartView.powerEvents().searchAlertConfigurations(null, null, 0, 100);
        assertTrue(configs.hasNextPage());
        assertEquals(1, configs.size());

        configs.loadAll();

        assertFalse(configs.hasNextPage());
        assertEquals(2, configs.size());
        assertEquals("cfg-0001", configs.get(0).getAlertConfigurationUid());
        assertEquals("cfg-0002", configs.get(1).getAlertConfigurationUid());
    }
}
