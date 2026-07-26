package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.model.Agent;
import com.eqixiac.equinix.fabric.model.AgentActivity;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.core.ResponseStubs.*;
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

    @Nested
    @DisplayName("list()")
    class ListAgents {

        @Test
        @DisplayName("GETs /fabric/v4/agents and returns the paginated agents")
        void listsAgents() {
            stubPaginatedGet(wireMock, "/fabric/v4/agents", "/json/fabric/paginated_agents.json");

            PaginatedList<Agent> agents = fabric.agents().list();

            assertNotNull(agents);
            assertEquals(2, agents.size());
            assertEquals("d1f8c2a4-9b3e-4c7a-8f21-6e5d4c3b2a10", agents.get(0).getUuid());
            assertEquals("Second-Fabric-Agent", agents.get(1).getName());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/agents")));
        }
    }

    @Nested
    @DisplayName("getByUuid(uuid)")
    class GetByUuid {

        @Test
        @DisplayName("GETs /fabric/v4/agents/{uuid} and returns the agent")
        void getsAgentByUuid() {
            String uuid = "d1f8c2a4-9b3e-4c7a-8f21-6e5d4c3b2a10";
            stubSingleton(wireMock, "/fabric/v4/agents/" + uuid, "/json/fabric/agent_response.json");

            Agent agent = fabric.agents().getByUuid(uuid);

            assertNotNull(agent);
            assertEquals(uuid, agent.getUuid());
            assertEquals("My-Fabric-Agent", agent.getName());
            assertEquals("ANO_AGENT", agent.getType());

            assertNotNull(agent.getAgentTemplate());
            assertEquals("657400f8-c0af-430c-8216-43d44f08c1c5", agent.getAgentTemplate().getUuid());
            assertNotNull(agent.getConfiguration());
            assertEquals("Connection uuid is <connection_uuid>. Alert rule is <alert_rule_uuid>. Upgrade bandwidth to 10GB.",
                    agent.getConfiguration().getPrompt());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/agents/" + uuid)));
        }
    }

    @Nested
    @DisplayName("activities(agentId)")
    class Activities {

        @Test
        @DisplayName("GETs /fabric/v4/agents/{agentId}/activities and returns the activities")
        void listsActivities() {
            String agentId = "d1f8c2a4-9b3e-4c7a-8f21-6e5d4c3b2a10";
            stubPaginatedGet(wireMock, "/fabric/v4/agents/" + agentId + "/activities",
                    "/json/fabric/paginated_agent_activities.json");

            List<AgentActivity> activities = fabric.agents().activities(agentId);

            assertNotNull(activities);
            assertEquals(2, activities.size());
            assertEquals("act-1", activities.get(0).getUuid());
            assertEquals("COMPLETED", activities.get(0).getStatus());
            assertEquals("IN_PROGRESS", activities.get(1).getStatus());

            AgentActivity first = activities.get(0);
            assertNotNull(first.getAgent());
            assertEquals("d1f8c2a4-9b3e-4c7a-8f21-6e5d4c3b2a10", first.getAgent().getUuid());
            assertEquals("ANO_AGENT", first.getAgent().getType());

            assertNotNull(first.getMetadata());
            assertNotNull(first.getMetadata().getChatMessage());
            assertEquals(2, first.getMetadata().getChatMessage().getMessages().size());
            assertEquals("user", first.getMetadata().getChatMessage().getMessages().get(0).getType());
            assertEquals("The agent setup process has been successfully completed.",
                    first.getMetadata().getChatMessage().getMessages().get(1).getContent());
            assertNotNull(first.getMetadata().getToolCallInformation());
            assertEquals(1, first.getMetadata().getToolCallInformation().size());
            assertEquals("search_connections", first.getMetadata().getToolCallInformation().get(0).getName());
            assertTrue(first.getMetadata().getToolCallInformation().get(0).getInput().contains("/uuid"));
            assertTrue(first.getMetadata().getToolCallInformation().get(0).getResponse().contains("Primary Connection"));

            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/fabric/v4/agents/" + agentId + "/activities")));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        private static final String AGENT_ID = "d1f8c2a4-9b3e-4c7a-8f21-6e5d4c3b2a10";

        @Test
        @DisplayName("PATCHes a JSON Patch array as application/json-patch+json")
        void savePatchesNameAndEnabled() {
            stubSingleton(wireMock, "/fabric/v4/agents/" + AGENT_ID,
                    "/json/fabric/agent_response.json");
            wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/agents/" + AGENT_ID))
                    .willReturn(okJson(loadFixture("/json/fabric/agent_response.json"))));

            Agent agent = fabric.agents().getByUuid(AGENT_ID);
            Agent updated = agent.update().name("Renamed-Agent").enabled(false).save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/fabric/v4/agents/" + AGENT_ID))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Agent\"},"
                            + "{\"op\":\"replace\",\"path\":\"/enabled\",\"value\":false}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/agents/" + AGENT_ID,
                    "/json/fabric/agent_response.json");

            Agent agent = fabric.agents().getByUuid(AGENT_ID);
            assertThrows(IllegalStateException.class, () -> agent.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/agents/.*")));
        }
    }

    @Nested
    @DisplayName("Wrapper refresh()")
    class WrapperRefresh {

        private static final String AGENT_ID = "d1f8c2a4-9b3e-4c7a-8f21-6e5d4c3b2a10";
        private static final String URL = "/fabric/v4/agents/" + AGENT_ID;

        @Test
        @DisplayName("re-GETs /agents/{uuid} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("agent-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/agent_response.json")))
                    .willSetStateTo("renamed"));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("agent-refresh")
                    .whenScenarioStateIs("renamed")
                    .willReturn(okJson(loadFixture("/json/fabric/agent_response.json")
                            .replace("My-Fabric-Agent", "My-Fabric-Agent-Renamed"))));

            Agent agent = fabric.agents().getByUuid(AGENT_ID);
            assertEquals("My-Fabric-Agent", agent.getName());

            agent.refresh();

            assertEquals("My-Fabric-Agent-Renamed", agent.getName(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Wrapper delete()")
    class WrapperDelete {

        private static final String AGENT_ID = "d1f8c2a4-9b3e-4c7a-8f21-6e5d4c3b2a10";

        @Test
        @DisplayName("DELETEs /agents/{uuid} and returns true")
        void deletesAgent() {
            stubSingleton(wireMock, "/fabric/v4/agents/" + AGENT_ID,
                    "/json/fabric/agent_response.json");
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/agents/" + AGENT_ID))
                    .willReturn(okJson(loadFixture("/json/fabric/agent_response.json"))));

            Agent agent = fabric.agents().getByUuid(AGENT_ID);
            Boolean deleted = agent.delete();

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/fabric/v4/agents/" + AGENT_ID)));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/agents/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Agent not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.agents().getByUuid("invalid-uuid"));
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/agents/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.agents().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/agents/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.agents().getByUuid("test-uuid"));
        }
    }
}
