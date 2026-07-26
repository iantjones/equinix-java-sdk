package com.eqixiac.equinix.fabric.wiremock;
import com.eqixiac.equinix.fabric.enums.AgentState;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.model.AgentTemplate;
import org.junit.jupiter.api.*;

import static com.eqixiac.equinix.core.ResponseStubs.*;
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
            assertEquals(AgentState.PROVISIONED, template.getState());
            assertNotNull(template.getChangeLog());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/agentTemplates/tmpl-1234")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/agentTemplates/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Agent template not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.agentTemplates().getByUuid("invalid-uuid"));
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/agentTemplates.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.agentTemplates().list());
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/agentTemplates.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.agentTemplates().list());
        }
    }
}
