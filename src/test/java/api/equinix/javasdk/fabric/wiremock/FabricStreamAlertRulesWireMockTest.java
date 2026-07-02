package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.StreamAlertRule;
import org.junit.jupiter.api.*;

import java.util.Map;

import static api.equinix.javasdk.core.ResponseStubs.*;
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
            assertEquals("METRIC_ALERT", rule.getType());
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
        @DisplayName("honours an explicit type override in the request body")
        void createsWithExplicitType() {
            stubCreate(wireMock, "/fabric/v4/streams/" + STREAM_ID + "/alertRules",
                    "/json/fabric/stream_alert_rule_response.json");

            fabric.streamAlertRules().define(STREAM_ID)
                    .type("HEALTH_ALERT")
                    .name("Stream-Health-Alert")
                    .create();

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/streams/" + STREAM_ID + "/alertRules"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("HEALTH_ALERT")))
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
            assertEquals("HEALTH_ALERT", rules.get(1).getType());

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
            assertEquals("METRIC_ALERT", rule.getType());
            assertTrue(rule.getEnabled());

            assertNotNull(rule.getMetricSelector());
            assertEquals(java.util.List.of("equinix.fabric.connection.bandwidth_tx.usage"),
                    rule.getMetricSelector().getInclude());
            assertNotNull(rule.getResourceSelector());
            assertEquals(java.util.List.of("/fabric/v4/connections/8b140c74-0331-46d1-9cb3-2981be84dd1b"),
                    rule.getResourceSelector().getInclude());
            assertNotNull(rule.getDetectionMethod());
            assertEquals("THRESHOLD", rule.getDetectionMethod().getType());
            assertEquals("PT15M", rule.getDetectionMethod().getWindowSize());
            assertEquals("ABOVE", rule.getDetectionMethod().getOperand());
            assertEquals("35000000", rule.getDetectionMethod().getWarningThreshold());
            assertEquals("45000000", rule.getDetectionMethod().getCriticalThreshold());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/streams/" + STREAM_ID + "/alertRules/" + RULE_UUID)));
        }
    }
}
