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
import com.eqixiac.equinix.fabric.beta.model.AppLinkAttachProcessorRequest;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAttachServiceRequest;
import com.eqixiac.equinix.fabric.beta.model.AppLinkPostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {@code fabric.beta().appLinks()}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBetaAppLinksWireMockTest extends WireMockTestBase {

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

    /** {@code POST /fabric/v4/appLinks} (createAppLink) (response example {@code AppLinkResponse}). */
    @Test
    void createAppLink() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appLinks"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/createAppLink.response.AppLinkResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/applinks/createAppLink.request.CreateAppLink.json"); // request example CreateAppLink
        AppLinkPostRequest request = BetaTestSupport.read(requestJson, AppLinkPostRequest.class);
        Object result = beta.appLinks().create(request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appLinks"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/createAppLink.response.AppLinkResponse.json"), result, "createAppLink");
    }

    /** {@code GET /fabric/v4/appLinks/{appLinkId}} (getAppLinkByUuid) (response example {@code AppLinkResponse}). */
    @Test
    void getAppLinkByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/getAppLinkByUuid.response.AppLinkResponse.json"))));
        Object result = beta.appLinks().getByUuid("appLinkId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/getAppLinkByUuid.response.AppLinkResponse.json"), result, "getAppLinkByUuid");
    }

    /** {@code DELETE /fabric/v4/appLinks/{appLinkId}} (deleteAppLinkByUuid) (response example {@code AppLinkResponse}). */
    @Test
    void deleteAppLinkByUuid() throws Exception {
        wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/deleteAppLinkByUuid.response.AppLinkResponse.json"))));
        Object result = beta.appLinks().delete("appLinkId-0001");
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/deleteAppLinkByUuid.response.AppLinkResponse.json"), result, "deleteAppLinkByUuid");
    }

    /** {@code PATCH /fabric/v4/appLinks/{appLinkId}} (updateAppLinkByUuid) (response example {@code AppLinkResponse}). */
    @Test
    void updateAppLinkByUuid() throws Exception {
        wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/updateAppLinkByUuid.response.AppLinkResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/applinks/updateAppLinkByUuid.request.UpdateName.json"); // request example UpdateName
        List<PatchOperation> ops = BetaTestSupport.patchOps(requestJson);
        Object result = beta.appLinks().update("appLinkId-0001", ops);
        wireMock.verify(1, patchRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json-patch\\+json(;.*)?"))
                .withRequestBody(equalToJson(requestJson, false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/updateAppLinkByUuid.response.AppLinkResponse.json"), result, "updateAppLinkByUuid");
    }

    /** {@code POST /fabric/v4/appLinks/search} (searchAppLinks) (response example {@code Example}). */
    @Test
    void searchAppLinks() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appLinks/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/searchAppLinks.response.Example.json"))));
        var page = beta.appLinks().search(Filter.filter().and().equals("/name", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/name\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/applinks/searchAppLinks.response.Example.json"), page.toList(), "searchAppLinks");
    }

    /** {@code PUT /fabric/v4/appLinks/{appLinkId}/appDomains/{appDomainId}} (attachAppDomainToAppLink) (response example {@code AppLinkAttachDomainResponse}). */
    @Test
    void attachAppDomainToAppLink() throws Exception {
        wireMock.stubFor(put(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appDomains/appDomainId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/attachAppDomainToAppLink.response.AppLinkAttachDomainResponse.json"))));
        Object result = beta.appLinks().attachAppDomain("appLinkId-0001", "appDomainId-0001");
        wireMock.verify(1, putRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appDomains/appDomainId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/attachAppDomainToAppLink.response.AppLinkAttachDomainResponse.json"), result, "attachAppDomainToAppLink");
    }

    /** {@code DELETE /fabric/v4/appLinks/{appLinkId}/appDomains/{appDomainId}} (detachAppDomainFromAppLink) (response example {@code AppLinkDetachDomainResponse}). */
    @Test
    void detachAppDomainFromAppLink() throws Exception {
        wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appDomains/appDomainId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/detachAppDomainFromAppLink.response.AppLinkDetachDomainResponse.json"))));
        Object result = beta.appLinks().detachAppDomain("appLinkId-0001", "appDomainId-0001");
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appDomains/appDomainId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/detachAppDomainFromAppLink.response.AppLinkDetachDomainResponse.json"), result, "detachAppDomainFromAppLink");
    }

    /** {@code GET /fabric/v4/appLinks/{appLinkId}/appDomains/{appDomainId}} (getAttachedAppDomainByUuid) (response example {@code AppLinkDomainResponse}). */
    @Test
    void getAttachedAppDomainByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appDomains/appDomainId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/getAttachedAppDomainByUuid.response.AppLinkDomainResponse.json"))));
        Object result = beta.appLinks().getAttachedAppDomain("appLinkId-0001", "appDomainId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appDomains/appDomainId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/getAttachedAppDomainByUuid.response.AppLinkDomainResponse.json"), result, "getAttachedAppDomainByUuid");
    }

    /** {@code POST /fabric/v4/appLinks/{appLinkId}/appDomains/search} (searchAttachedAppDomains) (response example {@code Example}). */
    @Test
    void searchAttachedAppDomains() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appDomains/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/searchAttachedAppDomains.response.Example.json"))));
        var page = beta.appLinks().searchAttachedAppDomains("appLinkId-0001", Filter.filter().and().equals("/attachmentStatus", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appDomains/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/attachmentStatus\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/applinks/searchAttachedAppDomains.response.Example.json"), page.toList(), "searchAttachedAppDomains");
    }

    /** {@code GET /fabric/v4/appLinks/{appLinkId}/appDomains} (getAttachedAppDomainsByAppLinkId) (response example {@code Example}). */
    @Test
    void getAttachedAppDomainsByAppLinkId() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appDomains"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/getAttachedAppDomainsByAppLinkId.response.Example.json"))));
        var page = beta.appLinks().listAttachedAppDomains("appLinkId-0001", new AttachmentQuery().states("S1", "S2").order(SortOrder.ASC).style(AttachmentQuery.Style.FULL));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appDomains"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withQueryParam("offset", equalTo("0")).withQueryParam("limit", matching("\\d+"))
                .withQueryParam("attachmentStatus", havingExactly("S1", "S2")).withQueryParam("order", equalTo("ASC"))
                .withQueryParam("style", equalTo("FULL")));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/applinks/getAttachedAppDomainsByAppLinkId.response.Example.json"), page.toList(), "getAttachedAppDomainsByAppLinkId");
    }

    /** {@code PUT /fabric/v4/appLinks/{appLinkId}/appServices/{appServiceId}} (attachAppServiceToAppLink) (response example {@code AppLinkResponse}). */
    @Test
    void attachAppServiceToAppLink() throws Exception {
        wireMock.stubFor(put(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices/appServiceId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/attachAppServiceToAppLink.response.AppLinkResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/applinks/attachAppServiceToAppLink.request.AttachAppLinkToService.json"); // request example AttachAppLinkToService
        AppLinkAttachServiceRequest request = BetaTestSupport.read(requestJson, AppLinkAttachServiceRequest.class);
        Object result = beta.appLinks().attachAppService("appLinkId-0001", "appServiceId-0001", request);
        wireMock.verify(1, putRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices/appServiceId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/attachAppServiceToAppLink.response.AppLinkResponse.json"), result, "attachAppServiceToAppLink");
    }

    /** {@code DELETE /fabric/v4/appLinks/{appLinkId}/appServices/{appServiceId}} (detachAppServiceFromAppLink) (response example {@code AppLinkResponse}). */
    @Test
    void detachAppServiceFromAppLink() throws Exception {
        wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices/appServiceId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/detachAppServiceFromAppLink.response.AppLinkResponse.json"))));
        Object result = beta.appLinks().detachAppService("appLinkId-0001", "appServiceId-0001");
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices/appServiceId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/detachAppServiceFromAppLink.response.AppLinkResponse.json"), result, "detachAppServiceFromAppLink");
    }

    /** {@code PATCH /fabric/v4/appLinks/{appLinkId}/appServices/{appServiceId}} (updateAppServiceAttachmentToAppLink) (response example {@code AppLinkResponse}). */
    @Test
    void updateAppServiceAttachmentToAppLink() throws Exception {
        wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices/appServiceId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/updateAppServiceAttachmentToAppLink.response.AppLinkResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/applinks/updateAppServiceAttachmentToAppLink.request.UpdateDestinationIp.json"); // request example UpdateDestinationIp
        List<PatchOperation> ops = BetaTestSupport.patchOps(requestJson);
        Object result = beta.appLinks().updateAppServiceAttachment("appLinkId-0001", "appServiceId-0001", ops);
        wireMock.verify(1, patchRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices/appServiceId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json-patch\\+json(;.*)?"))
                .withRequestBody(equalToJson(requestJson, false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/updateAppServiceAttachmentToAppLink.response.AppLinkResponse.json"), result, "updateAppServiceAttachmentToAppLink");
    }

    /** {@code GET /fabric/v4/appLinks/{appLinkId}/appServices/{appServiceId}} (getAttachedAppServiceByUuid) (response example {@code AppLinkResponse}). */
    @Test
    void getAttachedAppServiceByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices/appServiceId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/getAttachedAppServiceByUuid.response.AppLinkResponse.json"))));
        Object result = beta.appLinks().getAttachedAppService("appLinkId-0001", "appServiceId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices/appServiceId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/getAttachedAppServiceByUuid.response.AppLinkResponse.json"), result, "getAttachedAppServiceByUuid");
    }

    /** {@code POST /fabric/v4/appLinks/{appLinkId}/appServices/search} (searchAttachedAppServices) (response example {@code Example}). */
    @Test
    void searchAttachedAppServices() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/searchAttachedAppServices.response.Example.json"))));
        var page = beta.appLinks().searchAttachedAppServices("appLinkId-0001", Filter.filter().and().equals("/attachmentStatus", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/attachmentStatus\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/applinks/searchAttachedAppServices.response.Example.json"), page.toList(), "searchAttachedAppServices");
    }

    /** {@code GET /fabric/v4/appLinks/{appLinkId}/appServices} (getAttachedAppServicesByAppLinkId) (response example {@code Example}). */
    @Test
    void getAttachedAppServicesByAppLinkId() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/getAttachedAppServicesByAppLinkId.response.Example.json"))));
        var page = beta.appLinks().listAttachedAppServices("appLinkId-0001", new AttachmentQuery().states("S1", "S2").order(SortOrder.ASC).style(AttachmentQuery.Style.FULL));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appServices"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withQueryParam("offset", equalTo("0")).withQueryParam("limit", matching("\\d+"))
                .withQueryParam("attachmentStatus", havingExactly("S1", "S2")).withQueryParam("order", equalTo("ASC"))
                .withQueryParam("style", equalTo("FULL")));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/applinks/getAttachedAppServicesByAppLinkId.response.Example.json"), page.toList(), "getAttachedAppServicesByAppLinkId");
    }

    /** {@code PUT /fabric/v4/appLinks/{appLinkId}/appProcessors/{appProcessorId}} (attachAppProcessorToAppLink) (response example {@code AppLinkAttachProcessorResponse}). */
    @Test
    void attachAppProcessorToAppLink() throws Exception {
        wireMock.stubFor(put(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appProcessors/appProcessorId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/attachAppProcessorToAppLink.response.AppLinkAttachProcessorResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/applinks/attachAppProcessorToAppLink.request.AttachAppProcessorToAppLink.json"); // request example AttachAppProcessorToAppLink
        AppLinkAttachProcessorRequest request = BetaTestSupport.read(requestJson, AppLinkAttachProcessorRequest.class);
        Object result = beta.appLinks().attachAppProcessor("appLinkId-0001", "appProcessorId-0001", request);
        wireMock.verify(1, putRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appProcessors/appProcessorId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/attachAppProcessorToAppLink.response.AppLinkAttachProcessorResponse.json"), result, "attachAppProcessorToAppLink");
    }

    /** {@code DELETE /fabric/v4/appLinks/{appLinkId}/appProcessors/{appProcessorId}} (detachAppProcessorFromAppLink) (response example {@code AppLinkDetachProcessorResponse}). */
    @Test
    void detachAppProcessorFromAppLink() throws Exception {
        wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appProcessors/appProcessorId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/detachAppProcessorFromAppLink.response.AppLinkDetachProcessorResponse.json"))));
        Object result = beta.appLinks().detachAppProcessor("appLinkId-0001", "appProcessorId-0001");
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appProcessors/appProcessorId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/detachAppProcessorFromAppLink.response.AppLinkDetachProcessorResponse.json"), result, "detachAppProcessorFromAppLink");
    }

    /** {@code GET /fabric/v4/appLinks/{appLinkId}/appProcessors/{appProcessorId}} (getAttachedAppProcessor) (response example {@code AppLinkAttachProcessorGetResponse}). */
    @Test
    void getAttachedAppProcessor() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appProcessors/appProcessorId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/getAttachedAppProcessor.response.AppLinkAttachProcessorGetResponse.json"))));
        Object result = beta.appLinks().getAttachedAppProcessor("appLinkId-0001", "appProcessorId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appProcessors/appProcessorId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/applinks/getAttachedAppProcessor.response.AppLinkAttachProcessorGetResponse.json"), result, "getAttachedAppProcessor");
    }

    /** {@code POST /fabric/v4/appLinks/{appLinkId}/appProcessors/search} (searchAttachedAppProcessors) (response example {@code Example}). */
    @Test
    void searchAttachedAppProcessors() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appProcessors/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/searchAttachedAppProcessors.response.Example.json"))));
        var page = beta.appLinks().searchAttachedAppProcessors("appLinkId-0001", Filter.filter().and().equals("/attachmentStatus", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appProcessors/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/attachmentStatus\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/applinks/searchAttachedAppProcessors.response.Example.json"), page.toList(), "searchAttachedAppProcessors");
    }

    /** {@code GET /fabric/v4/appLinks/{appLinkId}/appProcessors} (getAttachedAppProcessorsByAppLinkId) (response example {@code Example}). */
    @Test
    void getAttachedAppProcessorsByAppLinkId() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appProcessors"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/applinks/getAttachedAppProcessorsByAppLinkId.response.Example.json"))));
        var page = beta.appLinks().listAttachedAppProcessors("appLinkId-0001", new AttachmentQuery().states("S1", "S2").order(SortOrder.ASC).style(AttachmentQuery.Style.FULL));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appLinks/appLinkId-0001/appProcessors"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withQueryParam("offset", equalTo("0")).withQueryParam("limit", matching("\\d+"))
                .withQueryParam("attachmentStatus", havingExactly("S1", "S2")).withQueryParam("order", equalTo("ASC"))
                .withQueryParam("style", equalTo("FULL")));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/applinks/getAttachedAppProcessorsByAppLinkId.response.Example.json"), page.toList(), "getAttachedAppProcessorsByAppLinkId");
    }
}
