package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.TicketCategory;
import api.equinix.javasdk.customerportal.enums.TicketPriority;
import api.equinix.javasdk.customerportal.model.TroubleTicket;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Trouble Tickets.
 */
class CustomerPortalTroubleTicketsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns trouble ticket for valid UUID")
        void returnsTroubleTicket() {
            stubSingleton(wireMock, "/v2/tickets/.*",
                    "/json/customerportal/trouble_ticket_response.json");

            TroubleTicket ticket = customerPortal.troubleTickets().getByUuid("c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f");
            assertNotNull(ticket);
            assertEquals("c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f", ticket.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/tickets/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Trouble ticket not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.troubleTickets().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("define()...create()")
    class Create {

        @Test
        @DisplayName("POSTs to the CreateTroubleTicket endpoint and returns the created object")
        void createsTroubleTicket() {
            stubCreate(wireMock, "/v2/tickets",
                    "/json/customerportal/trouble_ticket_response.json");

            TroubleTicket ticket = customerPortal.troubleTickets().define()
                    .category(TicketCategory.CONNECTIVITY)
                    .priority(TicketPriority.HIGH)
                    .subject("Intermittent packet loss on cross-connect XC-1042")
                    .ibxCode("SV5")
                    .accountNumber("128745")
                    .create();

            assertNotNull(ticket);
            assertEquals("c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f", ticket.getUuid());
            assertEquals("Intermittent packet loss on cross-connect XC-1042", ticket.getSubject());

            // CreateTroubleTicket is POST /v2/tickets (rootUri "tickets", no requestUri);
            // confirms the recent Post->Create endpoint-name fix routes here and serializes the body.
            wireMock.verify(postRequestedFor(urlPathEqualTo("/v2/tickets"))
                    .withRequestBody(matchingJsonPath("$.category", equalTo("CONNECTIVITY")))
                    .withRequestBody(matchingJsonPath("$.priority", equalTo("HIGH")))
                    .withRequestBody(matchingJsonPath("$.ibxCode", equalTo("SV5")))
                    .withRequestBody(matchingJsonPath("$.accountNumber", equalTo("128745"))));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v2/tickets/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.troubleTickets().getByUuid("test-uuid"));
        }
    }
}
