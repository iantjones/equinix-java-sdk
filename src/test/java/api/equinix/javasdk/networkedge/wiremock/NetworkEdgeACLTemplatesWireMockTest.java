package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge ACL Templates.
 */
class NetworkEdgeACLTemplatesWireMockTest extends WireMockTestBase {

    static NetworkEdge networkEdge;

    @BeforeAll
    static void setUp() {
        networkEdge = new NetworkEdge(testCredentials());
        redirectToWireMock(networkEdge);
        networkEdge.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (networkEdge != null) networkEdge.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns ACL template for valid UUID")
        void returnsAclTemplate() {
            stubSingleton(wireMock, "/ne/v1/aclTemplates/.*",
                    "/json/networkedge/acltemplate_response.json");

            ACLTemplate template = networkEdge.aclTemplates().getByUuid("acl-1111-2222-3333-444455556666");
            assertNotNull(template);
            assertEquals("acl-1111-2222-3333-444455556666", template.getUuid());
            assertEquals("test-acl-template", template.getName());
        }

        @Test
        @DisplayName("returns ACL template when accountUcmId supplied")
        void returnsAclTemplateWithAccount() {
            stubSingleton(wireMock, "/ne/v1/aclTemplates/.*",
                    "/json/networkedge/acltemplate_response.json");

            ACLTemplate template = networkEdge.aclTemplates()
                    .getByUuid("acl-1111-2222-3333-444455556666", "account-ucm-123");
            assertNotNull(template);
            assertEquals("acl-1111-2222-3333-444455556666", template.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/ne/v1/aclTemplates/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"ACL template not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> networkEdge.aclTemplates().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/aclTemplates/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.aclTemplates().getByUuid("test-uuid"));
        }
    }
}
