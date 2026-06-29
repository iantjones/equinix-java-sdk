package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.customerportal.model.Report;
import org.junit.jupiter.api.*;

import java.util.List;

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
        assertEquals("SUCCESS", results.get(0).getStatus());
        assertEquals("r2", results.get(1).getReportId());
        assertEquals("ERROR", results.get(1).getStatus());
        wireMock.verify(deleteRequestedFor(urlPathEqualTo("/v1/reportCenter/reports"))
                .withQueryParam("reportIds", equalTo("r1"))
                .withQueryParam("reportIds", equalTo("r2")));
    }
}
