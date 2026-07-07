package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.customerportal.enums.ReportScheduleStatus;
import api.equinix.javasdk.customerportal.enums.ReportPeriod;
import api.equinix.javasdk.customerportal.enums.ReportScheduleType;
import api.equinix.javasdk.customerportal.enums.ReportStatus;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.FileType;
import api.equinix.javasdk.customerportal.model.json.ReportJson;
import api.equinix.javasdk.customerportal.model.json.ScheduledReportJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ReportDeserializationTest {

    private static ObjectMapper objectMapper;
    private static ReportJson report;
    private static ScheduledReportJson scheduledReport;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.mapper();

        InputStream reportIs = ReportDeserializationTest.class.getResourceAsStream("/json/customerportal/report_response.json");
        assertNotNull(reportIs, "report_response.json fixture not found on classpath");
        report = objectMapper.readValue(reportIs, ReportJson.class);

        InputStream scheduledIs = ReportDeserializationTest.class.getResourceAsStream("/json/customerportal/scheduled_report_response.json");
        assertNotNull(scheduledIs, "scheduled_report_response.json fixture not found on classpath");
        scheduledReport = objectMapper.readValue(scheduledIs, ScheduledReportJson.class);
    }

    @Test
    void report_coreFields() {
        assertEquals("8f204c59-f70f-437b-92d5-dbbde2932de5", report.getReportId());
        assertEquals("886fc5de-6304-46c7-902c-d946c656c169", report.getScheduledId());
        assertEquals("power-usage.xls", report.getFileName());
        assertEquals(FileType.XLS, report.getFileType());
        assertEquals(95806395L, report.getFileSize());
        assertEquals(ReportStatus.SUCCESS, report.getStatus());
        assertEquals(1, report.getNumberOfDownloads());
    }

    @Test
    void report_detailFields() {
        assertEquals("john.doe", report.getCreatedBy());
        assertEquals("2024-08-28T13:30:00.000Z", report.getStartTime());
        assertEquals("2024-08-28T13:34:33.735Z", report.getEndTime());
        assertEquals(1, report.getNumberOfAttempts());
        assertNotNull(report.getParameters());
        assertEquals("metro", report.getParameters().get(0).getName());
    }

    @Test
    void scheduledReport_fields() {
        assertEquals("806f281c-6295-465e-bd41-04559d4d4960", scheduledReport.getScheduledId());
        assertEquals("my_report", scheduledReport.getReportName());
        assertEquals(ReportScheduleType.WEEKLY, scheduledReport.getScheduleType());
        assertEquals(ReportPeriod.DAYS_30, scheduledReport.getPeriod());
        assertEquals("john.doe", scheduledReport.getCreatedBy());
        assertEquals(44010, scheduledReport.getCustomerOrganizationId());
        assertEquals(ReportScheduleStatus.ACTIVE, scheduledReport.getStatus());
        assertEquals(0, scheduledReport.getNumberOfFailedAttempts());
    }

    @Test
    void scheduledReport_nestedReports() {
        assertNotNull(scheduledReport.getReports());
        assertEquals(1, scheduledReport.getReports().size());
        assertEquals("71df1306-94f9-4cbd-b6b0-735098bf7f87", scheduledReport.getReports().get(0).getReportId());
        assertNotNull(scheduledReport.getParameters());
        assertEquals("user_key", scheduledReport.getParameters().get(0).getName());
        assertTrue(scheduledReport.getParameters().get(0).getRequired());
    }
}
