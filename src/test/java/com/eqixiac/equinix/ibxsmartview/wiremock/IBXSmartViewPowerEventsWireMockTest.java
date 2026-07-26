package com.eqixiac.equinix.ibxsmartview.wiremock;

import com.eqixiac.equinix.IBXSmartView;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixConflictException;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.ibxsmartview.model.PowerAlertConfiguration;
import com.eqixiac.equinix.ibxsmartview.model.PowerEvent;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertCondition;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertConfigurationAsset;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertContactMethod;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertRecipient;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertThreshold;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.stubErrorInline;
import static com.eqixiac.equinix.core.TestFixtures.load;
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
    @DisplayName("filter query params")
    class FilterParams {

        @Test
        @DisplayName("search() sends ibx (comma-joined), status (comma-joined), edgeCollectedOn, offset and limit")
        void searchSendsAllFilters() {
            wireMock.stubFor(get(urlPathEqualTo("/dcim/v3/powerEvents/search"))
                    .willReturn(okJson(load("/json/ibxsmartview/power_events_page2.json"))));

            PaginatedList<PowerEvent> events = ibxSmartView.powerEvents().search(
                    List.of("SV5", "DC6"), List.of("ACTIVE", "CLEARED"),
                    "2026-06-30T00:00:00Z", 0, 50);

            assertEquals(1, events.size());
            assertEquals(1043L, events.get(0).getId());

            // Multi-valued ibx/status lists are comma-joined into a single query param each.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/dcim/v3/powerEvents/search"))
                    .withQueryParam("ibx", equalTo("SV5,DC6"))
                    .withQueryParam("status", equalTo("ACTIVE,CLEARED"))
                    .withQueryParam("edgeCollectedOn", equalTo("2026-06-30T00:00:00Z"))
                    .withQueryParam("offset", equalTo("0"))
                    .withQueryParam("limit", equalTo("50")));
        }

        @Test
        @DisplayName("search() omits ibx/status/edgeCollectedOn when null or empty")
        void searchOmitsNullFilters() {
            wireMock.stubFor(get(urlPathEqualTo("/dcim/v3/powerEvents/search"))
                    .willReturn(okJson(load("/json/ibxsmartview/power_events_page2.json"))));

            ibxSmartView.powerEvents().search(null, List.of(), null, 0, 100);

            wireMock.verify(getRequestedFor(urlPathEqualTo("/dcim/v3/powerEvents/search"))
                    .withoutQueryParam("ibx")
                    .withoutQueryParam("status")
                    .withoutQueryParam("edgeCollectedOn")
                    .withQueryParam("offset", equalTo("0"))
                    .withQueryParam("limit", equalTo("100")));
        }

        @Test
        @DisplayName("searchAlertConfigurations() sends ibx and state comma-joined plus offset/limit")
        void alertConfigurationsSearchSendsFilters() {
            wireMock.stubFor(get(urlPathEqualTo("/dcim/v3/powerEvents/configurations/search"))
                    .willReturn(okJson(load("/json/ibxsmartview/alert_configurations_page2.json"))));

            PaginatedList<PowerAlertConfiguration> configs = ibxSmartView.powerEvents()
                    .searchAlertConfigurations(List.of("SV5", "DC6"), List.of("ACTIVE", "PAUSED"), 0, 50);

            assertEquals(1, configs.size());
            assertEquals("cfg-0002", configs.get(0).getAlertConfigurationUid());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/dcim/v3/powerEvents/configurations/search"))
                    .withQueryParam("ibx", equalTo("SV5,DC6"))
                    .withQueryParam("state", equalTo("ACTIVE,PAUSED"))
                    .withQueryParam("offset", equalTo("0"))
                    .withQueryParam("limit", equalTo("50")));
        }

        @Test
        @DisplayName("searchAlertConfigurations() omits ibx/state when null or empty")
        void alertConfigurationsSearchOmitsNullFilters() {
            wireMock.stubFor(get(urlPathEqualTo("/dcim/v3/powerEvents/configurations/search"))
                    .willReturn(okJson(load("/json/ibxsmartview/alert_configurations_page2.json"))));

            ibxSmartView.powerEvents().searchAlertConfigurations(List.of(), null, 0, 100);

            wireMock.verify(getRequestedFor(urlPathEqualTo("/dcim/v3/powerEvents/configurations/search"))
                    .withoutQueryParam("ibx")
                    .withoutQueryParam("state")
                    .withQueryParam("offset", equalTo("0"))
                    .withQueryParam("limit", equalTo("100")));
        }
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

    @Nested
    @DisplayName("Error mapping")
    class Errors {

        static final String ERROR_500 =
                "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]";
        static final String ERROR_404 =
                "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Alert configuration not found\"}]";
        static final String ERROR_409 =
                "[{\"errorCode\":\"ERR-409\",\"errorMessage\":\"Configuration already exists\"}]";

        @Test
        @DisplayName("500 on search throws EquinixServerException")
        void searchServerError() {
            stubErrorInline(wireMock, "/dcim/v3/powerEvents/search", 500, ERROR_500);

            assertThrows(EquinixServerException.class,
                    () -> ibxSmartView.powerEvents().search(null, null, null, 0, 100));
        }

        @Test
        @DisplayName("500 on searchAlertConfigurations throws EquinixServerException")
        void alertConfigurationsSearchServerError() {
            stubErrorInline(wireMock, "/dcim/v3/powerEvents/configurations/search", 500, ERROR_500);

            assertThrows(EquinixServerException.class,
                    () -> ibxSmartView.powerEvents().searchAlertConfigurations(null, null, 0, 100));
        }

        @Test
        @DisplayName("409 on create throws EquinixConflictException")
        void createConflict() {
            stubErrorInline(wireMock, "/dcim/v3/powerEvents/configurations", 409, ERROR_409);

            assertThrows(EquinixConflictException.class,
                    () -> ibxSmartView.powerEvents().defineAlertConfiguration()
                            .withAccountNo("123456")
                            .withIbx("SV5")
                            .withSection("CAGE")
                            .withCondition(new PowerAlertCondition(
                                    "EXCEEDS", "CAGE_DRAW", new PowerAlertThreshold("%", "80")))
                            .create());
        }

        @Test
        @DisplayName("404 on update throws EquinixNotFoundException")
        void updateNotFound() {
            stubErrorInline(wireMock, "/dcim/v3/powerEvents/configurations", 404, ERROR_404);

            assertThrows(EquinixNotFoundException.class,
                    () -> ibxSmartView.powerEvents().updateAlertConfiguration("cfg-missing")
                            .withState("ACTIVE")
                            .update());
        }

        @Test
        @DisplayName("404 on pause throws EquinixNotFoundException")
        void pauseNotFound() {
            stubErrorInline(wireMock,
                    "/dcim/v3/powerEvents/configurations/cfg-missing/pause", 404, ERROR_404);

            assertThrows(EquinixNotFoundException.class,
                    () -> ibxSmartView.powerEvents().pauseAlertConfiguration("cfg-missing"));
        }

        @Test
        @DisplayName("404 on resume throws EquinixNotFoundException")
        void resumeNotFound() {
            stubErrorInline(wireMock,
                    "/dcim/v3/powerEvents/configurations/cfg-missing/resume", 404, ERROR_404);

            assertThrows(EquinixNotFoundException.class,
                    () -> ibxSmartView.powerEvents().resumeAlertConfiguration("cfg-missing"));
        }

        @Test
        @DisplayName("404 on delete throws EquinixNotFoundException")
        void deleteNotFound() {
            stubErrorInline(wireMock,
                    "/dcim/v3/powerEvents/configurations/cfg-missing", 404, ERROR_404);

            assertThrows(EquinixNotFoundException.class,
                    () -> ibxSmartView.powerEvents().deleteAlertConfiguration("cfg-missing"));
        }
    }
}
