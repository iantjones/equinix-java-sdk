package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.TicketCode;
import api.equinix.javasdk.customerportal.enums.TicketStatus;
import api.equinix.javasdk.customerportal.model.TroubleTicket;
import api.equinix.javasdk.customerportal.model.json.creators.ContactUpdate;
import api.equinix.javasdk.customerportal.model.json.creators.TicketCancelRequest;
import api.equinix.javasdk.customerportal.model.json.creators.TicketNoteRequest;
import api.equinix.javasdk.customerportal.model.json.creators.TicketUpdateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketCreateRequest;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Trouble Tickets (Tickets v2 API).
 *
 * <p>Exercises {@code create(...)} (POST {@code /v2/tickets}, id from {@code Location} header),
 * {@code getByUuid(...)} (GET {@code /v2/tickets/{id}}), {@code addNote(...)} and
 * {@code cancel(...)}.</p>
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
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("POSTs the ticket and returns the Location-header id")
        void createsTicket() {
            wireMock.stubFor(post(urlPathEqualTo("/v2/tickets"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Location", "/tickets/1-34891")));

            String ticketId = customerPortal.troubleTickets().create(
                    TroubleTicketCreateRequest.builder(TicketCode.CODE_0001_0000,
                                    "Intermittent packet loss on cross-connect XC-1042",
                                    "2024-11-10T03:00:00Z", "SV5:01:000ABC")
                            .customerReferenceId("REF-9981")
                            .details(Map.of("callFromCage", true, "availability", "WORK_HOURS"))
                            .build());

            assertEquals("1-34891", ticketId);

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v2/tickets"))
                    .withRequestBody(matchingJsonPath("$.code", equalTo("0001-0000")))
                    .withRequestBody(matchingJsonPath("$.primaryId", equalTo("SV5:01:000ABC")))
                    .withRequestBody(matchingJsonPath("$.details.callFromCage", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.details.availability", equalTo("WORK_HOURS"))));
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns trouble ticket for valid id")
        void returnsTroubleTicket() {
            stubSingleton(wireMock, "/v2/tickets/.*",
                    "/json/customerportal/trouble_ticket_response.json");

            TroubleTicket ticket = customerPortal.troubleTickets().getByUuid("1-9808089098");
            assertNotNull(ticket);
            assertEquals("1-9808089098", ticket.getId());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/tickets/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Trouble ticket not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.troubleTickets().getByUuid("invalid-id"));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("PATCHes the ticket's notification contacts")
        void updatesContacts() {
            wireMock.stubFor(patch(urlPathEqualTo("/v2/tickets/1-34891"))
                    .willReturn(aResponse().withStatus(202).withHeader("Location", "/tickets/1-34891")));

            Boolean accepted = customerPortal.troubleTickets().update("1-34891",
                    new TicketUpdateRequest(List.of(
                            new ContactUpdate(List.of("jsmith", "adoe"), "WORK_HOURS", "America/Los_Angeles"))));

            assertTrue(accepted);

            wireMock.verify(patchRequestedFor(urlPathEqualTo("/v2/tickets/1-34891"))
                    .withRequestBody(matchingJsonPath("$.contacts[0].registeredUsers[0]", equalTo("jsmith")))
                    .withRequestBody(matchingJsonPath("$.contacts[0].registeredUsers[1]", equalTo("adoe")))
                    .withRequestBody(matchingJsonPath("$.contacts[0].type", equalTo("NOTIFICATION")))
                    .withRequestBody(matchingJsonPath("$.contacts[0].availability", equalTo("WORK_HOURS")))
                    .withRequestBody(matchingJsonPath("$.contacts[0].timezone", equalTo("America/Los_Angeles"))));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/tickets/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Trouble ticket not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.troubleTickets().update("invalid-id",
                            new TicketUpdateRequest(List.of(new ContactUpdate(List.of("jsmith"))))));
        }
    }

    @Nested
    @DisplayName("addNote() / cancel()")
    class Actions {

        @Test
        @DisplayName("POSTs a note to the ticket")
        void addsNote() {
            wireMock.stubFor(post(urlPathEqualTo("/v2/tickets/1-34891/notes"))
                    .willReturn(aResponse().withStatus(201).withHeader("Location", "/tickets/1-34891")));

            assertTrue(customerPortal.troubleTickets().addNote("1-34891",
                    new TicketNoteRequest("Customer confirms the issue persists.")));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v2/tickets/1-34891/notes"))
                    .withRequestBody(matchingJsonPath("$.text")));
        }

        @Test
        @DisplayName("POSTs a cancellation")
        void cancels() {
            wireMock.stubFor(post(urlPathEqualTo("/v2/tickets/1-34891/cancel"))
                    .willReturn(aResponse().withStatus(202).withHeader("Location", "/tickets/1-34891")));

            assertTrue(customerPortal.troubleTickets().cancel("1-34891",
                    new TicketCancelRequest("Resolved by customer.")));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v2/tickets/1-34891/cancel"))
                    .withRequestBody(matchingJsonPath("$.reason")));
        }
    }

    @Nested
    @DisplayName("TroubleTicketWrapper.refresh()")
    class Refresh {

        @Test
        @DisplayName("re-GETs /v2/tickets/{id} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            stubSingleton(wireMock, "/v2/tickets/.*",
                    "/json/customerportal/trouble_ticket_response.json");

            TroubleTicket ticket = customerPortal.troubleTickets().getByUuid("1-9808089098");
            assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());

            // The ticket is closed server-side: the most-recently-registered stub wins, so the
            // refresh GET sees the CLOSED state.
            wireMock.stubFor(get(urlPathEqualTo("/v2/tickets/1-9808089098"))
                    .willReturn(okJson("{\"id\":\"1-9808089098\",\"status\":\"CLOSED\"}")));

            ticket.refresh();

            assertEquals(TicketStatus.CLOSED, ticket.getStatus());
            assertEquals("1-9808089098", ticket.getId());

            // Exactly two GETs: the original read plus the refresh re-read of the same path.
            wireMock.verify(2, getRequestedFor(urlPathEqualTo("/v2/tickets/1-9808089098")));
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
                    () -> customerPortal.troubleTickets().getByUuid("test-id"));
        }
    }
}
