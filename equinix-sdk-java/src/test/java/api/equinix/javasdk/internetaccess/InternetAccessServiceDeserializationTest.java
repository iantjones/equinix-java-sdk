package api.equinix.javasdk.internetaccess;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.internetaccess.enums.ServiceState;
import api.equinix.javasdk.internetaccess.enums.ServiceTypeV2;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessServiceJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link InternetAccessServiceJson}, the read-only {@code ServiceV2}
 * response returned when creating an Equinix Internet Access (EIA) v2 service.
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
        assertEquals("e1f2a3b4-c5d6-4e7f-8091-021324354657", service.getUuid());
    }

    @Test
    void type_isDeserialized() {
        assertEquals(ServiceTypeV2.SINGLE, service.getType());
    }

    @Test
    void bandwidth_isDeserialized() {
        assertEquals(Integer.valueOf(1000), service.getBandwidth());
    }

    @Test
    void state_isDeserialized() {
        assertEquals(ServiceState.ACTIVE, service.getState());
    }
}
