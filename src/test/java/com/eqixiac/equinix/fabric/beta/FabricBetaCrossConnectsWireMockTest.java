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
import com.eqixiac.equinix.fabric.beta.model.CreateCrossConnect;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectActionRequest;
import com.eqixiac.equinix.fabric.beta.model.DiverseCrossConnectRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {@code fabric.beta().crossConnects()}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBetaCrossConnectsWireMockTest extends WireMockTestBase {

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

    /** {@code POST /fabric/v4/crossConnects} (createCrossConnect) (response example {@code StandardCrossConnectByPatchPanel}). */
    @Test
    void createCrossConnect() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/crossConnects"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/createCrossConnect.response.StandardCrossConnectByPatchPanel.json"))));
        String requestJson = loadFixture("/json/fabric/beta/crossconnects/createCrossConnect.request.StandardCrossConnect.json"); // request example StandardCrossConnect
        CreateCrossConnect request = BetaTestSupport.read(requestJson, CreateCrossConnect.class);
        Object result = beta.crossConnects().create(request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/crossconnects/createCrossConnect.response.StandardCrossConnectByPatchPanel.json"), result, "createCrossConnect");
    }

    /** {@code POST /fabric/v4/crossConnects/bulk} (createDiverseCrossConnect) (response example {@code DiverseCrossConnectPair}). */
    @Test
    void createDiverseCrossConnect() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/crossConnects/bulk"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/createDiverseCrossConnect.response.DiverseCrossConnectPair.json"))));
        String requestJson = loadFixture("/json/fabric/beta/crossconnects/createDiverseCrossConnect.request.DiverseCrossConnectPair.json"); // request example DiverseCrossConnectPair
        DiverseCrossConnectRequest request = BetaTestSupport.read(requestJson, DiverseCrossConnectRequest.class);
        Object result = beta.crossConnects().createDiverse(request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects/bulk"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/crossconnects/createDiverseCrossConnect.response.DiverseCrossConnectPair.json"), java.util.Map.of("data", result), "createDiverseCrossConnect");
    }

    /** {@code POST /fabric/v4/crossConnects/search} (searchCrossConnects) (response example {@code example}). */
    @Test
    void searchCrossConnects() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/crossConnects/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/searchCrossConnects.response.example.json"))));
        var page = beta.crossConnects().search(Filter.filter().and().equals("/name", "sdk-probe"), Sort.sort().desc("/changeLog/updatedDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/name\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/updatedDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/crossconnects/searchCrossConnects.response.example.json"), page.toList(), "searchCrossConnects");
    }

    /** {@code GET /fabric/v4/crossConnects/{crossConnectId}} (getCrossConnectByUuid) (response example {@code StandardCrossConnectByPatchPanel}). */
    @Test
    void getCrossConnectByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectByUuid.response.StandardCrossConnectByPatchPanel.json"))));
        Object result = beta.crossConnects().getByUuid("crossConnectId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectByUuid.response.StandardCrossConnectByPatchPanel.json"), result, "getCrossConnectByUuid");
    }

    /** {@code GET /fabric/v4/crossConnects/{crossConnectId}/schedules} (getCrossConnectSchedulesByUuid) (response example {@code SchedulesExample}). */
    @Test
    void getCrossConnectSchedulesByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/schedules"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectSchedulesByUuid.response.SchedulesExample.json"))));
        var items = beta.crossConnects().getSchedules("crossConnectId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/schedules"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectSchedulesByUuid.response.SchedulesExample.json"), items, "getCrossConnectSchedulesByUuid");
    }

    /** {@code GET /fabric/v4/crossConnects/{crossConnectId}/services} (getCrossConnectServicesByUuid) (response example {@code GetCrossConnectServices}). */
    @Test
    void getCrossConnectServicesByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/services"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectServicesByUuid.response.GetCrossConnectServices.json"))));
        var items = beta.crossConnects().getServices("crossConnectId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/services"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectServicesByUuid.response.GetCrossConnectServices.json"), items, "getCrossConnectServicesByUuid");
    }

    /** {@code GET /fabric/v4/crossConnects/{crossConnectId}/attachments} (getCrossConnectAttachmentsByUuid) (response example {@code GetCrossConnectAttachments}). */
    @Test
    void getCrossConnectAttachmentsByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/attachments"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectAttachmentsByUuid.response.GetCrossConnectAttachments.json"))));
        var items = beta.crossConnects().getAttachments("crossConnectId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/attachments"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectAttachmentsByUuid.response.GetCrossConnectAttachments.json"), items, "getCrossConnectAttachmentsByUuid");
    }

    /** {@code GET /fabric/v4/crossConnects/{crossConnectId}/notes} (getCrossConnectNotesByUuid) (response example {@code GetCrossConnectNotes}). */
    @Test
    void getCrossConnectNotesByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/notes"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectNotesByUuid.response.GetCrossConnectNotes.json"))));
        var page = beta.crossConnects().listNotes("crossConnectId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/notes"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withQueryParam("offset", equalTo("0")).withQueryParam("limit", matching("\\d+")));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectNotesByUuid.response.GetCrossConnectNotes.json"), page.toList(), "getCrossConnectNotesByUuid");
    }

    /** {@code POST /fabric/v4/crossConnects/{crossConnectId}/actions} (performCrossConnectAction) (response example {@code SendSignatureQuote}). */
    @Test
    void performCrossConnectAction() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/actions"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/performCrossConnectAction.response.SendSignatureQuote.json"))));
        String requestJson = loadFixture("/json/fabric/beta/crossconnects/performCrossConnectAction.request.SendSignatureQuote.json"); // request example SendSignatureQuote
        CrossConnectActionRequest request = BetaTestSupport.read(requestJson, CrossConnectActionRequest.class);
        Object result = beta.crossConnects().performAction("crossConnectId-0001", request);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/actions"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/crossconnects/performCrossConnectAction.response.SendSignatureQuote.json"), result, "performCrossConnectAction");
    }

    /** {@code GET /fabric/v4/crossConnects/{crossConnectId}/actions/{actionId}} (getCrossConnectActionByUuid) (response example {@code SendSignatureQuote}). */
    @Test
    void getCrossConnectActionByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/actions/actionId-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectActionByUuid.response.SendSignatureQuote.json"))));
        Object result = beta.crossConnects().getAction("crossConnectId-0001", "actionId-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/actions/actionId-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
        BetaTestSupport.assertCovers(loadFixture("/json/fabric/beta/crossconnects/getCrossConnectActionByUuid.response.SendSignatureQuote.json"), result, "getCrossConnectActionByUuid");
    }

    /** {@code POST /fabric/v4/crossConnects/{crossConnectId}/actions/search} (searchCrossConnectAction) (response example {@code CrossConnectActionSearchExample}). */
    @Test
    void searchCrossConnectAction() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/actions/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/searchCrossConnectAction.response.CrossConnectActionSearchExample.json"))));
        var page = beta.crossConnects().searchActions("crossConnectId-0001", Filter.filter().and().equals("/state", "sdk-probe"), Sort.sort().desc("/changeLog/createdDateTime"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/crossConnects/crossConnectId-0001/actions/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/state\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/changeLog/createdDateTime\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/crossconnects/searchCrossConnectAction.response.CrossConnectActionSearchExample.json"), page.toList(), "searchCrossConnectAction");
    }

    /** {@code POST /fabric/v4/crossConnectSchedules/search} (searchCrossConnectSchedules) (response example {@code SearchInstallSchedules}). */
    @Test
    void searchCrossConnectSchedules() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/crossConnectSchedules/search"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/beta/crossconnects/searchCrossConnectSchedules.response.SearchInstallSchedules.json"))));
        var page = beta.crossConnects().searchSchedules(Filter.filter().and().equals("/aSide/location/ibxCode", "sdk-probe"), Sort.sort().desc("/code"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/crossConnectSchedules/search"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", matching("application/json(;.*)?"))
                .withRequestBody(equalToJson("{\"filter\": {\"and\": [{\"property\": \"/aSide/location/ibxCode\", \"operator\": \"=\", \"values\": [\"sdk-probe\"]}]}, \"pagination\": {\"offset\": 0, \"limit\": 100}, \"sort\": [{\"property\": \"/code\", \"direction\": \"DESC\"}]}", false, false)));
        BetaTestSupport.assertPageCovers(loadFixture("/json/fabric/beta/crossconnects/searchCrossConnectSchedules.response.SearchInstallSchedules.json"), page.toList(), "searchCrossConnectSchedules");
    }
}
