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

package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.AccountDetails;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
