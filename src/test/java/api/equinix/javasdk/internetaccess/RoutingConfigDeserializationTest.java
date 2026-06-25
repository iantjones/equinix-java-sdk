package api.equinix.javasdk.internetaccess;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.internetaccess.model.json.RoutingConfigJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link RoutingConfigJson}.
 */
class RoutingConfigDeserializationTest {

    private static ObjectMapper objectMapper;
    private static RoutingConfigJson routingConfig;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = RoutingConfigDeserializationTest.class
                .getResourceAsStream("/json/internetaccess/routing_config_response.json");
        assertNotNull(is, "routing_config_response.json fixture not found on classpath");
        routingConfig = objectMapper.readValue(is, RoutingConfigJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertNotNull(routingConfig.getUuid());
    }

    @Test
    void type_isDeserialized() {
        assertNotNull(routingConfig.getType());
    }

    @Test
    void asn_isDeserialized() {
        assertNotNull(routingConfig.getAsn());
    }

    @Test
    void prefixes_isDeserialized() {
        assertNotNull(routingConfig.getPrefixes());
        assertFalse(routingConfig.getPrefixes().isEmpty());
    }

    @Test
    void status_isDeserialized() {
        assertNotNull(routingConfig.getStatus());
    }
}
