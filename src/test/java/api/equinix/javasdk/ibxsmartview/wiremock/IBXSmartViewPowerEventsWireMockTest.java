package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.model.PowerAlertConfiguration;
import api.equinix.javasdk.ibxsmartview.model.PowerEvent;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertCondition;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertConfigurationAsset;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertContactMethod;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertRecipient;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertThreshold;
import org.junit.jupiter.api.*;

import java.util.List;

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

    @Nested
    @DisplayName("defineAlertConfiguration().create()")
    class Create {

        @Test
        @DisplayName("POSTs to /configurations, serializes the body, returns the new UID")
        void createsAlertConfiguration() {
            // POST /dcim/v3/powerEvents/configurations -> 201 with the new alertConfigurationUid.
            wireMock.stubFor(post(urlPathEqualTo("/dcim/v3/powerEvents/configurations"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"alertConfigurationUid\":\"cfg-9001\"}")));

            PowerAlertThreshold threshold = new PowerAlertThreshold("%", "80");
            PowerAlertCondition condition = new PowerAlertCondition("EXCEEDS", "CAGE_DRAW", threshold);
            PowerAlertRecipient recipient = new PowerAlertRecipient(
                    "Ada", "Lovelace",
                    new PowerAlertContactMethod("+15551234567", true),
                    new PowerAlertContactMethod("ada@example.com", true));
            PowerAlertConfigurationAsset asset = new PowerAlertConfigurationAsset("asset-42", "Cage SV5:01:0100");

            String uid = ibxSmartView.powerEvents().defineAlertConfiguration()
                    .withAccountNo("123456")
                    .withIbx("SV5")
                    .withSection("CAGE")
                    .withCondition(condition)
                    .addRecipient(recipient)
                    .addAssets("cage", List.of(asset))
                    .create();

            assertEquals("cfg-9001", uid);

            wireMock.verify(postRequestedFor(urlPathEqualTo("/dcim/v3/powerEvents/configurations"))
                    .withRequestBody(matchingJsonPath("$.accountNo", equalTo("123456")))
                    .withRequestBody(matchingJsonPath("$.ibx", equalTo("SV5")))
                    .withRequestBody(matchingJsonPath("$.section", equalTo("CAGE")))
                    .withRequestBody(matchingJsonPath("$.condition.conditionType", equalTo("EXCEEDS")))
                    .withRequestBody(matchingJsonPath("$.condition.eventType", equalTo("CAGE_DRAW")))
                    .withRequestBody(matchingJsonPath("$.condition.threshold.unit", equalTo("%")))
                    .withRequestBody(matchingJsonPath("$.condition.threshold.value", equalTo("80")))
                    .withRequestBody(matchingJsonPath("$.recipients[0].firstName", equalTo("Ada")))
                    .withRequestBody(matchingJsonPath("$.recipients[0].email.value", equalTo("ada@example.com")))
                    .withRequestBody(matchingJsonPath("$.assets.cage[0].assetId", equalTo("asset-42"))));
        }
    }

    @Nested
    @DisplayName("updateAlertConfiguration(uid).update()")
    class Update {

        @Test
        @DisplayName("PUTs to /configurations with the uid and mutated fields in the body")
        void updatesAlertConfiguration() {
            // PUT /dcim/v3/powerEvents/configurations -> 200 (void op).
            wireMock.stubFor(put(urlPathEqualTo("/dcim/v3/powerEvents/configurations"))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{}")));

            PowerAlertCondition condition = new PowerAlertCondition(
                    "FALLS_BELOW", "CAGE_DRAW", new PowerAlertThreshold("%", "20"));

            ibxSmartView.powerEvents().updateAlertConfiguration("cfg-9001")
                    .withState("ACTIVE")
                    .withCondition(condition)
                    .withRecipients(List.of(new PowerAlertRecipient(
                            "Grace", "Hopper", null,
                            new PowerAlertContactMethod("grace@example.com", true))))
                    .update();

            wireMock.verify(putRequestedFor(urlPathEqualTo("/dcim/v3/powerEvents/configurations"))
                    .withRequestBody(matchingJsonPath("$.alertConfigurationUid", equalTo("cfg-9001")))
                    .withRequestBody(matchingJsonPath("$.state", equalTo("ACTIVE")))
                    .withRequestBody(matchingJsonPath("$.condition.conditionType", equalTo("FALLS_BELOW")))
                    .withRequestBody(matchingJsonPath("$.condition.threshold.value", equalTo("20")))
                    .withRequestBody(matchingJsonPath("$.recipients[0].firstName", equalTo("Grace")))
                    .withRequestBody(matchingJsonPath("$.recipients[0].email.value", equalTo("grace@example.com"))));
        }
    }

    @Nested
    @DisplayName("pauseAlertConfiguration(uid) / resumeAlertConfiguration(uid)")
    class PauseResume {

        @Test
        @DisplayName("pause PUTs to /configurations/{uid}/pause")
        void pauses() {
            wireMock.stubFor(put(urlPathEqualTo("/dcim/v3/powerEvents/configurations/cfg-9001/pause"))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "application/json").withBody("{}")));

            ibxSmartView.powerEvents().pauseAlertConfiguration("cfg-9001");

            wireMock.verify(putRequestedFor(
                    urlPathEqualTo("/dcim/v3/powerEvents/configurations/cfg-9001/pause")));
        }

        @Test
        @DisplayName("resume PUTs to /configurations/{uid}/resume")
        void resumes() {
            wireMock.stubFor(put(urlPathEqualTo("/dcim/v3/powerEvents/configurations/cfg-9001/resume"))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "application/json").withBody("{}")));

            ibxSmartView.powerEvents().resumeAlertConfiguration("cfg-9001");

            wireMock.verify(putRequestedFor(
                    urlPathEqualTo("/dcim/v3/powerEvents/configurations/cfg-9001/resume")));
        }
    }

    @Nested
    @DisplayName("deleteAlertConfiguration(uid)")
    class Delete {

        @Test
        @DisplayName("DELETEs /configurations/{uid}")
        void deletesAlertConfiguration() {
            wireMock.stubFor(delete(urlPathEqualTo("/dcim/v3/powerEvents/configurations/cfg-9001"))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "application/json").withBody("{}")));

            ibxSmartView.powerEvents().deleteAlertConfiguration("cfg-9001");

            wireMock.verify(deleteRequestedFor(
                    urlPathEqualTo("/dcim/v3/powerEvents/configurations/cfg-9001")));
        }
    }
}
