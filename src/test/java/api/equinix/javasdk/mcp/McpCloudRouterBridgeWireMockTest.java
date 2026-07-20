package api.equinix.javasdk.mcp;

import api.equinix.javasdk.Mcp;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.mcp.bridge.McpBridge;
import api.equinix.javasdk.mcp.bridge.McpCloudRouterBridge;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based read/search coverage for {@link McpCloudRouterBridge}.
 *
 * <p>Mirrors the setup in {@code McpWireMockTest}: each test builds a fresh, already-initialized
 * {@link Mcp} client wired to WireMock so scenario/token state never bleeds across tests. Every
 * bridge read method maps to a JSON-RPC 2.0 {@code tools/call} against the Fabric endpoint; the
 * tests assert both the typed parse of the result and the exact serialized request envelope
 * (jsonrpc / method / params.name / params.arguments).</p>
 */
@Tag("wiremock")
class McpCloudRouterBridgeWireMockTest extends WireMockTestBase {

    /**
     * Builds a fresh, already-initialized {@link Mcp} client pointed at WireMock's Fabric endpoint.
     * initialize() performs a JSON-RPC handshake, which is stubbed here.
     */
    private static Mcp newInitializedClient() {
        McpClientConfig config = McpClientConfig.builder()
                .fabricEndpoint(wireMockUrl() + "/mcp/fabric")
                .tokenEndpoint(wireMockUrl() + "/oauth2/v1/token")
                .connectTimeoutMs(5000)
                .readTimeoutMs(5000)
                .maxRetries(0)
                .build();

        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("initialize")))
                .willReturn(okJson(loadFixture("/json/mcp/initialize_response.json"))));

        Mcp client = new Mcp(testCredentials(), config);
        client.initialize();
        return client;
    }

    @Test
    @DisplayName("searchRouters posts search_routers with filter args and parses the data array")
    void searchRoutersPostsSearchRouters() throws Exception {
        resetStubs();
        Mcp client = newInitializedClient();
        try {
            wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                    .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_routers")))
                    .willReturn(okJson(loadFixture("/json/mcp/search_router_result.json"))));

            McpCloudRouterBridge bridge = new McpBridge(client).cloudRouters();
            List<McpCloudRouterBridge.McpCloudRouter> routers =
                    bridge.searchRouters(Map.of("metroCode", "SV", "state", "PROVISIONED"));

            assertEquals(2, routers.size());
            McpCloudRouterBridge.McpCloudRouter first = routers.get(0);
            assertEquals("router-uuid-1", first.getUuid());
            assertEquals("prod-cr-sv", first.getName());
            assertEquals("PROVISIONED", first.getState());
            assertEquals("STANDARD", first.getPackageType());
            assertEquals("SV", first.getMetroCode());
            assertEquals(4, first.getConnectionsCount());

            // JSON-RPC 2.0 envelope: tools/call with tool name search_routers and the filters
            // passed straight through as the arguments object.
            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                    .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                    .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                    .withRequestBody(matchingJsonPath("$.params.name", equalTo("search_routers")))
                    .withRequestBody(matchingJsonPath("$.params.arguments.metroCode", equalTo("SV")))
                    .withRequestBody(matchingJsonPath("$.params.arguments.state", equalTo("PROVISIONED"))));
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("getRouterPackage posts list_router_packages with routerPackageCode arg")
    void getRouterPackagePostsListRouterPackages() throws Exception {
        resetStubs();
        Mcp client = newInitializedClient();
        try {
            wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                    .withRequestBody(matchingJsonPath("$.params.name", equalTo("list_router_packages")))
                    .willReturn(okJson(loadFixture("/json/mcp/router_package_result.json"))));

            McpCloudRouterBridge bridge = new McpBridge(client).cloudRouters();
            JsonNode pkg = bridge.getRouterPackage("STANDARD");

            assertEquals("STANDARD", pkg.get("code").asText());
            assertEquals("Standard Cloud Router", pkg.get("name").asText());
            assertEquals(4000, pkg.get("totalIPv4RoutesMax").asInt());

            // The single package code is mapped to the routerPackageCode argument key.
            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                    .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                    .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                    .withRequestBody(matchingJsonPath("$.params.name", equalTo("list_router_packages")))
                    .withRequestBody(matchingJsonPath("$.params.arguments.routerPackageCode", equalTo("STANDARD"))));
        } finally {
            client.close();
        }
    }
}
