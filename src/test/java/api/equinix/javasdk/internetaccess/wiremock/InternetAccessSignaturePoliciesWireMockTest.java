package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.SignaturePolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * WireMock-backed tests for the Equinix Internet Access (EIA) v1 signature-policies lookup
 * ({@code GET /internetAccess/v1/signaturePolicies}). These verify the v1 URI/version routing
 * (the {@code SignaturePoliciesV1} group in {@code apiParams_InternetAccess.json}, whose
 * {@code rootUri} is {@code signaturePolicies}), that the unfiltered {@code list()} sends no
 * query parameters, and that {@code list(countryCode)} places the country code on the
 * {@code location.countryCode} query parameter.
 */
class InternetAccessSignaturePoliciesWireMockTest extends WireMockTestBase {

    static InternetAccess internetAccess;

    @BeforeAll
    static void setUp() {
        internetAccess = new InternetAccess(testCredentials());
        redirectToWireMock(internetAccess);
        internetAccess.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (internetAccess != null) internetAccess.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    private static String page(String dataJson) {
        return "{ \"pagination\": { \"offset\": 0, \"limit\": 50, \"total\": 1 }, \"data\": [" + dataJson + "] }";
    }

    @Nested
    class ListAll {

        @Test
        void list_routesToV1WithNoQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/signaturePolicies"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page("{ \"signature\": { \"signatory\": \"DELEGATE\" }, "
                                    + "\"location\": { \"countryCode\": \"US\" } }"))));

            PaginatedList<SignaturePolicy> policies = internetAccess.signaturePolicies().list();

            assertEquals(1, policies.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/signaturePolicies"))
                    .withQueryParam("location.countryCode", absent()));
        }
    }

    @Nested
    class ListByCountry {

        @Test
        void list_placesCountryCodeOnLocationCountryCodeParam() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/signaturePolicies"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page("{ \"signature\": { \"signatory\": \"DELEGATE\" }, "
                                    + "\"location\": { \"countryCode\": \"PL\" } }"))));

            PaginatedList<SignaturePolicy> policies = internetAccess.signaturePolicies().list("PL");

            assertEquals(1, policies.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/signaturePolicies"))
                    .withQueryParam("location.countryCode", equalTo("PL")));
        }
    }
}
