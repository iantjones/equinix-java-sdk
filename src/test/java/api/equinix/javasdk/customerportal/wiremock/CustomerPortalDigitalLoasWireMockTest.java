package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.customerportal.model.DigitalLoa;
import api.equinix.javasdk.customerportal.model.LoaCustomerOrganization;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaSearchRequest;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

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
}
