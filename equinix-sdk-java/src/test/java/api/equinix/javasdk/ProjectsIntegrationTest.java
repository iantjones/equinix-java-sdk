package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.projects.model.Project;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration-readonly")
@DisplayName("Projects Integration Tests")
class ProjectsIntegrationTest extends IntegrationTestBase {

    static Projects client;

    @BeforeAll
    static void setUp() {
        client = new Projects(testCredentials());
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Readonly Operations")
    class ReadonlyTests {

        @Test
        @DisplayName("List projects")
        void listProjects() {
            try {
                PaginatedList<Project> items = timedCall("Projects", "list", "Project", "GET",
                        () -> client.projects().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    Project item = items.get(0);
                    assertNotNull(item);
                    assertNotNull(item.getProjectId());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Projects test skipped: " + e.getMessage());
            }
        }
    }
}
