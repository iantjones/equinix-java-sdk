package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.AgentTemplate;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric AgentTemplates (read-only: list + get-by-uuid).
 *
 * <p>Endpoints (apiParams_Fabric.json &raquo; AgentTemplates, rootUri "agentTemplates"):
 * <ul>
 *   <li>GetAgentTemplates: GET  /fabric/v4/agentTemplates</li>
 *   <li>GetAgentTemplate:  GET  /fabric/v4/agentTemplates/{uuid}</li>
 * </ul>
 */
class FabricAgentTemplatesWireMockTest extends WireMockTestBase {

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
    @DisplayName("list()")
    class ListAgentTemplates {

        @Test
        @DisplayName("GETs /fabric/v4/agentTemplates and returns a paginated list")
        void returnsAgentTemplates() {
            stubPaginatedGet(wireMock, "/fabric/v4/agentTemplates", "/json/fabric/paginated_agent_templates.json");

            PaginatedList<AgentTemplate> templates = fabric.agentTemplates().list();

            assertNotNull(templates);
            assertEquals(2, templates.size());
            AgentTemplate first = templates.get(0);
            assertEquals("tmpl-1234", first.getUuid());
            assertEquals("Network-Operations-Template", first.getName());
            assertEquals("AGENT_TEMPLATE", first.getType());
            assertTrue(first.getEnabled());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/agentTemplates")));
        }
    }

    @Nested
    @DisplayName("getByUuid(uuid)")
    class GetAgentTemplate {

        @Test
        @DisplayName("GETs /fabric/v4/agentTemplates/{uuid} and returns the agent template")
        void returnsAgentTemplate() {
            stubSingleton(wireMock, "/fabric/v4/agentTemplates/tmpl-1234", "/json/fabric/agent_template_response.json");

            AgentTemplate template = fabric.agentTemplates().getByUuid("tmpl-1234");

            assertNotNull(template);
            assertEquals("tmpl-1234", template.getUuid());
            assertEquals("Network-Operations-Template", template.getName());
            assertEquals("ACTIVE", template.getState());
            assertNotNull(template.getChangeLog());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/agentTemplates/tmpl-1234")));
        }
    }
}
