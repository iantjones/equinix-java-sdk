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
import api.equinix.javasdk.core.exception.EquinixAuthenticationException;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.PatchPanel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static api.equinix.javasdk.core.ResponseStubs.stubErrorInline;
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
 * WireMock-backed tests for the Equinix Internet Access (EIA) v1 Patch Panels read surface — the
 * {@code PatchPanelsV1} group in {@code apiParams_InternetAccess.json}. Covers the
 * {@code list(ibx, accountNumber, cageSpaceId, cabinetSpaceId)} four-arg overload and the
 * {@code list(..., mediaTypesName)} five-arg overload. Both route through the shared uriFormat
 * {@code internetAccess/v{version}/{rootUri}} with {@code defaultVersion: 1} and
 * {@code rootUri: patchPanels}, i.e. {@code GET /internetAccess/v1/patchPanels}, asserting the
 * {@code location.ibx}, {@code account.accountNumber}, {@code cage.spaceId}, {@code cabinet.spaceId}
 * and optional {@code mediaTypes.name} query parameters.
 */
class InternetAccessPatchPanelsReadWireMockTest extends WireMockTestBase {

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

    private static final String PATCH_PANEL_JSON =
            "{ \"number\": \"PP-001\", \"customerRefNumber\": \"CRN-9\", "
                    + "\"type\": \"EQUINIX_PROVIDED\", \"prewired\": true, \"availablePortsCount\": 12, \"availablePorts\": [1, 2, 3], "
                    + "\"ownedPortsCount\": 0, \"mediaTypes\": [\"SM\"], "
                    + "\"cage\": { \"spaceId\": \"cage-1\" }, \"cabinet\": { \"spaceId\": \"cab-1\" }, "
                    + "\"location\": { \"ibx\": \"WA1\" }, \"account\": { \"accountNumber\": \"100013200\" } }";

    @Nested
    class List {

        @Test
        void list_fourArg_addsCoreQueryParams_omitsMediaType() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page(PATCH_PANEL_JSON))));

            PaginatedList<PatchPanel> panels =
                    internetAccess.patchPanels().list("WA1", "100013200", "cage-1", "cab-1");

            assertEquals(1, panels.size());
            assertEquals("PP-001", panels.get(0).getNumber());
            assertEquals("CRN-9", panels.get(0).getCustomerRefNumber());
            assertEquals(Integer.valueOf(12), panels.get(0).getAvailablePortsCount());
            assertNotNull(panels.get(0).getCage());
            assertNotNull(panels.get(0).getCabinet());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                    .withQueryParam("location.ibx", equalTo("WA1"))
                    .withQueryParam("account.accountNumber", equalTo("100013200"))
                    .withQueryParam("cage.spaceId", equalTo("cage-1"))
                    .withQueryParam("cabinet.spaceId", equalTo("cab-1"))
                    .withQueryParam("mediaTypes.name", absent()));
        }

        @Test
        void list_fiveArg_addsMediaTypeQueryParam() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page(PATCH_PANEL_JSON))));

            PaginatedList<PatchPanel> panels =
                    internetAccess.patchPanels().list("WA1", "100013200", "cage-1", "cab-1", "SM");

            assertEquals(1, panels.size());
            assertEquals("PP-001", panels.get(0).getNumber());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                    .withQueryParam("location.ibx", equalTo("WA1"))
                    .withQueryParam("account.accountNumber", equalTo("100013200"))
                    .withQueryParam("cage.spaceId", equalTo("cage-1"))
                    .withQueryParam("cabinet.spaceId", equalTo("cab-1"))
                    .withQueryParam("mediaTypes.name", equalTo("SM")));
        }

        @Test
        void list_fiveArg_withNullMediaType_omitsMediaTypeQueryParam() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page(PATCH_PANEL_JSON))));

            PaginatedList<PatchPanel> panels =
                    internetAccess.patchPanels().list("WA1", "100013200", "cage-1", "cab-1", null);

            assertEquals(1, panels.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                    .withQueryParam("location.ibx", equalTo("WA1"))
                    .withQueryParam("account.accountNumber", equalTo("100013200"))
                    .withQueryParam("cage.spaceId", equalTo("cage-1"))
                    .withQueryParam("cabinet.spaceId", equalTo("cab-1"))
                    .withQueryParam("mediaTypes.name", absent()));
        }

        @Test
        void list_loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
            // First request always carries offset=0&limit=100 (PAGE_LIMIT default); page 2 must
            // advance the offset from the SERVER-reported pagination, carrying the filters.
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson("{ \"pagination\": { \"offset\": 0, \"limit\": 100, \"total\": 150 }, "
                            + "\"data\": [ { \"number\": \"PAGE1_PANEL\" } ] }")));
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson("{ \"pagination\": { \"offset\": 100, \"limit\": 100, \"total\": 150 }, "
                            + "\"data\": [ { \"number\": \"PAGE2_PANEL\" } ] }")));

            PaginatedList<PatchPanel> panels =
                    internetAccess.patchPanels().list("WA1", "100013200", "cage-1", "cab-1");
            assertEquals(1, panels.size());
            assertTrue(panels.hasNextPage());

            panels.loadAll();

            assertEquals(2, panels.size());
            assertEquals("PAGE1_PANEL", panels.get(0).getNumber());
            assertEquals("PAGE2_PANEL", panels.get(1).getNumber());
            assertFalse(panels.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100"))
                    .withQueryParam("location.ibx", equalTo("WA1"))
                    .withQueryParam("account.accountNumber", equalTo("100013200")));
        }

        @Test
        void list_unauthorized401_throwsEquinixAuthenticationException() {
            stubErrorInline(wireMock, "/internetAccess/v1/patchPanels",
                    401, "[{\"errorCode\":\"EQ-3000401\",\"errorMessage\":\"Authentication failed\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> internetAccess.patchPanels().list("WA1", "100013200", "cage-1", "cab-1"));
        }
    }
}
