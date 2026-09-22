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

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.mcp.server.broker.BrokerToolFactory;
import com.eqixiac.equinix.mcp.server.broker.ProposalStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.eqixiac.equinix.core.ResponseStubs.stubCreate;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire proofs for the human-confirmation step of {@code fabric_confirm_change}. Each scenario binds
 * a stub client exchange for the confirm call, then counts real creates on the wire: a real
 * {@code POST /fabric/v4/connections} (no {@code dryRun} parameter) may exist only after an accepted
 * prompt, or when the client cannot be prompted at all.
 *
 * <p>The class lives in {@code mcp.server}, not {@code mcp.server.broker}, because binding an
 * exchange ({@code ServerContext.withExchange}) and the exchange test doubles are package-private
 * to it. The broker is reached only through its public seam.</p>
 */
@DisplayName("Safe Mutation Broker — human confirmation at confirm time (wire proofs)")
class BrokerHumanConfirmWireMockTest extends WireMockTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CONNECTIONS = "/fabric/v4/connections";

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
                          "link_protocol": {"type": "dot1q", "vlan_tag": 1002}}}}""";

    /** A mutable clock, so the prompt's proposal age is an exact figure. */
    private static final class SteppingClock extends Clock {
        private Instant now = Instant.parse("2026-09-21T12:00:00Z");

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
            return now;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }
    }

    static Fabric fabric;

    private SteppingClock clock;
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
        // Real-create stub first, dryRun=true stub second: most-recent-match-wins routes them.
        stubCreate(wireMock, CONNECTIONS, "/json/fabric/connection_response.json");
        wireMock.stubFor(post(urlPathEqualTo(CONNECTIONS))
                .withQueryParam("dryRun", equalTo("true"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("/json/fabric/connection_response.json"))));
        clock = new SteppingClock();
        tools = BrokerToolFactory.tools(new ProposalStore(clock, ProposalStore.DEFAULT_TTL,
                ProposalStore.DEFAULT_CAPACITY));
        context = ServerContext.builder().fabric(fabric).environment(Map.of()).build();
    }

    private ToolRegistration tool(String name) {
        return tools.stream().filter(t -> t.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("tool not registered: " + name));
    }

    private String propose() throws Exception {
        return tool("fabric_propose_change").getHandler()
                .handle(MAPPER.readTree(CONNECTION_PROPOSAL), context).get("confirm_token").asText();
    }

    private ObjectNode confirm(ServerContext ctx, McpSyncServerExchange exchange, String token) throws Exception {
        JsonNode args = MAPPER.readTree("{\"confirm_token\":\"" + token + "\"}");
        return ctx.withExchange(exchange, () -> tool("fabric_confirm_change").getHandler().handle(args, ctx));
    }

    private static void verifyRealCreates(int expected) {
        wireMock.verify(expected, postRequestedFor(urlPathEqualTo(CONNECTIONS))
                .withQueryParam("dryRun", absent()));
    }

    private static void assertAllChecksPassed(ObjectNode payload) {
        JsonNode checks = payload.get("confirm_checks");
        assertNotNull(checks, "every confirm result echoes the redemption predicates: " + payload);
        for (String predicate : List.of("proposal_exists", "not_expired", "not_previously_used",
                "spec_matches_binding")) {
            assertTrue(checks.get(predicate).asBoolean(), predicate + " in " + checks);
        }
    }

    private void assertNotExecuted(ObjectNode payload, String status, String token) {
        assertEquals("not_executed", payload.get("phase").asText());
        assertFalse(payload.get("executed").asBoolean());
        assertFalse(payload.has("result"), "no entity exists, so none is reported: " + payload);
        assertEquals(status, payload.get("human_confirmation").get("status").asText());
        assertFalse(payload.get("human_confirmation").get("detail").asText().isBlank());
        assertAllChecksPassed(payload);
        assertEquals("consumed", payload.get("token_state").asText(),
                "the single-use rule holds: the attempt consumed the token");
        assertTrue(payload.get("next_step").asText().contains("fabric_propose_change"));
        verifyRealCreates(0);

        // The token is gone: a second confirm is a replay and still executes nothing.
        IllegalArgumentException replay = assertThrows(IllegalArgumentException.class,
                () -> confirm(context, StubExchanges.confirms(true), token));
        assertTrue(replay.getMessage().contains("already"), replay.getMessage());
        assertTrue(replay.getMessage().contains("not_previously_used=false"), replay.getMessage());
        verifyRealCreates(0);
    }

    @Test
    @DisplayName("ACCEPT with confirm=true executes the real create exactly once and reports accepted")
    void acceptExecutes() throws Exception {
        String token = propose();
        ObjectNode payload = confirm(context, StubExchanges.confirms(true), token);

        assertEquals("executed", payload.get("phase").asText());
        assertTrue(payload.get("executed").asBoolean());
        assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", payload.get("result").get("uuid").asText());
        assertEquals("accepted", payload.get("human_confirmation").get("status").asText());
        assertAllChecksPassed(payload);
        assertEquals("consumed", payload.get("token_state").asText());
        verifyRealCreates(1);
    }

    @Test
    @DisplayName("the prompt is sent before any real create and shows type, target, price context, age and binding")
    void promptShowsTheChangeBeforeExecution() throws Exception {
        String token = propose();
        clock.advance(Duration.ofSeconds(42));

        AtomicReference<McpSchema.ElicitRequest> seen = new AtomicReference<>();
        McpSyncServerExchange exchange = StubExchanges.stub(StubExchanges.elicitationCapable(), request -> {
            seen.set(request);
            // At prompt time nothing has been created: the only POST so far is the dry run.
            verifyRealCreates(0);
            return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.ACCEPT,
                    Map.of(ElicitationSupport.CONFIRM_FIELD, true));
        });
        ObjectNode payload = confirm(context, exchange, token);

        String prompt = seen.get().message();
        assertTrue(prompt.contains("Change type: connection_create"), prompt);
        assertTrue(prompt.contains("EVPL_VC connection 'Broker-EVPL', 1000 Mbps"), prompt);
        assertTrue(prompt.contains("A-side port_uuid=c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee"), prompt);
        assertTrue(prompt.contains("Z-side service_profile_uuid=20d32a80-0d61-4333-bc03-4b2d446794a0"), prompt);
        // Live Equinix pricing is unstubbed, so the layered chain reports the bundled reference figure.
        assertTrue(prompt.contains("Price context: USD ") && prompt.contains("(REFERENCE)"), prompt);
        assertTrue(prompt.contains("not a quote"), prompt);
        assertTrue(prompt.contains("Proposal age: 42 s (proposals expire 600 s after the dry run)"), prompt);
        assertTrue(prompt.contains("Spec SHA-256: " + payload.get("spec_sha256").asText()), prompt);
        verifyRealCreates(1);
    }

    @Test
    @DisplayName("DECLINE executes nothing; the token stays consumed")
    void declineExecutesNothing() throws Exception {
        String token = propose();
        assertNotExecuted(confirm(context, StubExchanges.declines(), token), "declined", token);
    }

    @Test
    @DisplayName("CANCEL executes nothing; the token stays consumed")
    void cancelExecutesNothing() throws Exception {
        String token = propose();
        assertNotExecuted(confirm(context, StubExchanges.cancels(), token), "cancelled", token);
    }

    @Test
    @DisplayName("ACCEPT without confirm=true is not an approval and executes nothing")
    void acceptWithoutConfirmTrueExecutesNothing() throws Exception {
        String token = propose();
        assertNotExecuted(confirm(context, StubExchanges.confirms(false), token), "declined", token);
    }

    @Test
    @Timeout(20)
    @DisplayName("an unanswered prompt times out, executes nothing, and the token stays consumed")
    void timeoutExecutesNothing() throws Exception {
        ServerContext quick = ServerContext.builder().fabric(fabric)
                .environment(Map.of(ServerContext.ENV_ELICIT_TIMEOUT_MS, "200")).build();
        McpSyncServerExchange stalled = StubExchanges.stub(StubExchanges.elicitationCapable(), request -> {
            try {
                Thread.sleep(60_000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.ACCEPT,
                    Map.of(ElicitationSupport.CONFIRM_FIELD, true));
        });
        String token = propose();
        assertNotExecuted(confirm(quick, stalled, token), "timed_out", token);
    }

    @Test
    @DisplayName("a failed elicitation round trip fails closed: nothing is executed")
    void transportFailureExecutesNothing() throws Exception {
        McpSyncServerExchange broken = StubExchanges.stub(StubExchanges.elicitationCapable(), request -> {
            throw new IllegalStateException("connection reset");
        });
        String token = propose();
        ObjectNode payload = confirm(context, broken, token);
        assertTrue(payload.get("human_confirmation").get("detail").asText().contains("connection reset"),
                payload.toString());
        assertNotExecuted(payload, "failed", token);
    }

    @Test
    @DisplayName("a client without elicitation keeps the earlier flow and the result says no human check ran")
    void unsupportedClientExecutesAndSaysSo() throws Exception {
        // StubExchanges.unsupported() fails the test if a prompt is ever sent.
        ObjectNode declaredNothing = confirm(context, StubExchanges.unsupported(), propose());
        assertEquals("executed", declaredNothing.get("phase").asText());
        assertTrue(declaredNothing.get("executed").asBoolean());
        JsonNode confirmation = declaredNothing.get("human_confirmation");
        assertEquals("unsupported_by_client", confirmation.get("status").asText());
        assertTrue(confirmation.get("detail").asText().contains("without a server-side check"),
                confirmation.toString());
        assertAllChecksPassed(declaredNothing);
        verifyRealCreates(1);

        // No exchange bound at all (a direct handler call) is the same case.
        ObjectNode noExchange = confirm(context, null, propose());
        assertEquals("unsupported_by_client", noExchange.get("human_confirmation").get("status").asText());
        verifyRealCreates(2);
    }
}
