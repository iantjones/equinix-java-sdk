package api.equinix.javasdk.internetaccess;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessPortJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link InternetAccessPortJson}.
 */
class InternetAccessPortDeserializationTest {

    private static ObjectMapper objectMapper;
    private static InternetAccessPortJson port;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = InternetAccessPortDeserializationTest.class
                .getResourceAsStream("/json/internetaccess/internet_access_port_response.json");
        assertNotNull(is, "internet_access_port_response.json fixture not found on classpath");
        port = objectMapper.readValue(is, InternetAccessPortJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertNotNull(port.getUuid());
    }

    @Test
    void name_isDeserialized() {
        assertNotNull(port.getName());
    }

    @Test
    void type_isDeserialized() {
        assertNotNull(port.getType());
    }

    @Test
    void speed_isDeserialized() {
        assertNotNull(port.getSpeed());
    }

    @Test
    void status_isDeserialized() {
        assertNotNull(port.getStatus());
    }

    @Test
    void ibx_isDeserialized() {
        assertNotNull(port.getIbx());
    }
}
