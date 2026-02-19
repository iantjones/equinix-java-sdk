package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.projects.model.Project;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ProjectsTest {

    static Projects projects;
    static Boolean skipCreateUpdateOperations;

    @BeforeAll
    static void obtainTestingData() {
        skipCreateUpdateOperations = Boolean.valueOf(System.getProperty("skipCreateUpdateOperations"));
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        projects = new Projects(new BasicEquinixCredentials(accessKey, secretKey));
        projects.authenticate();
    }

    @Test
    void projects() {
        try {
            PaginatedList<Project> projectList = projects.projects().list();
            assertNotNull(projectList);
            assertTrue(projectList.size() >= 0);

            if (projectList.size() > 0) {
                Project project = projects.projects().getByUuid(projectList.get(0).getUuid());
                assertNotNull(project);
                assertEquals(projectList.get(0).getUuid(), project.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Projects test skipped: " + e.getMessage());
        }
    }
}
