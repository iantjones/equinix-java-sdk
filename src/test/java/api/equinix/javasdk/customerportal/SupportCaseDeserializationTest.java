package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.CasePriority;
import api.equinix.javasdk.customerportal.enums.CaseStatus;
import api.equinix.javasdk.customerportal.model.json.SupportCaseJson;
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
        objectMapper = Constants.objectMapper;
        InputStream is = SupportCaseDeserializationTest.class.getResourceAsStream("/json/customerportal/support_case_response.json");
        assertNotNull(is, "support_case_response.json fixture not found on classpath");
        supportCase = objectMapper.readValue(is, SupportCaseJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("c9d0e1f2-a3b4-4c5d-6e7f-809102132435", supportCase.getUuid());
    }

    @Test
    void subject_isDeserialized() {
        assertEquals("Request for additional power capacity in cabinet C-14", supportCase.getSubject());
    }

    @Test
    void description_isDeserialized() {
        assertEquals("Requesting upgrade from 10kW to 15kW power allocation for cabinet C-14 in cage SV5:01:000ABC to support new server deployment.", supportCase.getDescription());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(CaseStatus.OPEN, supportCase.getStatus());
    }

    @Test
    void priority_isDeserialized() {
        assertEquals(CasePriority.MEDIUM, supportCase.getPriority());
    }

    @Test
    void caseNumber_isDeserialized() {
        assertEquals("CS-2024-0042156", supportCase.getCaseNumber());
    }

    @Test
    void createdDate_isDeserialized() {
        assertEquals("2024-11-09T13:00:00.000Z", supportCase.getCreatedDate());
    }
}
