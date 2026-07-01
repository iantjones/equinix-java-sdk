package api.equinix.javasdk.mcp;
import api.equinix.javasdk.Mcp;

import api.equinix.javasdk.core.TestFixtures;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.mcp.bridge.McpBridge;
import api.equinix.javasdk.mcp.bridge.McpCloudRouterBridge;
import api.equinix.javasdk.mcp.bridge.McpConnectionBridge;
import api.equinix.javasdk.mcp.bridge.McpMetroBridge;
import api.equinix.javasdk.mcp.bridge.McpObservabilityBridge;
import api.equinix.javasdk.mcp.model.McpToolDefinition;
import api.equinix.javasdk.mcp.model.McpToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based tests for the MCP client and bridge layer.
 */
@Tag("wiremock")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class McpWireMockTest extends WireMockTestBase {

    private static Mcp mcpClient;
    private static McpBridge mcpBridge;

    @BeforeAll
    static void setUp() {
        McpClientConfig config = McpClientConfig.builder()
                .fabricEndpoint(wireMockUrl() + "/mcp/fabric")
                .tokenEndpoint(wireMockUrl() + "/oauth2/v1/token")
                .connectTimeoutMs(5000)
                .readTimeoutMs(5000)
                .build();

        mcpClient = new Mcp(testCredentials(), config);
        mcpBridge = new McpBridge(mcpClient);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (mcpClient != null) {
            mcpClient.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("MCP client initialization handshake")
    void testInitialize() {
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("initialize")))
                .willReturn(okJson(loadFixture("/json/mcp/initialize_response.json"))));

        mcpClient.initialize();
        assertTrue(mcpClient.isInitialized());
    }

    @Test
    @Order(2)
    @DisplayName("MCP tools/list returns tool definitions")
    void testListTools() {
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("tools/list")))
                .willReturn(okJson(loadFixture("/json/mcp/tools_list_response.json"))));

        Map<String, McpToolDefinition> tools = mcpClient.listTools();

        assertFalse(tools.isEmpty());
        assertTrue(tools.containsKey("get_metro"));
        assertTrue(tools.containsKey("list_metro"));
        assertTrue(tools.containsKey("search_connection"));
        assertTrue(tools.containsKey("validate_connection"));
        assertEquals("Retrieve details about a specific metro by code",
                tools.get("get_metro").getDescription());
    }

    @Test
    @Order(3)
    @DisplayName("McpMetroBridge.listMetros returns typed metro objects")
    void testListMetros() {
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.params.name", equalTo("list_metro")))
                .willReturn(okJson(loadFixture("/json/mcp/list_metro_result.json"))));

        List<McpMetroBridge.McpMetro> metros = mcpBridge.metros().listMetros();

        assertEquals(3, metros.size());

        McpMetroBridge.McpMetro sv = metros.get(0);
        assertEquals("SV", sv.getCode());
        assertEquals("Silicon Valley", sv.getName());
        assertEquals("AMER", sv.getRegion());
        assertEquals("US", sv.getCountry());
        assertEquals(3, sv.getConnectedMetroCount());
    }

    @Test
    @Order(4)
    @DisplayName("McpConnectionBridge.validateConnection returns validation result")
    void testValidateConnection() {
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.params.name", equalTo("validate_connection")))
                .willReturn(okJson(loadFixture("/json/mcp/validate_connection_result.json"))));

        McpConnectionBridge.McpConnectionValidation result =
                mcpBridge.connections().validateConnection(Map.of(
                        "type", "EVPL_VC",
                        "name", "test-connection",
                        "bandwidth", 1000
                ));

        assertTrue(result.isValid());
        assertEquals("Connection configuration is valid", result.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("MCP JSON-RPC error throws McpException")
    void testJsonRpcError() {
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.params.name", equalTo("get_metro")))
                .willReturn(okJson(loadFixture("/json/mcp/error_response.json"))));

        McpException ex = assertThrows(McpException.class, () ->
                mcpClient.callTool("get_metro", Map.of()));

        assertTrue(ex.getMessage().contains("-32602"));
        assertTrue(ex.getMessage().contains("Invalid params"));
    }

    @Test
    @Order(6)
    @DisplayName("MCP client requires initialization before tool calls")
    void testUninitializedClient() {
        McpClientConfig config = McpClientConfig.builder()
                .fabricEndpoint(wireMockUrl() + "/mcp/fabric")
                .tokenEndpoint(wireMockUrl() + "/oauth2/v1/token")
                .build();

        Mcp freshClient = new Mcp(testCredentials(), config);

        McpException ex = assertThrows(McpException.class, () ->
                freshClient.callTool("get_metro", Map.of("metroCode", "SV")));

        assertTrue(ex.getMessage().contains("not initialized"));
    }

    @Test
    @Order(7)
    @DisplayName("McpBridge.availableTools returns cached tools")
    void testAvailableTools() {
        // Tools were loaded in testListTools, so registry should be populated
        Map<String, McpToolDefinition> tools = mcpBridge.availableTools();
        assertFalse(tools.isEmpty());
    }

    // ------------------------------------------------------------------
    // Helpers for the mutation/action + retry coverage below.
    // ------------------------------------------------------------------

    /**
     * Builds a fresh, already-initialized {@link Mcp} client wired to WireMock, with an
     * explicit Peering Insights endpoint and the given retry budget. Each caller uses its
     * own client so scenario state and token caches never bleed across tests.
     */
    private static Mcp newInitializedClient(int maxRetries) {
        McpClientConfig config = McpClientConfig.builder()
                .fabricEndpoint(wireMockUrl() + "/mcp/fabric")
                .peeringInsightsEndpoint(wireMockUrl() + "/mcp/peeringInsights")
                .tokenEndpoint(wireMockUrl() + "/oauth2/v1/token")
                .connectTimeoutMs(5000)
                .readTimeoutMs(5000)
                .maxRetries(maxRetries)
                .build();

        // initialize() performs a JSON-RPC handshake against the Fabric endpoint.
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("initialize")))
                .willReturn(okJson(loadFixture("/json/mcp/initialize_response.json"))));

        Mcp client = new Mcp(testCredentials(), config);
        client.initialize();
        return client;
    }

    @Nested
    @DisplayName("McpBridge plain accessors (no HTTP)")
    class BridgeAccessors {

        /**
         * {@code getClient()} is a plain getter: it must return the exact {@link Mcp}
         * instance the bridge was constructed with and must not perform any HTTP.
         */
        @Test
        @DisplayName("getClient() returns the underlying Mcp without any HTTP call")
        void getClientReturnsUnderlyingClient() {
            resetStubs();

            Mcp returned = mcpBridge.getClient();

            assertNotNull(returned, "getClient() must not return null");
            assertSame(mcpClient, returned, "getClient() must return the injected Mcp instance");

            // A plain getter performs no network traffic: nothing should have hit WireMock.
            wireMock.verify(0, postRequestedFor(urlPathEqualTo("/mcp/fabric")));
            wireMock.verify(0, postRequestedFor(urlPathEqualTo("/mcp/peeringInsights")));
        }

        /**
         * getClient() on a bridge wrapping a freshly-built (uninitialized) client returns that
         * same client, confirming the getter is a pure accessor independent of client state.
         */
        @Test
        @DisplayName("getClient() reflects the exact client passed to a new McpBridge")
        void getClientReflectsConstructorArgument() {
            McpClientConfig config = McpClientConfig.builder()
                    .fabricEndpoint(wireMockUrl() + "/mcp/fabric")
                    .tokenEndpoint(wireMockUrl() + "/oauth2/v1/token")
                    .build();

            Mcp fresh = new Mcp(testCredentials(), config);
            try {
                McpBridge bridge = new McpBridge(fresh);
                assertSame(fresh, bridge.getClient(),
                        "getClient() must return the client passed to the McpBridge constructor");
            } finally {
                assertDoesNotThrow(fresh::close);
            }
        }
    }

    @Nested
    @DisplayName("Action / mutation tool invocations")
    class ActionTools {

        @Test
        @DisplayName("callPeeringTool posts tools/call to the Peering Insights endpoint")
        void callPeeringToolHitsPeeringEndpoint() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/peeringInsights"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("get_asn")))
                        .willReturn(okJson(loadFixture("/json/mcp/peering_tool_result.json"))));

                McpToolResult result = client.callPeeringTool("get_asn", Map.of("asn", 15169));

                assertFalse(result.isError());
                JsonNode json = result.getJsonContent(client.getObjectMapper());
                assertEquals(15169, json.get("asn").asInt());
                assertEquals("Google LLC", json.get("name").asText());

                // The call MUST target the Peering Insights endpoint (not Fabric) and carry
                // the JSON-RPC 2.0 envelope with the tool name + arguments.
                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/peeringInsights"))
                        .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                        .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("get_asn")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.asn", equalTo("15169"))));

                // And must NOT leak onto the Fabric endpoint.
                wireMock.verify(0, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("get_asn"))));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("McpCloudRouterBridge.executeCommand posts create_router_commands with ping args")
        void executeCommandPostsRouterCommand() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("create_router_commands")))
                        .willReturn(okJson(loadFixture("/json/mcp/router_command_result.json"))));

                McpCloudRouterBridge bridge = new McpBridge(client).cloudRouters();
                JsonNode result = bridge.executeCommand("router-uuid-1", "ping", "8.8.8.8");

                assertEquals("COMPLETED", result.get("status").asText());
                assertEquals("ping", result.get("type").asText());

                // Verify the serialized JSON-RPC request body: tool name + the three mapped args.
                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("create_router_commands")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.routerUuid", equalTo("router-uuid-1")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.type", equalTo("ping")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.host", equalTo("8.8.8.8"))));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("Mcp.close() releases the client without error")
        void closeIsIdempotentAction() throws Exception {
            Mcp client = newInitializedClient(0);
            // close() is the terminal action on the client; it must not throw.
            assertDoesNotThrow(client::close);
        }
    }

    @Nested
    @DisplayName("JSON-RPC transport retry paths")
    class RetryPaths {

        private static final String SCENARIO = "mcp-retry";

        @Test
        @DisplayName("401 -> token invalidated, refreshed, and request retried once")
        void retriesOnceAfterUnauthorized() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                // First tool call returns 401; the inline handler in doExecuteRpc invalidates the
                // token and retries the SAME request once with a fresh bearer.
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .inScenario(SCENARIO)
                        .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router")))
                        .willReturn(aResponse().withStatus(401).withBody("{\"error\":\"unauthorized\"}"))
                        .willSetStateTo("authed"));

                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .inScenario(SCENARIO)
                        .whenScenarioStateIs("authed")
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router")))
                        .willReturn(okJson(loadFixture("/json/mcp/generic_tool_result.json"))));

                McpToolResult result = client.callTool("search_router", Map.of());
                assertFalse(result.isError());

                // Exactly two POSTs to the tool endpoint: the 401 and the retried 200.
                wireMock.verify(2, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router"))));

                // Token was invalidated -> a second OAuth token fetch occurred for the refresh.
                wireMock.verify(moreThanOrExactly(2),
                        postRequestedFor(urlPathEqualTo("/oauth2/v1/token")));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("429 -> rate-limit error retried, then succeeds")
        void retriesOnRateLimit() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(2);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .inScenario(SCENARIO)
                        .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router")))
                        .willReturn(aResponse().withStatus(429).withBody("{\"error\":\"rate limited\"}"))
                        .willSetStateTo("recovered"));

                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .inScenario(SCENARIO)
                        .whenScenarioStateIs("recovered")
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router")))
                        .willReturn(okJson(loadFixture("/json/mcp/generic_tool_result.json"))));

                McpToolResult result = client.callTool("search_router", Map.of());
                assertFalse(result.isError());

                // First attempt (429) + retry (200) == 2 POSTs.
                wireMock.verify(2, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router"))));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("5xx -> server error retried with backoff, then succeeds")
        void retriesOnServerError() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(2);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .inScenario(SCENARIO)
                        .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router")))
                        .willReturn(aResponse().withStatus(503).withBody("{\"error\":\"unavailable\"}"))
                        .willSetStateTo("recovered"));

                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .inScenario(SCENARIO)
                        .whenScenarioStateIs("recovered")
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router")))
                        .willReturn(okJson(loadFixture("/json/mcp/generic_tool_result.json"))));

                McpToolResult result = client.callTool("search_router", Map.of());
                assertFalse(result.isError());

                wireMock.verify(2, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router"))));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("persistent 5xx exhausts retries and throws EquinixServerException")
        void throwsAfterExhaustingServerErrorRetries() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(1);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router")))
                        .willReturn(aResponse().withStatus(500).withBody("{\"error\":\"boom\"}")));

                assertThrows(api.equinix.javasdk.core.exception.EquinixServerException.class,
                        () -> client.callTool("search_router", Map.of()));

                // maxRetries=1 -> initial attempt + one retry == 2 POSTs before giving up.
                wireMock.verify(2, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_router"))));
            } finally {
                client.close();
            }
        }
    }

    @Nested
    @DisplayName("McpMetroBridge read ops")
    class MetroReads {

        @Test
        @DisplayName("getMetro posts tools/call get_metro with metroCode arg and parses the single metro")
        void getMetroPostsGetMetroToolCall() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("get_metro")))
                        .willReturn(okJson(loadFixture("/json/mcp/get_metro_result.json"))));

                McpMetroBridge.McpMetro metro = new McpBridge(client).metros().getMetro("SV");

                // Typed parse of the single metro object embedded in the tool result.
                assertEquals("SV", metro.getCode());
                assertEquals("Silicon Valley", metro.getName());
                assertEquals("AMER", metro.getRegion());
                assertEquals("US", metro.getCountry());
                assertEquals(3, metro.getConnectedMetroCount());

                // Verify the JSON-RPC 2.0 request body: tools/call envelope on the Fabric endpoint,
                // the get_metro tool, and the single mapped argument key "metroCode".
                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                        .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("get_metro")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.metroCode", equalTo("SV"))));
            } finally {
                client.close();
            }
        }
    }

    @Nested
    @DisplayName("McpConnectionBridge SEARCH tool invocations")
    class ConnectionSearchTools {

        @Test
        @DisplayName("searchConnections posts search_connection tools/call with mapped filters and parses data[]")
        void searchConnectionsPostsSearchConnection() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_connection")))
                        .willReturn(okJson(loadFixture("/json/mcp/search_connection_result.json"))));

                McpConnectionBridge bridge = new McpBridge(client).connections();
                List<McpConnectionBridge.McpConnection> connections =
                        bridge.searchConnections(Map.of("metro", "SV", "state", "ACTIVE"));

                assertEquals(2, connections.size());
                McpConnectionBridge.McpConnection first = connections.get(0);
                assertEquals("conn-uuid-1", first.getUuid());
                assertEquals("prod-vc-sv", first.getName());
                assertEquals("EVPL_VC", first.getType());
                assertEquals("ACTIVE", first.getState());
                assertEquals(1000, first.getBandwidth());

                // JSON-RPC 2.0 envelope: tools/call to search_connection with the filter map
                // mapped straight into params.arguments.*
                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                        .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_connection")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.metro", equalTo("SV")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.state", equalTo("ACTIVE"))));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("searchPrices posts search_prices tools/call with mapped filters and returns raw JSON")
        void searchPricesPostsSearchPrices() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_prices")))
                        .willReturn(okJson(loadFixture("/json/mcp/search_prices_result.json"))));

                McpConnectionBridge bridge = new McpBridge(client).connections();
                JsonNode prices = bridge.searchPrices(Map.of("type", "VIRTUAL_CONNECTION_PRODUCT", "bandwidth", 1000));

                assertNotNull(prices);
                assertTrue(prices.has("data"));
                assertTrue(prices.get("data").isArray());
                assertEquals(250.0,
                        prices.get("data").get(0).get("charges").get(0).get("price").asDouble());

                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                        .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_prices")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.type", equalTo("VIRTUAL_CONNECTION_PRODUCT")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.bandwidth", equalTo("1000"))));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("McpBridge.getClient() is a plain getter returning the underlying Mcp (no HTTP)")
        void getClientReturnsUnderlyingClient() throws Exception {
            Mcp client = newInitializedClient(0);
            try {
                McpBridge bridge = new McpBridge(client);
                assertNotNull(bridge.getClient());
                assertSame(client, bridge.getClient());
            } finally {
                client.close();
            }
        }
    }

    // ------------------------------------------------------------------
    // Observability bridge read/search coverage.
    //
    // Every McpObservabilityBridge read op maps to a JSON-RPC tools/call posted
    // to the Fabric endpoint (Mcp.callTool). Each test asserts the JSON-RPC 2.0
    // envelope (jsonrpc/method/params.name) plus the tool-specific arguments.
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("McpObservabilityBridge read/search operations")
    class ObservabilityReads {

        private McpObservabilityBridge observability(Mcp client) {
            return new McpBridge(client).observability();
        }

        @Test
        @DisplayName("getMetrics posts get_metrics with all five arguments")
        void getMetricsPostsGetMetrics() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("get_metrics")))
                        .willReturn(okJson(loadFixture("/json/mcp/observability_metrics_result.json"))));

                McpObservabilityBridge.McpMetrics metrics = observability(client).getMetrics(
                        "connection", "conn-uuid-1", "bandwidth",
                        "2026-06-01T00:00:00Z", "2026-06-02T00:00:00Z");

                assertEquals("conn-uuid-1", metrics.getAssetId());
                assertEquals("bandwidth", metrics.getMetricType());
                assertEquals(10.5, metrics.getMin());
                assertEquals(95.2, metrics.getMax());
                assertEquals(52.4, metrics.getAvg());

                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                        .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("get_metrics")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.assetType", equalTo("connection")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.assetId", equalTo("conn-uuid-1")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.metricType", equalTo("bandwidth")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.startTime", equalTo("2026-06-01T00:00:00Z")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.endTime", equalTo("2026-06-02T00:00:00Z"))));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("searchMetrics posts search_metrics with filter arguments and parses the data array")
        void searchMetricsPostsSearchMetrics() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_metrics")))
                        .willReturn(okJson(loadFixture("/json/mcp/observability_search_metrics_result.json"))));

                List<McpObservabilityBridge.McpMetrics> metrics =
                        observability(client).searchMetrics(Map.of(
                                "metricType", "bandwidth",
                                "assetType", "connection"));

                assertEquals(2, metrics.size());
                assertEquals("conn-uuid-1", metrics.get(0).getAssetId());
                assertEquals("conn-uuid-2", metrics.get(1).getAssetId());

                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                        .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_metrics")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.metricType", equalTo("bandwidth")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.assetType", equalTo("connection"))));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("searchCloudEvents posts search_cloud_events with filter arguments and returns raw JSON")
        void searchCloudEventsPostsSearchCloudEvents() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_cloud_events")))
                        .willReturn(okJson(loadFixture("/json/mcp/observability_search_cloud_events_result.json"))));

                JsonNode events = observability(client).searchCloudEvents(Map.of(
                        "type", "connection.state.changed"));

                assertNotNull(events);
                assertEquals(1, events.get("data").size());
                assertEquals("evt-1", events.get("data").get(0).get("id").asText());

                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                        .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_cloud_events")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.type", equalTo("connection.state.changed"))));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("listStreams posts list_streams with empty arguments and parses the data array")
        void listStreamsPostsListStreams() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("list_streams")))
                        .willReturn(okJson(loadFixture("/json/mcp/observability_list_streams_result.json"))));

                List<McpObservabilityBridge.McpStream> streams = observability(client).listStreams();

                assertEquals(2, streams.size());
                McpObservabilityBridge.McpStream s = streams.get(0);
                assertEquals("stream-uuid-1", s.getUuid());
                assertEquals("prod-telemetry", s.getName());
                assertEquals("TELEMETRY_STREAM", s.getType());
                assertEquals("ACTIVE", s.getState());

                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                        .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("list_streams"))));
            } finally {
                client.close();
            }
        }

        @Test
        @DisplayName("listAlertRules posts list_stream_alert_rules with the streamId argument")
        void listAlertRulesPostsListStreamAlertRules() throws Exception {
            resetStubs();
            Mcp client = newInitializedClient(0);
            try {
                wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("list_stream_alert_rules")))
                        .willReturn(okJson(loadFixture("/json/mcp/observability_list_alert_rules_result.json"))));

                JsonNode rules = observability(client).listAlertRules("stream-uuid-1");

                assertNotNull(rules);
                assertEquals(1, rules.get("data").size());
                assertEquals("rule-uuid-1", rules.get("data").get(0).get("uuid").asText());

                wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                        .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                        .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                        .withRequestBody(matchingJsonPath("$.params.name", equalTo("list_stream_alert_rules")))
                        .withRequestBody(matchingJsonPath("$.params.arguments.streamId", equalTo("stream-uuid-1"))));
            } finally {
                client.close();
            }
        }
    }
}
