package api.equinix.javasdk.internetaccess;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessServiceJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link InternetAccessServiceJson}.
 */
class InternetAccessServiceDeserializationTest {

    private static ObjectMapper objectMapper;
    private static InternetAccessServiceJson service;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = InternetAccessServiceDeserializationTest.class
                .getResourceAsStream("/json/internetaccess/internet_access_service_response.json");
        assertNotNull(is, "internet_access_service_response.json fixture not found on classpath");
        service = objectMapper.readValue(is, InternetAccessServiceJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertNotNull(service.getUuid());
    }

    @Test
    void name_isDeserialized() {
        assertNotNull(service.getName());
    }

    @Test
    void type_isDeserialized() {
        assertNotNull(service.getType());
    }

    @Test
    void bandwidth_isDeserialized() {
        assertNotNull(service.getBandwidth());
    }

    @Test
    void ibx_isDeserialized() {
        assertNotNull(service.getIbx());
    }

    @Test
    void state_isDeserialized() {
        assertNotNull(service.getState());
    }
}
