package api.equinix.javasdk.projects;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.projects.model.json.ProjectJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link ProjectJson}.
 */
class ProjectDeserializationTest {

    private static ObjectMapper objectMapper;
    private static ProjectJson project;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.mapper();
        InputStream is = ProjectDeserializationTest.class
                .getResourceAsStream("/json/projects/project_single.json");
        assertNotNull(is, "project_single.json fixture not found on classpath");
        project = objectMapper.readValue(is, ProjectJson.class);
    }

    @Test
    void projectId_isDeserialized() {
        assertEquals("1234", project.getProjectId());
    }

    @Test
    void projectName_isDeserialized() {
        assertEquals("Default project", project.getProjectName());
    }

    @Test
    void inboxResource_isDeserialized() {
        assertTrue(project.getInboxResource());
    }

    @Test
    void parentOrganizationId_isDeserialized() {
        assertEquals("5678", project.getParentOrganizationId());
    }

    @Test
    void labels_isDeserialized() {
        assertNotNull(project.getLabels());
        assertEquals("Network Edge", project.getLabels().get("application"));
    }

    @Test
    void silentProject_isDeserialized() {
        assertFalse(project.getSilentProject());
    }

    @Test
    void permissions_isDeserialized() {
        assertNotNull(project.getPermissions());
        assertEquals(1, project.getPermissions().size());
        assertEquals("L2_CONNECTION", project.getPermissions().get(0).getResourceType());
        assertTrue(project.getPermissions().get(0).getActions().contains("resourcemanager.project.read"));
    }
}
