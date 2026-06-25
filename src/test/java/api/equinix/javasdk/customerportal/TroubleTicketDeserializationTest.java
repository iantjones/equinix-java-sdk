package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.TicketCategory;
import api.equinix.javasdk.customerportal.enums.TicketPriority;
import api.equinix.javasdk.customerportal.enums.TicketStatus;
import api.equinix.javasdk.customerportal.model.json.TroubleTicketJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class TroubleTicketDeserializationTest {

    private static ObjectMapper objectMapper;
    private static TroubleTicketJson troubleTicket;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = TroubleTicketDeserializationTest.class.getResourceAsStream("/json/customerportal/trouble_ticket_response.json");
        assertNotNull(is, "trouble_ticket_response.json fixture not found on classpath");
        troubleTicket = objectMapper.readValue(is, TroubleTicketJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f", troubleTicket.getUuid());
    }

    @Test
    void href_isDeserialized() {
        assertEquals("/v1/troubleTickets/c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f", troubleTicket.getHref());
    }

    @Test
    void ticketNumber_isDeserialized() {
        assertEquals("TT-2024-0034891", troubleTicket.getTicketNumber());
    }

    @Test
    void category_isDeserialized() {
        assertEquals(TicketCategory.CONNECTIVITY, troubleTicket.getCategory());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(TicketStatus.OPEN, troubleTicket.getStatus());
    }

    @Test
    void priority_isDeserialized() {
        assertEquals(TicketPriority.HIGH, troubleTicket.getPriority());
    }

    @Test
    void subject_isDeserialized() {
        assertEquals("Intermittent packet loss on cross-connect XC-1042", troubleTicket.getSubject());
    }

    @Test
    void description_isDeserialized() {
        assertEquals("Experiencing intermittent packet loss averaging 2.5% on cross-connect XC-1042 between cage SV5:01:000ABC and SV5:01:000DEF since 2024-11-10 03:00 UTC.", troubleTicket.getDescription());
    }

    @Test
    void ibxCode_isDeserialized() {
        assertEquals("SV5", troubleTicket.getIbxCode());
    }

    @Test
    void accountNumber_isDeserialized() {
        assertEquals("128745", troubleTicket.getAccountNumber());
    }

    @Test
    void requestorName_isDeserialized() {
        assertEquals("Robert Chen", troubleTicket.getRequestorName());
    }

    @Test
    void requestorEmail_isDeserialized() {
        assertEquals("robert.chen@acmecloudservices.com", troubleTicket.getRequestorEmail());
    }

    @Test
    void createdDate_isDeserialized() {
        assertEquals("2024-11-10T10:15:30.000Z", troubleTicket.getCreatedDate());
    }
}
