package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.projects.model.Project;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Live integration tests for the Projects domain of the Equinix Java SDK, catalog-complete against
 * the {@code getprojectsv2.yaml} spec ({@code GET /resourceManager/v2/projects}, operationId
 * {@code getAllProjects} — the spec's only operation; the resource is read-only).
 *
 * <p>Spec-vs-reality contract: every call runs through
 * {@code IntegrationTestBase.requireEntitled}, which skips only on a 401/403 entitlement gap and
 * fails on any other defect (deserialization crash, 5xx, unmapped enum).</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * mvn test -Pintegration-readonly -DaccessKey=ID -DsecretKey=SECRET
 * </pre>
 */
@Tag("integration-readonly")
@DisplayName("Projects Integration Tests")
class ProjectsIntegrationTest extends IntegrationTestBase {

    static Projects projects;

    @BeforeAll
    static void setUpProjects() {
        projects = new Projects(testCredentials());
    }

    // ════════════════════════════════════════════════════════════════════
    //  READONLY TESTS - Safe GET/list operations
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Projects Read-Only Tests")
    class ReadonlyTests {

        @Test
        @DisplayName("projects_list - List all projects (getAllProjects)")
        void projects_list() {
            PaginatedList<Project> items = requireEntitled("Projects", "getAllProjects", "Project", "GET",
                    () -> projects.projects().list());
            assertNotNull(items);

            if (!items.isEmpty()) {
                Project first = items.get(0);
                assertNotNull(first);
                assertNotNull(first.getProjectId(), "getAllProjects returned a project without a projectId");
                // Touch further getters to force deserialization of the full item shape.
                first.getProjectName();
                first.getParentOrganizationId();
                first.getLabels();
                first.getSilentProject();
            }
        }

        @Test
        @DisplayName("projects_list_withFlags - List projects with includePermissions + includeInbox (getAllProjects)")
        void projects_list_withFlags() {
            PaginatedList<Project> items = requireEntitled("Projects", "getAllProjects", "Project", "GET",
                    () -> projects.projects().list(true, true));
            assertNotNull(items);

            if (!items.isEmpty()) {
                Project first = items.get(0);
                assertNotNull(first);
                assertNotNull(first.getProjectId(), "getAllProjects returned a project without a projectId");
                // includePermissions=true should populate the permissions view; the spec does not
                // guarantee it per project, so touch (deserialize) without asserting presence.
                first.getPermissions();
                first.getInboxResource();
            }
        }
    }
}
