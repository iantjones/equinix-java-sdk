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
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.fabric.beta.model.AppSubscriptionPostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {@code fabric.beta().appSubscriptions()}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBetaAppSubscriptionsWireMockTest extends WireMockTestBase {

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

    /** {@code POST /fabric/v4/appSubscriptions} (createAppSubscription) (response example {@code AppSubscriptionResponse}). */
    @Test
    void createAppSubscription() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appSubscriptions"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appsubscriptions/createAppSubscription.response.AppSubscriptionResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/appsubscriptions/createAppSubscription.request.CreateAppSubscription.json"); // request example CreateAppSubscription
        AppSubscriptionPostRequest request = BetaTestSupport.read(requestJson, AppSubscriptionPostRequest.class);
        Object result = beta.appSubscriptions().create(request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appSubscriptions"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appsubscriptions/createAppSubscription.response.AppSubscriptionResponse.json"), result, "createAppSubscription");
    }

    /** {@code GET /fabric/v4/appSubscriptions/{appSubscriptionId}} (getAppSubscriptionByUuid) (response example {@code AppSubscriptionResponse}). */
    @Test
    void getAppSubscriptionByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appSubscriptions/appSubscriptionId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appsubscriptions/getAppSubscriptionByUuid.response.AppSubscriptionResponse.json"))));
        Object result = beta.appSubscriptions().getByUuid("appSubscriptionId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appSubscriptions/appSubscriptionId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appsubscriptions/getAppSubscriptionByUuid.response.AppSubscriptionResponse.json"), result, "getAppSubscriptionByUuid");
    }

    /** {@code DELETE /fabric/v4/appSubscriptions/{appSubscriptionId}} (deleteAppSubscriptionByUuid) (response example {@code AppSubscriptionResponse}). */
    @Test
    void deleteAppSubscriptionByUuid() throws Exception {
        wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/appSubscriptions/appSubscriptionId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appsubscriptions/deleteAppSubscriptionByUuid.response.AppSubscriptionResponse.json"))));
        Object result = beta.appSubscriptions().delete("appSubscriptionId-0001");
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/fabric/v4/appSubscriptions/appSubscriptionId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appsubscriptions/deleteAppSubscriptionByUuid.response.AppSubscriptionResponse.json"), result, "deleteAppSubscriptionByUuid");
    }

    /** {@code PATCH /fabric/v4/appSubscriptions/{appSubscriptionId}} (updateAppSubscriptionByUuid) (response example {@code AppSubscriptionResponse}). */
    @Test
    void updateAppSubscriptionByUuid() throws Exception {
        wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/appSubscriptions/appSubscriptionId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appsubscriptions/updateAppSubscriptionByUuid.response.AppSubscriptionResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/appsubscriptions/updateAppSubscriptionByUuid.request.UpdateSourceIpSubnets.json"); // request example UpdateSourceIpSubnets
        List<PatchOperation> ops = BetaTestSupport.patchOps(requestJson);
        Object result = beta.appSubscriptions().update("appSubscriptionId-0001", ops);
        wireMock.verify(1, patchRequestedFor(urlPathEqualTo("/fabric/v4/appSubscriptions/appSubscriptionId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json-patch\\+json(;.*)?"))
                .withRequestBody(equalToJson(requestJson, false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appsubscriptions/updateAppSubscriptionByUuid.response.AppSubscriptionResponse.json"), result, "updateAppSubscriptionByUuid");
    }

    /** {@code POST /fabric/v4/appSubscriptions/search} (searchAppSubscriptions) (response example {@code Example}). */
    @Test
    void searchAppSubscriptions() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appSubscriptions/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appsubscriptions/searchAppSubscriptions.response.Example.json"))));
        var page = beta.appSubscriptions().search(Filter.filter().and().equals("/changeLog/createdDateTime", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appSubscriptions/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/changeLog/createdDateTime\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/appsubscriptions/searchAppSubscriptions.response.Example.json"), page.toList(), "searchAppSubscriptions");
    }
}
