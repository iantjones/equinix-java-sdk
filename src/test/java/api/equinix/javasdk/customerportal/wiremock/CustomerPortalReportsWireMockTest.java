package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.customerportal.enums.ReportPeriod;
import api.equinix.javasdk.customerportal.enums.ReportScheduleType;
import api.equinix.javasdk.customerportal.enums.ReportStatus;
import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.Report;
import api.equinix.javasdk.customerportal.model.ScheduledReport;
import api.equinix.javasdk.customerportal.model.json.ReportDefinitionJson;
import api.equinix.javasdk.customerportal.model.json.creators.ScheduleReportRequest;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal Report Center (reports v1) client, focused on the
 * bulk {@code deleteReports} operation. The API returns a per-report result array
 * ({@code [{reportId, status}, ...]}) rather than a single boolean, so the client surfaces the
 * per-report SUCCESS/ERROR outcomes (allowing callers to detect partial failures).
 */
class CustomerPortalReportsWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

    @BeforeAll
    static void setUp() {
        customerPortal = new CustomerPortal(testCredentials());
        redirectToWireMock(customerPortal);
        customerPortal.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (customerPortal != null) customerPortal.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("deleteReports returns the per-report SUCCESS/ERROR results")
    void deleteReports_returnsPerReportResults() {
        wireMock.stubFor(delete(urlPathEqualTo("/v1/reportCenter/reports"))
                .willReturn(okJson("[{\"reportId\":\"r1\",\"status\":\"SUCCESS\"},"
                        + "{\"reportId\":\"r2\",\"status\":\"ERROR\"}]")));

        List<? extends Report> results = customerPortal.reports().deleteReports(List.of("r1", "r2"));

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("r1", results.get(0).getReportId());
        assertEquals(ReportStatus.SUCCESS, results.get(0).getStatus());
        assertEquals("r2", results.get(1).getReportId());
        assertEquals(ReportStatus.ERROR, results.get(1).getStatus());
        wireMock.verify(deleteRequestedFor(urlPathEqualTo("/v1/reportCenter/reports"))
                .withQueryParam("reportIds", equalTo("r1"))
                .withQueryParam("reportIds", equalTo("r2")));
    }

    @Nested
    @DisplayName("scheduleReport()")
    class ScheduleReport {

        @Test
        @DisplayName("POSTs the request body to /v1/reportCenter/reports/scheduler and returns the created schedule")
        void schedulesReport() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/reportCenter/reports/scheduler"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/customerportal/scheduled_report_response.json"))));

            ScheduleReportRequest request = ScheduleReportRequest.builder(
                            "my_report",
                            List.of(Map.of("name", "user_key", "value", "143534,908373", "type", "ARRAY", "required", true)),
                            "WEEKLY",
                            "30_DAYS")
                    .control("CUSTOMER")
                    .categories(List.of("POWER"))
                    .build();

            ScheduledReport created = customerPortal.reports().scheduleReport(request);

            assertNotNull(created);
            assertEquals("806f281c-6295-465e-bd41-04559d4d4960", created.getScheduledId());
            assertEquals("my_report", created.getReportName());
            assertEquals(ReportScheduleType.WEEKLY, created.getScheduleType());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/scheduler"))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("my_report")))
                    .withRequestBody(matchingJsonPath("$.scheduleType", equalTo("WEEKLY")))
                    .withRequestBody(matchingJsonPath("$.period", equalTo("30_DAYS")))
                    .withRequestBody(matchingJsonPath("$.control", equalTo("CUSTOMER")))
                    .withRequestBody(matchingJsonPath("$.categories[0]", equalTo("POWER")))
                    .withRequestBody(matchingJsonPath("$.parameters[0].name", equalTo("user_key")))
                    .withRequestBody(matchingJsonPath("$.parameters[0].value", equalTo("143534,908373"))));
        }
    }

    @Nested
    @DisplayName("updateScheduledReport()")
    class UpdateScheduledReport {

        @Test
        @DisplayName("PUTs the request body to /v1/reportCenter/reports/scheduler/{scheduledId} and returns the updated schedule")
        void updatesScheduledReport() {
            String scheduledId = "806f281c-6295-465e-bd41-04559d4d4960";
            wireMock.stubFor(put(urlPathEqualTo("/v1/reportCenter/reports/scheduler/" + scheduledId))
                    .willReturn(okJson(loadFixture("/json/customerportal/scheduled_report_response.json"))));

            ScheduleReportRequest request = ScheduleReportRequest.builder(
                            "my_report",
                            List.of(Map.of("name", "user_key", "value", "143534", "type", "ARRAY", "required", true)),
                            "MONTHLY",
                            "90_DAYS")
                    .build();

            ScheduledReport updated = customerPortal.reports().updateScheduledReport(scheduledId, request);

            assertNotNull(updated);
            assertEquals(scheduledId, updated.getScheduledId());

            wireMock.verify(putRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/scheduler/" + scheduledId))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("my_report")))
                    .withRequestBody(matchingJsonPath("$.scheduleType", equalTo("MONTHLY")))
                    .withRequestBody(matchingJsonPath("$.period", equalTo("90_DAYS")))
                    .withRequestBody(matchingJsonPath("$.parameters[0].name", equalTo("user_key"))));
        }
    }

    @Nested
    @DisplayName("deleteScheduledReports()")
    class DeleteScheduledReports {

        @Test
        @DisplayName("DELETEs /v1/reportCenter/reports/scheduler with the scheduledIds query param and returns true on 204")
        void deletesScheduledReports() {
            wireMock.stubFor(delete(urlPathEqualTo("/v1/reportCenter/reports/scheduler"))
                    .willReturn(noContent()));

            boolean result = customerPortal.reports().deleteScheduledReports(List.of("s1", "s2"));

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/scheduler"))
                    .withQueryParam("scheduledIds", equalTo("s1"))
                    .withQueryParam("scheduledIds", equalTo("s2")));
        }
    }

    @Nested
    @DisplayName("downloadReports()")
    class DownloadReports {

        @Test
        @DisplayName("GETs /v1/reportCenter/reports/files with the reportIds query param and returns the raw bytes")
        void downloadsReportsBundle() {
            byte[] zipBytes = "PK combined-reports-archive".getBytes(StandardCharsets.UTF_8);
            wireMock.stubFor(get(urlPathEqualTo("/v1/reportCenter/reports/files"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/zip")
                            .withBody(zipBytes)));

            byte[] downloaded = customerPortal.reports().downloadReports(List.of("r1", "r2"));

            assertNotNull(downloaded);
            assertArrayEquals(zipBytes, downloaded);
            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/files"))
                    .withQueryParam("reportIds", equalTo("r1"))
                    .withQueryParam("reportIds", equalTo("r2")));
        }
    }

    @Nested
    @DisplayName("downloadReport()")
    class DownloadReport {

        @Test
        @DisplayName("GETs /v1/reportCenter/reports/{reportId}/file and returns the raw bytes")
        void downloadsSingleReport() {
            String reportId = "8f204c59-f70f-437b-92d5-dbbde2932de5";
            byte[] fileBytes = "power-usage-spreadsheet-contents".getBytes(StandardCharsets.UTF_8);
            wireMock.stubFor(get(urlPathEqualTo("/v1/reportCenter/reports/" + reportId + "/file"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/vnd.ms-excel")
                            .withBody(fileBytes)));

            byte[] downloaded = customerPortal.reports().downloadReport(reportId);

            assertNotNull(downloaded);
            assertArrayEquals(fileBytes, downloaded);
            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/" + reportId + "/file")));
        }
    }

    @Nested
    @DisplayName("generateReport()")
    class GenerateReport {

        @Test
        @DisplayName("POSTs /v1/reportCenter/reports/scheduler/{scheduledId}/report and returns the generated report")
        void generatesReport() {
            String scheduledId = "886fc5de-6304-46c7-902c-d946c656c169";
            wireMock.stubFor(post(urlPathEqualTo("/v1/reportCenter/reports/scheduler/" + scheduledId + "/report"))
                    .willReturn(okJson(loadFixture("/json/customerportal/report_response.json"))));

            Report generated = customerPortal.reports().generateReport(scheduledId);

            assertNotNull(generated);
            assertEquals("8f204c59-f70f-437b-92d5-dbbde2932de5", generated.getReportId());
            assertEquals(ReportStatus.SUCCESS, generated.getStatus());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/scheduler/" + scheduledId + "/report")));
        }
    }

    @Nested
    @DisplayName("getReports()")
    class GetReports {

        @Test
        @DisplayName("GETs /v1/reportCenter/reports and returns the paginated reports")
        void listsReports() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/reportCenter/reports"))
                    .willReturn(okJson(loadFixture("/json/customerportal/paginated_reports.json"))));

            PaginatedList<Report> reports = customerPortal.reports().getReports();

            assertNotNull(reports);
            assertEquals(2, reports.size());
            assertEquals("8f204c59-f70f-437b-92d5-dbbde2932de5", reports.get(0).getReportId());
            assertEquals("Power Usage", reports.get(0).getReportName());
            assertEquals(ReportStatus.SUCCESS, reports.get(0).getStatus());
            assertEquals("71df1306-94f9-4cbd-b6b0-735098bf7f87", reports.get(1).getReportId());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/reportCenter/reports")));
        }

        // getReports() is a plain paginated GET: dispatch stamps offset=0/limit=100 onto the
        // first request, and page 2 is requested by advancing the offset/limit QUERY PARAMETERS
        // from the SERVER-reported pagination.
        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "reportId": "PAGE1_REPORT" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "reportId": "PAGE2_REPORT" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() fetches page 2 by advancing the offset query param")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/reportCenter/reports"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/v1/reportCenter/reports"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<Report> reports = customerPortal.reports().getReports();
            assertEquals(1, reports.size());
            assertTrue(reports.hasNextPage());

            reports.loadAll();

            assertEquals(2, reports.size());
            assertEquals("PAGE1_REPORT", reports.get(0).getReportId());
            assertEquals("PAGE2_REPORT", reports.get(1).getReportId());
            assertFalse(reports.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/v1/reportCenter/reports"))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100")));
        }
    }

    @Nested
    @DisplayName("getReportById()")
    class GetReportById {

        @Test
        @DisplayName("GETs /v1/reportCenter/reports/{reportId} and returns the report")
        void getsReportById() {
            String reportId = "8f204c59-f70f-437b-92d5-dbbde2932de5";
            wireMock.stubFor(get(urlPathEqualTo("/v1/reportCenter/reports/" + reportId))
                    .willReturn(okJson(loadFixture("/json/customerportal/report_response.json"))));

            Report report = customerPortal.reports().getReportById(reportId);

            assertNotNull(report);
            assertEquals(reportId, report.getReportId());
            assertEquals("Power Usage", report.getReportName());
            assertEquals(ReportStatus.SUCCESS, report.getStatus());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/" + reportId)));
        }
    }

    @Nested
    @DisplayName("getScheduledReports()")
    class GetScheduledReports {

        @Test
        @DisplayName("GETs /v1/reportCenter/reports/scheduler and returns the scheduled reports from the data array")
        void listsScheduledReports() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/reportCenter/reports/scheduler"))
                    .willReturn(okJson(loadFixture("/json/customerportal/scheduled_reports_response.json"))));

            List<? extends ScheduledReport> scheduled = customerPortal.reports().getScheduledReports();

            assertNotNull(scheduled);
            assertEquals(2, scheduled.size());
            assertEquals("806f281c-6295-465e-bd41-04559d4d4960", scheduled.get(0).getScheduledId());
            assertEquals("my_report", scheduled.get(0).getReportName());
            assertEquals(ReportScheduleType.WEEKLY, scheduled.get(0).getScheduleType());
            assertEquals("power_report", scheduled.get(1).getReportName());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/scheduler")));
        }
    }

    @Nested
    @DisplayName("getScheduledReport()")
    class GetScheduledReport {

        @Test
        @DisplayName("GETs /v1/reportCenter/reports/scheduler/{scheduledId} and returns the scheduled report")
        void getsScheduledReportById() {
            String scheduledId = "806f281c-6295-465e-bd41-04559d4d4960";
            wireMock.stubFor(get(urlPathEqualTo("/v1/reportCenter/reports/scheduler/" + scheduledId))
                    .willReturn(okJson(loadFixture("/json/customerportal/scheduled_report_response.json"))));

            ScheduledReport scheduled = customerPortal.reports().getScheduledReport(scheduledId);

            assertNotNull(scheduled);
            assertEquals(scheduledId, scheduled.getScheduledId());
            assertEquals("my_report", scheduled.getReportName());
            assertEquals(ReportScheduleType.WEEKLY, scheduled.getScheduleType());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/scheduler/" + scheduledId)));
        }
    }

    @Nested
    @DisplayName("getReportDefinitions()")
    class GetReportDefinitions {

        @Test
        @DisplayName("GETs /v1/reportCenter/reports/definitions and returns the definitions array")
        void listsReportDefinitions() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/reportCenter/reports/definitions"))
                    .willReturn(okJson(loadFixture("/json/customerportal/report_definitions_response.json"))));

            List<ReportDefinitionJson> definitions = customerPortal.reports().getReportDefinitions();

            assertNotNull(definitions);
            assertEquals(2, definitions.size());
            assertEquals("power_usage", definitions.get(0).getName());
            assertEquals(ReportScheduleType.WEEKLY, definitions.get(0).getScheduleType());
            assertEquals("cross_connect_inventory", definitions.get(1).getName());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/definitions")));
        }
    }

    @Nested
    @DisplayName("getReportDefinition()")
    class GetReportDefinition {

        @Test
        @DisplayName("GETs /v1/reportCenter/reports/definitions/{reportName} and returns the definition")
        void getsReportDefinitionByName() {
            String reportName = "power_usage";
            wireMock.stubFor(get(urlPathEqualTo("/v1/reportCenter/reports/definitions/" + reportName))
                    .willReturn(okJson(loadFixture("/json/customerportal/report_definition_response.json"))));

            ReportDefinitionJson definition = customerPortal.reports().getReportDefinition(reportName);

            assertNotNull(definition);
            assertEquals("power_usage", definition.getName());
            assertEquals(ReportScheduleType.WEEKLY, definition.getScheduleType());
            assertEquals(ReportPeriod.DAYS_30, definition.getPeriod());
            assertNotNull(definition.getParameters());
            assertEquals(2, definition.getParameters().size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/reportCenter/reports/definitions/" + reportName)));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 on getReportById() throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/reportCenter/reports/[^/]+",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Report not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.reports().getReportById("missing-report"));
        }

        @Test
        @DisplayName("401 on the destructive deleteReports() throws EquinixAuthenticationException")
        void unauthorizedDelete() {
            stubErrorInline(wireMock, "/v1/reportCenter/reports",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> customerPortal.reports().deleteReports(List.of("r1")));
        }

        @Test
        @DisplayName("403 on the destructive deleteScheduledReports() throws EquinixAuthorizationException")
        void forbiddenScheduledDelete() {
            stubErrorInline(wireMock, "/v1/reportCenter/reports/scheduler",
                    403, "[{\"errorCode\":\"ERR-403\",\"errorMessage\":\"Forbidden\"}]");

            assertThrows(EquinixAuthorizationException.class,
                    () -> customerPortal.reports().deleteScheduledReports(List.of("s1")));
        }

        @Test
        @DisplayName("500 on getReports() throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v1/reportCenter/reports",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.reports().getReports());
        }
    }
}
