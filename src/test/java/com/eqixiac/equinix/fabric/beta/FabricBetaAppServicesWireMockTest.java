/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.fabric.beta.client.FabricBeta;
import com.eqixiac.equinix.core.enums.SortOrder;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.fabric.beta.client.AttachmentQuery;
import com.eqixiac.equinix.fabric.beta.model.AppServicePostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {@code fabric.beta().appServices()}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBetaAppServicesWireMockTest extends WireMockTestBase {

    static Fabric fabric;
    static FabricBeta beta;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
        beta = fabric.beta();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void reset() {
        resetStubs();
    }

    /** {@code POST /fabric/v4/appServices} (createAppService) (response example {@code AppServiceResponse}). */
    @Test
    void createAppService() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appServices"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appservices/createAppService.response.AppServiceResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/appservices/createAppService.request.CreateAppService.json"); // request example CreateAppService
        AppServicePostRequest request = BetaTestSupport.read(requestJson, AppServicePostRequest.class);
        Object result = beta.appServices().create(request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appServices"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appservices/createAppService.response.AppServiceResponse.json"), result, "createAppService");
    }

    /** {@code GET /fabric/v4/appServices/{appServiceId}} (getAppServiceByUuid) (response example {@code AppServiceResponse}). */
    @Test
    void getAppServiceByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appservices/getAppServiceByUuid.response.AppServiceResponse.json"))));
        Object result = beta.appServices().getByUuid("appServiceId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appservices/getAppServiceByUuid.response.AppServiceResponse.json"), result, "getAppServiceByUuid");
    }

    /** {@code DELETE /fabric/v4/appServices/{appServiceId}} (deleteAppServiceByUuid) (response example {@code AppServiceResponse}). */
    @Test
    void deleteAppServiceByUuid() throws Exception {
        wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appservices/deleteAppServiceByUuid.response.AppServiceResponse.json"))));
        Object result = beta.appServices().delete("appServiceId-0001");
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appservices/deleteAppServiceByUuid.response.AppServiceResponse.json"), result, "deleteAppServiceByUuid");
    }

    /** {@code PATCH /fabric/v4/appServices/{appServiceId}} (updateAppServiceByUuid) (response example {@code AppServiceResponse}). */
    @Test
    void updateAppServiceByUuid() throws Exception {
        wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appservices/updateAppServiceByUuid.response.AppServiceResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/appservices/updateAppServiceByUuid.request.UpdateName.json"); // request example UpdateName
        List<PatchOperation> ops = BetaTestSupport.patchOps(requestJson);
        Object result = beta.appServices().update("appServiceId-0001", ops);
        wireMock.verify(1, patchRequestedFor(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json-patch\\+json(;.*)?"))
                .withRequestBody(equalToJson(requestJson, false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appservices/updateAppServiceByUuid.response.AppServiceResponse.json"), result, "updateAppServiceByUuid");
    }

    /** {@code POST /fabric/v4/appServices/search} (searchAppServices) (response example {@code Example}). */
    @Test
    void searchAppServices() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appServices/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appservices/searchAppServices.response.Example.json"))));
        var page = beta.appServices().search(Filter.filter().and().equals("/name", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appServices/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/name\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/appservices/searchAppServices.response.Example.json"), page.toList(), "searchAppServices");
    }

    /** {@code GET /fabric/v4/appServices/{appServiceId}/appLinks} (getAttachedAppLinksByAppServiceId) (response example {@code Example}). */
    @Test
    void getAttachedAppLinksByAppServiceId() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001/appLinks"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appservices/getAttachedAppLinksByAppServiceId.response.Example.json"))));
        var page = beta.appServices().listAttachedAppLinks("appServiceId-0001", new AttachmentQuery().states("S1", "S2").order(SortOrder.ASC).style(AttachmentQuery.Style.FULL));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001/appLinks"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withQueryParam("offset", equalTo("0")).withQueryParam("limit", matching("\\d+"))
                .withQueryParam("state", havingExactly("S1", "S2")).withQueryParam("order", equalTo("ASC"))
                .withQueryParam("style", equalTo("FULL")));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/appservices/getAttachedAppLinksByAppServiceId.response.Example.json"), page.toList(), "getAttachedAppLinksByAppServiceId");
    }

    /** {@code GET /fabric/v4/appServices/{appServiceId}/appSubscriptions} (getAttachedAppSubscriptionsByAppServiceId) (response example {@code Example}). */
    @Test
    void getAttachedAppSubscriptionsByAppServiceId() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001/appSubscriptions"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appservices/getAttachedAppSubscriptionsByAppServiceId.response.Example.json"))));
        var page = beta.appServices().listAttachedAppSubscriptions("appServiceId-0001", new AttachmentQuery().states("S1", "S2").order(SortOrder.ASC).style(AttachmentQuery.Style.FULL));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001/appSubscriptions"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withQueryParam("offset", equalTo("0")).withQueryParam("limit", matching("\\d+"))
                .withQueryParam("state", havingExactly("S1", "S2")).withQueryParam("order", equalTo("ASC"))
                .withQueryParam("style", equalTo("FULL")));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/appservices/getAttachedAppSubscriptionsByAppServiceId.response.Example.json"), page.toList(), "getAttachedAppSubscriptionsByAppServiceId");
    }

    /** {@code POST /fabric/v4/appServices/{appServiceId}/appSubscriptions/search} (searchAttachedAppSubscriptionsByAppServiceId) (response example {@code Example}). */
    @Test
    void searchAttachedAppSubscriptionsByAppServiceId() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001/appSubscriptions/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appservices/searchAttachedAppSubscriptionsByAppServiceId.response.Example.json"))));
        var page = beta.appServices().searchAttachedAppSubscriptions("appServiceId-0001", Filter.filter().and().equals("/state", "sdk-probe"), Sort.sort().desc("/state"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appServices/appServiceId-0001/appSubscriptions/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/state\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/state\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/appservices/searchAttachedAppSubscriptionsByAppServiceId.response.Example.json"), page.toList(), "searchAttachedAppSubscriptionsByAppServiceId");
    }
}
