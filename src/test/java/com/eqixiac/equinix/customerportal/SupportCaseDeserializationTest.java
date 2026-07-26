package com.eqixiac.equinix.customerportal;

import com.eqixiac.equinix.customerportal.enums.OrderProductType;
import com.eqixiac.equinix.customerportal.enums.SupportCaseNoteType;
import com.eqixiac.equinix.customerportal.enums.OrderContactType;
import com.eqixiac.equinix.customerportal.enums.Channel;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.customerportal.enums.AttachmentSource;
import com.eqixiac.equinix.customerportal.enums.SupportCaseStatus;
import com.eqixiac.equinix.customerportal.model.json.SupportCaseJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class SupportCaseDeserializationTest {

    private static ObjectMapper objectMapper;
    private static SupportCaseJson supportCase;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.mapper();
        InputStream is = SupportCaseDeserializationTest.class.getResourceAsStream("/json/customerportal/support_case_response.json");
        assertNotNull(is, "support_case_response.json fixture not found on classpath");
        supportCase = objectMapper.readValue(is, SupportCaseJson.class);
    }

    @Test
    void scalars_areDeserialized() {
        assertEquals("11150929", supportCase.getId());
        assertEquals("182389736", supportCase.getAccountNumber());
        assertEquals("VjT Existing Reseller New BA", supportCase.getAccountName());
        assertEquals("REF-44211", supportCase.getCustomerReferenceId());
        assertEquals(Channel.PORTAL, supportCase.getChannel());
        assertEquals("1-204976070710", supportCase.getOrderId());
        assertEquals(SupportCaseStatus.IN_PROGRESS, supportCase.getStatus());
        assertEquals("2024-11-09T13:00:00Z", supportCase.getCreatedDateTime());
    }

    @Test
    void location_isDeserialized() {
        assertEquals(2, supportCase.getLocation().getIbx().size());
        assertEquals("SV5", supportCase.getLocation().getIbx().get(0));
    }

    @Test
    void contacts_areDeserialized() {
        assertEquals(1, supportCase.getContacts().size());
        assertEquals("John", supportCase.getContacts().get(0).getFirstName());
        assertEquals(OrderContactType.NOTIFICATION, supportCase.getContacts().get(0).getType());
        assertEquals("EMAIL", supportCase.getContacts().get(0).getDetails().get(0).getType());
        assertEquals("SomeContact@email.com", supportCase.getContacts().get(0).getDetails().get(0).getValue());
    }

    @Test
    void notes_areDeserialized() {
        assertEquals(1, supportCase.getNotes().size());
        assertEquals("note-1", supportCase.getNotes().get(0).getId());
        assertEquals(SupportCaseNoteType.RESOLUTION_NOTES, supportCase.getNotes().get(0).getType());
    }

    @Test
    void attachments_areDeserialized() {
        assertEquals(1, supportCase.getAttachments().size());
        assertEquals("c77c5f58-a7ea-4e88-9fc4-1a2900027425", supportCase.getAttachments().get(0).getId());
        assertEquals(AttachmentSource.CUSTOMER, supportCase.getAttachments().get(0).getSource());
    }

    @Test
    void email_isDeserialized() {
        assertEquals(1, supportCase.getEmail().size());
        assertEquals("Ali bin Carud", supportCase.getEmail().get(0).getAuthorName());
        assertEquals("abc@equinix.com", supportCase.getEmail().get(0).getFromAddress());
        assertEquals("xyz@example.com", supportCase.getEmail().get(0).getToAddress());
    }

    @Test
    void otherDetails_isDeserialized() {
        assertEquals("Trouble Services", supportCase.getOtherDetails().getCategory());
        assertEquals("Incident", supportCase.getOtherDetails().getSubCategory());
        assertEquals(1, supportCase.getOtherDetails().getStatusHistory().size());
        assertEquals(SupportCaseStatus.SUBMITTED, supportCase.getOtherDetails().getStatusHistory().get(0).getStatus());
        assertEquals(1, supportCase.getOtherDetails().getDetails().size());
        assertEquals(OrderProductType.SMART_HANDS, supportCase.getOtherDetails().getDetails().get(0).getProductType());
        assertTrue(supportCase.getOtherDetails().getCancellable());
        assertFalse(supportCase.getOtherDetails().getModifiable());
    }
}
