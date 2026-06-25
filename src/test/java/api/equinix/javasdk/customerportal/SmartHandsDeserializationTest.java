package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.SmartHandsStatus;
import api.equinix.javasdk.customerportal.enums.SmartHandsType;
import api.equinix.javasdk.customerportal.model.json.SmartHandsJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class SmartHandsDeserializationTest {

    private static ObjectMapper objectMapper;
    private static SmartHandsJson smartHands;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = SmartHandsDeserializationTest.class.getResourceAsStream("/json/customerportal/smart_hands_response.json");
        assertNotNull(is, "smart_hands_response.json fixture not found on classpath");
        smartHands = objectMapper.readValue(is, SmartHandsJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8091", smartHands.getUuid());
    }

    @Test
    void href_isDeserialized() {
        assertEquals("/v1/smartHands/e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8091", smartHands.getHref());
    }

    @Test
    void requestId_isDeserialized() {
        assertEquals("SH-2024-0008923", smartHands.getRequestId());
    }

    @Test
    void type_isDeserialized() {
        assertEquals(SmartHandsType.STANDARD, smartHands.getType());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(SmartHandsStatus.IN_PROGRESS, smartHands.getStatus());
    }

    @Test
    void ibxCode_isDeserialized() {
        assertEquals("SV5", smartHands.getIbxCode());
    }

    @Test
    void accountNumber_isDeserialized() {
        assertEquals("128745", smartHands.getAccountNumber());
    }

    @Test
    void summary_isDeserialized() {
        assertEquals("Install Cat6A patch cables in cabinet C-14", smartHands.getSummary());
    }

    @Test
    void description_isDeserialized() {
        assertEquals("Install 12x Cat6A patch cables from patch panel PP-A-1042 ports 1-12 to ToR switch ports Eth1/1-1/12 in cabinet C-14. Label both ends per standard naming convention.", smartHands.getDescription());
    }

    @Test
    void requestorName_isDeserialized() {
        assertEquals("Robert Chen", smartHands.getRequestorName());
    }

    @Test
    void createdDate_isDeserialized() {
        assertEquals("2024-11-11T09:45:00.000Z", smartHands.getCreatedDate());
    }
}
