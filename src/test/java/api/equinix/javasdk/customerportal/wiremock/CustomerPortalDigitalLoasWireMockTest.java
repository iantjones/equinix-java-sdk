package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.model.BetaTermsAgreement;
import api.equinix.javasdk.customerportal.model.DigitalLoa;
import api.equinix.javasdk.customerportal.model.DigitalLoaChange;
import api.equinix.javasdk.customerportal.model.LoaCustomerOrganization;
import api.equinix.javasdk.customerportal.model.PrivateBetaPermission;
import api.equinix.javasdk.customerportal.model.implementation.LoaLocation;
import api.equinix.javasdk.customerportal.model.implementation.LoaPatchPanel;
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
 * (and optional {@code product.type}) on {@code listOrganizations}, and the optional
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
    @DisplayName("listOrganizations(ibx) forwards the required location.ibx query param")
    void listOrganizations_forwardsLocationIbx() {
        wireMock.stubFor(get(urlPathEqualTo("/diloa/v1/organizations"))
                .willReturn(okJson("[{\"orgIds\":[\"123\"],\"name\":\"Acme Corp\"}]")));

        List<? extends LoaCustomerOrganization> organizations =
                customerPortal.digitalLoas().listOrganizations("AM11");

        assertNotNull(organizations);
        assertEquals(1, organizations.size());
        assertEquals("Acme Corp", organizations.get(0).getName());
        assertEquals(List.of("123"), organizations.get(0).getOrgIds());
        wireMock.verify(getRequestedFor(urlPathEqualTo("/diloa/v1/organizations"))
                .withQueryParam("location.ibx", equalTo("AM11")));
    }

    @Test
    @DisplayName("listOrganizations(ibx, productTypes) forwards location.ibx and product.type")
    void listOrganizations_forwardsLocationIbxAndProductType() {
        wireMock.stubFor(get(urlPathEqualTo("/diloa/v1/organizations"))
                .willReturn(okJson("[{\"orgIds\":[\"123\"],\"name\":\"Acme Corp\"}]")));

        customerPortal.digitalLoas().listOrganizations("AM11", List.of("CROSS_CONNECT"));

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

            DigitalLoa patched = customerPortal.digitalLoas().update("loa-abc-123", operations);

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

    @Nested
    @DisplayName("findByUuid")
    class FindByUuid {

        @Test
        @DisplayName("findByUuid(uuid) GETs /diloa/v1/digitalLoas/{uuid} and maps the document")
        void findByUuid_getsDocument() {
            stubSingleton(wireMock, "/diloa/v1/digitalLoas/loa-abc-123",
                    "/json/customerportal/digital_loa_response.json");

            DigitalLoa loa = customerPortal.digitalLoas().findByUuid("loa-abc-123");

            assertNotNull(loa);
            assertEquals("loa-abc-123", loa.getUuid());

            // Cross-connect a-side patch panel including its spec-required location object
            // (diLOA v1 PatchPanel.location, Location schema).
            LoaPatchPanel patchPanel = loa.getProducts().get(0).getCrossConnect().getASide().getPatchPanel();
            assertNotNull(patchPanel);
            assertEquals("CP:0218:0102:13008148", patchPanel.getId());
            assertEquals("AM11:04:000050:1613-S14", patchPanel.getCabinetSpaceId());
            assertEquals("AM11:04:000050", patchPanel.getCageSpaceId());

            LoaLocation location = patchPanel.getLocation();
            assertNotNull(location);
            assertEquals("AM11", location.getIbx());
            assertEquals(Region.EMEA, location.getRegion());
            assertEquals("Amsterdam", location.getMetroName());
            assertEquals("AM", location.getMetroCode());
            assertEquals("NL", location.getCountryCode());
            assertEquals("8 Buckingham Avenue, Slough Trading Estate, SL1 4AX Slough, England",
                    location.getAddress());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/diloa/v1/digitalLoas/loa-abc-123")));
        }
    }

    @Nested
    @DisplayName("findChangesByLoaUuid")
    class FindChangesByLoaUuid {

        @Test
        @DisplayName("findChangesByLoaUuid(uuid) GETs /diloa/v1/digitalLoas/{uuid}/changes and maps the array")
        void findChangesByLoaUuid_getsChanges() {
            stubPaginatedGet(wireMock, "/diloa/v1/digitalLoas/loa-abc-123/changes",
                    "/json/customerportal/paginated_digital_loa_changes.json");

            List<? extends DigitalLoaChange> changes =
                    customerPortal.digitalLoas().findChangesByLoaUuid("loa-abc-123");

            assertNotNull(changes);
            assertEquals(2, changes.size());
            assertEquals("change-1", changes.get(0).getUuid());
            assertEquals("change-2", changes.get(1).getUuid());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/diloa/v1/digitalLoas/loa-abc-123/changes")));
        }
    }

    @Nested
    @DisplayName("findChangeByUuid")
    class FindChangeByUuid {

        @Test
        @DisplayName("findChangeByUuid(uuid, changeUuid) GETs /diloa/v1/digitalLoas/{uuid}/changes/{changeUuid}")
        void findChangeByUuid_getsChange() {
            wireMock.stubFor(get(urlPathEqualTo("/diloa/v1/digitalLoas/loa-abc-123/changes/change-1"))
                    .willReturn(okJson("{\"uuid\":\"change-1\",\"type\":\"LOA_CREATION\",\"status\":\"COMPLETED\"}")));

            DigitalLoaChange change =
                    customerPortal.digitalLoas().findChangeByUuid("loa-abc-123", "change-1");

            assertNotNull(change);
            assertEquals("change-1", change.getUuid());
            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/diloa/v1/digitalLoas/loa-abc-123/changes/change-1")));
        }
    }

    @Nested
    @DisplayName("getBetaTermsAgreement")
    class GetBetaTermsAgreement {

        @Test
        @DisplayName("getBetaTermsAgreement() GETs /diloa/v1/betaTermsAgreement and maps the flag")
        void getBetaTermsAgreement_getsAgreement() {
            wireMock.stubFor(get(urlPathEqualTo("/diloa/v1/betaTermsAgreement"))
                    .willReturn(okJson("{\"agreementAccepted\":true}")));

            BetaTermsAgreement agreement = customerPortal.digitalLoas().getBetaTermsAgreement();

            assertNotNull(agreement);
            assertTrue(agreement.getAgreementAccepted());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/diloa/v1/betaTermsAgreement")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 on findByUuid() (GET) throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/diloa/v1/digitalLoas/[^/]+",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"LOA not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.digitalLoas().findByUuid("missing-loa"));
        }

        @Test
        @DisplayName("401 on listOrganizations() (GET) throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/diloa/v1/organizations",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> customerPortal.digitalLoas().listOrganizations("AM11"));
        }

        @Test
        @DisplayName("403 on cancel() (DELETE) throws EquinixAuthorizationException")
        void forbiddenCancel() {
            stubErrorInline(wireMock, "/diloa/v1/digitalLoas/loa-abc-123",
                    403, "[{\"errorCode\":\"ERR-403\",\"errorMessage\":\"Forbidden\"}]");

            assertThrows(EquinixAuthorizationException.class,
                    () -> customerPortal.digitalLoas().cancel("loa-abc-123"));
        }

        @Test
        @DisplayName("409 on update() (PATCH) throws EquinixConflictException")
        void conflictOnPatch() {
            stubErrorInline(wireMock, "/diloa/v1/digitalLoas/loa-abc-123",
                    409, "[{\"errorCode\":\"ERR-409\",\"errorMessage\":\"LOA state conflict\"}]");

            List<Map<String, Object>> operations = List.of(
                    Map.of("op", "replace", "path", "/notes", "value", "Updated notes"));

            assertThrows(EquinixConflictException.class,
                    () -> customerPortal.digitalLoas().update("loa-abc-123", operations));
        }

        @Test
        @DisplayName("429 on create() (POST) throws EquinixRateLimitException")
        void rateLimitedCreate() {
            stubErrorInline(wireMock, "/diloa/v1/digitalLoas",
                    429, "[{\"errorCode\":\"ERR-429\",\"errorMessage\":\"Too many requests\"}]");

            DigitalLoaCreateRequest request = DigitalLoaCreateRequest
                    .builder(
                            List.of(Map.of("type", "CROSS_CONNECT", "ibx", "AM11")),
                            Map.of("name", "Acme Corp", "accountNumber", "1234567"),
                            Map.of("name", "Globex LLC", "accountNumber", "7654321"))
                    .build();

            assertThrows(EquinixRateLimitException.class,
                    () -> customerPortal.digitalLoas().create(request));
        }

        @Test
        @DisplayName("500 on search() (POST) throws EquinixServerException")
        void serverErrorOnSearch() {
            stubErrorInline(wireMock, "/diloa/v1/digitalLoas/search",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.digitalLoas()
                            .search(new DigitalLoaSearchRequest(Map.of("state", "ACTIVE"))));
        }
    }
}
