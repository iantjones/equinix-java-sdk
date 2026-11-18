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
package com.eqixiac.equinix.design.plan;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.design.access.AccessKind;
import com.eqixiac.equinix.design.access.AccessMode;
import com.eqixiac.equinix.design.access.AccessPlan;
import com.eqixiac.equinix.design.access.AccessSource;
import com.eqixiac.equinix.design.access.MetroAccess;
import com.eqixiac.equinix.design.access.PlannedAccessItem;
import com.eqixiac.equinix.design.access.PlannedAccessLink;
import com.eqixiac.equinix.design.optimizer.wizard.enums.ConnectionPurpose;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentOutcome;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.ExecutionInputs;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.RedundancyPriority;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.eqixiac.equinix.design.plan.PlanFixtures.DC;
import static com.eqixiac.equinix.design.plan.PlanFixtures.PROJECT;
import static com.eqixiac.equinix.design.plan.PlanFixtures.ROUTER_KEY;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Execution of a plan with metro access against a WireMock Fabric: the connection from the Cloud Router
 * to an existing port is created with the port and its VLAN tag on the Z-side, and nothing is sent for
 * a port that is still an order or for the access items themselves. Bodies follow the Fabric v4
 * {@code Connection} shape (Z-side access point of type {@code COLO}).
 */
class AccessConnectionExecutionWireMockTest extends WireMockTestBase {

    private static final String R1 = "11111111-aaaa-4bbb-8ccc-000000000001";
    private static final String A1 = "55555555-aaaa-4bbb-8ccc-000000000005";
    private static final String PORT = "77777777-aaaa-4bbb-8ccc-000000000007";
    private static final String READY_KEY = "access/DC/cage-a";
    private static final String WAITING_KEY = "access/DC/DC-port-secondary";

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
    void reset() {
        resetStubs();
        String router = "{\"uuid\":\"" + R1 + "\",\"name\":\"FCR-DC\",\"type\":\"XF_ROUTER\",\"state\":\"PROVISIONED\","
                + "\"location\":{\"metroCode\":\"DC\"},\"package\":{\"code\":\"STANDARD\"},"
                + "\"project\":{\"projectId\":\"" + PROJECT + "\"},"
                + "\"notifications\":[{\"type\":\"ALL\",\"emails\":[\"noc@example.com\"]}]}";
        String connection = "{\"uuid\":\"" + A1 + "\",\"name\":\"FCR-DC-access-primary\",\"type\":\"IP_VC\","
                + "\"state\":\"PROVISIONED\",\"bandwidth\":1000,"
                + "\"aSide\":{\"accessPoint\":{\"type\":\"CLOUD_ROUTER\",\"router\":{\"uuid\":\"" + R1 + "\"}}},"
                + "\"zSide\":{\"accessPoint\":{\"type\":\"COLO\",\"port\":{\"uuid\":\"" + PORT + "\"},"
                + "\"linkProtocol\":{\"type\":\"DOT1Q\",\"vlanTag\":220}}},"
                + "\"project\":{\"projectId\":\"" + PROJECT + "\"}}";
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers")).willReturn(aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json").withBody(router)));
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/routers/" + R1)).willReturn(okJson(router)));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections")).willReturn(aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json").withBody(connection)));
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/connections/" + A1)).willReturn(okJson(connection)));
        String empty = "{\"pagination\":{\"offset\":0,\"limit\":20,\"total\":0},\"data\":[]}";
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers/search")).willReturn(okJson(empty)));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/search")).willReturn(okJson(empty)));
    }

    private static PlannedConnection accessConnection(String key, String name, String item, String portUuid) {
        return PlannedConnection.builder().key(key).name(name).connectionType(ConnectionType.IP_VC)
                .purpose(ConnectionPurpose.ACCESS).bandwidthMbps(1000).aSideMetro(DC).aSideRouterName("FCR-DC")
                .zSideMetro(DC).zSideAccessItem(item).zSidePortUuid(portUuid).projectId(PROJECT)
                .notificationEmails(List.of("noc@example.com")).build();
    }

    private static DeploymentPlan plan() {
        AccessPlan access = new AccessPlan(null, List.of(new MetroAccess("DC", AccessMode.PHYSICAL_PORTS, "test",
                List.of(new PlannedAccessItem("cage-a", AccessKind.FABRIC_PORT, "DC", AccessSource.EXISTING, PORT,
                                "DC2", 10_000, RedundancyPriority.PRIMARY, null, null),
                        new PlannedAccessItem("DC-port-secondary", AccessKind.FABRIC_PORT, "DC", AccessSource.ORDER,
                                null, null, 10_000, RedundancyPriority.SECONDARY, null, null)),
                List.of(new PlannedAccessLink("DC-access-primary", "DC", "cage-a", AccessKind.FABRIC_PORT, PORT, 1000,
                                RedundancyPriority.PRIMARY, "test"),
                        new PlannedAccessLink("DC-access-secondary", "DC", "DC-port-secondary",
                                AccessKind.FABRIC_PORT, null, 1000, RedundancyPriority.SECONDARY, "test")),
                List.of())), List.of());
        return DeploymentPlan.builder().deploymentName("demo").namePrefix("FCR").projectId(PROJECT)
                .cloudRouters(List.of(PlanFixtures.router())).providerConnections(List.of())
                .backboneLinks(List.of()).routingProtocols(List.of())
                .access(access)
                .accessConnections(List.of(
                        accessConnection(READY_KEY, "FCR-DC-access-primary", "cage-a", PORT),
                        accessConnection(WAITING_KEY, "FCR-DC-access-secondary", "DC-port-secondary", null)))
                .valid(true).validationErrors(List.of()).fabric(fabric).build();
    }

    private static ExecutionOptions fast() {
        return ExecutionOptions.builder().pollInterval(Duration.ZERO).provisionTimeout(Duration.ofSeconds(10))
                .deleteTimeout(Duration.ofSeconds(10)).build();
    }

    private static StepAction action(DeploymentOutcome outcome, String key) {
        return outcome.getSteps().stream().filter(s -> s.key().equals(key)).findFirst()
                .orElseThrow(() -> new AssertionError("no step " + key + ": " + outcome.toMarkdown())).action();
    }

    @Test
    @DisplayName("the connection to an existing port is created with the port and VLAN on the Z-side; an order is not executed")
    void createsTheConnectionToAnExistingPort() {
        DeploymentOutcome outcome = plan().execute(
                ExecutionInputs.builder().vlanTag("FCR-DC-access-primary", 220).build(), fast());

        assertTrue(outcome.isFullySuccessful(), outcome::toMarkdown);
        assertEquals(StepAction.CREATED, action(outcome, ROUTER_KEY));
        assertEquals(StepAction.CREATED, action(outcome, READY_KEY));
        assertEquals(StepAction.NOT_EXECUTABLE, action(outcome, WAITING_KEY));
        assertEquals(StepAction.NOT_EXECUTABLE, action(outcome, "access/cage-a"));
        assertEquals(StepAction.NOT_EXECUTABLE, action(outcome, "access/DC-port-secondary"));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")).withQueryParam("dryRun", absent()));
        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections")).withQueryParam("dryRun", absent())
                .withRequestBody(matchingJsonPath("$.aSide.accessPoint.type", equalTo("CLOUD_ROUTER")))
                .withRequestBody(matchingJsonPath("$.aSide.accessPoint.router.uuid", equalTo(R1)))
                .withRequestBody(matchingJsonPath("$.zSide.accessPoint.type", equalTo("COLO")))
                .withRequestBody(matchingJsonPath("$.zSide.accessPoint.port.uuid", equalTo(PORT)))
                .withRequestBody(matchingJsonPath("$.zSide.accessPoint.linkProtocol.vlanTag", equalTo("220")))
                .withRequestBody(matchingJsonPath("$.additionalInfo[?(@.key == 'eqxSdkKey')].value",
                        equalTo(READY_KEY))));
    }

    @Test
    @DisplayName("without the VLAN tag for the port nothing is created and the outcome says what is missing")
    void vlanTagIsRequired() {
        DeploymentOutcome outcome = plan().execute(ExecutionInputs.none(), fast());

        assertFalse(outcome.isFullySuccessful());
        assertTrue(outcome.toMarkdown().contains("missing required VLAN tag"), outcome::toMarkdown);
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/routers")));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")));
    }
}
