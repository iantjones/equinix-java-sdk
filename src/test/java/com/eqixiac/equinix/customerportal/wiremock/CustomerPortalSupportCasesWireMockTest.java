package com.eqixiac.equinix.customerportal.wiremock;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.customerportal.model.json.creators.SupportCaseAttachment;
import com.eqixiac.equinix.customerportal.model.json.creators.SupportCaseCancelRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.SupportCaseCreateRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.SupportCaseNoteRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.OrderContact;
import com.eqixiac.equinix.customerportal.model.EmailDetails;
import com.eqixiac.equinix.customerportal.model.SupportCase;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal {@code SupportCases} client, covering the support
 * v2 mutation/action endpoints under {@code /support/v2/tickets} (create, add-notes-by-id, cancel,
 * attachment download) plus the v1 add-notes-by-case-number fallback at
 * {@code /support/v1/tickets/{caseNumber}/notes}. Each test drives the operation through WireMock
 * and asserts the exact request path/verb and (for bodies) the serialized JSON.
 */
class CustomerPortalSupportCasesWireMockTest extends WireMockTestBase {

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
        @DisplayName("POSTs /support/v2/tickets with the serialized body and returns the case/order id")
        void createPostsBodyAndReturnsId() {
            wireMock.stubFor(post(urlPathEqualTo("/support/v2/tickets"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":\"2-987654321\",\"type\":\"CASE\"}")));

            SupportCaseCreateRequest request = SupportCaseCreateRequest
                    .builder("PORTAL_ACCESS", "Cannot access the customer portal")
                    .primaryId("AM1")
                    .customerReferenceId("REF-42")
                    .contacts(List.of(
                            OrderContact.registered("NOTIFICATION", List.of("jondoe"))))
                    .attachments(List.of(
                            new SupportCaseAttachment("att-1", "screenshot.png")))
                    .details(Map.of("severity", "HIGH"))
                    .build();

            String id = customerPortal.supportCases().create(request);

            assertEquals("2-987654321", id);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/support/v2/tickets"))
                    .withRequestBody(matchingJsonPath("$.code", equalTo("PORTAL_ACCESS")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Cannot access the customer portal")))
                    .withRequestBody(matchingJsonPath("$.primaryId", equalTo("AM1")))
                    .withRequestBody(matchingJsonPath("$.customerReferenceId", equalTo("REF-42")))
                    .withRequestBody(matchingJsonPath("$.contacts[0].type", equalTo("NOTIFICATION")))
                    .withRequestBody(matchingJsonPath("$.contacts[0].registeredUsers[0]", equalTo("jondoe")))
                    .withRequestBody(matchingJsonPath("$.attachments[0].id", equalTo("att-1")))
                    .withRequestBody(matchingJsonPath("$.details.severity", equalTo("HIGH"))));
        }
    }

    @Nested
    @DisplayName("addNotesById()")
    class AddNotesById {

        @Test
        @DisplayName("POSTs /support/v2/tickets/{id}/notes with the note text and returns true")
        void addNotesByIdPostsNote() {
            wireMock.stubFor(post(urlPathEqualTo("/support/v2/tickets/2-987654321/notes"))
                    .willReturn(aResponse().withStatus(200)));

            SupportCaseNoteRequest request = new SupportCaseNoteRequest(
                    "Adding a follow-up note",
                    List.of(new SupportCaseAttachment("att-9", "log.txt")));

            Boolean result = customerPortal.supportCases().addNotesById("2-987654321", request);

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/support/v2/tickets/2-987654321/notes"))
                    .withRequestBody(matchingJsonPath("$.text", equalTo("Adding a follow-up note")))
                    .withRequestBody(matchingJsonPath("$.attachments[0].id", equalTo("att-9")))
                    .withRequestBody(matchingJsonPath("$.attachments[0].name", equalTo("log.txt"))));
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("POSTs /support/v2/tickets/{id}/cancel with the reason and returns true")
        void cancelPostsReason() {
            wireMock.stubFor(post(urlPathEqualTo("/support/v2/tickets/2-987654321/cancel"))
                    .willReturn(aResponse().withStatus(200)));

            Boolean result = customerPortal.supportCases()
                    .cancel("2-987654321", new SupportCaseCancelRequest("Issue resolved on our side"));

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/support/v2/tickets/2-987654321/cancel"))
                    .withRequestBody(matchingJsonPath("$.reason", equalTo("Issue resolved on our side"))));
        }
    }

    @Nested
    @DisplayName("addNotesByCaseNumber()")
    class AddNotesByCaseNumber {

        @Test
        @DisplayName("POSTs the v1 fallback /support/v1/tickets/{caseNumber}/notes and returns true")
        void addNotesByCaseNumberPostsToV1() {
            wireMock.stubFor(post(urlPathEqualTo("/support/v1/tickets/2-987654321/notes"))
                    .willReturn(aResponse().withStatus(200)));

            Boolean result = customerPortal.supportCases()
                    .addNotesByCaseNumber("2-987654321", new SupportCaseNoteRequest("A v1 note"));

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/support/v1/tickets/2-987654321/notes"))
                    .withRequestBody(matchingJsonPath("$.text", equalTo("A v1 note"))));
        }
    }

    @Nested
    @DisplayName("downloadAttachment()")
    class DownloadAttachment {

        @Test
        @DisplayName("GETs /support/v2/tickets/attachment/download/{caseId}/{attachmentId} and returns the raw bytes")
        void downloadReturnsBytes() {
            byte[] pdfBytes = "%PDF-1.4 support-case-attachment".getBytes(StandardCharsets.UTF_8);
            wireMock.stubFor(get(urlPathEqualTo("/support/v2/tickets/attachment/download/2-987654321/att-1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/pdf")
                            .withBody(pdfBytes)));

            byte[] downloaded = customerPortal.supportCases().downloadAttachment("2-987654321", "att-1");

            assertNotNull(downloaded);
            assertArrayEquals(pdfBytes, downloaded);
            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/support/v2/tickets/attachment/download/2-987654321/att-1")));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            wireMock.stubFor(get(urlPathMatching("/support/v2/tickets/attachment/download/.*"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Attachment not found\"}]")));

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.supportCases().downloadAttachment("2-987654321", "missing"));
        }
    }

    @Nested
    @DisplayName("getByCaseOrOrderNumber()")
    class GetByCaseOrOrderNumber {

        @Test
        @DisplayName("GETs /support/v2/tickets/{id} and deserializes the case")
        void getReturnsCase() {
            wireMock.stubFor(get(urlPathEqualTo("/support/v2/tickets/11150929"))
                    .willReturn(okJson(loadFixture("/json/customerportal/support_case_response.json"))));

            SupportCase supportCase = customerPortal.supportCases().getByCaseOrOrderNumber("11150929");

            assertNotNull(supportCase);
            assertEquals("11150929", supportCase.getId());
            assertEquals("1-204976070710", supportCase.getOrderId());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/support/v2/tickets/11150929")));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            wireMock.stubFor(get(urlPathMatching("/support/v2/tickets/.*"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Case not found\"}]")));

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.supportCases().getByCaseOrOrderNumber("missing"));
        }
    }

    @Nested
    @DisplayName("getEmailDetails()")
    class GetEmailDetails {

        @Test
        @DisplayName("GETs the v1 /support/v1/tickets/emailDetails/{emailId}/caseNumber/{caseNumber} path and deserializes")
        void getEmailDetailsReturnsDetails() {
            wireMock.stubFor(get(urlPathEqualTo(
                    "/support/v1/tickets/emailDetails/02s7z00000D30lZAAR/caseNumber/11150929"))
                    .willReturn(okJson(loadFixture("/json/customerportal/email_details_response.json"))));

            EmailDetails details = customerPortal.supportCases()
                    .getEmailDetails("02s7z00000D30lZAAR", "11150929");

            assertNotNull(details);
            assertEquals("Issue with device", details.getSubject());
            assertEquals("abc@equinix.com", details.getFromAddress());
            assertEquals("xyz@example.com", details.getToAddress());
            assertEquals("ops@example.com", details.getCcAddress());
            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/support/v1/tickets/emailDetails/02s7z00000D30lZAAR/caseNumber/11150929")));
        }
    }
}
