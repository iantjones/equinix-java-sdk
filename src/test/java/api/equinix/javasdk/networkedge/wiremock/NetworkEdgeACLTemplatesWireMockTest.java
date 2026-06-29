package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.enums.Protocol;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
    @DisplayName("define() / save()")
    class Create {

        // Valid UUID for the 201 Location header (Constants.UUID_PATTERN = 8-4-4-4-12 hex).
        private static final String NEW_UUID = "c1d2e3f4-a5b6-7890-cdef-123456789012";

        @Test
        @DisplayName("POSTs the create body, follows the 201 Location header, and GETs the new ACL template")
        void createsAclTemplate() {
            // POST /ne/v1/aclTemplates -> 201 with Location header carrying the new uuid.
            wireMock.stubFor(post(urlPathMatching("/ne/v1/aclTemplates/?"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Location", "https://localhost/ne/v1/aclTemplates/" + NEW_UUID)));
            // GET /ne/v1/aclTemplates/{uuid} -> returns the created object body.
            stubSingleton(wireMock, "/ne/v1/aclTemplates/.*", "/json/networkedge/acltemplate_response.json");

            ACLTemplate template = networkEdge.aclTemplates()
                    .define("test-acl-template")
                    .forAccount("account-ucm-123")
                    .withDescription("Test ACL template for WireMock")
                    .withRule(Protocol.TCP, "any", "22", "10.0.0.0/24", 1)
                    .save();

            assertNotNull(template);
            // getUuid()/getName() reflect the fixture body returned by the follow-up GET.
            assertEquals("acl-1111-2222-3333-444455556666", template.getUuid());
            assertEquals("test-acl-template", template.getName());

            // Verify the outgoing create request body. accountUcmId is @JsonIgnore on
            // ACLTemplateCreatorJson, so it never appears in the POST body.
            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/aclTemplates/?"))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("test-acl-template")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Test ACL template for WireMock")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[0].protocol", equalTo("TCP")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[0].srcPort", equalTo("any")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[0].dstPort", equalTo("22")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[0].subnet", equalTo("10.0.0.0/24")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[0].seqNo", equalTo("1"))));

            // Regression guard: ACLTemplateCreatorJson's constructor previously dropped accountUcmId
            // (set via .forAccount(...)), so neither the create POST nor its follow-up GET carried
            // the required accountUcmId query param. Fixed — both now include it.
            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/aclTemplates/?"))
                    .withQueryParam("accountUcmId", equalTo("account-ucm-123")));
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/aclTemplates/" + NEW_UUID))
                    .withQueryParam("accountUcmId", equalTo("account-ucm-123")));
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
