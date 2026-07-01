package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.fabric.model.Agent;
import org.junit.jupiter.api.*;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Agents.
 * Tests the create-from-builder (define().create()) path and asserts the POST request body.
 */
class FabricAgentsWireMockTest extends WireMockTestBase {

    static Fabric fabric;

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
    @DisplayName("define(type).create()")
    class Create {

        @Test
        @DisplayName("POSTs the agent creator body to /fabric/v4/agents and returns the created agent")
        void createsAgent() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/agents"))
                    .willReturn(okJson(loadFixture("/json/fabric/agent_response.json"))));

            Agent agent = fabric.agents()
                    .define("ANO_AGENT")
                    .name("My-Fabric-Agent")
                    .description("Automated network operations agent")
                    .enabled(true)
                    .agentTemplate("tmpl-1234")
                    .configuration(Map.of("region", "us-east-1"))
                    .create();

            assertNotNull(agent);
            assertEquals("d1f8c2a4-9b3e-4c7a-8f21-6e5d4c3b2a10", agent.getUuid());
            assertEquals("My-Fabric-Agent", agent.getName());
            assertEquals("ANO_AGENT", agent.getType());
            assertTrue(agent.getEnabled());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/agents"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("ANO_AGENT")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("My-Fabric-Agent")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Automated network operations agent")))
                    .withRequestBody(matchingJsonPath("$.enabled", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.agentTemplate.uuid", equalTo("tmpl-1234")))
                    .withRequestBody(matchingJsonPath("$.configuration.region", equalTo("us-east-1"))));
        }

        @Test
        @DisplayName("omits null optional fields from the request body (NON_NULL)")
        void createsAgentWithMinimalBody() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/agents"))
                    .willReturn(okJson(loadFixture("/json/fabric/agent_response.json"))));

            Agent agent = fabric.agents().define("ANO_AGENT").create();

            assertNotNull(agent);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/agents"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("ANO_AGENT")))
                    .withRequestBody(equalToJson("{\"type\":\"ANO_AGENT\"}", true, true)));
        }
    }
}
