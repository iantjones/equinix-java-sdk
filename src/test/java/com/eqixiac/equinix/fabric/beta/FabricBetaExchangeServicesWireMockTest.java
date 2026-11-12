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
import com.eqixiac.equinix.fabric.beta.model.ExchangeServiceActionRequest;
import com.eqixiac.equinix.fabric.beta.model.ExchangeServiceCreateRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {@code fabric.beta().exchangeServices()}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBetaExchangeServicesWireMockTest extends WireMockTestBase {

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

    /** {@code POST /fabric/v4/exchangeServices} (createExchangeService) (response example {@code CreateExchangeServiceResponse}). */
    @Test
    void createExchangeService() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/exchangeServices"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/exchangeservices/createExchangeService.response.CreateExchangeServiceResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/exchangeservices/createExchangeService.request.ExchangeServiceCreateRequest.json"); // request example ExchangeServiceCreateRequest
        ExchangeServiceCreateRequest request = BetaTestSupport.read(requestJson, ExchangeServiceCreateRequest.class);
        Object result = beta.exchangeServices().create(request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/exchangeServices"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/exchangeservices/createExchangeService.response.CreateExchangeServiceResponse.json"), result, "createExchangeService");
    }

    /** {@code GET /fabric/v4/exchangeServices/{exchangeServiceId}} (getExchangeServiceById) (response example {@code GetExchangeServiceResponse}). */
    @Test
    void getExchangeServiceById() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/exchangeServices/exchangeServiceId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/exchangeservices/getExchangeServiceById.response.GetExchangeServiceResponse.json"))));
        Object result = beta.exchangeServices().getByUuid("exchangeServiceId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/exchangeServices/exchangeServiceId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/exchangeservices/getExchangeServiceById.response.GetExchangeServiceResponse.json"), result, "getExchangeServiceById");
    }

    /** {@code PATCH /fabric/v4/exchangeServices/{exchangeServiceId}} (updateExchangeServiceById) (response example {@code PatchExchangeServiceResponse}). */
    @Test
    void updateExchangeServiceById() throws Exception {
        wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/exchangeServices/exchangeServiceId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/exchangeservices/updateExchangeServiceById.response.PatchExchangeServiceResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/exchangeservices/updateExchangeServiceById.request.ExchangeServicePatchMacAddressRequest.json"); // request example ExchangeServicePatchMacAddressRequest
        List<PatchOperation> ops = BetaTestSupport.patchOps(requestJson);
        Object result = beta.exchangeServices().update("exchangeServiceId-0001", ops);
        wireMock.verify(1, patchRequestedFor(urlPathEqualTo("/fabric/v4/exchangeServices/exchangeServiceId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json-patch\\+json(;.*)?"))
                .withRequestBody(equalToJson(requestJson, false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/exchangeservices/updateExchangeServiceById.response.PatchExchangeServiceResponse.json"), result, "updateExchangeServiceById");
    }

    /** {@code POST /fabric/v4/exchangeServices/search} (searchExchangeService) (response example {@code example}). */
    @Test
    void searchExchangeService() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/exchangeServices/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/exchangeservices/searchExchangeService.response.example.json"))));
        var page = beta.exchangeServices().search(Filter.filter().and().equals("/name", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/exchangeServices/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/name\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/exchangeservices/searchExchangeService.response.example.json"), page.toList(), "searchExchangeService");
    }

    /** {@code POST /fabric/v4/exchangeServices/{exchangeServiceId}/actions} (createExchangeServiceAction) (response example {@code example}). */
    @Test
    void createExchangeServiceAction() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/exchangeServices/exchangeServiceId-0001/actions"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/exchangeservices/createExchangeServiceAction.response.example.json"))));
        String requestJson = loadFixture("/json/fabric/beta/exchangeservices/createExchangeServiceAction.request.ActivateExchangeServiceRequest.json"); // request example ActivateExchangeServiceRequest
        ExchangeServiceActionRequest request = BetaTestSupport.read(requestJson, ExchangeServiceActionRequest.class);
        Object result = beta.exchangeServices().performAction("exchangeServiceId-0001", request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/exchangeServices/exchangeServiceId-0001/actions"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/exchangeservices/createExchangeServiceAction.response.example.json"), result, "createExchangeServiceAction");
    }
}
