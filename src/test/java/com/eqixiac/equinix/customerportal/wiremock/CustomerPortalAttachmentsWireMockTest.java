package com.eqixiac.equinix.customerportal.wiremock;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.enums.AttachmentPurpose;
import com.eqixiac.equinix.customerportal.model.Attachment;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.eqixiac.equinix.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Attachments.
 */
class CustomerPortalAttachmentsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns attachment for valid UUID")
        void returnsAttachment() {
            stubSingleton(wireMock, "/v1/attachments/.*",
                    "/json/customerportal/attachment_response.json");

            Attachment attachment = customerPortal.attachments().getByUuid("f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b");
            assertNotNull(attachment);
            assertEquals("f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b", attachment.getAttachmentId());
            assertEquals("rack-diagram.pdf", attachment.getAttachmentName());
            assertEquals("application/pdf", attachment.getAttachmentType());
            assertEquals(482310L, attachment.getAttachmentSize());
            assertEquals("jane.doe@example.com", attachment.getCreatedBy());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/attachments/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Attachment not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.attachments().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("list()")
    class ListAll {

        @Test
        @DisplayName("GETs /v1/attachments and returns the paginated attachments")
        void listsAttachments() {
            stubPaginatedGet(wireMock, "/v1/attachments",
                    "/json/customerportal/paginated_attachments.json");

            PaginatedList<Attachment> attachments = customerPortal.attachments().list();

            assertNotNull(attachments);
            assertEquals(2, attachments.size());
            assertEquals("f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b", attachments.get(0).getAttachmentId());
            assertEquals("rack-diagram.pdf", attachments.get(0).getAttachmentName());
            assertEquals("a9b8c7d6-e5f4-4321-8a7b-6c5d4e3f2a1b", attachments.get(1).getAttachmentId());

            // GET /v1/attachments with no attachmentIds filter.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/attachments"))
                    .withoutQueryParam("attachmentIds"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v1/attachments",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.attachments().list());
        }
    }

    @Nested
    @DisplayName("list(List<String> attachmentIds)")
    class ListFiltered {

        @Test
        @DisplayName("GETs /v1/attachments with repeated attachmentIds query params")
        void listsFilteredByIds() {
            stubPaginatedGet(wireMock, "/v1/attachments",
                    "/json/customerportal/paginated_attachments.json");

            List<String> ids = List.of(
                    "f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b",
                    "a9b8c7d6-e5f4-4321-8a7b-6c5d4e3f2a1b");
            PaginatedList<Attachment> attachments = customerPortal.attachments().list(ids);

            assertNotNull(attachments);
            assertEquals(2, attachments.size());

            // Each id is sent as its own repeated attachmentIds query param.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/attachments"))
                    .withQueryParam("attachmentIds", equalTo("f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b"))
                    .withQueryParam("attachmentIds", equalTo("a9b8c7d6-e5f4-4321-8a7b-6c5d4e3f2a1b")));
        }

        @Test
        @DisplayName("null ids omits the filter and GETs the unfiltered list")
        void nullIdsOmitsFilter() {
            stubPaginatedGet(wireMock, "/v1/attachments",
                    "/json/customerportal/paginated_attachments.json");

            PaginatedList<Attachment> attachments = customerPortal.attachments().list(null);

            assertNotNull(attachments);
            assertEquals(2, attachments.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/attachments"))
                    .withoutQueryParam("attachmentIds"));
        }

        @Test
        @DisplayName("empty ids omits the filter and GETs the unfiltered list")
        void emptyIdsOmitsFilter() {
            stubPaginatedGet(wireMock, "/v1/attachments",
                    "/json/customerportal/paginated_attachments.json");

            PaginatedList<Attachment> attachments = customerPortal.attachments().list(List.of());

            assertNotNull(attachments);
            assertEquals(2, attachments.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/attachments"))
                    .withoutQueryParam("attachmentIds"));
        }
    }

    @Nested
    @DisplayName("upload()")
    class Upload {

        @Test
        @DisplayName("posts multipart/form-data with purpose query param")
        void uploadsMultipart() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/attachments/file"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/customerportal/attachment_response.json"))));

            byte[] fileBytes = "loa-document-contents".getBytes(StandardCharsets.UTF_8);
            Attachment attachment = customerPortal.attachments().upload(fileBytes, "loa.pdf", AttachmentPurpose.LOA);

            assertNotNull(attachment);
            assertEquals("f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b", attachment.getAttachmentId());

            // Verify the request was multipart with the file part and purpose query param.
            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/attachments/file"))
                    .withQueryParam("purpose", equalTo("LOA"))
                    .withHeader("content-type", containing("multipart/form-data"))
                    .withHeader("content-type", containing("boundary="))
                    .withRequestBody(containing("name=\"uploadFile\""))
                    .withRequestBody(containing("filename=\"loa.pdf\"")));
        }
    }

    @Nested
    @DisplayName("download()")
    class Download {

        @Test
        @DisplayName("GETs /v1/attachments/{id}/file and returns the raw bytes")
        void downloadsBytes() {
            byte[] pdfBytes = "%PDF-1.4 rack-diagram-contents".getBytes(StandardCharsets.UTF_8);
            wireMock.stubFor(get(urlPathEqualTo("/v1/attachments/f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b/file"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/pdf")
                            .withBody(pdfBytes)));

            byte[] downloaded = customerPortal.attachments().download("f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b");

            assertNotNull(downloaded);
            assertArrayEquals(pdfBytes, downloaded);

            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/v1/attachments/f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b/file")));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/attachments/.*/file",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Attachment not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.attachments().download("missing-uuid"));
        }
    }

    @Nested
    @DisplayName("AttachmentWrapper.refresh()")
    class Refresh {

        @Test
        @DisplayName("re-GETs /v1/attachments/{attachmentId} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            stubSingleton(wireMock, "/v1/attachments/.*",
                    "/json/customerportal/attachment_response.json");

            Attachment attachment = customerPortal.attachments()
                    .getByUuid("f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b");
            assertEquals("rack-diagram.pdf", attachment.getAttachmentName());

            // The attachment is renamed server-side: the most-recently-registered stub wins, so
            // the refresh GET sees the new name.
            wireMock.stubFor(get(urlPathEqualTo("/v1/attachments/f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b"))
                    .willReturn(okJson("{\"attachmentId\":\"f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b\","
                            + "\"attachmentName\":\"rack-diagram-v2.pdf\"}")));

            attachment.refresh();

            assertEquals("rack-diagram-v2.pdf", attachment.getAttachmentName());
            assertEquals("f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b", attachment.getAttachmentId());

            // Exactly two GETs: the original read plus the refresh re-read of the same path.
            wireMock.verify(2, getRequestedFor(
                    urlPathEqualTo("/v1/attachments/f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b")));
        }
    }

    @Nested
    @DisplayName("AttachmentWrapper.delete()")
    class Delete {

        @Test
        @DisplayName("DELETEs /v1/attachments/{attachmentId} (the wrapper is the only public deletion path)")
        void deletesAttachment() {
            stubSingleton(wireMock, "/v1/attachments/.*",
                    "/json/customerportal/attachment_response.json");
            wireMock.stubFor(delete(urlPathEqualTo("/v1/attachments/f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b"))
                    .willReturn(okJson(loadFixture("/json/customerportal/attachment_response.json"))));

            Attachment attachment = customerPortal.attachments()
                    .getByUuid("f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b");

            Boolean deleted = attachment.delete();

            assertTrue(deleted);
            wireMock.verify(deleteRequestedFor(
                    urlPathEqualTo("/v1/attachments/f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b")));
        }

        @Test
        @DisplayName("404 on the DELETE throws EquinixNotFoundException")
        void deleteNotFound() {
            stubSingleton(wireMock, "/v1/attachments/.*",
                    "/json/customerportal/attachment_response.json");

            Attachment attachment = customerPortal.attachments()
                    .getByUuid("f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b");

            wireMock.stubFor(delete(urlPathEqualTo("/v1/attachments/f1e2d3c4-b5a6-4978-9a0b-1c2d3e4f5a6b"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Attachment not found\"}]")));

            assertThrows(EquinixNotFoundException.class, attachment::delete);
        }
    }

    @Nested
    @DisplayName("Multi-page list paging")
    class Paging {

        // ListAttachments is a plain paginated GET: dispatch stamps offset=0/limit=100 onto the
        // first request, and page 2 is requested by advancing the offset/limit QUERY PARAMETERS
        // from the SERVER-reported pagination.
        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "attachmentId": "PAGE1_ATT", "attachmentName": "page1.pdf" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "attachmentId": "PAGE2_ATT", "attachmentName": "page2.pdf" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() fetches page 2 by advancing the offset query param")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/attachments"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/v1/attachments"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<Attachment> attachments = customerPortal.attachments().list();
            assertEquals(1, attachments.size());
            assertTrue(attachments.hasNextPage());

            attachments.loadAll();

            assertEquals(2, attachments.size());
            assertEquals("PAGE1_ATT", attachments.get(0).getAttachmentId());
            assertEquals("PAGE2_ATT", attachments.get(1).getAttachmentId());
            assertFalse(attachments.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/v1/attachments"))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v1/attachments/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.attachments().getByUuid("test-uuid"));
        }
    }
}
