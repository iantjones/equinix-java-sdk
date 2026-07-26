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

package com.eqixiac.equinix.ibxsmartview.wiremock;

import com.eqixiac.equinix.IBXSmartView;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.ibxsmartview.enums.AlertStatus;
import com.eqixiac.equinix.ibxsmartview.model.SystemAlert;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.SearchCondition;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.SearchFilter;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.SearchPagination;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.SearchRequest;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.SearchSort;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based read/search coverage for the IBX SmartView SystemAlerts client.
 *
 * <p>Both operations hit {@code /smartview/v2/systemAlerts/search} (defaultVersion 2, rootUri
 * {@code systemAlerts}, requestUri {@code search}); the GET variant carries the filter as query
 * params, the POST variant carries a typed {@link SearchRequest} body. Both page off the flat
 * envelope (top-level items/limit/offset/totalCount) and return a
 * {@link PaginatedList}{@code <SystemAlert>}.</p>
 */
class IBXSmartViewSystemAlertsWireMockTest extends WireMockTestBase {

    static IBXSmartView smartView;

    static final String SEARCH_PATH = "/smartview/v2/systemAlerts/search";
    static final String FIXTURE = "/json/ibxsmartview/system_alerts_search_response.json";

    @BeforeAll
    static void setUp() {
        smartView = new IBXSmartView(testCredentials());
        redirectToWireMock(smartView);
        smartView.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (smartView != null) smartView.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("search(status, assetClassification, edgeCollectedOn, offset, limit)")
    class SearchGet {

        @Test
        @DisplayName("GETs systemAlerts/search with all filter + pagination query params")
        void getsWithAllQueryParams() {
            stubPaginatedGet(wireMock, SEARCH_PATH, FIXTURE);

            // offset=5 with limit=100 is deliberately NOT page-aligned: regression guard for the
            // core paging seed quantizing caller offsets to page boundaries (5/100 -> page 0 -> offset=0).
            PaginatedList<SystemAlert> alerts =
                    smartView.systemAlerts().search("ACTIVE", "Mechanical", "2026-06-30", 5, 100);

            assertNotNull(alerts);
            assertEquals(2, alerts.size());
            SystemAlert first = alerts.get(0);
            assertEquals(5001L, first.getId());
            assertEquals("SV5.TEMP#EXCEEDS:27", first.getAlertUid());
            assertEquals(AlertStatus.ACTIVE, first.getStatus());
            assertEquals("SV5:01:A1234", first.getAsset().getAssetUid());
            assertEquals("28.4", first.getValue().getAlertTagValue());
            assertEquals("EXCEEDS", first.getConfiguration().getThresholdType());
            assertEquals(AlertStatus.INACTIVE, alerts.get(1).getStatus());

            wireMock.verify(getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("status", equalTo("ACTIVE"))
                    .withQueryParam("assetClassification", equalTo("Mechanical"))
                    .withQueryParam("edgeCollectedOn", equalTo("2026-06-30"))
                    .withQueryParam("offset", equalTo("5"))
                    .withQueryParam("limit", equalTo("100")));
        }

        @Test
        @DisplayName("omits the optional filter params when null but always sends offset/limit")
        void omitsNullFilterParams() {
            stubPaginatedGet(wireMock, SEARCH_PATH, FIXTURE);

            smartView.systemAlerts().search(null, null, null, 0, 25);

            wireMock.verify(getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withoutQueryParam("status")
                    .withoutQueryParam("assetClassification")
                    .withoutQueryParam("edgeCollectedOn")
                    .withQueryParam("offset", equalTo("0"))
                    .withQueryParam("limit", equalTo("25")));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubError(wireMock, SEARCH_PATH, 500, "/json/core/error_500_response.json");

            assertThrows(EquinixServerException.class,
                    () -> smartView.systemAlerts().search("ACTIVE", null, null, 0, 100));
        }
    }

    @Nested
    @DisplayName("searchPost(SearchRequest)")
    class SearchPost {

        @Test
        @DisplayName("POSTs systemAlerts/search with the typed filter/pagination/sort body")
        void postsTypedBody() {
            stubPaginatedPost(wireMock, SEARCH_PATH, FIXTURE);

            // Non-page-aligned offset (5 with limit 100) mirrors the GET-variant regression guard:
            // the caller's pagination body must reach the wire verbatim, not quantized.
            SearchRequest request = new SearchRequest(
                    new SearchFilter(
                            List.of(new SearchCondition("status", "EQUALS", List.of("ACTIVE"))),
                            null),
                    new SearchPagination(5L, 100),
                    List.of(new SearchSort("DESC", "id")));

            PaginatedList<SystemAlert> alerts = smartView.systemAlerts().searchPost(request);

            assertNotNull(alerts);
            assertEquals(2, alerts.size());
            assertEquals(5001L, alerts.get(0).getId());
            assertEquals(5002L, alerts.get(1).getId());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("status")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("EQUALS")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("ACTIVE")))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("5")))
                    .withRequestBody(matchingJsonPath("$.pagination.limit", equalTo("100")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("id"))));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubError(wireMock, SEARCH_PATH, 500, "/json/core/error_500_response.json");

            SearchRequest request = new SearchRequest(
                    new SearchFilter(List.of(new SearchCondition("status", "EQUALS", List.of("ACTIVE"))), null),
                    new SearchPagination(0L, 100),
                    null);

            assertThrows(EquinixServerException.class,
                    () -> smartView.systemAlerts().searchPost(request));
        }
    }

    @Nested
    @DisplayName("Multi-page paging")
    class Paging {

        // Spec-conformant PaginatedResponseAlertReadModel envelope: {data, pagination{offset,limit,total}}.
        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "id": 1, "alertUid": "PAGE1_ALERT" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "id": 2, "alertUid": "PAGE2_ALERT" } ]
                }
                """;

        @Test
        @DisplayName("searchPost: loadAll() fetches page 2 by advancing the body's pagination member (regression: ClassCastException on page 2)")
        void searchPostLoadAllFetchesSecondPage() {
            wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .willReturn(okJson(PAGE_2)));

            SearchRequest request = new SearchRequest(
                    new SearchFilter(List.of(new SearchCondition("status", "EQUALS", List.of("ACTIVE"))), null),
                    new SearchPagination(0L, 100),
                    null);

            PaginatedList<SystemAlert> alerts = smartView.systemAlerts().searchPost(request);
            assertEquals(1, alerts.size());
            assertTrue(alerts.hasNextPage());

            alerts.loadAll();

            assertEquals(2, alerts.size());
            assertEquals("PAGE1_ALERT", alerts.get(0).getAlertUid());
            assertEquals("PAGE2_ALERT", alerts.get(1).getAlertUid());
            assertFalse(alerts.hasNextPage());

            // Page 2 body: pagination advanced from the server-reported page, same filter re-sent.
            wireMock.verify(1, postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .withRequestBody(matchingJsonPath("$.pagination.limit", equalTo("100")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("ACTIVE"))));
        }

        @Test
        @DisplayName("search (GET): a server-clamped limit advances page 2 from the SERVER page, not the requested one (regression: skipped records)")
        void getSearchAdvancesFromServerClampedPagination() {
            // Caller requests limit=500; the server clamps to 100 and says so in the response
            // pagination. Page 2 must be requested at offset=100 (server offset + server limit),
            // NOT offset=500 — the old caller-side advance skipped records 100..499.
            wireMock.stubFor(get(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<SystemAlert> alerts = smartView.systemAlerts().search("ACTIVE", null, null, 0, 500);
            assertEquals(1, alerts.size());
            assertTrue(alerts.hasNextPage());

            alerts.loadAll();

            assertEquals(2, alerts.size());
            assertFalse(alerts.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100"))
                    .withQueryParam("status", equalTo("ACTIVE")));
            // And it never asked for the caller-side offset=500 page.
            wireMock.verify(0, getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("offset", equalTo("500")));
        }
    }
}
