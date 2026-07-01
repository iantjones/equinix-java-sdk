package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.enums.TermsProduct;
import api.equinix.javasdk.internetaccess.enums.TermsType;
import api.equinix.javasdk.internetaccess.model.TermsAndConditions;
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
 * WireMock-backed tests for the Equinix Internet Access (EIA) v1 terms-and-conditions lookup
 * ({@code GET /internetAccess/v1/terms}). These verify the {@code defaultVersion: 1} URI routing,
 * the fixed {@code connectivitySource.type=COLO} query param, and the placement of the required and
 * optional query parameters for both {@code list(...)} overloads.
 */
class InternetAccessTermsWireMockTest extends WireMockTestBase {

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

    private static final String TERM_JSON =
            "{ \"text\": \"These are the terms.\", \"version\": \"1.0\", \"language\": \"en-US\", "
                    + "\"type\": \"TERMS_AND_CONDITIONS\", \"product\": \"IA_C\", "
                    + "\"location\": { \"ibx\": \"WA1\" }, \"account\": { \"accountNumber\": \"100013200\" } }";

    @Nested
    class ListRequired {

        @Test
        void list_routesToV1AndSendsRequiredQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/terms"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page(TERM_JSON))));

            PaginatedList<TermsAndConditions> terms =
                    internetAccess.termsAndConditions().list("100013200", "WA1", TermsProduct.IA_C);

            assertEquals(1, terms.size());
            assertEquals("These are the terms.", terms.get(0).getText());
            assertEquals(TermsType.TERMS_AND_CONDITIONS, terms.get(0).getType());
            assertEquals(TermsProduct.IA_C, terms.get(0).getProduct());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/terms"))
                    .withQueryParam("account.accountNumber", equalTo("100013200"))
                    .withQueryParam("location.ibx", equalTo("WA1"))
                    .withQueryParam("connectivitySource.type", equalTo("COLO"))
                    .withQueryParam("product", equalTo("IA_C"))
                    .withQueryParam("type", absent())
                    .withQueryParam("language", absent()));
        }
    }

    @Nested
    class ListFiltered {

        @Test
        void list_sendsOptionalTypeAndLanguageQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/terms"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page(TERM_JSON))));

            PaginatedList<TermsAndConditions> terms =
                    internetAccess.termsAndConditions().list("100013200", "WA1", TermsProduct.IA_VC,
                            TermsType.RENEWAL_TERMS, "en-US");

            assertEquals(1, terms.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/terms"))
                    .withQueryParam("account.accountNumber", equalTo("100013200"))
                    .withQueryParam("location.ibx", equalTo("WA1"))
                    .withQueryParam("connectivitySource.type", equalTo("COLO"))
                    .withQueryParam("product", equalTo("IA_VC"))
                    .withQueryParam("type", equalTo("RENEWAL_TERMS"))
                    .withQueryParam("language", equalTo("en-US")));
        }

        @Test
        void list_omitsTypeAndLanguageWhenNull() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/terms"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page(TERM_JSON))));

            internetAccess.termsAndConditions().list("100013200", "WA1", TermsProduct.MC_C, null, null);

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/terms"))
                    .withQueryParam("product", equalTo("MC_C"))
                    .withQueryParam("connectivitySource.type", equalTo("COLO"))
                    .withQueryParam("type", absent())
                    .withQueryParam("language", absent()));
        }
    }
}
