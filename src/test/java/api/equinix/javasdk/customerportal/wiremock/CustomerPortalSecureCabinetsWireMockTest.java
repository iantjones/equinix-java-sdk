package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.SecureCabinet;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Secure Cabinets.
 *
 * <p>Exercises the list accessor {@code secureCabinets().list()} and the single-resource
 * accessor {@code secureCabinets().getByUuid(uuid)}. The SecureCabinets functional area is
 * keyed to API version 1 ({@code /v1/secureCabinets...}).</p>
 */
class CustomerPortalSecureCabinetsWireMockTest extends WireMockTestBase {

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
    @DisplayName("list()")
    class List {

        @Test
        @DisplayName("returns paginated secure cabinets")
        void returnsSecureCabinets() {
            stubPaginatedGet(wireMock, "/v1/secureCabinets",
                    "/json/customerportal/paginated_secure_cabinets.json");

            PaginatedList<SecureCabinet> cabinets = customerPortal.secureCabinets().list();

            assertNotNull(cabinets);
            assertEquals(2, cabinets.size());
            SecureCabinet first = cabinets.get(0);
            assertEquals("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", first.getUuid());
            assertEquals("SV5:01:0042", first.getCabinetId());
            assertEquals("SV5", first.getIbx());
            assertEquals("ACTIVE", first.getStatus());
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns secure cabinet for valid UUID")
        void returnsSecureCabinet() {
            stubSingleton(wireMock, "/v1/secureCabinets/.*",
                    "/json/customerportal/secure_cabinet_response.json");

            SecureCabinet cabinet = customerPortal.secureCabinets()
                    .getByUuid("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");

            assertNotNull(cabinet);
            assertEquals("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", cabinet.getUuid());
            assertEquals("SV5:01:0042", cabinet.getCabinetId());
            assertEquals("ELECTRONIC", cabinet.getLockType());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/secureCabinets/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Secure cabinet not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.secureCabinets().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v1/secureCabinets/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.secureCabinets().getByUuid("test-uuid"));
        }
    }
}
