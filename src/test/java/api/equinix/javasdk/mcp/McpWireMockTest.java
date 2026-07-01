package api.equinix.javasdk.mcp;
import api.equinix.javasdk.Mcp;

import api.equinix.javasdk.core.TestFixtures;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.mcp.bridge.McpBridge;
import api.equinix.javasdk.mcp.bridge.McpCloudRouterBridge;
import api.equinix.javasdk.mcp.bridge.McpConnectionBridge;
import api.equinix.javasdk.mcp.bridge.McpMetroBridge;
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
}
