package api.equinix.javasdk.projects.wiremock;

import api.equinix.javasdk.Projects;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Projects domain.
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
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns project for valid UUID")
        void returnsProject() {
            stubSingleton(wireMock, "/projects/v2/projects/prj-f1e2d3c4-b5a6-9807-fedc-ba9876543210",
                    "/json/projects/project_response.json");

            var project = projects.projects().getByUuid("prj-f1e2d3c4-b5a6-9807-fedc-ba9876543210");
            assertNotNull(project);
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/projects/v2/projects/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Project not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> projects.projects().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/projects/v2/projects/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> projects.projects().getByUuid("test-uuid"));
        }
    }
}
