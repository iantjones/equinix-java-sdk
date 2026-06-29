package api.equinix.javasdk.mcp;
import api.equinix.javasdk.Mcp;

import api.equinix.javasdk.core.TestFixtures;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.mcp.bridge.McpBridge;
import api.equinix.javasdk.mcp.bridge.McpConnectionBridge;
import api.equinix.javasdk.mcp.bridge.McpMetroBridge;
import api.equinix.javasdk.mcp.model.McpToolDefinition;
import api.equinix.javasdk.mcp.model.McpToolResult;
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
}
