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

package com.eqixiac.equinix.mcp.server.broker;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.mcp.server.ServerContext;
import com.eqixiac.equinix.mcp.server.ToolRegistration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.core.ResponseStubs.stubCreate;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire proofs for the two-phase protocol, in the repo's established wire-assertion style: the
 * propose phase MUST put {@code dryRun=true} on the exact create endpoint (a dropped parameter
 * would silently turn "verification" into a real mutation), and the confirm phase MUST send
 * the real POST without any {@code dryRun} parameter, carrying the stored spec.
 *
 * <p>Stub layering: each flow registers the real-create stub first and the
 * {@code dryRun=true}-matching stub second, so WireMock's most-recent-match-wins routing sends
 * dry-run requests to the dry-run stub and everything else to the real stub.</p>
 */
@DisplayName("Safe Mutation Broker - wire proofs (dryRun=true on propose, none on confirm)")
class BrokerWireMockTest extends WireMockTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CONNECTIONS = "/fabric/v4/connections";
    private static final String NETWORKS = "/fabric/v4/networks";
    private static final String SERVICE_TOKENS = "/fabric/v4/serviceTokens";

    private static final String CONNECTION_PROPOSAL = """
            {"change_type": "connection_create",
             "spec": {
               "type": "EVPL_VC",
               "name": "Broker-EVPL",
               "bandwidth_mbps": 1000,
               "notification_emails": ["ops@example.com"],
               "a_side": {"port_uuid": "c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee",
                          "link_protocol": {"type": "dot1q", "vlan_tag": 1001}},
               "z_side": {"service_profile_uuid": "20d32a80-0d61-4333-bc03-4b2d446794a0",
                          "link_protocol": {"type": "dot1q", "vlan_tag": 1002}, "metro_code": "SV"}}}""";

    private static final String NETWORK_PROPOSAL = """
            {"change_type": "network_create",
             "spec": {
               "type": "EVPLAN",
               "name": "Broker-EVPLAN",
               "scope": "GLOBAL",
               "notification_emails": ["ops@example.com"]}}""";

    private static final String SERVICE_TOKEN_PROPOSAL = """
            {"change_type": "service_token_create",
             "spec": {
               "issuer_side": "a_side",
               "type": "VC_TOKEN",
               "name": "Broker-Token",
               "expiry_days": 30,
               "connection_type": "EVPL_VC",
               "access_point": {"port_uuid": "c791f8cb-5cc9-4a9f-8b8a-1f2e3d4c5b6a",
                                "link_protocol": {"type": "dot1q", "vlan_tag": 1001}}}}""";

    static Fabric fabric;

    private ProposalStore store;
    private List<ToolRegistration> tools;
    private ServerContext context;

    @BeforeAll
    static void setUpFabric() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDownFabric() throws Exception {
        if (fabric != null) {
            fabric.close();
        }
    }

    @BeforeEach
    void setUpBroker() {
        resetStubs();
        store = new ProposalStore();
        tools = BrokerToolFactory.tools(store);
        // These tests exercise execution, replay and integrity with no client exchange bound; the
        // human-prompt gate itself is covered by BrokerHumanConfirmWireMockTest.
        context = ServerContext.builder().fabric(fabric)
                .environment(Map.of(ServerContext.ENV_ALLOW_UNCONFIRMED_MUTATIONS, "true")).build();
    }

    private ObjectNode call(String name, String argsJson) throws Exception {
        ToolRegistration tool = tools.stream().filter(t -> t.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("tool not registered: " + name));
        return tool.getHandler().handle(MAPPER.readTree(argsJson), context);
    }

    private ObjectNode confirm(String token) throws Exception {
        return call("fabric_confirm_change", "{\"confirm_token\":\"" + token + "\"}");
    }

    /** Real-create stub first, dryRun=true stub second: most-recent-match-wins routes them. */
    private static void stubCreateAndDryRun(String url, String realFixture, String dryRunBody) {
        stubCreate(wireMock, url, realFixture);
        wireMock.stubFor(post(urlPathEqualTo(url))
                .withQueryParam("dryRun", equalTo("true"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(dryRunBody)));
    }

    // connection_create

    @Test
    @DisplayName("a service-profile z_side without metro_code is rejected before any request (Fabric needs location.metroCode)")
    void serviceProfileZSideNeedsMetro() {
        String noMetro = CONNECTION_PROPOSAL.replace(", \"metro_code\": \"SV\"", "");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call("fabric_propose_change", noMetro));
        assertTrue(e.getMessage().contains("metro_code"), e.getMessage());
        wireMock.verify(0, postRequestedFor(urlPathEqualTo(CONNECTIONS)));
    }

    @Test
    @DisplayName("the service-profile z_side metro_code reaches zSide.accessPoint.location.metroCode")
    void serviceProfileZSideSendsMetro() throws Exception {
        stubCreateAndDryRun(CONNECTIONS, "/json/fabric/connection_response.json",
                loadFixture("/json/fabric/connection_response.json"));
        call("fabric_propose_change", CONNECTION_PROPOSAL);
        wireMock.verify(postRequestedFor(urlPathEqualTo(CONNECTIONS))
                .withRequestBody(matchingJsonPath("$.zSide.accessPoint.location.metroCode", equalTo("SV"))));
    }

    @Test
    @DisplayName("propose sends dryRun=true to POST /connections with the exact spec, and nothing else")
    void proposeConnectionIsDryRunOnly() throws Exception {
        stubCreateAndDryRun(CONNECTIONS, "/json/fabric/connection_response.json",
                loadFixture("/json/fabric/connection_response.json"));

        ObjectNode payload = call("fabric_propose_change", CONNECTION_PROPOSAL);

        assertEquals("dry_run", payload.get("phase").asText());
        assertEquals("connection_create", payload.get("change_type").asText());
        assertEquals("connection", payload.get("validation").get("resource").asText());
        assertTrue(payload.get("confirm_token").asText().startsWith("chg-"));
        assertEquals(ProposalStore.DEFAULT_TTL.toSeconds(), payload.get("expires_in_seconds").asLong());
        assertTrue(payload.get("spec_sha256").asText().matches("^[0-9a-f]{64}$"));
        assertEquals(1, store.size(), "a passed dry run mints exactly one pending proposal");

        // Regression lock: the propose phase MUST carry dryRun=true on the wire - without it,
        // "propose" would BE the mutation.
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CONNECTIONS)));
        wireMock.verify(postRequestedFor(urlPathEqualTo(CONNECTIONS))
                .withQueryParam("dryRun", equalTo("true"))
                .withRequestBody(matchingJsonPath("$.type", equalTo("EVPL_VC")))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Broker-EVPL")))
                .withRequestBody(matchingJsonPath("$.bandwidth", equalTo("1000"))));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo(CONNECTIONS))
                .withQueryParam("dryRun", absent()));
    }

    @Test
    @DisplayName("connection proposals carry a rate-card price context with provenance")
    void proposeConnectionCarriesPriceContext() throws Exception {
        stubCreateAndDryRun(CONNECTIONS, "/json/fabric/connection_response.json",
                loadFixture("/json/fabric/connection_response.json"));

        ObjectNode payload = call("fabric_propose_change", CONNECTION_PROPOSAL);
        ObjectNode price = (ObjectNode) payload.get("price_context");

        assertTrue(price.get("priced").asBoolean(), "a 1000 Mbps connection is estimable: " + price);
        // Live Equinix pricing is unavailable here (the prices endpoint is unstubbed), so the
        // layered chain honestly reports the bundled reference figures as the source.
        assertEquals("REFERENCE", price.get("price_source").asText());
        assertTrue(price.get("monthly_recurring").decimalValue().signum() > 0, price.toString());
        assertTrue(price.get("basis").asText().contains("not a quote"), "the estimate stays honest");
    }

    @Test
    @DisplayName("confirm executes the real POST /connections without dryRun, once, with the stored spec")
    void confirmConnectionExecutesForReal() throws Exception {
        stubCreateAndDryRun(CONNECTIONS, "/json/fabric/connection_response.json",
                loadFixture("/json/fabric/connection_response.json"));

        String token = call("fabric_propose_change", CONNECTION_PROPOSAL).get("confirm_token").asText();
        ObjectNode confirmed = confirm(token);

        assertEquals("executed", confirmed.get("phase").asText());
        assertEquals("connection_create", confirmed.get("change_type").asText());
        assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", confirmed.get("result").get("uuid").asText());

        // Regression lock: exactly one dry run (propose) and exactly one real create (confirm),
        // and the real create carries the STORED spec with no dryRun parameter.
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CONNECTIONS))
                .withQueryParam("dryRun", equalTo("true")));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CONNECTIONS))
                .withQueryParam("dryRun", absent()));
        wireMock.verify(postRequestedFor(urlPathEqualTo(CONNECTIONS))
                .withQueryParam("dryRun", absent())
                .withRequestBody(matchingJsonPath("$.type", equalTo("EVPL_VC")))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Broker-EVPL")))
                .withRequestBody(matchingJsonPath("$.bandwidth", equalTo("1000"))));
    }

    @Test
    @DisplayName("a replayed token executes nothing a second time")
    void replayedTokenExecutesNothing() throws Exception {
        stubCreateAndDryRun(CONNECTIONS, "/json/fabric/connection_response.json",
                loadFixture("/json/fabric/connection_response.json"));

        String token = call("fabric_propose_change", CONNECTION_PROPOSAL).get("confirm_token").asText();
        confirm(token);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> confirm(token));
        assertTrue(e.getMessage().contains("already"), e.getMessage());
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CONNECTIONS))
                .withQueryParam("dryRun", absent()));
    }

    @Test
    @DisplayName("different specs mint different tokens bound to different hashes")
    void specDriftMeansNewProposal() throws Exception {
        stubCreateAndDryRun(CONNECTIONS, "/json/fabric/connection_response.json",
                loadFixture("/json/fabric/connection_response.json"));

        ObjectNode first = call("fabric_propose_change", CONNECTION_PROPOSAL);
        ObjectNode second = call("fabric_propose_change",
                CONNECTION_PROPOSAL.replace("Broker-EVPL", "Broker-EVPL-v2"));

        assertNotEquals(first.get("confirm_token").asText(), second.get("confirm_token").asText());
        assertNotEquals(first.get("spec_sha256").asText(), second.get("spec_sha256").asText(),
                "any spec drift is a different binding - the old token cannot execute the new spec");
        assertEquals(2, store.size());
    }

    // network_create

    @Test
    @DisplayName("network propose/confirm: dry run first (echo, no uuid), then the real create")
    void networkTwoPhase() throws Exception {
        stubCreateAndDryRun(NETWORKS, "/json/fabric/network_response.json", """
                {
                  "type": "EVPLAN",
                  "name": "Broker-EVPLAN",
                  "scope": "GLOBAL",
                  "notifications": [ { "type": "ALL", "emails": [ "ops@example.com" ] } ]
                }
                """);

        ObjectNode payload = call("fabric_propose_change", NETWORK_PROPOSAL);

        assertEquals("network", payload.get("validation").get("resource").asText());
        assertEquals("Broker-EVPLAN", payload.get("validation").get("name").asText());
        assertFalse(payload.get("validation").has("uuid"),
                "the dry-run echo carries no uuid - nothing exists yet");
        assertFalse(payload.get("price_context").get("priced").asBoolean());
        assertTrue(payload.get("price_context").get("note").asText().contains("unpriced"),
                "network creates are honestly unpriced: " + payload.get("price_context"));

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(NETWORKS))
                .withQueryParam("dryRun", equalTo("true")));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo(NETWORKS))
                .withQueryParam("dryRun", absent()));

        ObjectNode confirmed = confirm(payload.get("confirm_token").asText());
        assertEquals("c3d4e5f6-a7b8-9012-cdef-234567890abc", confirmed.get("result").get("uuid").asText());
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(NETWORKS))
                .withQueryParam("dryRun", absent())
                .withRequestBody(matchingJsonPath("$.type", equalTo("EVPLAN")))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Broker-EVPLAN")))
                .withRequestBody(matchingJsonPath("$.scope", equalTo("GLOBAL"))));
    }

    // service_token_create

    @Test
    @DisplayName("service-token propose/confirm: dryRun=true validation, then the real create")
    void serviceTokenTwoPhase() throws Exception {
        stubCreateAndDryRun(SERVICE_TOKENS, "/json/fabric/service_token_response.json",
                loadFixture("/json/fabric/service_token_response.json"));

        ObjectNode payload = call("fabric_propose_change", SERVICE_TOKEN_PROPOSAL);

        assertEquals("service_token", payload.get("validation").get("resource").asText());
        assertFalse(payload.get("price_context").get("priced").asBoolean(),
                "service-token creates are honestly unpriced");

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(SERVICE_TOKENS))
                .withQueryParam("dryRun", equalTo("true"))
                .withRequestBody(matchingJsonPath("$.type", equalTo("VC_TOKEN")))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Broker-Token")))
                .withRequestBody(matchingJsonPath(
                        "$.connection.aSide.accessPointSelectors[0].port.uuid",
                        equalTo("c791f8cb-5cc9-4a9f-8b8a-1f2e3d4c5b6a"))));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo(SERVICE_TOKENS))
                .withQueryParam("dryRun", absent()));

        ObjectNode confirmed = confirm(payload.get("confirm_token").asText());
        assertEquals("ab7f685-41b0-1b07-6de0-3a7c54b08b8f", confirmed.get("result").get("uuid").asText());
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(SERVICE_TOKENS))
                .withQueryParam("dryRun", absent()));
    }

    // binding: the token is bound to the compiled request

    private static com.fasterxml.jackson.databind.JsonNode loggedBody(String url, boolean dryRun) throws Exception {
        List<LoggedRequest> requests = wireMock.findAll(postRequestedFor(urlPathEqualTo(url))
                .withQueryParam("dryRun", dryRun ? equalTo("true") : absent()));
        assertEquals(1, requests.size(), (dryRun ? "dry-run" : "real") + " requests to " + url);
        return MAPPER.readTree(requests.get(0).getBodyAsString());
    }

    @Test
    @DisplayName("the token binds the compiled request: propose shows it, the dry run and the real create send it")
    void compiledRequestIsWhatIsSent() throws Exception {
        stubCreateAndDryRun(CONNECTIONS, "/json/fabric/connection_response.json",
                loadFixture("/json/fabric/connection_response.json"));

        ObjectNode proposed = call("fabric_propose_change", CONNECTION_PROPOSAL);
        assertTrue(proposed.get("request_sha256").asText().matches("^[0-9a-f]{64}$"), proposed.toString());
        assertEquals(proposed.get("compiled_request"), loggedBody(CONNECTIONS, true),
                "the compiled request shown to the agent is the body the dry run sent");
        String expectedSha = SpecHash.sha256Hex("connection_create\n"
                + SpecHash.canonicalize(loggedBody(CONNECTIONS, true), MAPPER));
        assertEquals(expectedSha, proposed.get("request_sha256").asText(),
                "request_sha256 is the hash of the change type and the canonical body on the wire");

        ObjectNode confirmed = confirm(proposed.get("confirm_token").asText());
        assertEquals("executed", confirmed.get("phase").asText());
        assertTrue(confirmed.get("confirm_checks").get("request_matches_binding").asBoolean());
        assertEquals(loggedBody(CONNECTIONS, true), loggedBody(CONNECTIONS, false),
                "the real create sends exactly the body the dry run validated");
    }

    @Test
    @DisplayName("a stored spec that no longer compiles to the bound request executes nothing")
    void bindingDriftStopsTheCreate() throws Exception {
        stubCreateAndDryRun(CONNECTIONS, "/json/fabric/connection_response.json",
                loadFixture("/json/fabric/connection_response.json"));
        String canonical = SpecHash.canonicalize(MAPPER.readTree(CONNECTION_PROPOSAL).get("spec"), MAPPER);
        // The proposal's bound request hash differs from what the stored spec compiles to: drift
        // between proposal and execution (an SDK mapping change, a tampered store).
        PendingChange drifted = store.mint(ChangeType.CONNECTION_CREATE, canonical, SpecHash.sha256Hex(canonical),
                SpecHash.sha256Hex("connection_create\n{\"name\":\"something else\"}"), store.now(), null)
                .change();

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> confirm(drifted.token()));
        assertTrue(e.getMessage().contains("integrity check failed"), e.getMessage());
        assertTrue(e.getMessage().contains("Nothing was executed"), e.getMessage());
        assertTrue(e.getMessage().contains("request_matches_binding=false"), e.getMessage());
        wireMock.verify(0, postRequestedFor(urlPathEqualTo(CONNECTIONS)));
    }

    @Test
    @DisplayName("expiry_days is computed from the proposal's instant, so a later confirm sends the validated body")
    void serviceTokenExpiryStableAcrossPhases() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-12T09:30:15.123Z"));
        store = new ProposalStore(clock, ProposalStore.DEFAULT_TTL, ProposalStore.DEFAULT_CAPACITY);
        tools = BrokerToolFactory.tools(store);
        stubCreateAndDryRun(SERVICE_TOKENS, "/json/fabric/service_token_response.json",
                loadFixture("/json/fabric/service_token_response.json"));

        String token = call("fabric_propose_change", SERVICE_TOKEN_PROPOSAL).get("confirm_token").asText();
        clock.advance(java.time.Duration.ofMinutes(7));
        ObjectNode confirmed = confirm(token);

        assertEquals("executed", confirmed.get("phase").asText(), confirmed.toString());
        com.fasterxml.jackson.databind.JsonNode dryRun = loggedBody(SERVICE_TOKENS, true);
        assertEquals(dryRun, loggedBody(SERVICE_TOKENS, false));
        assertTrue(dryRun.get("expirationDateTime").asText().startsWith("2026-12-12T09:30:15"),
                "30 days after the proposal instant, whole seconds: " + dryRun);
    }

    @Test
    @DisplayName("proposing beyond capacity tells the proposer which proposal was evicted")
    void evictionIsReportedToTheProposer() throws Exception {
        store = new ProposalStore(java.time.Clock.systemUTC(), ProposalStore.DEFAULT_TTL, 1);
        tools = BrokerToolFactory.tools(store);
        stubCreateAndDryRun(CONNECTIONS, "/json/fabric/connection_response.json",
                loadFixture("/json/fabric/connection_response.json"));

        String first = call("fabric_propose_change", CONNECTION_PROPOSAL).get("confirm_token").asText();
        ObjectNode second = call("fabric_propose_change", CONNECTION_PROPOSAL);

        assertEquals(first, second.get("evicted_proposals").get(0).get("confirm_token").asText(), second.toString());
        assertTrue(second.get("eviction_note").asText().contains("can no longer be confirmed"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> confirm(first));
        assertTrue(e.getMessage().contains("was evicted"), e.getMessage());
        wireMock.verify(0, postRequestedFor(urlPathEqualTo(CONNECTIONS)).withQueryParam("dryRun", absent()));
    }

    /** A settable clock. */
    private static final class MutableClock extends java.time.Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        @Override
        public java.time.ZoneId getZone() {
            return java.time.ZoneOffset.UTC;
        }

        @Override
        public java.time.Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(java.time.Duration by) {
            now = now.plus(by);
        }
    }
}
