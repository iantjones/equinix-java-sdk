package com.eqixiac.equinix.internetaccess.wiremock;

import com.eqixiac.equinix.InternetAccess;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.internetaccess.enums.TermsProduct;
import com.eqixiac.equinix.internetaccess.enums.TermsType;
import com.eqixiac.equinix.internetaccess.model.TermsAndConditions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.eqixiac.equinix.core.ResponseStubs.stubErrorInline;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Nested
    class PagingAndErrors {

        @Test
        void list_loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
            // First request always carries offset=0&limit=100 (PAGE_LIMIT default); page 2 must
            // advance the offset from the SERVER-reported pagination, re-sending the filters.
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/terms"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson("{ \"pagination\": { \"offset\": 0, \"limit\": 100, \"total\": 150 }, "
                            + "\"data\": [ { \"text\": \"PAGE1_TERMS\" } ] }")));
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/terms"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson("{ \"pagination\": { \"offset\": 100, \"limit\": 100, \"total\": 150 }, "
                            + "\"data\": [ { \"text\": \"PAGE2_TERMS\" } ] }")));

            PaginatedList<TermsAndConditions> terms =
                    internetAccess.termsAndConditions().list("100013200", "WA1", TermsProduct.IA_C);
            assertEquals(1, terms.size());
            assertTrue(terms.hasNextPage());

            terms.loadAll();

            assertEquals(2, terms.size());
            assertEquals("PAGE1_TERMS", terms.get(0).getText());
            assertEquals("PAGE2_TERMS", terms.get(1).getText());
            assertFalse(terms.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/internetAccess/v1/terms"))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100"))
                    .withQueryParam("account.accountNumber", equalTo("100013200"))
                    .withQueryParam("location.ibx", equalTo("WA1"))
                    .withQueryParam("product", equalTo("IA_C")));
        }

        @Test
        void list_badRequest400_throwsEquinixServiceException() {
            stubErrorInline(wireMock, "/internetAccess/v1/terms",
                    400, "[{\"errorCode\":\"EQ-3000400\",\"errorMessage\":\"Invalid product\"}]");

            assertThrows(EquinixServiceException.class,
                    () -> internetAccess.termsAndConditions().list("100013200", "WA1", TermsProduct.IA_C));
        }
    }
}
