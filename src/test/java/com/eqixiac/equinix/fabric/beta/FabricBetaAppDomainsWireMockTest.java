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
import com.eqixiac.equinix.fabric.beta.model.AppDomainPostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {@code fabric.beta().appDomains()}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBetaAppDomainsWireMockTest extends WireMockTestBase {

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

    /** {@code POST /fabric/v4/appDomains} (createAppDomain) (response example {@code AppDomainResponse}). */
    @Test
    void createAppDomain() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appDomains"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appdomains/createAppDomain.response.AppDomainResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/appdomains/createAppDomain.request.CreateAppDomain.json"); // request example CreateAppDomain
        AppDomainPostRequest request = BetaTestSupport.read(requestJson, AppDomainPostRequest.class);
        Object result = beta.appDomains().create(request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appDomains"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appdomains/createAppDomain.response.AppDomainResponse.json"), result, "createAppDomain");
    }

    /** {@code GET /fabric/v4/appDomains/{appDomainId}} (getAppDomainByUuid) (response example {@code AppDomainResponse}). */
    @Test
    void getAppDomainByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appDomains/appDomainId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appdomains/getAppDomainByUuid.response.AppDomainResponse.json"))));
        Object result = beta.appDomains().getByUuid("appDomainId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appDomains/appDomainId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appdomains/getAppDomainByUuid.response.AppDomainResponse.json"), result, "getAppDomainByUuid");
    }

    /** {@code DELETE /fabric/v4/appDomains/{appDomainId}} (deleteAppDomainByUuid) (response example {@code AppDomainResponse}). */
    @Test
    void deleteAppDomainByUuid() throws Exception {
        wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/appDomains/appDomainId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appdomains/deleteAppDomainByUuid.response.AppDomainResponse.json"))));
        Object result = beta.appDomains().delete("appDomainId-0001");
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/fabric/v4/appDomains/appDomainId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appdomains/deleteAppDomainByUuid.response.AppDomainResponse.json"), result, "deleteAppDomainByUuid");
    }

    /** {@code PATCH /fabric/v4/appDomains/{appDomainId}} (updateAppDomainByUuid) (response example {@code AppDomainResponse}). */
    @Test
    void updateAppDomainByUuid() throws Exception {
        wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/appDomains/appDomainId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appdomains/updateAppDomainByUuid.response.AppDomainResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/appdomains/updateAppDomainByUuid.request.UpdateDescription.json"); // request example UpdateDescription
        List<PatchOperation> ops = BetaTestSupport.patchOps(requestJson);
        Object result = beta.appDomains().update("appDomainId-0001", ops);
        wireMock.verify(1, patchRequestedFor(urlPathEqualTo("/fabric/v4/appDomains/appDomainId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json-patch\\+json(;.*)?"))
                .withRequestBody(equalToJson(requestJson, false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appdomains/updateAppDomainByUuid.response.AppDomainResponse.json"), result, "updateAppDomainByUuid");
    }

    /** {@code POST /fabric/v4/appDomains/search} (searchAppDomains) (response example {@code Example}). */
    @Test
    void searchAppDomains() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appDomains/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appdomains/searchAppDomains.response.Example.json"))));
        var page = beta.appDomains().search(Filter.filter().and().equals("/name", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appDomains/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/name\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/appdomains/searchAppDomains.response.Example.json"), page.toList(), "searchAppDomains");
    }

    /** {@code GET /fabric/v4/appDomains/{appDomainId}/appLinks} (getAttachedAppLinksByAppDomainId) (response example {@code Example}). */
    @Test
    void getAttachedAppLinksByAppDomainId() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appDomains/appDomainId-0001/appLinks"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appdomains/getAttachedAppLinksByAppDomainId.response.Example.json"))));
        var page = beta.appDomains().listAttachedAppLinks("appDomainId-0001", new AttachmentQuery().states("S1", "S2").order(SortOrder.ASC).style(AttachmentQuery.Style.FULL));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appDomains/appDomainId-0001/appLinks"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withQueryParam("offset", equalTo("0")).withQueryParam("limit", matching("\\d+"))
                .withQueryParam("state", havingExactly("S1", "S2")).withQueryParam("order", equalTo("ASC"))
                .withQueryParam("style", equalTo("FULL")));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/appdomains/getAttachedAppLinksByAppDomainId.response.Example.json"), page.toList(), "getAttachedAppLinksByAppDomainId");
    }
}
