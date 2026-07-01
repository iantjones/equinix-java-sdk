package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.NegotiationAction;
import api.equinix.javasdk.customerportal.model.Order;
import api.equinix.javasdk.customerportal.model.OrderNegotiation;
import api.equinix.javasdk.customerportal.model.json.creators.AttachmentReference;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal Orders v2 client, covering the
 * {@code colocations/v2/orders} base path and the negotiations (GET/POST), notes (POST) and
 * cancel (POST) sub-actions. The three POST actions return 202/204 with no body and surface
 * as a {@code Boolean}.
 */
class CustomerPortalOrdersWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

    private static final String ORDER_ID = "1-23232322";

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
        @DisplayName("hits the colocations/v2/orders path and returns the order")
        void returnsOrder() {
            stubSingleton(wireMock, "/colocations/v2/orders/.*",
                    "/json/customerportal/order_response.json");

            Order order = customerPortal.orders().getByUuid(ORDER_ID);

            assertNotNull(order);
            assertEquals(ORDER_ID, order.getOrderId());
            assertEquals(api.equinix.javasdk.customerportal.enums.OrderStatus.IN_PROGRESS, order.getStatus());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID)));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/colocations/v2/orders/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Order not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.orders().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("getNegotiations()")
    class GetNegotiations {

        @Test
        @DisplayName("returns the list of negotiation messages")
        void returnsList() {
            wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/negotiations"))
                    .willReturn(okJson("[{\"referenceId\":\"4-12312312132\","
                            + "\"proposedDateTime\":\"2020-08-25T11:24:10.282Z\","
                            + "\"expedited\":false,\"message\":\"Alternative time proposed\"}]")));

            List<? extends OrderNegotiation> negotiations = customerPortal.orders().getNegotiations(ORDER_ID);

            assertNotNull(negotiations);
            assertEquals(1, negotiations.size());
            assertEquals("4-12312312132", negotiations.get(0).getReferenceId());
            assertFalse(negotiations.get(0).getExpedited());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/negotiations")));
        }
    }

    @Nested
    @DisplayName("replyNegotiation()")
    class ReplyNegotiation {

        @Test
        @DisplayName("posts the action and referenceId, returning true on 202")
        void postsActionAndReferenceId() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/negotiations"))
                    .willReturn(aResponse().withStatus(202)));

            Boolean result = customerPortal.orders().replyNegotiation(ORDER_ID, NegotiationAction.APPROVE, "4-9091830");

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/negotiations"))
                    .withRequestBody(matchingJsonPath("$.action", equalTo("APPROVE")))
                    .withRequestBody(matchingJsonPath("$.referenceId", equalTo("4-9091830"))));
        }

        @Test
        @DisplayName("CANCEL includes the reason, returning true on 204")
        void cancelIncludesReason() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/negotiations"))
                    .willReturn(aResponse().withStatus(204)));

            Boolean result = customerPortal.orders()
                    .replyNegotiation(ORDER_ID, NegotiationAction.CANCEL, "4-9091830", "Cancelling the new time");

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/negotiations"))
                    .withRequestBody(matchingJsonPath("$.action", equalTo("CANCEL")))
                    .withRequestBody(matchingJsonPath("$.reason", equalTo("Cancelling the new time"))));
        }
    }

    @Nested
    @DisplayName("addNote()")
    class AddNote {

        @Test
        @DisplayName("posts the note text")
        void postsText() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/notes"))
                    .willReturn(aResponse().withStatus(202)));

            Boolean result = customerPortal.orders().addNote(ORDER_ID, "problem description");

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/notes"))
                    .withRequestBody(matchingJsonPath("$.text", equalTo("problem description"))));
        }

        @Test
        @DisplayName("posts text, referenceId and attachment references")
        void postsTextReferenceIdAndAttachments() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/notes"))
                    .willReturn(aResponse().withStatus(202)));

            Boolean result = customerPortal.orders().addNote(
                    ORDER_ID,
                    "problem description",
                    "4-12312312132",
                    List.of(new AttachmentReference("att-1", "diagram.pdf")));

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/notes"))
                    .withRequestBody(equalToJson("{"
                            + "\"text\":\"problem description\","
                            + "\"referenceId\":\"4-12312312132\","
                            + "\"attachments\":[{\"id\":\"att-1\",\"name\":\"diagram.pdf\"}]"
                            + "}")));
        }

        @Test
        @DisplayName("omits null referenceId and attachments from the serialized body")
        void omitsNullOptionalFields() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/notes"))
                    .willReturn(aResponse().withStatus(202)));

            customerPortal.orders().addNote(ORDER_ID, "just text", null, null);

            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/notes"))
                    .withRequestBody(matchingJsonPath("$.text", equalTo("just text")))
                    .withRequestBody(notMatching("(?s).*referenceId.*"))
                    .withRequestBody(notMatching("(?s).*attachments.*")));
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("posts the cancellation reason")
        void postsReason() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/cancel"))
                    .willReturn(aResponse().withStatus(202)));

            Boolean result = customerPortal.orders().cancel(ORDER_ID, "No longer required");

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/cancel"))
                    .withRequestBody(matchingJsonPath("$.reason", equalTo("No longer required"))));
        }

        @Test
        @DisplayName("posts reason, attachments and lineIds")
        void postsReasonAttachmentsAndLineIds() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/cancel"))
                    .willReturn(aResponse().withStatus(202)));

            Boolean result = customerPortal.orders().cancel(
                    ORDER_ID,
                    "No longer required",
                    List.of(new AttachmentReference("att-9", "authorisation.pdf")),
                    List.of("1-line-a", "1-line-b"));

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/cancel"))
                    .withRequestBody(equalToJson("{"
                            + "\"reason\":\"No longer required\","
                            + "\"attachments\":[{\"id\":\"att-9\",\"name\":\"authorisation.pdf\"}],"
                            + "\"lineIds\":[\"1-line-a\",\"1-line-b\"]"
                            + "}")));
        }

        @Test
        @DisplayName("omits null attachments and lineIds from the serialized body")
        void omitsNullOptionalFields() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/cancel"))
                    .willReturn(aResponse().withStatus(204)));

            customerPortal.orders().cancel(ORDER_ID, "just the reason", null, null);

            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/" + ORDER_ID + "/cancel"))
                    .withRequestBody(matchingJsonPath("$.reason", equalTo("just the reason")))
                    .withRequestBody(notMatching(".*attachments.*"))
                    .withRequestBody(notMatching(".*lineIds.*")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/colocations/v2/orders/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> customerPortal.orders().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/colocations/v2/orders/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.orders().getByUuid("test-uuid"));
        }
    }
}
