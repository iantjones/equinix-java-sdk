package api.equinix.javasdk.core;

import api.equinix.javasdk.core.util.ResourceFileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ResourceFileUtils} contract: a missing classpath resource fails fast with the resource
 * name in the message (it must never silently return {@code null} — that surfaced as opaque
 * NPEs during client bootstrap).
 */
class ResourceFileUtilsTest {

    @Test
    void loadsAnExistingResource() throws Exception {
        JsonNode node = ResourceFileUtils.loadResourceFileJson("json/apiParams_Core.json");
        assertNotNull(node);
        assertTrue(node.has("functionalAreas"));
    }

    @Test
    void missingResourceThrowsNamingTheFile() {
        FileNotFoundException fnf = assertThrows(FileNotFoundException.class,
                () -> ResourceFileUtils.loadResourceFileJson("json/apiParams_DoesNotExist.json"));
        assertTrue(fnf.getMessage().contains("json/apiParams_DoesNotExist.json"),
                "the failure must name the missing resource, got: " + fnf.getMessage());
    }
}
