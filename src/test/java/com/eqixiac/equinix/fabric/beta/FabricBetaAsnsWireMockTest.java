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
import com.eqixiac.equinix.fabric.beta.model.CreateAsnRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {@code fabric.beta().asns()}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBetaAsnsWireMockTest extends WireMockTestBase {

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

    /** {@code POST /fabric/v4/asns} (createAsn) (response example {@code PublicAsnCreated}). */
    @Test
    void createAsn() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/asns"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/asns/createAsn.response.PublicAsnCreated.json"))));
        String requestJson = loadFixture("/json/fabric/beta/asns/createAsn.request.CreatePublicAsn.json"); // request example CreatePublicAsn
        CreateAsnRequest request = BetaTestSupport.read(requestJson, CreateAsnRequest.class);
        Object result = beta.asns().create(request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/asns"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/asns/createAsn.response.PublicAsnCreated.json"), result, "createAsn");
    }

    /** {@code POST /fabric/v4/asns/search} (searchAsns) (response example {@code SearchAsnsByProject}). */
    @Test
    void searchAsns() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/asns/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/asns/searchAsns.response.SearchAsnsByProject.json"))));
        var page = beta.asns().search(Filter.filter().and().equals("/name", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/asns/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/name\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/asns/searchAsns.response.SearchAsnsByProject.json"), page.toList(), "searchAsns");
    }

    /** {@code GET /fabric/v4/asns/{asnId}} (getAsnById) (response example {@code PublicAsnRetrieved}). */
    @Test
    void getAsnById() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/asns/asnId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/asns/getAsnById.response.PublicAsnRetrieved.json"))));
        Object result = beta.asns().getByUuid("asnId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/asns/asnId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/asns/getAsnById.response.PublicAsnRetrieved.json"), result, "getAsnById");
    }

    /** {@code PATCH /fabric/v4/asns/{asnId}} (patchAsnById) (response example {@code PublicAsnCreated}). */
    @Test
    void patchAsnById() throws Exception {
        wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/asns/asnId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/asns/patchAsnById.response.PublicAsnCreated.json"))));
        String requestJson = loadFixture("/json/fabric/beta/asns/patchAsnById.request.AddOwnershipSignature.json"); // request example AddOwnershipSignature
        List<PatchOperation> ops = BetaTestSupport.patchOps(requestJson);
        Object result = beta.asns().update("asnId-0001", ops);
        wireMock.verify(1, patchRequestedFor(urlPathEqualTo("/fabric/v4/asns/asnId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(requestJson, false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/asns/patchAsnById.response.PublicAsnCreated.json"), result, "patchAsnById");
    }

    /** {@code DELETE /fabric/v4/asns/{asnId}} (deleteAsnById) (response example {@code PublicAsnCreated}). */
    @Test
    void deleteAsnById() throws Exception {
        wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/asns/asnId-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/asns/deleteAsnById.response.PublicAsnCreated.json"))));
        Object result = beta.asns().delete("asnId-0001");
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/fabric/v4/asns/asnId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/asns/deleteAsnById.response.PublicAsnCreated.json"), result, "deleteAsnById");
    }
}
