package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.CrossConnectStatus;
import api.equinix.javasdk.customerportal.enums.CrossConnectType;
import api.equinix.javasdk.customerportal.enums.MediaType;
import api.equinix.javasdk.customerportal.model.json.CrossConnectJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class CrossConnectDeserializationTest {

    private static ObjectMapper objectMapper;
    private static CrossConnectJson crossConnect;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = CrossConnectDeserializationTest.class.getResourceAsStream("/json/customerportal/cross_connect_response.json");
        assertNotNull(is, "cross_connect_response.json fixture not found on classpath");
        crossConnect = objectMapper.readValue(is, CrossConnectJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", crossConnect.getUuid());
    }

    @Test
    void href_isDeserialized() {
        assertEquals("/v1/crossConnects/a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", crossConnect.getHref());
    }

    @Test
    void name_isDeserialized() {
        assertEquals("Primary-DB-CrossConnect-SV5", crossConnect.getName());
    }

    @Test
    void type_isDeserialized() {
        assertEquals(CrossConnectType.STANDARD, crossConnect.getType());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(CrossConnectStatus.ACTIVE, crossConnect.getStatus());
    }

    @Test
    void mediaType_isDeserialized() {
        assertEquals(MediaType.SINGLE_MODE_FIBER, crossConnect.getMediaType());
    }

    @Test
    void aEndIbx_isDeserialized() {
        assertEquals("SV5", crossConnect.getAEndIbx());
    }

    @Test
    void zEndIbx_isDeserialized() {
        assertEquals("SV5", crossConnect.getZEndIbx());
    }

    @Test
    void aEndCageId_isDeserialized() {
        assertEquals("SV5:01:000ABC", crossConnect.getAEndCageId());
    }

    @Test
    void zEndCageId_isDeserialized() {
        assertEquals("SV5:01:000DEF", crossConnect.getZEndCageId());
    }

    @Test
    void bandwidth_isDeserialized() {
        assertEquals(10000, crossConnect.getBandwidth());
    }

    @Test
    void accountNumber_isDeserialized() {
        assertEquals("128745", crossConnect.getAccountNumber());
    }

    @Test
    void createdDate_isDeserialized() {
        assertEquals("2024-03-01T14:22:35.000Z", crossConnect.getCreatedDate());
    }
}
