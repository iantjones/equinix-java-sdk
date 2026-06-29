package api.equinix.javasdk.projects.wiremock;

import api.equinix.javasdk.Projects;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.projects.model.Project;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the read-only Projects domain
 * ({@code GET /resourceManager/v2/projects}).
 */
class ProjectsWireMockTest extends WireMockTestBase {

    static Projects projects;

    @BeforeAll
    static void setUp() {
        projects = new Projects(testCredentials());
        redirectToWireMock(projects);
        projects.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (projects != null) projects.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("list()")
    class ListProjects {

        @Test
        @DisplayName("returns a paginated list of projects")
        void returnsProjects() {
            stubPaginatedGet(wireMock, "/resourceManager/v2/projects",
                    "/json/projects/project_response.json");

            PaginatedList<Project> projectList = projects.projects().list();

            assertNotNull(projectList);
            assertEquals(2, projectList.size());
            Project first = projectList.get(0);
            assertEquals("1234", first.getProjectId());
            assertEquals("Default project", first.getProjectName());
            assertTrue(first.getInboxResource());
            assertEquals("5678", first.getParentOrganizationId());
            assertEquals("Network Edge", first.getLabels().get("application"));
            assertEquals(1, first.getPermissions().size());
            assertEquals("L2_CONNECTION", first.getPermissions().get(0).getResourceType());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/resourceManager/v2/projects")));
        }

        @Test
        @DisplayName("passes includePermissions and includeInbox query params")
        void passesQueryParams() {
            stubPaginatedGet(wireMock, "/resourceManager/v2/projects",
                    "/json/projects/project_response.json");

            projects.projects().list(true, false);

            wireMock.verify(getRequestedFor(urlPathEqualTo("/resourceManager/v2/projects"))
                    .withQueryParam("includePermissions", equalTo("true"))
                    .withQueryParam("includeInbox", equalTo("false")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/resourceManager/v2/projects.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> projects.projects().list());
        }
    }
}
