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
        objectMapper = Constants.objectMapper;
        InputStream is = ProjectDeserializationTest.class
                .getResourceAsStream("/json/projects/project_response.json");
        assertNotNull(is, "project_response.json fixture not found on classpath");
        project = objectMapper.readValue(is, ProjectJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertNotNull(project.getUuid());
    }

    @Test
    void name_isDeserialized() {
        assertNotNull(project.getName());
    }

    @Test
    void description_isDeserialized() {
        assertNotNull(project.getDescription());
    }

    @Test
    void status_isDeserialized() {
        assertNotNull(project.getStatus());
    }

    @Test
    void createdDate_isDeserialized() {
        assertNotNull(project.getCreatedDate());
    }

    @Test
    void updatedDate_isDeserialized() {
        assertNotNull(project.getUpdatedDate());
    }
}
