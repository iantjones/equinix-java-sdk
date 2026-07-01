package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.customerportal.model.BetaTermsAgreement;
import api.equinix.javasdk.customerportal.model.DigitalLoa;
import api.equinix.javasdk.customerportal.model.LoaCustomerOrganization;
import api.equinix.javasdk.customerportal.model.PrivateBetaPermission;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaCreateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.PrivateBetaAccessRequest;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal Digital LOA (diLOA v1) client, focused on the
 * query-parameter forwarding that the public API exposes: the spec-required {@code location.ibx}
 * (and optional {@code product.type}) on {@code findOrganizations}, and the optional
 * {@code offset}/{@code limit}/{@code sort} paging params on {@code search}.
 */
class CustomerPortalDigitalLoasWireMockTest extends WireMockTestBase {

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

    @Test
    @DisplayName("findOrganizations(ibx) forwards the required location.ibx query param")
    void findOrganizations_forwardsLocationIbx() {
        wireMock.stubFor(get(urlPathEqualTo("/diloa/v1/organizations"))
                .willReturn(okJson("[{\"orgIds\":[\"123\"],\"name\":\"Acme Corp\"}]")));

        List<? extends LoaCustomerOrganization> organizations =
                customerPortal.digitalLoas().findOrganizations("AM11");

        assertNotNull(organizations);
        assertEquals(1, organizations.size());
        assertEquals("Acme Corp", organizations.get(0).getName());
        assertEquals(List.of("123"), organizations.get(0).getOrgIds());
        wireMock.verify(getRequestedFor(urlPathEqualTo("/diloa/v1/organizations"))
                .withQueryParam("location.ibx", equalTo("AM11")));
    }

    @Test
    @DisplayName("findOrganizations(ibx, productTypes) forwards location.ibx and product.type")
    void findOrganizations_forwardsLocationIbxAndProductType() {
        wireMock.stubFor(get(urlPathEqualTo("/diloa/v1/organizations"))
                .willReturn(okJson("[{\"orgIds\":[\"123\"],\"name\":\"Acme Corp\"}]")));

        customerPortal.digitalLoas().findOrganizations("AM11", List.of("CROSS_CONNECT"));

        wireMock.verify(getRequestedFor(urlPathEqualTo("/diloa/v1/organizations"))
                .withQueryParam("location.ibx", equalTo("AM11"))
                .withQueryParam("product.type", equalTo("CROSS_CONNECT")));
    }

    @Test
    @DisplayName("search(request, offset, limit, sort) forwards the paging/sort query params")
    void search_forwardsPagingAndSort() {
        wireMock.stubFor(post(urlPathEqualTo("/diloa/v1/digitalLoas/search"))
                .willReturn(okJson("{\"data\":[{\"uuid\":\"loa-1\"}]}")));

        List<? extends DigitalLoa> results = customerPortal.digitalLoas()
                .search(new DigitalLoaSearchRequest(Map.of("state", "ACTIVE")), 10, 20, List.of("-/expiryDateTime"));

        assertNotNull(results);
        assertEquals(1, results.size());
        wireMock.verify(postRequestedFor(urlPathEqualTo("/diloa/v1/digitalLoas/search"))
                .withQueryParam("offset", equalTo("10"))
                .withQueryParam("limit", equalTo("20"))
                .withQueryParam("sort", equalTo("-/expiryDateTime"))
                .withRequestBody(matchingJsonPath("$.filter.state", equalTo("ACTIVE"))));
    }

    @Test
    @DisplayName("search(request) sends no paging query params")
    void search_noPagingParams() {
        wireMock.stubFor(post(urlPathEqualTo("/diloa/v1/digitalLoas/search"))
                .willReturn(okJson("{\"data\":[]}")));

        customerPortal.digitalLoas().search(new DigitalLoaSearchRequest(Map.of("state", "ACTIVE")));

        wireMock.verify(postRequestedFor(urlPathEqualTo("/diloa/v1/digitalLoas/search"))
                .withQueryParam("offset", absent())
                .withQueryParam("limit", absent())
                .withQueryParam("sort", absent()));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("create(request) POSTs to /diloa/v1/digitalLoas with the serialized body")
        void create_postsBody() {
            stubCreate(wireMock, "/diloa/v1/digitalLoas", "/json/customerportal/digital_loa_response.json");

            DigitalLoaCreateRequest request = DigitalLoaCreateRequest
                    .builder(
                            List.of(Map.of("type", "CROSS_CONNECT", "ibx", "AM11")),
                            Map.of("name", "Acme Corp", "accountNumber", "1234567"),
                            Map.of("name", "Globex LLC", "accountNumber", "7654321"))
                    .notes("Cross connect authorization for AM11")
                    .expiryDateTime("2026-12-31T23:59:59Z")
                    .build();

            DigitalLoa created = customerPortal.digitalLoas().create(request);

            assertNotNull(created);
            assertEquals("loa-abc-123", created.getUuid());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/diloa/v1/digitalLoas"))
                    .withRequestBody(matchingJsonPath("$.products[0].type", equalTo("CROSS_CONNECT")))
                    .withRequestBody(matchingJsonPath("$.requestor.name", equalTo("Acme Corp")))
                    .withRequestBody(matchingJsonPath("$.provider.name", equalTo("Globex LLC")))
                    .withRequestBody(matchingJsonPath("$.notes", equalTo("Cross connect authorization for AM11")))
                    .withRequestBody(matchingJsonPath("$.expiryDateTime", equalTo("2026-12-31T23:59:59Z"))));
        }
    }

    @Nested
    @DisplayName("patch")
    class Patch {

        @Test
        @DisplayName("patch(uuid, operations) PATCHes /diloa/v1/digitalLoas/{uuid} with the JSON-patch array")
        void patch_sendsPatchArray() {
            wireMock.stubFor(patch(urlPathEqualTo("/diloa/v1/digitalLoas/loa-abc-123"))
                    .willReturn(okJson(loadFixture("/json/customerportal/digital_loa_response.json"))));

            List<Map<String, Object>> operations = List.of(
                    Map.of("op", "replace", "path", "/notes", "value", "Updated notes"));

            DigitalLoa patched = customerPortal.digitalLoas().patch("loa-abc-123", operations);

            assertNotNull(patched);
            assertEquals("loa-abc-123", patched.getUuid());
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/diloa/v1/digitalLoas/loa-abc-123"))
                    .withRequestBody(matchingJsonPath("$[0].op", equalTo("replace")))
                    .withRequestBody(matchingJsonPath("$[0].path", equalTo("/notes")))
                    .withRequestBody(matchingJsonPath("$[0].value", equalTo("Updated notes"))));
        }
    }

    @Nested
    @DisplayName("updateBetaTermsAgreement")
    class UpdateBetaTermsAgreement {

        @Test
        @DisplayName("updateBetaTermsAgreement(true) PUTs /diloa/v1/betaTermsAgreement with the flag body")
        void updateBetaTermsAgreement_putsFlag() {
            wireMock.stubFor(put(urlPathEqualTo("/diloa/v1/betaTermsAgreement"))
                    .willReturn(okJson("{\"agreementAccepted\":true}")));

            BetaTermsAgreement agreement = customerPortal.digitalLoas().updateBetaTermsAgreement(true);

            assertNotNull(agreement);
            assertTrue(agreement.getAgreementAccepted());
            wireMock.verify(putRequestedFor(urlPathEqualTo("/diloa/v1/betaTermsAgreement"))
                    .withRequestBody(matchingJsonPath("$.agreementAccepted", equalTo("true"))));
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("cancel(uuid) DELETEs /diloa/v1/digitalLoas/{uuid} and returns true on 204")
        void cancel_deletes() {
            stubDeleteNoContent(wireMock, "/diloa/v1/digitalLoas/loa-abc-123");

            Boolean result = customerPortal.digitalLoas().cancel("loa-abc-123");

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/diloa/v1/digitalLoas/loa-abc-123")));
        }
    }

    @Nested
    @DisplayName("performAction")
    class PerformAction {

        @Test
        @DisplayName("performAction(uuid, action) POSTs /diloa/v1/digitalLoas/{uuid}/actions with the action body")
        void performAction_postsAction() {
            wireMock.stubFor(post(urlPathEqualTo("/diloa/v1/digitalLoas/loa-abc-123/actions"))
                    .willReturn(okJson(loadFixture("/json/customerportal/digital_loa_response.json"))));

            DigitalLoa actioned = customerPortal.digitalLoas()
                    .performAction("loa-abc-123", Map.of("type", "SUBMIT"));

            assertNotNull(actioned);
            assertEquals("loa-abc-123", actioned.getUuid());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/diloa/v1/digitalLoas/loa-abc-123/actions"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("SUBMIT"))));
        }
    }

    @Nested
    @DisplayName("createRequest")
    class CreateRequest {

        @Test
        @DisplayName("createRequest(request) POSTs /diloa/v1/digitalLoas/loaRequests and returns true on 2xx")
        void createRequest_postsBody() {
            wireMock.stubFor(post(urlPathEqualTo("/diloa/v1/digitalLoas/loaRequests"))
                    .willReturn(aResponse().withStatus(201)));

            Boolean result = customerPortal.digitalLoas()
                    .createRequest(Map.of("ibx", "AM11", "product", Map.of("type", "CROSS_CONNECT")));

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/diloa/v1/digitalLoas/loaRequests"))
                    .withRequestBody(matchingJsonPath("$.ibx", equalTo("AM11")))
                    .withRequestBody(matchingJsonPath("$.product.type", equalTo("CROSS_CONNECT"))));
        }
    }

    @Nested
    @DisplayName("isPrivateBetaAllowed")
    class IsPrivateBetaAllowed {

        @Test
        @DisplayName("isPrivateBetaAllowed() GETs /diloa/v1/privateBetaAccess and maps the permission flag")
        void isPrivateBetaAllowed_getsPermission() {
            wireMock.stubFor(get(urlPathEqualTo("/diloa/v1/privateBetaAccess"))
                    .willReturn(okJson("{\"privateBetaTests\":true}")));

            PrivateBetaPermission permission = customerPortal.digitalLoas().isPrivateBetaAllowed();

            assertNotNull(permission);
            assertTrue(permission.getPrivateBetaTests());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/diloa/v1/privateBetaAccess")));
        }
    }

    @Nested
    @DisplayName("createPrivateBetaAccessRequest")
    class CreatePrivateBetaAccessRequest {

        @Test
        @DisplayName("createPrivateBetaAccessRequest(request) POSTs /diloa/v1/privateBetaAccess with the body")
        void createPrivateBetaAccessRequest_postsBody() {
            wireMock.stubFor(post(urlPathEqualTo("/diloa/v1/privateBetaAccess"))
                    .willReturn(aResponse().withStatus(201)));

            PrivateBetaAccessRequest request = PrivateBetaAccessRequest
                    .builder("user@acme.com", "Acme Corp")
                    .build();

            Boolean result = customerPortal.digitalLoas().createPrivateBetaAccessRequest(request);

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/diloa/v1/privateBetaAccess"))
                    .withRequestBody(matchingJsonPath("$.email", equalTo("user@acme.com")))
                    .withRequestBody(matchingJsonPath("$.companyName", equalTo("Acme Corp"))));
        }
    }
}
