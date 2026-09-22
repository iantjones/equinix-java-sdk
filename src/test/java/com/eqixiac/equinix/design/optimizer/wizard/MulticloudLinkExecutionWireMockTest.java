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

package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentOutcome;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.ExecutionInputs;
import com.eqixiac.equinix.design.optimizer.wizard.model.ProvisioningError;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.eqixiac.equinix.core.ResponseStubs.stubSingleton;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code DeploymentPlan.execute()} never sends a request for a native multicloud link. The plans
 * are produced by the real engine against a WireMock-backed {@link Fabric}; the request journal
 * of a {@code COMPARE} run is compared, request by request, with that of an {@code EQUINIX_ONLY}
 * run of the same fixture.
 *
 * <p>Fixtures: the Fabric responses are the recorded fixtures {@code DeploymentPlanExecutionWireMockTest}
 * uses. The recorded service profile lists AWS seller region {@code us-east-1} at DC; the Google
 * Cloud profile stub is that same recording with the DC seller-region key replaced by
 * {@code us-east4}, because the suite holds no recorded Google Cloud profile.</p>
 */
@DisplayName("execute(): native multicloud links are never provisioned")
class MulticloudLinkExecutionWireMockTest extends WireMockTestBase {

    private static final String CONNECTION_UUID = "3a58dd05-f46d-4b1d-a154-2e85c396ea85";

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
        stubFabric();
    }

    @Test
    @DisplayName("COMPARE: execute() sends exactly the requests an EQUINIX_ONLY run sends, and records one informational entry")
    void compareSendsNoExtraRequest() {
        DeploymentPlan equinixOnly = plan(CloudToCloudStrategy.EQUINIX_ONLY);
        wireMock.resetRequests();
        DeploymentOutcome baseline = equinixOnly.execute(inputs());
        List<String> baselineJournal = journal();

        DeploymentPlan compare = plan(CloudToCloudStrategy.COMPARE);
        assertEquals(1, compare.getMulticloudLinks().size());
        wireMock.resetRequests();
        DeploymentOutcome outcome = compare.execute(inputs());
        List<String> journal = journal();

        assertEquals(baselineJournal, journal, "zero extra requests for the native link");
        assertTrue(journal.stream().allMatch(line -> line.contains(" /fabric/v4/")), () -> String.valueOf(journal));
        assertTrue(journal.stream().noneMatch(line -> line.toLowerCase(Locale.ROOT).contains("multicloud")
                || line.toLowerCase(Locale.ROOT).contains("interconnect")), () -> String.valueOf(journal));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/routers")));
        wireMock.verify(2, postRequestedFor(urlPathEqualTo("/fabric/v4/connections"))
                .withQueryParam("dryRun", equalTo("true")));
        wireMock.verify(2, postRequestedFor(urlPathEqualTo("/fabric/v4/connections"))
                .withQueryParam("dryRun", absent()));
        wireMock.verify(4, postRequestedFor(
                urlPathEqualTo("/fabric/v4/connections/" + CONNECTION_UUID + "/routingProtocols")));

        assertTrue(outcome.isFullySuccessful(), () -> String.valueOf(outcome.getErrors()));
        assertTrue(outcome.getErrors().isEmpty());
        assertEquals(baseline.getResources().size(), outcome.getResources().size());
        assertEquals(7, outcome.getResources().size());
        assertTrue(outcome.getResources().stream().noneMatch(r -> r.getResourceType().contains("Multicloud")));

        assertNull(baseline.getInformational(), "a plan without links produces the outcome it did before");
        assertEquals(1, outcome.getInformational().size());
        ProvisioningError info = outcome.getInformational().get(0);
        assertEquals("MulticloudInterconnect", info.getResourceType());
        assertEquals("aws-gcp-DC", info.getResourceName());
        assertFalse(info.isRecoverable());
        assertTrue(info.getReason().startsWith("created outside Fabric: follow the create-then-accept recipe"),
                info.getReason());
        assertTrue(info.getReason().contains("sent no request for it"), info.getReason());
        assertTrue(info.getReason().contains("The plan does not depend on this link."), info.getReason());

        assertTrue(outcome.toSummary().startsWith("Deployment SUCCEEDED: 7/7 resources provisioned"), outcome.toSummary());
        assertTrue(outcome.toSummary().endsWith("1 native multicloud link(s) not provisioned: created outside Fabric "
                + "by the customer."), outcome.toSummary());
        assertTrue(outcome.toMarkdown().contains("## Not provisioned by this SDK"));
        assertFalse(outcome.toMarkdown().contains("## Errors"));
    }

    @Test
    @DisplayName("NATIVE_WHEN_AVAILABLE: execute() creates the Cloud Router only, posts no connection, and says the plan depends on the link")
    void replacementPostsNoConnection() {
        DeploymentPlan plan = plan(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);
        assertTrue(plan.getProviderConnections().isEmpty());
        wireMock.resetRequests();

        DeploymentOutcome outcome = plan.execute();

        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/routers")));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")));
        wireMock.verify(0, postRequestedFor(urlPathMatching("/fabric/v4/connections/[^/]+/routingProtocols")));
        assertTrue(journal().stream().allMatch(line -> line.contains(" /fabric/v4/routers")), () -> String.valueOf(journal()));

        assertTrue(outcome.isFullySuccessful(), () -> String.valueOf(outcome.getErrors()));
        assertEquals(1, outcome.getResources().size());
        assertEquals(1, outcome.getInformational().size());
        assertTrue(outcome.getInformational().get(0).getReason()
                .contains("The plan omits [FCR-DC-to-aws, FCR-DC-to-gcp] and depends on this link."));
    }

    @Test
    @DisplayName("plan(): COMPARE issues the same plan-time requests as EQUINIX_ONLY; pricing and the catalog use no network")
    void planningSendsNoExtraRequest() {
        wireMock.resetRequests();
        plan(CloudToCloudStrategy.EQUINIX_ONLY);
        List<String> baselineJournal = journal();

        wireMock.resetRequests();
        plan(CloudToCloudStrategy.COMPARE);

        assertEquals(baselineJournal, journal());
    }

    @Test
    @DisplayName("a fail-fast outcome (missing authorization key) still carries the informational entry and posts nothing")
    void failFastOutcomeCarriesInformationalEntry() {
        DeploymentPlan plan = plan(CloudToCloudStrategy.COMPARE);
        wireMock.resetRequests();

        DeploymentOutcome outcome = plan.execute(ExecutionInputs.none());

        assertFalse(outcome.isFullySuccessful());
        assertEquals(2, outcome.getErrors().size(), "one missing key per provider connection; the link is not an error");
        assertEquals(1, outcome.getInformational().size());
        assertTrue(wireMock.getAllServeEvents().isEmpty());
    }

    // ── helpers ──

    /** Method and URL of every request WireMock served, oldest first. */
    private static List<String> journal() {
        List<String> lines = new ArrayList<>();
        for (ServeEvent event : wireMock.getAllServeEvents()) {
            lines.add(event.getRequest().getMethod() + " " + event.getRequest().getUrl());
        }
        Collections.reverse(lines);
        return lines;
    }

    private static ExecutionInputs inputs() {
        return ExecutionInputs.builder()
                .authenticationKey("FCR-DC-to-aws", "123456789012")
                .vlanTag("FCR-DC-to-aws", 1001)
                .authenticationKey("FCR-DC-to-gcp", "7e51371e-72a3-40b5-b844-2e3efefaee59/us-east4/2")
                .vlanTag("FCR-DC-to-gcp", 1002)
                .build();
    }

    private static DeploymentPlan plan(CloudToCloudStrategy strategy) {
        return fabric.deploymentWizard(MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .cloudToCloudStrategy(strategy)
                .plan();
    }

    private static void stubFabric() {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers"))
                .willReturn(created("/json/fabric/cloud_router_response.json")));
        stubSingleton(wireMock, "/fabric/v4/routers/[^/]+", "/json/fabric/cloud_router_response.json");

        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections"))
                .willReturn(created("/json/fabric/connection_provisioned_response.json")));
        stubSingleton(wireMock, "/fabric/v4/connections/[^/]+", "/json/fabric/connection_provisioned_response.json");

        wireMock.stubFor(post(urlPathMatching("/fabric/v4/connections/[^/]+/routingProtocols"))
                .willReturn(created("/json/fabric/routing_protocol_response.json")));

        String recordedProfile = loadFixture("/json/fabric/service_profile_response.json");
        wireMock.stubFor(get(urlPathMatching("/fabric/v4/serviceProfiles/.*")).willReturn(okJson(recordedProfile)));
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/serviceProfiles/sp-gcp"))
                .willReturn(okJson(recordedProfile.replace("\"us-east-1\":", "\"us-east4\":"))));
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder created(String fixture) {
        return aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(loadFixture(fixture));
    }
}
