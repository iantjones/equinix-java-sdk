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

package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.EquinixServerException;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.enums.AlertStatus;
import api.equinix.javasdk.ibxsmartview.model.SystemAlert;
import api.equinix.javasdk.ibxsmartview.model.json.creators.SearchCondition;
import api.equinix.javasdk.ibxsmartview.model.json.creators.SearchFilter;
import api.equinix.javasdk.ibxsmartview.model.json.creators.SearchPagination;
import api.equinix.javasdk.ibxsmartview.model.json.creators.SearchRequest;
import api.equinix.javasdk.ibxsmartview.model.json.creators.SearchSort;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
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

            PaginatedList<SystemAlert> alerts =
                    smartView.systemAlerts().search("ACTIVE", "Mechanical", "2026-06-30", 0, 100);

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
                    .withQueryParam("offset", equalTo("0"))
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

            SearchRequest request = new SearchRequest(
                    new SearchFilter(
                            List.of(new SearchCondition("status", "EQUALS", List.of("ACTIVE"))),
                            null),
                    new SearchPagination(0L, 100),
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
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
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
}
