package api.equinix.javasdk.core;

import api.equinix.javasdk.core.internal.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility for loading JSON test fixtures from the classpath.
 */
public final class TestFixtures {

    private TestFixtures() {}

    /**
     * Loads a fixture file from the classpath as a String.
     *
     * @param classpathResource path relative to classpath root, e.g. "/json/fabric/connection_response.json"
     * @return the file contents as a String
     */
    public static String load(String classpathResource) {
        try (InputStream is = TestFixtures.class.getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IllegalArgumentException("Fixture not found: " + classpathResource);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load fixture: " + classpathResource, e);
        }
    }

    /**
     * Loads a fixture file from the classpath as an InputStream.
     */
    public static InputStream loadStream(String classpathResource) {
        InputStream is = TestFixtures.class.getResourceAsStream(classpathResource);
        if (is == null) {
            throw new IllegalArgumentException("Fixture not found: " + classpathResource);
        }
        return is;
    }

    /**
     * Deserializes a fixture file into the specified type using the SDK's ObjectMapper.
     */
    public static <T> T deserialize(String classpathResource, Class<T> clazz) {
        try (InputStream is = loadStream(classpathResource)) {
            return Constants.mapper().readValue(is, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize fixture: " + classpathResource, e);
        }
    }
}
