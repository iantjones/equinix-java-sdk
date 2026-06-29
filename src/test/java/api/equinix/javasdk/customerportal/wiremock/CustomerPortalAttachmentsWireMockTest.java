package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.AttachmentPurpose;
import api.equinix.javasdk.customerportal.model.Attachment;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static api.equinix.javasdk.core.ResponseStubs.*;
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
