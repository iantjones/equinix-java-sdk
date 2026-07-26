/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.internetaccess.wiremock;

import com.eqixiac.equinix.InternetAccess;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixAuthenticationException;
import com.eqixiac.equinix.core.exception.EquinixAuthorizationException;
import com.eqixiac.equinix.core.exception.EquinixConflictException;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.exception.EquinixRateLimitException;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.internetaccess.model.AccountAgreement;
import com.eqixiac.equinix.internetaccess.model.AccountDetails;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock-backed tests for the Equinix Internet Access (EIA) v1 Accounts read surface — the
 * {@code AccountsV1} group in {@code apiParams_InternetAccess.json}. Covers the project-narrowed
 * {@code list(operationalUnitsIbx, projectId)} overload (adding the {@code project.projectId} query
 * parameter, absent from the single-arg overload) and the single-account
 * {@code getByNumber(accountNumber)} path lookup. These route through the shared uriFormat
 * {@code internetAccess/v{version}/{rootUri}/{requestUri}} with {@code defaultVersion: 1} and
 * {@code rootUri: accounts}, i.e. {@code GET /internetAccess/v1/accounts} and
 * {@code GET /internetAccess/v1/accounts/{accountNumber}}.
 */
class InternetAccessAccountsReadWireMockTest extends WireMockTestBase {

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

    @Test
    void list_withProjectId_addsProjectQueryParam() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/accounts"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(page("{ \"accountNumber\": \"100013200\", \"accountName\": \"Acme\", \"orgId\": \"org-1\", "
                                + "\"organizationName\": \"Acme Org\", "
                                + "\"billing\": { \"currency\": \"USD\", \"poBearing\": true, \"poExempted\": false, \"signatureRequired\": true } }"))));

        PaginatedList<AccountDetails> accounts = internetAccess.accounts().list("SG1", "proj-42");

        assertEquals(1, accounts.size());
        assertEquals("100013200", accounts.get(0).getAccountNumber());
        assertEquals("Acme", accounts.get(0).getAccountName());
        assertEquals("org-1", accounts.get(0).getOrgId());
        assertNotNull(accounts.get(0).getBilling());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/accounts"))
                .withQueryParam("operationalUnits.ibxs.ibx", equalTo("SG1"))
                .withQueryParam("project.projectId", equalTo("proj-42")));
    }

    @Test
    void list_withNullProjectId_omitsProjectQueryParam() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/accounts"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(page("{ \"accountNumber\": \"100013200\", \"accountName\": \"Acme\", "
                                + "\"billing\": { \"currency\": \"USD\", \"poBearing\": true, \"poExempted\": false, \"signatureRequired\": true } }"))));

        PaginatedList<AccountDetails> accounts = internetAccess.accounts().list("SG1", null);

        assertEquals(1, accounts.size());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/accounts"))
                .withQueryParam("operationalUnits.ibxs.ibx", equalTo("SG1"))
                .withQueryParam("project.projectId", absent()));
    }

    @Test
    void getByNumber_singleGetOnAccountPath() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/accounts/100013200"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"accountNumber\": \"100013200\", \"accountName\": \"Acme\", \"orgId\": \"org-1\", "
                                + "\"organizationName\": \"Acme Org\", "
                                + "\"billing\": { \"currency\": \"USD\", \"poBearing\": true, \"poExempted\": false, \"signatureRequired\": true } }")));

        AccountDetails account = internetAccess.accounts().getByNumber("100013200");

        assertEquals("100013200", account.getAccountNumber());
        assertEquals("Acme", account.getAccountName());
        assertEquals("Acme Org", account.getOrganizationName());
        assertNotNull(account.getBilling());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/accounts/100013200")));
    }

    /**
     * Multi-page crossing for the v1 GET-list pipeline. The first request always carries
     * {@code offset=0&limit=100} (the SDK's PAGE_LIMIT default); page 2 must advance the offset
     * from the SERVER-reported pagination while carrying the original filter query parameters.
     */
    @Nested
    class Paging {

        private static String pageOf(int offset, String dataJson) {
            return "{ \"pagination\": { \"offset\": " + offset + ", \"limit\": 100, \"total\": 150 }, "
                    + "\"data\": [" + dataJson + "] }";
        }

        @Test
        void list_loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/accounts"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(pageOf(0, "{ \"accountNumber\": \"PAGE1_ACCT\" }"))));
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/accounts"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(pageOf(100, "{ \"accountNumber\": \"PAGE2_ACCT\" }"))));

            PaginatedList<AccountDetails> accounts = internetAccess.accounts().list("SG1");
            assertEquals(1, accounts.size());
            assertTrue(accounts.hasNextPage());

            accounts.loadAll();

            assertEquals(2, accounts.size());
            assertEquals("PAGE1_ACCT", accounts.get(0).getAccountNumber());
            assertEquals("PAGE2_ACCT", accounts.get(1).getAccountNumber());
            assertFalse(accounts.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried,
            // and the original ibx filter re-sent.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/internetAccess/v1/accounts"))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100"))
                    .withQueryParam("operationalUnits.ibxs.ibx", equalTo("SG1")));
        }

        @Test
        void agreements_loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
            String path = "/internetAccess/v1/accounts/100013200/agreements";
            wireMock.stubFor(get(urlPathEqualTo(path))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(pageOf(0, "{ \"ibx\": \"WA1\", \"type\": \"MCA_GTC\", \"valid\": true }"))));
            wireMock.stubFor(get(urlPathEqualTo(path))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(pageOf(100, "{ \"ibx\": \"WA1\", \"type\": \"MCA_GTC\", \"valid\": false }"))));

            PaginatedList<AccountAgreement> agreements = internetAccess.accounts().agreements("100013200", "WA1");
            assertEquals(1, agreements.size());
            assertTrue(agreements.hasNextPage());

            agreements.loadAll();

            assertEquals(2, agreements.size());
            assertEquals(Boolean.TRUE, agreements.get(0).getValid());
            assertEquals(Boolean.FALSE, agreements.get(1).getValid());
            assertFalse(agreements.hasNextPage());

            // Page 2 request: offset advanced, limit carried, ibx filter re-sent.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo(path))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100"))
                    .withQueryParam("ibx", equalTo("WA1")));
        }
    }

    /**
     * v1 error mapping — the first error-status coverage anywhere on the internetaccess v1 read
     * surface. The v1 error payload is a JSON array of {@code {"errorCode", "errorMessage"}}
     * objects, same envelope the shared {@code ResponseErrorMapper} parses for v2.
     */
    @Nested
    class Errors {

        private void list() {
            internetAccess.accounts().list("SG1");
        }

        @Test
        void badRequest400_throwsEquinixServiceException() {
            stubErrorInline(wireMock, "/internetAccess/v1/accounts",
                    400, "[{\"errorCode\":\"EQ-3000400\",\"errorMessage\":\"Invalid ibx\"}]");

            assertThrows(EquinixServiceException.class, this::list);
        }

        @Test
        void unauthorized401_throwsEquinixAuthenticationException() {
            stubErrorInline(wireMock, "/internetAccess/v1/accounts",
                    401, "[{\"errorCode\":\"EQ-3000401\",\"errorMessage\":\"Authentication failed\"}]");

            assertThrows(EquinixAuthenticationException.class, this::list);
        }

        @Test
        void forbidden403_throwsEquinixAuthorizationException() {
            stubErrorInline(wireMock, "/internetAccess/v1/accounts",
                    403, "[{\"errorCode\":\"EQ-3000403\",\"errorMessage\":\"Access denied\"}]");

            assertThrows(EquinixAuthorizationException.class, this::list);
        }

        @Test
        void notFound404_throwsEquinixNotFoundException() {
            stubErrorInline(wireMock, "/internetAccess/v1/accounts",
                    404, "[{\"errorCode\":\"EQ-3000404\",\"errorMessage\":\"Not found\"}]");

            assertThrows(EquinixNotFoundException.class, this::list);
        }

        @Test
        void conflict409_throwsEquinixConflictException() {
            stubErrorInline(wireMock, "/internetAccess/v1/accounts",
                    409, "[{\"errorCode\":\"EQ-3000409\",\"errorMessage\":\"Conflict\"}]");

            assertThrows(EquinixConflictException.class, this::list);
        }

        @Test
        void rateLimited429_throwsEquinixRateLimitException() {
            stubErrorInline(wireMock, "/internetAccess/v1/accounts",
                    429, "[{\"errorCode\":\"EQ-3000429\",\"errorMessage\":\"Too many requests\"}]");

            assertThrows(EquinixRateLimitException.class, this::list);
        }

        @Test
        void serverError500_throwsEquinixServerException() {
            stubErrorInline(wireMock, "/internetAccess/v1/accounts",
                    500, "[{\"errorCode\":\"EQ-3000500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class, this::list);
        }
    }
}
