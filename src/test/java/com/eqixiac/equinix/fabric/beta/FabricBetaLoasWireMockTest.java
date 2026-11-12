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
import com.eqixiac.equinix.fabric.beta.model.CreateLoa;
import com.eqixiac.equinix.fabric.beta.model.CreateLoaNote;
import com.eqixiac.equinix.fabric.beta.model.LoaActionRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {@code fabric.beta().loas()}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBetaLoasWireMockTest extends WireMockTestBase {

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

    /** {@code POST /fabric/v4/loas} (createLoa) (response example {@code ByRequestor}). */
    @Test
    void createLoa() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/loas"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/loas/createLoa.response.ByRequestor.json"))));
        String requestJson = loadFixture("/json/fabric/beta/loas/createLoa.request.IssueLoa-OnCageWithCageId.json"); // request example IssueLoa-OnCageWithCageId
        CreateLoa request = BetaTestSupport.read(requestJson, CreateLoa.class);
        Object result = beta.loas().create(request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/loas"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/loas/createLoa.response.ByRequestor.json"), result, "createLoa");
    }

    /** {@code GET /fabric/v4/loas/{loaId}} (getLoaByUuid) (response example {@code LoaExample}). */
    @Test
    void getLoaByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/loas/loaId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/loas/getLoaByUuid.response.LoaExample.json"))));
        Object result = beta.loas().getByUuid("loaId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/loas/loaId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/loas/getLoaByUuid.response.LoaExample.json"), result, "getLoaByUuid");
    }

    /** {@code PATCH /fabric/v4/loas/{loaId}} (updateLoaByUuid) (response example {@code PatchLoaResponse}). */
    @Test
    void updateLoaByUuid() throws Exception {
        wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/loas/loaId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/loas/updateLoaByUuid.response.PatchLoaResponse.json"))));
        String requestJson = loadFixture("/json/fabric/beta/loas/updateLoaByUuid.request.LoaPatchRequest.json"); // request example LoaPatchRequest
        List<PatchOperation> ops = BetaTestSupport.patchOps(requestJson);
        Object result = beta.loas().update("loaId-0001", ops);
        wireMock.verify(1, patchRequestedFor(urlPathEqualTo("/fabric/v4/loas/loaId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json-patch\\+json(;.*)?"))
                .withRequestBody(equalToJson(requestJson, false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/loas/updateLoaByUuid.response.PatchLoaResponse.json"), result, "updateLoaByUuid");
    }

    /** {@code POST /fabric/v4/loas/{loaId}/actions} (performLoaAction) (response example {@code AuthorizeLoa}). */
    @Test
    void performLoaAction() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/loas/loaId-0001/actions"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/loas/performLoaAction.response.AuthorizeLoa.json"))));
        String requestJson = loadFixture("/json/fabric/beta/loas/performLoaAction.request.AuthorizeLoa.json"); // request example AuthorizeLoa
        LoaActionRequest request = BetaTestSupport.read(requestJson, LoaActionRequest.class);
        Object result = beta.loas().performAction("loaId-0001", request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/loas/loaId-0001/actions"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/loas/performLoaAction.response.AuthorizeLoa.json"), result, "performLoaAction");
    }

    /** {@code GET /fabric/v4/loas/{loaId}/actions/{actionId}} (getLoaActionsByUuid) (response example {@code AuthorizeLoaActionExample}). */
    @Test
    void getLoaActionsByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/loas/loaId-0001/actions/actionId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/loas/getLoaActionsByUuid.response.AuthorizeLoaActionExample.json"))));
        Object result = beta.loas().getAction("loaId-0001", "actionId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/loas/loaId-0001/actions/actionId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/loas/getLoaActionsByUuid.response.AuthorizeLoaActionExample.json"), result, "getLoaActionsByUuid");
    }

    /** {@code POST /fabric/v4/loas/search} (searchLoa) (response example {@code example}). */
    @Test
    void searchLoa() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/loas/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/loas/searchLoa.response.example.json"))));
        var page = beta.loas().search(Filter.filter().and().equals("/name", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/loas/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/name\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/loas/searchLoa.response.example.json"), page.toList(), "searchLoa");
    }

    /** {@code POST /fabric/v4/loas/{loaId}/actions/search} (searchLoaAction) (response example {@code LoaActionSearchExample}). */
    @Test
    void searchLoaAction() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/loas/loaId-0001/actions/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/loas/searchLoaAction.response.LoaActionSearchExample.json"))));
        var page = beta.loas().searchActions("loaId-0001", Filter.filter().and().equals("/state", "sdk-probe"), Sort.sort().desc("/changeLog/createdDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/loas/loaId-0001/actions/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/state\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/createdDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/loas/searchLoaAction.response.LoaActionSearchExample.json"), page.toList(), "searchLoaAction");
    }

    /** {@code GET /fabric/v4/loas/{loaId}/consumers} (getLoaConsumersByLoaId) (response example {@code LoaConsumersExample}). */
    @Test
    void getLoaConsumersByLoaId() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/loas/loaId-0001/consumers"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/loas/getLoaConsumersByLoaId.response.LoaConsumersExample.json"))));
        var items = beta.loas().getConsumers("loaId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/loas/loaId-0001/consumers"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/loas/getLoaConsumersByLoaId.response.LoaConsumersExample.json"), items, "getLoaConsumersByLoaId");
    }

    /** {@code GET /fabric/v4/loas/{loaId}/notes} (getLoaNotesByUuid) (response example {@code GetLoaNotesExample}). */
    @Test
    void getLoaNotesByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/loas/loaId-0001/notes"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/loas/getLoaNotesByUuid.response.GetLoaNotesExample.json"))));
        var items = beta.loas().getNotes("loaId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/loas/loaId-0001/notes"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/loas/getLoaNotesByUuid.response.GetLoaNotesExample.json"), items, "getLoaNotesByUuid");
    }

    /** {@code POST /fabric/v4/loas/{loaId}/notes} (createLoaNoteByLoaId) (response example {@code CreateLoaNoteExample}). */
    @Test
    void createLoaNoteByLoaId() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/loas/loaId-0001/notes"))
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/loas/createLoaNoteByLoaId.response.CreateLoaNoteExample.json"))));
        String requestJson = loadFixture("/json/fabric/beta/loas/createLoaNoteByLoaId.request.LoaNoteRequest.json"); // request example LoaNoteRequest
        CreateLoaNote request = BetaTestSupport.read(requestJson, CreateLoaNote.class);
        Object result = beta.loas().createNote("loaId-0001", request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/loas/loaId-0001/notes"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/loas/createLoaNoteByLoaId.response.CreateLoaNoteExample.json"), result, "createLoaNoteByLoaId");
    }
}
