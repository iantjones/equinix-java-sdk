package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.model.WorkVisit;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Work Visits.
 */
class CustomerPortalWorkVisitsWireMockTest extends WireMockTestBase {

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

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns work visit for valid UUID")
        void returnsWorkVisit() {
            stubSingleton(wireMock, "/v2/workVisits/.*",
                    "/json/customerportal/work_visit_response.json");

            WorkVisit workVisit = customerPortal.workVisits().getByUuid("d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f80");
            assertNotNull(workVisit);
            assertEquals("d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f80", workVisit.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/workVisits/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Work visit not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.workVisits().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v2/workVisits/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.workVisits().getByUuid("test-uuid"));
        }
    }
}
