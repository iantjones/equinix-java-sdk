package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.enums.ACLInterfaceType;
import api.equinix.javasdk.networkedge.enums.DeviceACLStatus;
import api.equinix.javasdk.networkedge.enums.Protocol;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.implementation.VirtualDeviceACLDetail;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

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

            // Spec ACLTemplateDetailsResponse.virtualDeviceDetails (VirtualDeviceACLDetails[]) —
            // the devices this template is attached to must map.
            assertNotNull(template.getVirtualDeviceDetails());
            assertEquals(1, template.getVirtualDeviceDetails().size());
            VirtualDeviceACLDetail detail = template.getVirtualDeviceDetails().get(0);
            assertEquals("Test Device", detail.getName());
            assertEquals("ce7ef79e-31e7-4769-be5b-e192496f48ab", detail.getUuid());
            assertEquals(ACLInterfaceType.WAN, detail.getInterfaceType());
            assertEquals(DeviceACLStatus.PROVISIONED, detail.getAclStatus());
            // Spec createdDateTime maps through the Lifecycle createdDate accessor.
            assertEquals(LocalDateTime.of(2020, 10, 3, 19, 41, 17), template.getCreatedDate());
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
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("getByUuid then update().save() PUTs the changed body and re-GETs the template")
        void updatesAclTemplate() {
            // GET seeds the updater from the current template body (incl. existing inboundRules),
            // and the PUT's follow-up GET returns the refreshed body.
            stubSingleton(wireMock, "/ne/v1/aclTemplates/.*", "/json/networkedge/acltemplate_response.json");
            // PUT /ne/v1/aclTemplates/{uuid} -> UpdateACLTemplate (200, body ignored by the client).
            wireMock.stubFor(put(urlPathMatching("/ne/v1/aclTemplates/.*"))
                    .willReturn(aResponse().withStatus(200)));

            ACLTemplate template = networkEdge.aclTemplates().getByUuid("acl-1111-2222-3333-444455556666");

            ACLTemplate updated = template.update()
                    .withName("renamed-acl-template")
                    .withDescription("Updated description")
                    .addRule(Protocol.UDP, "any", "53", "192.168.0.0/24", 2)
                    .save();

            assertNotNull(updated);

            // The updater is seeded from the fixture (rule 0 preserved) and rule 1 is appended.
            // accountUcmId is @JsonIgnore on ACLTemplateUpdaterJson, so it never enters the body.
            wireMock.verify(putRequestedFor(urlPathMatching("/ne/v1/aclTemplates/acl-1111-2222-3333-444455556666"))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("renamed-acl-template")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Updated description")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[0].protocol", equalTo("TCP")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[0].dstPort", equalTo("22")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[1].protocol", equalTo("UDP")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[1].srcPort", equalTo("any")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[1].dstPort", equalTo("53")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[1].subnet", equalTo("192.168.0.0/24")))
                    .withRequestBody(matchingJsonPath("$.inboundRules[1].seqNo", equalTo("2"))));
        }

        @Test
        @DisplayName("update().forCustomer(...).save() carries accountUcmId as a query param, not in the body")
        void updatesAclTemplateWithAccount() {
            stubSingleton(wireMock, "/ne/v1/aclTemplates/.*", "/json/networkedge/acltemplate_response.json");
            wireMock.stubFor(put(urlPathMatching("/ne/v1/aclTemplates/.*"))
                    .willReturn(aResponse().withStatus(200)));

            ACLTemplate template = networkEdge.aclTemplates().getByUuid("acl-1111-2222-3333-444455556666");

            template.update()
                    .forCustomer("account-ucm-123")
                    .withDescription("Account-scoped update")
                    .save();

            wireMock.verify(putRequestedFor(urlPathMatching("/ne/v1/aclTemplates/acl-1111-2222-3333-444455556666"))
                    .withQueryParam("accountUcmId", equalTo("account-ucm-123"))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Account-scoped update"))));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("delete() DELETEs the template with no accountUcmId query param")
        void deletesAclTemplate() {
            stubSingleton(wireMock, "/ne/v1/aclTemplates/.*", "/json/networkedge/acltemplate_response.json");
            wireMock.stubFor(delete(urlPathMatching("/ne/v1/aclTemplates/.*"))
                    .willReturn(aResponse().withStatus(204)));

            ACLTemplate template = networkEdge.aclTemplates().getByUuid("acl-1111-2222-3333-444455556666");

            assertTrue(template.delete());

            wireMock.verify(deleteRequestedFor(urlPathMatching("/ne/v1/aclTemplates/acl-1111-2222-3333-444455556666"))
                    .withoutQueryParam("accountUcmId"));
        }

        @Test
        @DisplayName("delete(accountUcmId) DELETEs the template carrying accountUcmId as a query param")
        void deletesAclTemplateWithAccount() {
            stubSingleton(wireMock, "/ne/v1/aclTemplates/.*", "/json/networkedge/acltemplate_response.json");
            wireMock.stubFor(delete(urlPathMatching("/ne/v1/aclTemplates/.*"))
                    .willReturn(aResponse().withStatus(204)));

            ACLTemplate template = networkEdge.aclTemplates().getByUuid("acl-1111-2222-3333-444455556666");

            assertTrue(template.delete("account-ucm-123"));

            wireMock.verify(deleteRequestedFor(urlPathMatching("/ne/v1/aclTemplates/acl-1111-2222-3333-444455556666"))
                    .withQueryParam("accountUcmId", equalTo("account-ucm-123")));
        }
    }

    @Nested
    @DisplayName("list()")
    class List {

        @Test
        @DisplayName("list() GETs /ne/v1/aclTemplates with no accountUcmId query param")
        void listsAclTemplates() {
            stubPaginatedGet(wireMock, "/ne/v1/aclTemplates/?",
                    "/json/networkedge/acltemplate_list_response.json");

            PaginatedList<ACLTemplate> templates = networkEdge.aclTemplates().list();

            assertNotNull(templates);
            assertEquals(2, templates.size());
            assertEquals("acl-1111-2222-3333-444455556666", templates.get(0).getUuid());
            assertEquals("test-acl-template", templates.get(0).getName());
            assertEquals("acl-aaaa-bbbb-cccc-ddddeeeeffff", templates.get(1).getUuid());
            assertEquals("second-acl-template", templates.get(1).getName());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/aclTemplates"))
                    .withoutQueryParam("accountUcmId"));
        }

        @Test
        @DisplayName("list(accountUcmId) GETs /ne/v1/aclTemplates carrying accountUcmId as a query param")
        void listsAclTemplatesForAccount() {
            stubPaginatedGet(wireMock, "/ne/v1/aclTemplates/?",
                    "/json/networkedge/acltemplate_list_response.json");

            PaginatedList<ACLTemplate> templates = networkEdge.aclTemplates().list("account-ucm-123");

            assertNotNull(templates);
            assertEquals(2, templates.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/aclTemplates"))
                    .withQueryParam("accountUcmId", equalTo("account-ucm-123")));
        }
    }

    @Nested
    @DisplayName("refresh() / refresh(accountUcmId)")
    class Refresh {

        private static final String UUID = "acl-1111-2222-3333-444455556666";
        private static final String PATH = "/ne/v1/aclTemplates/" + UUID;

        private void stubRefreshScenario() {
            // First GET returns the original state; the second GET — triggered by
            // wrapper.refresh() — returns a DIFFERENT payload (renamed, new description).
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .inScenario("acl-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/networkedge/acltemplate_response.json")))
                    .willSetStateTo("state-changed"));
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .inScenario("acl-refresh")
                    .whenScenarioStateIs("state-changed")
                    .willReturn(okJson(loadFixture("/json/networkedge/acltemplate_response_refreshed.json"))));
        }

        @Test
        @DisplayName("refresh() re-GETs the template (no accountUcmId param) and updates the wrapper in place")
        void refreshesInPlace() {
            stubRefreshScenario();

            ACLTemplate template = networkEdge.aclTemplates().getByUuid(UUID);
            assertEquals("test-acl-template", template.getName());
            assertEquals("Test ACL template for WireMock", template.getDescription());

            assertTrue(template.refresh());

            // The same wrapper instance now reflects the re-fetched server state.
            assertEquals("renamed-acl-template", template.getName());
            assertEquals("Refreshed ACL template description", template.getDescription());
            assertEquals(UUID, template.getUuid());

            wireMock.verify(2, getRequestedFor(urlPathEqualTo(PATH)));
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(PATH))
                    .withoutQueryParam("accountUcmId"));
        }

        @Test
        @DisplayName("refresh(accountUcmId) re-GETs the template carrying accountUcmId as a query param")
        void refreshesWithAccount() {
            stubRefreshScenario();

            ACLTemplate template = networkEdge.aclTemplates().getByUuid(UUID);
            assertEquals("test-acl-template", template.getName());

            assertTrue(template.refresh("account-ucm-123"));

            assertEquals("renamed-acl-template", template.getName());

            wireMock.verify(2, getRequestedFor(urlPathEqualTo(PATH)));
            // The refresh GET (and only it) carries the accountUcmId query param.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH))
                    .withQueryParam("accountUcmId", equalTo("account-ucm-123")));
        }
    }

    @Nested
    @DisplayName("Multi-page list paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 1, "total": 2 },
                  "data": [ {
                    "uuid": "acl-1111-2222-3333-444455556666",
                    "name": "page1-acl-template",
                    "description": "First page template",
                    "inboundRules": [ {
                      "subnet": "10.0.0.0/24",
                      "protocol": "TCP",
                      "srcPort": "any",
                      "dstPort": "22"
                    } ]
                  } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 1, "limit": 1, "total": 2 },
                  "data": [ {
                    "uuid": "acl-aaaa-bbbb-cccc-ddddeeeeffff",
                    "name": "page2-acl-template",
                    "description": "Second page template",
                    "inboundRules": [ {
                      "subnet": "192.168.0.0/24",
                      "protocol": "UDP",
                      "srcPort": "any",
                      "dstPort": "53"
                    } ]
                  } ]
                }
                """;

        @Test
        @DisplayName("loadAll() fetches page 2 by advancing the offset/limit query params")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/aclTemplates"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/aclTemplates"))
                    .withQueryParam("offset", equalTo("1"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<ACLTemplate> templates = networkEdge.aclTemplates().list();
            assertEquals(1, templates.size());
            assertTrue(templates.hasNextPage());

            templates.loadAll();

            assertEquals(2, templates.size());
            assertEquals("page1-acl-template", templates.get(0).getName());
            assertEquals("page2-acl-template", templates.get(1).getName());
            assertFalse(templates.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/ne/v1/aclTemplates"))
                    .withQueryParam("offset", equalTo("1"))
                    .withQueryParam("limit", equalTo("1")));
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
