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
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.ibxsmartview.model.AssetDetail;
import com.eqixiac.equinix.ibxsmartview.model.AssetDetailsResponse;
import com.eqixiac.equinix.ibxsmartview.model.Assets;
import com.eqixiac.equinix.ibxsmartview.model.AssetsList;
import com.eqixiac.equinix.ibxsmartview.model.HierarchyNodeForAssetAPI;
import com.eqixiac.equinix.ibxsmartview.model.TagPointData;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.AssetDetailsRequest;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.CurrentTagPointRequest;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the IBX SmartView Assets action endpoints
 * (POST asset/details and POST asset/tagpoint/current). These are the mutation-style
 * "get multiple by typed request body" operations that lacked coverage.
 */
class IBXSmartViewAssetsWireMockTest extends WireMockTestBase {

    static IBXSmartView smartView;

    static final String ACCOUNT_NO = "123456";
    static final String IBX = "SV5";
    static final String CLASSIFICATION = "Electrical";

    static final String ASSET_DETAILS_PATH = "/smartview/v1/asset/details";
    static final String CURRENT_TAGPOINTS_PATH = "/smartview/v1/asset/tagpoint/current";
    static final String LIST_PATH = "/smartview/v1/asset/list";
    static final String SEARCH_PATH = "/smartview/v1/asset/search";
    static final String AFFECTED_ASSETS_PATH = "/smartview/v1/asset/tagpoint/affected-assets";

    static final String ASSET_ID = "ASSET-SV5-001";
    static final String TAG_ID = "TAG-SV5-001";

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
    @DisplayName("getMultipleAssetDetails()")
    class GetMultipleAssetDetails {

        @Test
        @DisplayName("POSTs the typed request body and deserializes the payLoad/status envelope")
        void postsRequestBodyAndReadsResponse() {
            wireMock.stubFor(post(urlPathEqualTo(ASSET_DETAILS_PATH))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/asset_details_response.json"))));

            AssetDetailsRequest request = new AssetDetailsRequest(
                    ACCOUNT_NO, IBX, CLASSIFICATION,
                    List.of("ASSET-SV5-001", "ASSET-SV5-002"));

            AssetDetailsResponse response = smartView.smartViewAssets().getMultipleAssetDetails(request);

            assertNotNull(response);
            assertNotNull(response.getPayLoad());
            assertEquals(2, response.getPayLoad().getTotalCount());
            assertEquals(2, response.getPayLoad().getAssetDetails().size());
            assertEquals("ASSET-SV5-001", response.getPayLoad().getAssetDetails().get(0).getAssetId());
            assertEquals("PDU", response.getPayLoad().getAssetDetails().get(0).getAssetType());
            assertEquals("TAG-SV5-001",
                    response.getPayLoad().getAssetDetails().get(0).getTags().get(0).getTagId());
            assertNotNull(response.getStatus());
            assertEquals(200, response.getStatus().getStatuscode());

            // Assert the request path, verb, and the exact serialized request body.
            wireMock.verify(postRequestedFor(urlPathEqualTo(ASSET_DETAILS_PATH))
                    .withRequestBody(matchingJsonPath("$.accountNo", equalTo(ACCOUNT_NO)))
                    .withRequestBody(matchingJsonPath("$.ibx", equalTo(IBX)))
                    .withRequestBody(matchingJsonPath("$.classification", equalTo(CLASSIFICATION)))
                    .withRequestBody(matchingJsonPath("$.assetIds[0]", equalTo("ASSET-SV5-001")))
                    .withRequestBody(matchingJsonPath("$.assetIds[1]", equalTo("ASSET-SV5-002"))));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            wireMock.stubFor(post(urlPathEqualTo(ASSET_DETAILS_PATH))
                    .willReturn(aResponse().withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]")));

            AssetDetailsRequest request = new AssetDetailsRequest(
                    ACCOUNT_NO, IBX, CLASSIFICATION, List.of("ASSET-SV5-001"));

            assertThrows(EquinixServerException.class,
                    () -> smartView.smartViewAssets().getMultipleAssetDetails(request));
        }
    }

    @Nested
    @DisplayName("getMultipleCurrentTagPoints()")
    class GetMultipleCurrentTagPoints {

        @Test
        @DisplayName("POSTs the typed request body and deserializes the current tag-point list")
        void postsRequestBodyAndReadsResponse() {
            wireMock.stubFor(post(urlPathEqualTo(CURRENT_TAGPOINTS_PATH))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/current_tag_points_response.json"))));

            CurrentTagPointRequest request = new CurrentTagPointRequest(
                    ACCOUNT_NO, List.of("TAG-SV5-001", "TAG-SV5-002"), IBX);

            TagPointData response = smartView.smartViewAssets().getMultipleCurrentTagPoints(request);

            assertNotNull(response);
            assertNotNull(response.getPayLoad());
            assertEquals(2, response.getPayLoad().size());
            assertEquals("TAG-SV5-001", response.getPayLoad().get(0).getTagId());
            assertEquals("12.5", response.getPayLoad().get(0).getValue());
            assertEquals("kW", response.getPayLoad().get(0).getUom());
            assertNotNull(response.getStatus());
            assertEquals("SUCCESS", response.getStatus().getType());

            wireMock.verify(postRequestedFor(urlPathEqualTo(CURRENT_TAGPOINTS_PATH))
                    .withRequestBody(matchingJsonPath("$.accountNo", equalTo(ACCOUNT_NO)))
                    .withRequestBody(matchingJsonPath("$.ibx", equalTo(IBX)))
                    .withRequestBody(matchingJsonPath("$.tagIds[0]", equalTo("TAG-SV5-001")))
                    .withRequestBody(matchingJsonPath("$.tagIds[1]", equalTo("TAG-SV5-002"))));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            wireMock.stubFor(post(urlPathEqualTo(CURRENT_TAGPOINTS_PATH))
                    .willReturn(aResponse().withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Tag not found\"}]")));

            CurrentTagPointRequest request = new CurrentTagPointRequest(
                    ACCOUNT_NO, List.of("TAG-UNKNOWN"), IBX);

            assertThrows(EquinixNotFoundException.class,
                    () -> smartView.smartViewAssets().getMultipleCurrentTagPoints(request));
        }
    }

    @Nested
    @DisplayName("list()")
    class ListAssets {

        @Test
        @DisplayName("GETs asset/list with account/ibx/classification and the multi-valued cages query params")
        void getsListWithQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo(LIST_PATH))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/assets_list_response.json"))));

            AssetsList response = smartView.smartViewAssets().list(
                    ACCOUNT_NO, IBX, CLASSIFICATION, List.of("SV5:01:0001", "SV5:01:0002"));

            assertNotNull(response);
            assertNotNull(response.getPayLoad());
            assertEquals("Electrical", response.getPayLoad().getClassification());
            assertEquals(1, response.getPayLoad().getCategories().size());
            assertEquals("Power Distribution",
                    response.getPayLoad().getCategories().get(0).getCategoryName());
            assertEquals("TMPL-PDU",
                    response.getPayLoad().getCategories().get(0).getTemplates().get(0).getTemplateId());
            assertEquals(200, response.getStatus().getStatuscode());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("accountNo", equalTo(ACCOUNT_NO))
                    .withQueryParam("ibx", equalTo(IBX))
                    .withQueryParam("classification", equalTo(CLASSIFICATION))
                    .withQueryParam("cages", equalTo("SV5:01:0001"))
                    .withQueryParam("cages", equalTo("SV5:01:0002")));
        }

        @Test
        @DisplayName("omits the cages query param when the cages list is null")
        void omitsCagesWhenNull() {
            wireMock.stubFor(get(urlPathEqualTo(LIST_PATH))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/assets_list_response.json"))));

            smartView.smartViewAssets().list(ACCOUNT_NO, IBX, CLASSIFICATION, null);

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("accountNo", equalTo(ACCOUNT_NO))
                    .withQueryParam("ibx", equalTo(IBX))
                    .withQueryParam("classification", equalTo(CLASSIFICATION))
                    .withoutQueryParam("cages"));
        }
    }

    @Nested
    @DisplayName("getAssetDetails()")
    class GetAssetDetails {

        @Test
        @DisplayName("GETs asset/details with accountNo/ibx/classification/assetId query params")
        void getsSingleAssetDetails() {
            wireMock.stubFor(get(urlPathEqualTo(ASSET_DETAILS_PATH))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/asset_details_get_response.json"))));

            AssetDetail response = smartView.smartViewAssets().getAssetDetails(
                    ACCOUNT_NO, IBX, CLASSIFICATION, ASSET_ID);

            assertNotNull(response);
            assertNotNull(response.getPayLoad());
            assertEquals(ASSET_ID, response.getPayLoad().getAssetId());
            assertEquals("PDU", response.getPayLoad().getAssetType());
            assertEquals("Schneider Electric", response.getPayLoad().getManufacturerName());
            assertEquals("TAG-SV5-001", response.getPayLoad().getTags().get(0).getTagId());
            assertEquals(200, response.getStatus().getStatuscode());

            wireMock.verify(getRequestedFor(urlPathEqualTo(ASSET_DETAILS_PATH))
                    .withQueryParam("accountNo", equalTo(ACCOUNT_NO))
                    .withQueryParam("ibx", equalTo(IBX))
                    .withQueryParam("classification", equalTo(CLASSIFICATION))
                    .withQueryParam("assetId", equalTo(ASSET_ID)));
        }
    }

    @Nested
    @DisplayName("search()")
    class SearchAssets {

        @Test
        @DisplayName("GETs asset/search with accountNo/ibx/searchString query params")
        void getsSearchResults() {
            wireMock.stubFor(get(urlPathEqualTo(SEARCH_PATH))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/assets_search_response.json"))));

            Assets response = smartView.smartViewAssets().search(ACCOUNT_NO, IBX, "PDU");

            assertNotNull(response);
            assertNotNull(response.getPayLoad());
            assertEquals(2, response.getPayLoad().getTotalCount());
            assertEquals(2, response.getPayLoad().getAssetsList().size());
            assertEquals(ASSET_ID, response.getPayLoad().getAssetsList().get(0).getAssetId());
            assertEquals("PDU", response.getPayLoad().getAssetsList().get(0).getType());
            assertEquals("Electrical",
                    response.getPayLoad().getAssetsList().get(0).getAssetClassification());
            assertEquals(200, response.getStatus().getStatuscode());

            wireMock.verify(getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("accountNo", equalTo(ACCOUNT_NO))
                    .withQueryParam("ibx", equalTo(IBX))
                    .withQueryParam("searchString", equalTo("PDU")));
        }
    }

    @Nested
    @DisplayName("getAffectedAssets()")
    class GetAffectedAssets {

        @Test
        @DisplayName("GETs asset/tagpoint/affected-assets with accountNo/ibx/assetId/classification query params")
        void getsAffectedAssets() {
            wireMock.stubFor(get(urlPathEqualTo(AFFECTED_ASSETS_PATH))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/affected_assets_response.json"))));

            HierarchyNodeForAssetAPI response = smartView.smartViewAssets().getAffectedAssets(
                    ACCOUNT_NO, IBX, ASSET_ID, CLASSIFICATION);

            assertNotNull(response);
            assertNotNull(response.getPayLoad());
            assertEquals(1, response.getPayLoad().getCages().size());
            assertEquals("SV5:01:0001", response.getPayLoad().getCages().get(0).getName());
            assertEquals("CAGE", response.getPayLoad().getCages().get(0).getType());
            assertEquals(200, response.getStatus().getStatuscode());

            wireMock.verify(getRequestedFor(urlPathEqualTo(AFFECTED_ASSETS_PATH))
                    .withQueryParam("accountNo", equalTo(ACCOUNT_NO))
                    .withQueryParam("ibx", equalTo(IBX))
                    .withQueryParam("assetId", equalTo(ASSET_ID))
                    .withQueryParam("classification", equalTo(CLASSIFICATION)));
        }
    }

    @Nested
    @DisplayName("getCurrentTagPoint()")
    class GetCurrentTagPoint {

        @Test
        @DisplayName("GETs asset/tagpoint/current with accountNo/ibx and the lowercase tagid query param")
        void getsCurrentTagPoint() {
            wireMock.stubFor(get(urlPathEqualTo(CURRENT_TAGPOINTS_PATH))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/current_tag_point_response.json"))));

            TagPointData response = smartView.smartViewAssets().getCurrentTagPoint(ACCOUNT_NO, IBX, TAG_ID);

            assertNotNull(response);
            assertNotNull(response.getPayLoad());
            assertEquals(1, response.getPayLoad().size());
            assertEquals(TAG_ID, response.getPayLoad().get(0).getTagId());
            assertEquals("12.5", response.getPayLoad().get(0).getValue());
            assertEquals("kW", response.getPayLoad().get(0).getUom());
            assertEquals("SUCCESS", response.getStatus().getType());

            // getAssetDetails uses "assetId" but this op sends the tag id under the lowercase "tagid" key.
            wireMock.verify(getRequestedFor(urlPathEqualTo(CURRENT_TAGPOINTS_PATH))
                    .withQueryParam("accountNo", equalTo(ACCOUNT_NO))
                    .withQueryParam("ibx", equalTo(IBX))
                    .withQueryParam("tagid", equalTo(TAG_ID)));
        }
    }
}
