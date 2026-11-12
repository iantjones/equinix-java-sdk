package com.eqixiac.equinix.operations.wiremock;

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.operations.enums.TicketCode;
import com.eqixiac.equinix.operations.enums.TicketStatus;
import com.eqixiac.equinix.operations.model.TroubleTicket;
import com.eqixiac.equinix.operations.model.json.creators.ContactUpdate;
import com.eqixiac.equinix.operations.model.json.creators.TicketCancelRequest;
import com.eqixiac.equinix.operations.model.json.creators.TicketNoteRequest;
import com.eqixiac.equinix.operations.model.json.creators.TicketUpdateRequest;
import com.eqixiac.equinix.operations.model.json.creators.TroubleTicketCreateRequest;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Operations Trouble Tickets (Tickets v2 API).
 *
 * <p>Exercises {@code create(...)} (POST {@code /v2/tickets}, id from {@code Location} header),
 * {@code getByUuid(...)} (GET {@code /v2/tickets/{id}}), {@code addNote(...)} and
 * {@code cancel(...)}.</p>
 */
class OperationsTroubleTicketsWireMockTest extends WireMockTestBase {

    static Operations operations;

    @BeforeAll
    static void setUp() {
        operations = new Operations(testCredentials());
        redirectToWireMock(operations);
        operations.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (operations != null) operations.close();
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

            String ticketId = operations.troubleTickets().create(
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
                    "/json/operations/trouble_ticket_response.json");

            TroubleTicket ticket = operations.troubleTickets().getByUuid("1-9808089098");
            assertNotNull(ticket);
            assertEquals("1-9808089098", ticket.getId());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/tickets/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Trouble ticket not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> operations.troubleTickets().getByUuid("invalid-id"));
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

            Boolean accepted = operations.troubleTickets().update("1-34891",
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
                    () -> operations.troubleTickets().update("invalid-id",
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

            assertTrue(operations.troubleTickets().addNote("1-34891",
                    new TicketNoteRequest("Customer confirms the issue persists.")));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v2/tickets/1-34891/notes"))
                    .withRequestBody(matchingJsonPath("$.text")));
        }

        @Test
        @DisplayName("POSTs a cancellation")
        void cancels() {
            wireMock.stubFor(post(urlPathEqualTo("/v2/tickets/1-34891/cancel"))
                    .willReturn(aResponse().withStatus(202).withHeader("Location", "/tickets/1-34891")));

            assertTrue(operations.troubleTickets().cancel("1-34891",
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
                    "/json/operations/trouble_ticket_response.json");

            TroubleTicket ticket = operations.troubleTickets().getByUuid("1-9808089098");
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
                    () -> operations.troubleTickets().getByUuid("test-id"));
        }
    }
}
