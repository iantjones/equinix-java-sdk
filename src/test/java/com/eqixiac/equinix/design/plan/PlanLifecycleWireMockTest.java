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
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentOutcome;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.ExecutionInputs;
import com.github.tomakehurst.wiremock.client.ScenarioMappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.design.plan.PlanFixtures.BGP_KEY;
import static com.eqixiac.equinix.design.plan.PlanFixtures.CONN_KEY;
import static com.eqixiac.equinix.design.plan.PlanFixtures.DIRECT_KEY;
import static com.eqixiac.equinix.design.plan.PlanFixtures.PROJECT;
import static com.eqixiac.equinix.design.plan.PlanFixtures.ROUTER_KEY;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The plan lifecycle against a WireMock Fabric: rollback ordering and waiting, the orphan report,
 * the failure policy, resume from a checkpoint without duplicate creates, a no-op re-run, drift with
 * opt-in updates, and dry-run reconcile. Response bodies follow the Fabric v4 spec shapes
 * ({@code CloudRouter}, {@code Connection}, {@code RoutingProtocolData}, search pages).
 */
class PlanLifecycleWireMockTest extends WireMockTestBase {

    private static final String R1 = "11111111-aaaa-4bbb-8ccc-000000000001";
    private static final String C1 = "22222222-aaaa-4bbb-8ccc-000000000002";
    private static final String D1 = "33333333-aaaa-4bbb-8ccc-000000000003";
    private static final String B1 = "44444444-aaaa-4bbb-8ccc-000000000004";
    private static final String C9 = "99999999-aaaa-4bbb-8ccc-000000000009";

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
    }

    // JSON bodies (Fabric v4 spec shapes)

    private static String router(String uuid, String state, String pkg) {
        return "{\"uuid\":\"" + uuid + "\",\"name\":\"FCR-DC\",\"type\":\"XF_ROUTER\",\"state\":\"" + state + "\","
                + "\"location\":{\"metroCode\":\"DC\"},\"package\":{\"code\":\"" + pkg + "\"},"
                + "\"project\":{\"projectId\":\"" + PROJECT + "\"},"
                + "\"notifications\":[{\"type\":\"ALL\",\"emails\":[\"noc@example.com\"]}]}";
    }

    private static String connection(String uuid, String name, String state, int mbps, String key) {
        String info = key == null ? "" : ",\"additionalInfo\":[{\"key\":\"eqxSdkDeployment\",\"value\":\"demo\"},"
                + "{\"key\":\"eqxSdkKey\",\"value\":\"" + key + "\"}]";
        return "{\"uuid\":\"" + uuid + "\",\"name\":\"" + name + "\",\"type\":\"IP_VC\",\"state\":\"" + state + "\","
                + "\"bandwidth\":" + mbps + ",\"direction\":\"OUTGOING\","
                + "\"aSide\":{\"accessPoint\":{\"type\":\"CLOUD_ROUTER\",\"router\":{\"uuid\":\"" + R1 + "\"}}},"
                + "\"zSide\":{\"accessPoint\":{\"type\":\"SP\",\"profile\":{\"type\":\"L2_PROFILE\",\"uuid\":\"sp-aws\"},"
                + "\"sellerRegion\":\"us-east-1\",\"location\":{\"metroCode\":\"DC\"}}},"
                + "\"project\":{\"projectId\":\"" + PROJECT + "\"},"
                + "\"notifications\":[{\"type\":\"ALL\",\"emails\":[\"noc@example.com\"]}]" + info + "}";
    }

    private static String direct(String state) {
        return "{\"uuid\":\"" + D1 + "\",\"name\":\"FCR-DC-to-aws-DIRECT\",\"type\":\"DIRECT\",\"state\":\"" + state
                + "\",\"directIpv4\":{\"equinixIfaceIp\":\"10.100.0.1/30\"}}";
    }

    private static String bgp(String state) {
        return "{\"uuid\":\"" + B1 + "\",\"name\":\"FCR-DC-to-aws-BGP\",\"type\":\"BGP\",\"state\":\"" + state
                + "\",\"bgpIpv4\":{\"customerPeerIp\":\"10.100.0.2\",\"enabled\":true},\"customerAsn\":65100,"
                + "\"bfd\":{\"enabled\":true,\"interval\":\"300\"}}";
    }

    private static String page(String... items) {
        return "{\"pagination\":{\"offset\":0,\"limit\":20,\"total\":" + items.length + "},\"data\":["
                + String.join(",", items) + "]}";
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder created(String body) {
        return aResponse().withStatus(201).withHeader("Content-Type", "application/json").withBody(body);
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder error(int status) {
        return aResponse().withStatus(status).withHeader("Content-Type", "application/json")
                .withBody("[{\"errorCode\":\"EQ-" + status + "\",\"errorMessage\":\"stubbed failure\"}]");
    }

    // stubs

    /** Create endpoints that succeed and resources that read PROVISIONED; live searches empty. */
    private static void stubCreates() {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers")).withQueryParam("dryRun", absent())
                .willReturn(created(router(R1, "PROVISIONING", "STANDARD"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers")).withQueryParam("dryRun", equalTo("true"))
                .willReturn(okJson(router(R1, "PROVISIONING", "STANDARD"))));
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/routers/" + R1)).willReturn(okJson(router(R1, "PROVISIONED", "STANDARD"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections")).withQueryParam("dryRun", equalTo("true"))
                .willReturn(okJson(connection(C1, "FCR-DC-to-aws", "PROVISIONING", 1000, CONN_KEY))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections")).withQueryParam("dryRun", absent())
                .willReturn(created(connection(C1, "FCR-DC-to-aws", "PROVISIONING", 1000, CONN_KEY))));
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/connections/" + C1))
                .willReturn(okJson(connection(C1, "FCR-DC-to-aws", "PROVISIONED", 1000, CONN_KEY))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/" + C1 + "/routingProtocols"))
                .withRequestBody(containing("DIRECT")).willReturn(created(direct("PROVISIONING"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/" + C1 + "/routingProtocols"))
                .withRequestBody(containing("BGP")).willReturn(created(bgp("PROVISIONING"))));
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/connections/" + C1 + "/routingProtocols/" + D1))
                .willReturn(okJson(direct("PROVISIONED"))));
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/connections/" + C1 + "/routingProtocols/" + B1))
                .willReturn(okJson(bgp("PROVISIONED"))));
        stubLive(List.of(), List.of(), List.of());
    }

    /** What the search and list endpoints report as live. */
    private static void stubLive(List<String> routers, List<String> connections, List<String> protocols) {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers/search"))
                .willReturn(okJson(page(routers.toArray(String[]::new)))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/search"))
                .willReturn(okJson(page(connections.toArray(String[]::new)))));
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/connections/" + C1 + "/routingProtocols"))
                .willReturn(okJson(page(protocols.toArray(String[]::new)))));
    }

    /** GET reads {@code body} until the DELETE, then the given states in turn (the last one sticks). */
    private static void stubAsyncDelete(String path, String body, String... afterDelete) {
        String scenario = "delete " + path;
        wireMock.stubFor(get(urlPathEqualTo(path)).inScenario(scenario).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(okJson(body)));
        wireMock.stubFor(delete(urlPathEqualTo(path)).inScenario(scenario).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(okJson(body)).willSetStateTo("after-0"));
        for (int i = 0; i < afterDelete.length; i++) {
            String state = afterDelete[i];
            ScenarioMappingBuilder getAfter = get(urlPathEqualTo(path)).inScenario(scenario)
                    .whenScenarioStateIs("after-" + i)
                    .willReturn("404".equals(state) ? error(404)
                            : okJson(body.replace("\"state\":\"PROVISIONED\"", "\"state\":\"" + state + "\"")));
            if (i < afterDelete.length - 1) {
                getAfter = getAfter.willSetStateTo("after-" + (i + 1));
            }
            wireMock.stubFor(getAfter);
        }
    }

    private static ExecutionOptions fast() {
        return ExecutionOptions.builder().pollInterval(Duration.ZERO)
                .provisionTimeout(Duration.ofSeconds(10)).deleteTimeout(Duration.ofSeconds(10)).build();
    }

    private static ExecutionInputs inputs() {
        return ExecutionInputs.builder().authenticationKey("FCR-DC-to-aws", "123456789012").vlanTag("FCR-DC-to-aws", 1001).build();
    }

    private static DeploymentPlan plan() {
        return PlanFixtures.plan(fabric);
    }

    private static int realPosts(String path) {
        return wireMock.findAll(postRequestedFor(urlPathEqualTo(path)).withQueryParam("dryRun", absent())).size();
    }

    private static int rpPosts() {
        return wireMock.findAll(postRequestedFor(urlPathEqualTo("/fabric/v4/connections/" + C1 + "/routingProtocols"))).size();
    }

    private static StepAction action(DeploymentOutcome outcome, String key) {
        return outcome.getSteps().stream().filter(s -> s.key().equals(key)).findFirst()
                .orElseThrow(() -> new AssertionError("no step " + key + ": " + outcome.toMarkdown())).action();
    }

    /** A clock that moves ten seconds every time it is read, so any wait is bounded without sleeping. */
    private static final class TickingClock extends Clock {
        private Instant now = Instant.parse("2026-11-12T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            now = now.plusSeconds(10);
            return now;
        }
    }

    // tests

    @Test
    @DisplayName("execute creates every step in order, marks the connection and sends its project; the Interconnect is NOT_EXECUTABLE")
    void executeHappyPath() {
        stubCreates();
        DeploymentOutcome outcome = plan().execute(inputs(), fast());

        assertTrue(outcome.isFullySuccessful(), outcome::toMarkdown);
        assertEquals(List.of(ROUTER_KEY, CONN_KEY, DIRECT_KEY, BGP_KEY, "interconnect/DC/core"),
                outcome.getSteps().stream().map(StepResult::key).toList());
        assertEquals(StepAction.NOT_EXECUTABLE, action(outcome, "interconnect/DC/core"));
        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections")).withQueryParam("dryRun", absent())
                .withRequestBody(matchingJsonPath("$.additionalInfo[?(@.key == 'eqxSdkKey')].value", equalTo(CONN_KEY)))
                .withRequestBody(matchingJsonPath("$.additionalInfo[?(@.key == 'eqxSdkDeployment')].value", equalTo("demo")))
                .withRequestBody(matchingJsonPath("$.project.projectId", equalTo(PROJECT))));
        assertEquals(1, realPosts("/fabric/v4/routers"));
        assertEquals(2, rpPosts());
    }

    @Test
    @DisplayName("abort: rollback waits for the connection delete to reach DEPROVISIONED before deleting the router")
    void rollbackWaitsForConnectionDeleteBeforeRouter() {
        stubCreates();
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/" + C1 + "/routingProtocols"))
                .withRequestBody(containing("BGP")).willReturn(error(500)));
        stubAsyncDelete("/fabric/v4/connections/" + C1, connection(C1, "FCR-DC-to-aws", "PROVISIONED", 1000, CONN_KEY),
                "DEPROVISIONING", "DEPROVISIONING", "DEPROVISIONED");
        stubAsyncDelete("/fabric/v4/routers/" + R1, router(R1, "PROVISIONED", "STANDARD"), "404");
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();

        DeploymentOutcome outcome = plan().reconcile(inputs(), fast(), store);

        assertFalse(outcome.isFullySuccessful());
        assertEquals(StepAction.FAILED, action(outcome, BGP_KEY));
        RollbackReport rollback = outcome.getRollback();
        assertNotNull(rollback);
        assertTrue(rollback.isClean(), rollback::toMarkdown);
        assertEquals(List.of(CONN_KEY, DIRECT_KEY, ROUTER_KEY), rollback.removed().stream().map(RemovedResource::key).toList());

        // Order on the wire: the router DELETE comes after the connection was observed DEPROVISIONED.
        List<ServeEvent> events = new ArrayList<>(wireMock.getAllServeEvents());
        java.util.Collections.reverse(events); // oldest first
        int routerDelete = -1;
        int connectionGone = -1;
        for (int i = 0; i < events.size(); i++) {
            ServeEvent e = events.get(i);
            String url = e.getRequest().getUrl();
            String method = e.getRequest().getMethod().getName();
            if ("DELETE".equals(method) && url.startsWith("/fabric/v4/routers/" + R1)) {
                routerDelete = i;
            }
            if ("GET".equals(method) && url.startsWith("/fabric/v4/connections/" + C1 + "?") || url.equals("/fabric/v4/connections/" + C1)) {
                if ("GET".equals(method) && e.getResponse().getBodyAsString().contains("\"DEPROVISIONED\"")) {
                    connectionGone = i;
                }
            }
        }
        assertTrue(connectionGone > 0, "the connection was polled to DEPROVISIONED");
        assertTrue(routerDelete > connectionGone, "router deleted only after the connection delete finished");
        // The checkpoint no longer lists what was removed.
        assertTrue(store.load("demo").orElseThrow().steps().isEmpty(), store.load("demo").toString());
    }

    @Test
    @DisplayName("a connection delete that never finishes is an orphan with its uuid; the router it attaches to is kept and reported")
    void orphanReport() {
        stubCreates();
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/" + C1 + "/routingProtocols"))
                .withRequestBody(containing("BGP")).willReturn(error(500)));
        stubAsyncDelete("/fabric/v4/connections/" + C1, connection(C1, "FCR-DC-to-aws", "PROVISIONED", 1000, CONN_KEY),
                "DEPROVISIONING");
        ExecutionOptions options = fast().toBuilder().deleteTimeout(Duration.ofMinutes(2)).clock(new TickingClock()).build();

        DeploymentOutcome outcome = plan().execute(inputs(), options);

        RollbackReport rollback = outcome.getRollback();
        assertFalse(rollback.isClean());
        OrphanedResource conn = rollback.orphans().stream().filter(o -> o.key().equals(CONN_KEY)).findFirst().orElseThrow();
        assertEquals(C1, conn.uuid());
        assertTrue(conn.reason().contains("not confirmed within PT2M") && conn.reason().contains("DEPROVISIONING"), conn.reason());
        OrphanedResource router = rollback.orphans().stream().filter(o -> o.key().equals(ROUTER_KEY)).findFirst().orElseThrow();
        assertEquals(R1, router.uuid());
        assertTrue(router.reason().contains(CONN_KEY), router.reason());
        assertTrue(rollback.orphans().stream().anyMatch(o -> o.key().equals(DIRECT_KEY) && D1.equals(o.uuid())));
        wireMock.verify(0, deleteRequestedFor(urlPathMatching("/fabric/v4/routers/.*")));
        assertTrue(outcome.getErrors().stream().anyMatch(e -> e.getReason().startsWith("ORPHANED after rollback (uuid " + C1)),
                outcome::toMarkdown);
        assertTrue(outcome.toSummary().contains(R1), outcome.toSummary());
    }

    @Test
    @DisplayName("failure policy: a failed router create aborts by default; CONTINUE_ON_ERROR skips its dependents and reports partial state")
    void failurePolicy() {
        stubCreates();
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers")).withQueryParam("dryRun", absent()).willReturn(error(500)));

        DeploymentOutcome aborted = plan().execute(inputs(), fast());
        assertEquals(List.of(ROUTER_KEY), aborted.getSteps().stream().map(StepResult::key).toList(),
                "nothing after the failed step is attempted");
        assertEquals(StepAction.FAILED, action(aborted, ROUTER_KEY));
        assertTrue(aborted.getRollback().removed().isEmpty() && aborted.getRollback().isClean());
        assertEquals(0, realPosts("/fabric/v4/connections"));

        DeploymentOutcome partial = plan().execute(inputs(),
                fast().toBuilder().failurePolicy(FailurePolicy.CONTINUE_ON_ERROR).build());
        assertNull(partial.getRollback());
        assertEquals(StepAction.FAILED, action(partial, ROUTER_KEY));
        assertEquals(StepAction.SKIPPED, action(partial, CONN_KEY));
        assertEquals(StepAction.SKIPPED, action(partial, DIRECT_KEY));
        assertEquals(StepAction.SKIPPED, action(partial, BGP_KEY));
        assertEquals(StepAction.NOT_EXECUTABLE, action(partial, "interconnect/DC/core"));
        assertEquals(0, realPosts("/fabric/v4/connections"));
        assertEquals(0, wireMock.findAll(deleteRequestedFor(urlPathMatching("/fabric/v4/.*"))).size());
    }

    /** A store that dies on its n-th save: before writing (lost write) or after (written, then crash). */
    private static final class CrashingStore implements CheckpointStore {
        private final CheckpointStore delegate;
        private final int crashOn;
        private final boolean writeBeforeCrash;
        private int saves;

        CrashingStore(CheckpointStore delegate, int crashOn, boolean writeBeforeCrash) {
            this.delegate = delegate;
            this.crashOn = crashOn;
            this.writeBeforeCrash = writeBeforeCrash;
        }

        @Override
        public java.util.Optional<Checkpoint> load(String deploymentName) {
            return delegate.load(deploymentName);
        }

        @Override
        public void save(Checkpoint checkpoint) {
            saves++;
            if (saves == crashOn) {
                if (writeBeforeCrash) {
                    delegate.save(checkpoint);
                }
                throw new IllegalStateException("simulated crash at checkpoint " + saves);
            }
            delegate.save(checkpoint);
        }

        @Override
        public void delete(String deploymentName) {
            delegate.delete(deploymentName);
        }
    }

    @ParameterizedTest(name = "checkpoint write survived the crash: {0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("a run killed right after the connection create resumes from the checkpoint without duplicate POSTs")
    void resumeFromCheckpoint(boolean writeBeforeCrash) {
        stubCreates();
        InMemoryCheckpointStore durable = new InMemoryCheckpointStore();
        // Saves: 1 router accepted, 2 router provisioned, 3 connection accepted -> crash.
        assertThrows(IllegalStateException.class,
                () -> plan().reconcile(inputs(), fast(), new CrashingStore(durable, 3, writeBeforeCrash)));
        assertEquals(1, realPosts("/fabric/v4/routers"));
        assertEquals(1, realPosts("/fabric/v4/connections"));
        assertEquals(writeBeforeCrash, durable.load("demo").orElseThrow().step(CONN_KEY).isPresent());

        // The API now lists what the dead run created; the connection carries the plan marker.
        stubLive(List.of(router(R1, "PROVISIONED", "STANDARD")),
                List.of(connection(C1, "FCR-DC-to-aws", "PROVISIONED", 1000, CONN_KEY)), List.of());

        DeploymentOutcome resumed = plan().reconcile(inputs(), fast(), durable);

        assertTrue(resumed.isFullySuccessful(), resumed::toMarkdown);
        assertEquals(StepAction.ALREADY_DONE, action(resumed, ROUTER_KEY));
        assertEquals(writeBeforeCrash ? StepAction.ALREADY_DONE : StepAction.ADOPTED, action(resumed, CONN_KEY));
        assertEquals(StepAction.CREATED, action(resumed, DIRECT_KEY));
        assertEquals(StepAction.CREATED, action(resumed, BGP_KEY));
        assertEquals(1, realPosts("/fabric/v4/routers"), "no duplicate router");
        assertEquals(1, realPosts("/fabric/v4/connections"), "no duplicate connection");
        assertEquals(2, rpPosts());
        assertEquals(4, durable.load("demo").orElseThrow().steps().size());
    }

    @Test
    @DisplayName("re-running a completed plan is a no-op: no create is sent, every step is ALREADY_DONE")
    void rerunIsNoOp() {
        stubCreates();
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();
        assertTrue(plan().reconcile(inputs(), fast(), store).isFullySuccessful());
        stubLive(List.of(router(R1, "PROVISIONED", "STANDARD")),
                List.of(connection(C1, "FCR-DC-to-aws", "PROVISIONED", 1000, CONN_KEY)),
                List.of(direct("PROVISIONED"), bgp("PROVISIONED")));
        wireMock.resetRequests();

        DeploymentOutcome again = plan().reconcile(ExecutionInputs.none(), fast(), store);

        assertTrue(again.isFullySuccessful(), again::toMarkdown);
        for (String key : List.of(ROUTER_KEY, CONN_KEY, DIRECT_KEY, BGP_KEY)) {
            assertEquals(StepAction.ALREADY_DONE, action(again, key));
        }
        assertEquals(0, wireMock.findAll(postRequestedFor(urlPathMatching("/fabric/v4/(routers|connections)(/[^/]+/routingProtocols)?"))).size(),
                "no create or dry-run was sent");
        assertTrue(again.getResources().isEmpty());
    }

    @Test
    @DisplayName("diff reads live state in the plan's project; reconcile patches only patchable drift when asked")
    void driftAndOptInUpdates() {
        stubCreates();
        stubLive(List.of(router(R1, "PROVISIONED", "LAB")),
                List.of(connection(C1, "FCR-DC-to-aws", "PROVISIONED", 500, CONN_KEY),
                        connection(C9, "FCR-DC-to-azure", "PROVISIONED", 1000, "connection/DC/azure")),
                List.of(direct("PROVISIONED"), bgp("PROVISIONED")));
        wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/routers/" + R1)).willReturn(okJson(router(R1, "PROVISIONED", "STANDARD"))));
        wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/connections/" + C1))
                .willReturn(okJson(connection(C1, "FCR-DC-to-aws", "PROVISIONED", 1000, CONN_KEY))));

        PlanDiff diff = plan().diff();
        assertEquals(DriftStatus.PRESENT_DIFFERENT, diff.drift(ROUTER_KEY).orElseThrow().status());
        assertEquals(DriftStatus.PRESENT_DIFFERENT, diff.drift(CONN_KEY).orElseThrow().status());
        assertEquals("marker", diff.drift(CONN_KEY).orElseThrow().matchedBy());
        assertEquals(DriftStatus.PRESENT_MATCHING, diff.drift(BGP_KEY).orElseThrow().status());
        assertEquals(List.of(C9), diff.unmanaged().stream().map(UnmanagedResource::uuid).toList());
        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections/search"))
                .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/project/projectId")))
                .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo(PROJECT))));

        DeploymentOutcome reportOnly = plan().reconcile(ExecutionInputs.none(), fast(), new InMemoryCheckpointStore());
        assertEquals(StepAction.DRIFT_REPORTED, action(reportOnly, CONN_KEY));
        assertEquals(0, wireMock.findAll(patchRequestedFor(urlPathMatching("/fabric/v4/.*"))).size());

        DeploymentOutcome updated = plan().reconcile(ExecutionInputs.none(),
                fast().toBuilder().applyUpdates(true).build(), new InMemoryCheckpointStore());
        assertEquals(StepAction.UPDATED, action(updated, ROUTER_KEY));
        assertEquals(StepAction.UPDATED, action(updated, CONN_KEY));
        wireMock.verify(patchRequestedFor(urlPathEqualTo("/fabric/v4/routers/" + R1))
                .withRequestBody(equalToJson("[{\"op\":\"replace\",\"path\":\"/package/code\",\"value\":\"STANDARD\"}]")));
        wireMock.verify(patchRequestedFor(urlPathEqualTo("/fabric/v4/connections/" + C1))
                .withRequestBody(equalToJson("[{\"op\":\"replace\",\"path\":\"/bandwidth\",\"value\":1000}]")));
        assertEquals(0, realPosts("/fabric/v4/routers") + realPosts("/fabric/v4/connections"));
    }

    @Test
    @DisplayName("dry-run reconcile sends dryRun=true creates where the API offers them and creates and records nothing")
    void dryRunReconcile() {
        stubCreates();
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();

        DeploymentOutcome outcome = plan().reconcile(ExecutionInputs.none(),
                fast().toBuilder().dryRun(true).build(), store);

        assertTrue(outcome.isDryRun());
        assertEquals(StepAction.DRY_RUN_PASSED, action(outcome, ROUTER_KEY));
        assertEquals(StepAction.DRY_RUN_DEFERRED, action(outcome, CONN_KEY));
        assertEquals(StepAction.DRY_RUN_DEFERRED, action(outcome, BGP_KEY));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/routers")).withQueryParam("dryRun", equalTo("true")));
        assertEquals(0, realPosts("/fabric/v4/routers") + realPosts("/fabric/v4/connections") + rpPosts());
        assertTrue(store.load("demo").isEmpty(), "a dry run writes no checkpoint");
        assertTrue(outcome.getResources().isEmpty());
        assertTrue(Map.of().isEmpty());
    }

    @Test
    @DisplayName("connections the search returns from other projects are ignored; checkpointed connections are read by uuid")
    void connectionSearchProjectFilterIsNotTrusted() {
        stubCreates();
        // Live 2026-11-12: the connection search ignores the project filter. A same-named, unmarked
        // connection in another project must not be taken for the plan's connection.
        String foreign = connection(C9, "FCR-DC-to-aws", "PROVISIONED", 1000, null)
                .replace(PROJECT, "0ther000-0000-4000-8000-000000000000");
        stubLive(List.of(router(R1, "PROVISIONED", "STANDARD")), List.of(foreign), List.of());

        PlanDiff diff = plan().diff();
        assertEquals(DriftStatus.MISSING, diff.drift(CONN_KEY).orElseThrow().status(), diff::toMarkdown);
        assertTrue(diff.unmanaged().isEmpty(), diff::toMarkdown);
        assertTrue(diff.notes().stream().anyMatch(n -> n.contains("1 connection(s) from other projects")), diff::toMarkdown);

        // The checkpoint knows the real connection: it is read by uuid even though no search returns it.
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();
        store.save(Checkpoint.empty("x", "demo").with(new CompletedStep(CONN_KEY, ResourceKind.PROVIDER_CONNECTION,
                "FCR-DC-to-aws", C1, null, "PROVISIONED", true), Instant.EPOCH));
        PlanDiff withCheckpoint = plan().diff(store);
        assertEquals(C1, withCheckpoint.drift(CONN_KEY).orElseThrow().liveUuid());
        assertEquals("checkpoint", withCheckpoint.drift(CONN_KEY).orElseThrow().matchedBy());
        wireMock.verify(com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(
                urlPathEqualTo("/fabric/v4/connections/" + C1)));
    }
}
