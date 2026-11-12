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
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {@code fabric.beta().appProcessors()}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBetaAppProcessorsWireMockTest extends WireMockTestBase {

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

    /** {@code GET /fabric/v4/appProcessors/{appProcessorId}} (getAppProcessorByUuid) (response example {@code AppProcessorResponse}). */
    @Test
    void getAppProcessorByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/appProcessors/appProcessorId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appprocessors/getAppProcessorByUuid.response.AppProcessorResponse.json"))));
        Object result = beta.appProcessors().getByUuid("appProcessorId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/appProcessors/appProcessorId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/appprocessors/getAppProcessorByUuid.response.AppProcessorResponse.json"), result, "getAppProcessorByUuid");
    }

    /** {@code POST /fabric/v4/appProcessors/search} (searchAppProcessors) (response example {@code Example}). */
    @Test
    void searchAppProcessors() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/appProcessors/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/appprocessors/searchAppProcessors.response.Example.json"))));
        var page = beta.appProcessors().search(Filter.filter().and().equals("/name", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/appProcessors/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/name\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/appprocessors/searchAppProcessors.response.Example.json"), page.toList(), "searchAppProcessors");
    }
}
