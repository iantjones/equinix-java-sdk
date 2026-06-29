package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.WorkVisitStatus;
import api.equinix.javasdk.customerportal.model.json.WorkVisitJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class WorkVisitDeserializationTest {

    private static ObjectMapper objectMapper;
    private static WorkVisitJson workVisit;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = WorkVisitDeserializationTest.class.getResourceAsStream("/json/customerportal/work_visit_response.json");
        assertNotNull(is, "work_visit_response.json fixture not found on classpath");
        workVisit = objectMapper.readValue(is, WorkVisitJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f80", workVisit.getUuid());
    }

    @Test
    void href_isDeserialized() {
        assertEquals("/v1/workVisits/d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f80", workVisit.getHref());
    }

    @Test
    void visitId_isDeserialized() {
        assertEquals("WV-2024-0012467", workVisit.getVisitId());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(WorkVisitStatus.CONFIRMED, workVisit.getStatus());
    }

    @Test
    void ibxCode_isDeserialized() {
        assertEquals("SV5", workVisit.getIbxCode());
    }

    @Test
    void accountNumber_isDeserialized() {
        assertEquals("128745", workVisit.getAccountNumber());
    }

    @Test
    void description_isDeserialized() {
        assertEquals("Quarterly hardware maintenance and firmware upgrade for rack servers in cabinet C-14", workVisit.getDescription());
    }

    @Test
    void visitorName_isDeserialized() {
        assertEquals("David Park", workVisit.getVisitorName());
    }

    @Test
    void visitorEmail_isDeserialized() {
        assertEquals("david.park@acmecloudservices.com", workVisit.getVisitorEmail());
    }

    @Test
    void createdDate_isDeserialized() {
        assertEquals("2024-11-08T11:30:00.000Z", workVisit.getCreatedDate());
    }
}
