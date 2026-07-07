package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.customerportal.enums.OrderContactType;
import api.equinix.javasdk.customerportal.enums.TicketNoteType;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.TicketStatus;
import api.equinix.javasdk.customerportal.model.implementation.TicketContact;
import api.equinix.javasdk.customerportal.model.implementation.TicketNote;
import api.equinix.javasdk.customerportal.model.implementation.TicketResolution;
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
        objectMapper = Constants.mapper();
        InputStream is = TroubleTicketDeserializationTest.class.getResourceAsStream("/json/customerportal/trouble_ticket_response.json");
        assertNotNull(is, "trouble_ticket_response.json fixture not found on classpath");
        troubleTicket = objectMapper.readValue(is, TroubleTicketJson.class);
    }

    @Test
    void id_isDeserialized() {
        assertEquals("1-9808089098", troubleTicket.getId());
    }

    @Test
    void category_isDeserialized() {
        assertEquals("Network", troubleTicket.getCategory());
    }

    @Test
    void subCategory_isDeserialized() {
        assertEquals("NE Connectivity Issue", troubleTicket.getSubCategory());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(TicketStatus.IN_PROGRESS, troubleTicket.getStatus());
    }

    @Test
    void description_isDeserialized() {
        assertEquals("Experiencing intermittent packet loss averaging 2.5% on cross-connect XC-1042 between cage SV5:01:000ABC and SV5:01:000DEF since 2024-11-10 03:00 UTC.", troubleTicket.getDescription());
    }

    @Test
    void primaryId_isDeserialized() {
        assertEquals("SV5:01:000ABC", troubleTicket.getPrimaryId());
    }

    @Test
    void secondaryId_isDeserialized() {
        assertEquals("SV5:01:000ABC:001", troubleTicket.getSecondaryId());
    }

    @Test
    void customerReferenceId_isDeserialized() {
        assertEquals("REF-9981", troubleTicket.getCustomerReferenceId());
    }

    @Test
    void occurredDateTime_isDeserialized() {
        assertEquals("2024-11-10T03:00:00Z", troubleTicket.getOccurredDateTime());
    }

    @Test
    void resolutionDateTime_isDeserialized() {
        assertEquals("2024-11-12T14:22:18Z", troubleTicket.getResolutionDateTime());
    }

    @Test
    void details_areDeserializedAsFreeFormMap() {
        assertNotNull(troubleTicket.getDetails());
        assertEquals(2, troubleTicket.getDetails().size());
        assertEquals(Boolean.FALSE, troubleTicket.getDetails().get("callFromCage"));
        assertEquals("ANYTIME", troubleTicket.getDetails().get("availability"));
    }

    @Test
    void resolutions_areDeserialized() {
        assertEquals(1, troubleTicket.getResolutions().size());
        TicketResolution resolution = troubleTicket.getResolutions().get(0);
        assertEquals("5-2000000000", resolution.getId());
        assertEquals("Trouble", resolution.getType());
        assertEquals("Open - Dispatch", resolution.getStatus());
        assertEquals("40.00", resolution.getPrice());
        assertEquals("10", resolution.getHours());
    }

    @Test
    void notes_areDeserialized() {
        assertEquals(1, troubleTicket.getNotes().size());
        TicketNote note = troubleTicket.getNotes().get(0);
        assertEquals("1-ABCDE6IS", note.getId());
        assertEquals("Provide more details to understand the issue.", note.getText());
        assertEquals("Equinix Support", note.getAuthor());
        assertEquals("5-2000000000", note.getReferenceId());
        assertEquals(TicketNoteType.TECHNICIAN_QUERY, note.getType());
        assertEquals(1, note.getAttachments().size());
        assertEquals("c77c5f58-a7ea-4e88-9fc4-1a2900027425", note.getAttachments().get(0).getId());
        assertEquals("error-log", note.getAttachments().get(0).getName());
    }

    @Test
    void attachments_areDeserialized() {
        assertEquals(1, troubleTicket.getAttachments().size());
        assertEquals("c77c5f58-a7ea-4e88-9fc4-1a2900027425", troubleTicket.getAttachments().get(0).getId());
        assertEquals("error-log", troubleTicket.getAttachments().get(0).getName());
    }

    @Test
    void contacts_areDeserialized() {
        assertEquals(2, troubleTicket.getContacts().size());
        TicketContact registered = troubleTicket.getContacts().get(0);
        assertEquals("john_doe", registered.getRegisteredUser());
        assertEquals(OrderContactType.NOTIFICATION, registered.getType());

        TicketContact nonRegistered = troubleTicket.getContacts().get(1);
        assertEquals("John", nonRegistered.getFirstName());
        assertEquals("Doe", nonRegistered.getLastName());
        assertEquals(OrderContactType.TECHNICAL, nonRegistered.getType());
        assertEquals(2, nonRegistered.getDetails().size());
        assertEquals("EMAIL", nonRegistered.getDetails().get(0).getType());
        assertEquals("john.doe@example.com", nonRegistered.getDetails().get(0).getValue());
    }
}
