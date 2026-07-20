package api.equinix.javasdk.mcp;

import api.equinix.javasdk.Mcp;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.mcp.bridge.McpBridge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Wire-level regression lock for the MCP bridge tool names.
 *
 * <p>The Fabric MCP server catalog (docs.equinix.com, "Fabric MCP Server") documents the
 * tool names {@code list_metros}, {@code search_connections}, {@code check_connection},
 * {@code search_routers}, {@code list_router_packages}, and {@code get_metric}. Earlier
 * SDK builds shipped five wrong legacy names ({@code list_metro}, {@code search_connection},
 * {@code validate_connection}, {@code search_router}, {@code get_router_package}) plus the
 * undocumented {@code get_metrics}. This test invokes every bridge method against a
 * catch-all WireMock stub and then proves, from the recorded request journal, that none
 * of the known-wrong legacy names is ever sent and each documented name is sent exactly
 * once.</p>
 */
@Tag("wiremock")
class McpBridgeToolNameRegressionTest extends WireMockTestBase {

    /** The five known-wrong legacy tool names, plus the undocumented get_metrics variant. */
    private static final List<String> BANNED_LEGACY_TOOL_NAMES = List.of(
            "list_metro",
            "search_connection",
            "validate_connection",
            "search_router",
            "get_router_package",
            "get_metrics");

    /** The documented catalog names each bridge method must emit, one call each below. */
    private static final List<String> DOCUMENTED_TOOL_NAMES = List.of(
            "get_metro",
            "list_metros",
            "check_connection",
            "search_connections",
            "search_prices",
            "search_routers",
            "list_router_packages",
            "create_router_commands",
            "get_metric",
            "search_metrics",
            "list_streams",
            "search_cloud_events",
            "list_stream_alert_rules");

    @Test
    @DisplayName("no bridge method sends a legacy tool name; every tools/call uses the documented catalog name")
    void bridgesNeverSendLegacyToolNames() throws Exception {
        resetStubs();

        // initialize() handshake plus a catch-all tools/call stub. The empty-data result
        // parses safely through every bridge (data[] lists come back empty, single-object
        // parses return null-field snapshots), so every method can be driven end-to-end.
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("initialize")))
                .willReturn(okJson(loadFixture("/json/mcp/initialize_response.json"))));
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                .willReturn(okJson(loadFixture("/json/mcp/empty_data_tool_result.json"))));

        McpClientConfig config = McpClientConfig.builder()
                .fabricEndpoint(wireMockUrl() + "/mcp/fabric")
                .tokenEndpoint(wireMockUrl() + "/oauth2/v1/token")
                .connectTimeoutMs(5000)
                .readTimeoutMs(5000)
                .maxRetries(0)
                .build();

        Mcp client = new Mcp(testCredentials(), config);
        try {
            client.initialize();
            McpBridge bridge = new McpBridge(client);

            // Drive every tool-calling method on all four bridges exactly once.
            bridge.metros().getMetro("SV");
            bridge.metros().listMetros();

            bridge.connections().validateConnection(Map.of("type", "EVPL_VC"));
            bridge.connections().searchConnections(Map.of("metro", "SV"));
            bridge.connections().searchPrices(Map.of("bandwidth", 1000));

            bridge.cloudRouters().searchRouters(Map.of("state", "PROVISIONED"));
            bridge.cloudRouters().getRouterPackage("STANDARD");
            bridge.cloudRouters().executeCommand("router-uuid-1", "ping", "8.8.8.8");

            bridge.observability().getMetrics("connection", "conn-uuid-1", "bandwidth",
                    "2026-06-01T00:00:00Z", "2026-06-02T00:00:00Z");
            bridge.observability().searchMetrics(Map.of("assetType", "connection"));
            bridge.observability().listStreams();
            bridge.observability().searchCloudEvents(Map.of("type", "connection.state.changed"));
            bridge.observability().listAlertRules("stream-uuid-1");

            assertEquals(DOCUMENTED_TOOL_NAMES.size(),
                    wireMock.findAll(postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                            .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))).size(),
                    "each bridge method above maps to exactly one tools/call");

            // The regression lock: none of the known-wrong legacy names ever hits the wire.
            for (String legacyName : BANNED_LEGACY_TOOL_NAMES) {
                wireMock.verify(0, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo(legacyName))));
            }

            // And the positive side of the lock: every documented catalog name was sent
            // exactly once, so a future rename cannot slip through as an unmatched extra.
            for (String documentedName : DOCUMENTED_TOOL_NAMES) {
                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo(documentedName))));
            }
        } finally {
            client.close();
        }
    }
}
