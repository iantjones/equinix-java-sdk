package com.eqixiac.equinix.fabric.wiremock;
import com.eqixiac.equinix.fabric.enums.StreamAlertRuleType;
import com.eqixiac.equinix.fabric.enums.DetectionMethodType;
import com.eqixiac.equinix.fabric.enums.DetectionMethodOperand;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.model.StreamAlertRule;
import org.junit.jupiter.api.*;

import java.util.Map;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric StreamAlertRules.
 * Covers the create (define().create()) mutation path.
 */
class FabricStreamAlertRulesWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    private static final String STREAM_ID = "d4e5f6a7-b8c9-0123-defa-345678901bcd";

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("define().create()")
    class Create {

        @Test
        @DisplayName("POSTs a new alert rule to /streams/{streamId}/alertRules and returns it")
        void createsAlertRule() {
            stubCreate(wireMock, "/fabric/v4/streams/" + STREAM_ID + "/alertRules",
                    "/json/fabric/stream_alert_rule_response.json");

            StreamAlertRule rule = fabric.streamAlertRules().define(STREAM_ID)
                    .name("High-Egress-Bandwidth-Alert")
                    .description("Fires when egress bandwidth utilisation exceeds the configured threshold")
                    .enabled(true)
                    .metricSelector(Map.of("include", java.util.List.of("equinix.fabric.connection.bandwidth_tx.usage")))
                    .detectionMethod(Map.of("type", "STATIC", "warningThreshold", "8000000000"))
                    .create();

            assertNotNull(rule);
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", rule.getUuid());
            assertEquals("High-Egress-Bandwidth-Alert", rule.getName());
            assertEquals(StreamAlertRuleType.METRIC_ALERT, rule.getType());
            assertTrue(rule.getEnabled());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/streams/" + STREAM_ID + "/alertRules"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("METRIC_ALERT")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("High-Egress-Bandwidth-Alert")))
                    .withRequestBody(matchingJsonPath("$.enabled", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.metricSelector.include[0]",
                            equalTo("equinix.fabric.connection.bandwidth_tx.usage")))
                    .withRequestBody(matchingJsonPath("$.detectionMethod.type", equalTo("STATIC"))));
        }

        @Test
        @DisplayName("honours an explicitly-set type in the request body")
        void createsWithExplicitType() {
            stubCreate(wireMock, "/fabric/v4/streams/" + STREAM_ID + "/alertRules",
                    "/json/fabric/stream_alert_rule_response.json");

            // METRIC_ALERT is the only AlertRulePostRequest.type the spec declares.
            fabric.streamAlertRules().define(STREAM_ID)
                    .type(StreamAlertRuleType.METRIC_ALERT)
                    .name("Stream-Health-Alert")
                    .create();

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/streams/" + STREAM_ID + "/alertRules"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("METRIC_ALERT")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Stream-Health-Alert"))));
        }
    }

    @Nested
    @DisplayName("list()")
    class List {

        @Test
        @DisplayName("GETs /streams/{streamId}/alertRules and deserializes the page")
        void listsAlertRules() {
            stubPaginatedGet(wireMock, "/fabric/v4/streams/" + STREAM_ID + "/alertRules",
                    "/json/fabric/paginated_stream_alert_rules.json");

            PaginatedList<StreamAlertRule> rules = fabric.streamAlertRules().list(STREAM_ID);

            assertNotNull(rules);
            assertEquals(2, rules.size());
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", rules.get(0).getUuid());
            assertEquals("High-Egress-Bandwidth-Alert", rules.get(0).getName());
            assertEquals("b2c3d4e5-f6a7-8901-bcde-f01234567890", rules.get(1).getUuid());
            // "HEALTH_ALERT" is not a spec-declared StreamAlertRule type; the typed enum falls back to UNKNOWN.
            assertEquals(StreamAlertRuleType.UNKNOWN, rules.get(1).getType());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/streams/" + STREAM_ID + "/alertRules")));
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        private static final String RULE_UUID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

        @Test
        @DisplayName("GETs /streams/{streamId}/alertRules/{uuid} and returns the rule")
        void returnsAlertRule() {
            stubSingleton(wireMock, "/fabric/v4/streams/" + STREAM_ID + "/alertRules/" + RULE_UUID,
                    "/json/fabric/stream_alert_rule_response.json");

            StreamAlertRule rule = fabric.streamAlertRules().getByUuid(STREAM_ID, RULE_UUID);

            assertNotNull(rule);
            assertEquals(RULE_UUID, rule.getUuid());
            assertEquals("High-Egress-Bandwidth-Alert", rule.getName());
            assertEquals(StreamAlertRuleType.METRIC_ALERT, rule.getType());
            assertTrue(rule.getEnabled());

            assertNotNull(rule.getMetricSelector());
            assertEquals(java.util.List.of("equinix.fabric.connection.bandwidth_tx.usage"),
                    rule.getMetricSelector().getInclude());
            assertNotNull(rule.getResourceSelector());
            assertEquals(java.util.List.of("/fabric/v4/connections/8b140c74-0331-46d1-9cb3-2981be84dd1b"),
                    rule.getResourceSelector().getInclude());
            assertNotNull(rule.getDetectionMethod());
            assertEquals(DetectionMethodType.THRESHOLD, rule.getDetectionMethod().getType());
            assertEquals("PT15M", rule.getDetectionMethod().getWindowSize());
            assertEquals(DetectionMethodOperand.ABOVE, rule.getDetectionMethod().getOperand());
            assertEquals("35000000", rule.getDetectionMethod().getWarningThreshold());
            assertEquals("45000000", rule.getDetectionMethod().getCriticalThreshold());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/streams/" + STREAM_ID + "/alertRules/" + RULE_UUID)));
        }
    }

    @Nested
    @DisplayName("update(streamId) / save()")
    class Update {

        private static final String RULE_UUID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        private static final String URL = "/fabric/v4/streams/" + STREAM_ID + "/alertRules/" + RULE_UUID;

        @Test
        @DisplayName("PATCHes the full AlertRulePutRequest body as application/json")
        void savePatchesRule() {
            stubSingleton(wireMock, URL, "/json/fabric/stream_alert_rule_response.json");
            wireMock.stubFor(patch(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_alert_rule_response.json"))));

            StreamAlertRule rule = fabric.streamAlertRules().getByUuid(STREAM_ID, RULE_UUID);
            StreamAlertRule updated = rule.update(STREAM_ID)
                    .name("Renamed-Alert")
                    .enabled(false)
                    .detectionMethod(Map.of("type", "STATIC", "warningThreshold", "9000000000"))
                    .save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathEqualTo(URL))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("METRIC_ALERT")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Renamed-Alert")))
                    .withRequestBody(matchingJsonPath("$.enabled", equalTo("false")))
                    .withRequestBody(matchingJsonPath("$.detectionMethod.type", equalTo("STATIC")))
                    .withRequestBody(matchingJsonPath("$.detectionMethod.warningThreshold",
                            equalTo("9000000000"))));
        }
    }

    @Nested
    @DisplayName("Wrapper delete(streamId)")
    class WrapperDelete {

        private static final String RULE_UUID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        private static final String URL = "/fabric/v4/streams/" + STREAM_ID + "/alertRules/" + RULE_UUID;

        @Test
        @DisplayName("DELETEs /streams/{streamId}/alertRules/{uuid} and returns true")
        void deletesAlertRule() {
            stubSingleton(wireMock, URL, "/json/fabric/stream_alert_rule_response.json");
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_alert_rule_response.json"))));

            StreamAlertRule rule = fabric.streamAlertRules().getByUuid(STREAM_ID, RULE_UUID);
            Boolean deleted = rule.delete(STREAM_ID);

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/streams/.*/alertRules/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Alert rule not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.streamAlertRules().getByUuid(STREAM_ID, "invalid-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/streams/.*/alertRules/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.streamAlertRules().getByUuid(STREAM_ID, "test-uuid"));
        }
    }
}
