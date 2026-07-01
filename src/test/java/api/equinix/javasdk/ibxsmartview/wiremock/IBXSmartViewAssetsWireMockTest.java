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
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.ibxsmartview.model.AssetDetailsResponse;
import api.equinix.javasdk.ibxsmartview.model.TagPointData;
import api.equinix.javasdk.ibxsmartview.model.json.creators.AssetDetailsRequest;
import api.equinix.javasdk.ibxsmartview.model.json.creators.CurrentTagPointRequest;
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
}
