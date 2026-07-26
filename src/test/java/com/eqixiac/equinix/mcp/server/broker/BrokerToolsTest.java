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

import com.eqixiac.equinix.mcp.server.ServerContext;
import com.eqixiac.equinix.mcp.server.ToolRegistration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Handler-level tests over a context with no Fabric at all: every scenario here must resolve
 * — or fail loudly — <em>before</em> any HTTP could happen. Spec validation runs before the
 * SDK is touched, and every confirm-token error precedes execution.
 */
@DisplayName("Broker handlers — spec validation and the confirm-token state machine, no HTTP")
class BrokerToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A mutable test clock. */
    private static final class SteppingClock extends Clock {
        private Instant now = Instant.parse("2026-07-20T12:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
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

    private static final String NETWORK_SPEC =
            "{\"type\":\"EVPLAN\",\"name\":\"Broker-Net\",\"scope\":\"GLOBAL\","
                    + "\"notification_emails\":[\"ops@example.com\"]}";

    private SteppingClock clock;
    private ProposalStore store;
    private List<ToolRegistration> tools;
    private ServerContext context;

    @BeforeEach
    void setUp() {
        clock = new SteppingClock();
        store = new ProposalStore(clock, ProposalStore.DEFAULT_TTL, ProposalStore.DEFAULT_CAPACITY);
        tools = BrokerToolFactory.tools(store);
        // Deliberately no Fabric: reaching the SDK from any of these scenarios would throw
        // ServerContext's "Fabric is unavailable" IllegalStateException instead of the
        // expected message, failing the assertion.
        context = ServerContext.builder().environment(Map.of()).build();
    }

    private ObjectNode call(String name, String argsJson) throws Exception {
        ToolRegistration tool = tools.stream().filter(t -> t.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("tool not registered: " + name));
        return tool.getHandler().handle(MAPPER.readTree(argsJson), context);
    }

    private IllegalArgumentException proposeFails(String argsJson) {
        return assertThrows(IllegalArgumentException.class,
                () -> call("fabric_propose_change", argsJson));
    }

    private IllegalArgumentException confirmFails(String token) {
        return assertThrows(IllegalArgumentException.class,
                () -> call("fabric_confirm_change", "{\"confirm_token\":\"" + token + "\"}"));
    }

    private PendingChange mintNetworkProposal() {
        String canonical = SpecHash.canonicalize(readTree(NETWORK_SPEC), MAPPER);
        return store.mint(ChangeType.NETWORK_CREATE, canonical, SpecHash.sha256Hex(canonical));
    }

    private static com.fasterxml.jackson.databind.JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ── propose: spec validation, all pre-HTTP ──────────────────────────────

    @Nested
    @DisplayName("fabric_propose_change validation")
    class ProposeValidation {

        @Test
        @DisplayName("an unknown change_type lists the three valid creates")
        void unknownChangeType() {
            IllegalArgumentException e = proposeFails(
                    "{\"change_type\":\"connection_delete\",\"spec\":{\"x\":1}}");
            assertTrue(e.getMessage().contains("connection_create, network_create, service_token_create"),
                    e.getMessage());
            assertTrue(e.getMessage().contains("no update or delete change types"), e.getMessage());
        }

        @Test
        @DisplayName("a missing or empty spec fails before anything else")
        void missingSpec() {
            IllegalArgumentException e = proposeFails("{\"change_type\":\"connection_create\"}");
            assertTrue(e.getMessage().contains("'spec' is required"), e.getMessage());
        }

        @Test
        @DisplayName("unknown spec fields are rejected, never silently dropped")
        void unknownSpecField() {
            IllegalArgumentException e = proposeFails("{\"change_type\":\"network_create\",\"spec\":"
                    + "{\"type\":\"EVPLAN\",\"name\":\"N\",\"scope\":\"GLOBAL\","
                    + "\"notification_emails\":[\"a@b.io\"],\"bandwith\":100}}");
            assertTrue(e.getMessage().contains("unknown field"), e.getMessage());
            assertTrue(e.getMessage().contains("bandwith"), e.getMessage());
        }

        @Test
        @DisplayName("a connection side must identify exactly one endpoint")
        void ambiguousConnectionSide() {
            IllegalArgumentException e = proposeFails("{\"change_type\":\"connection_create\",\"spec\":"
                    + "{\"type\":\"EVPL_VC\",\"name\":\"C\",\"bandwidth_mbps\":1000,"
                    + "\"notification_emails\":[\"a@b.io\"],"
                    + "\"a_side\":{\"port_uuid\":\"p\",\"network_uuid\":\"n\","
                    + "\"link_protocol\":{\"type\":\"dot1q\",\"vlan_tag\":100}},"
                    + "\"z_side\":{\"cloud_router_uuid\":\"cr\"}}}");
            assertTrue(e.getMessage().contains("exactly one endpoint"), e.getMessage());
            assertTrue(e.getMessage().contains("spec.a_side"), e.getMessage());
        }

        @Test
        @DisplayName("dot1q without a vlan_tag fails with the precise path")
        void dot1qNeedsVlan() {
            IllegalArgumentException e = proposeFails("{\"change_type\":\"connection_create\",\"spec\":"
                    + "{\"type\":\"EVPL_VC\",\"name\":\"C\",\"bandwidth_mbps\":1000,"
                    + "\"notification_emails\":[\"a@b.io\"],"
                    + "\"a_side\":{\"port_uuid\":\"p\",\"link_protocol\":{\"type\":\"dot1q\"}},"
                    + "\"z_side\":{\"cloud_router_uuid\":\"cr\"}}}");
            assertTrue(e.getMessage().contains("spec.a_side.link_protocol.vlan_tag"), e.getMessage());
        }

        @Test
        @DisplayName("networks require notification emails")
        void networkNeedsNotifications() {
            IllegalArgumentException e = proposeFails("{\"change_type\":\"network_create\",\"spec\":"
                    + "{\"type\":\"EVPLAN\",\"name\":\"N\",\"scope\":\"GLOBAL\"}}");
            assertTrue(e.getMessage().contains("notification_emails"), e.getMessage());
        }

        @Test
        @DisplayName("no failed proposal ever mints a token")
        void failuresMintNothing() {
            proposeFails("{\"change_type\":\"connection_delete\",\"spec\":{\"x\":1}}");
            proposeFails("{\"change_type\":\"network_create\",\"spec\":{\"type\":\"EVPLAN\"}}");
            assertEquals(0, store.size(), "validation failures must not create pending proposals");
        }

        @Test
        @DisplayName("a valid spec that fails at the SDK boundary still mints nothing")
        void dryRunFailureMintsNothing() {
            // The spec is valid, so compilation reaches the SDK — where this context has no
            // Fabric. The dry run fails, and no token may exist for an unvalidated change.
            assertThrows(IllegalStateException.class, () -> call("fabric_propose_change",
                    "{\"change_type\":\"network_create\",\"spec\":" + NETWORK_SPEC + "}"));
            assertEquals(0, store.size());
        }
    }

    // ── confirm: the token state machine ────────────────────────────────────

    @Nested
    @DisplayName("fabric_confirm_change token handling")
    class ConfirmTokens {

        @Test
        @DisplayName("an unknown token tells the agent to re-propose")
        void unknownToken() {
            IllegalArgumentException e = confirmFails("chg-42-000000000000000000000000");
            assertTrue(e.getMessage().contains("unknown"), e.getMessage());
            assertTrue(e.getMessage().contains("fabric_propose_change"), e.getMessage());
        }

        @Test
        @DisplayName("an expired token names the TTL and tells the agent to re-propose")
        void expiredToken() {
            PendingChange minted = mintNetworkProposal();
            clock.advance(ProposalStore.DEFAULT_TTL.plusSeconds(1));
            IllegalArgumentException e = confirmFails(minted.token());
            assertTrue(e.getMessage().contains("expired"), e.getMessage());
            assertTrue(e.getMessage().contains("10 minutes"), e.getMessage());
            assertTrue(e.getMessage().contains("fabric_propose_change"), e.getMessage());
        }

        @Test
        @DisplayName("tokens are consumed on the ATTEMPT: a failed execution burns the token")
        void consumedOnAttempt() {
            PendingChange minted = mintNetworkProposal();

            // First attempt: token consumption succeeds, then execution fails (no Fabric here).
            assertThrows(IllegalStateException.class,
                    () -> call("fabric_confirm_change", "{\"confirm_token\":\"" + minted.token() + "\"}"));

            // Second attempt: the token is gone — replayed, not retried.
            IllegalArgumentException e = confirmFails(minted.token());
            assertTrue(e.getMessage().contains("already"), e.getMessage());
            assertTrue(e.getMessage().contains("single-use") || e.getMessage().contains("first confirm attempt"),
                    e.getMessage());
        }

        @Test
        @DisplayName("hash drift fails the integrity check before any execution")
        void hashDrift() {
            String canonical = SpecHash.canonicalize(readTree(NETWORK_SPEC), MAPPER);
            PendingChange minted = store.mint(ChangeType.NETWORK_CREATE, canonical,
                    "0000000000000000000000000000000000000000000000000000000000000000");
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> call("fabric_confirm_change", "{\"confirm_token\":\"" + minted.token() + "\"}"));
            assertTrue(e.getMessage().contains("integrity"), e.getMessage());
            assertTrue(e.getMessage().contains("Nothing was executed"), e.getMessage());
        }

        @Test
        @DisplayName("a missing confirm_token is a plain argument error")
        void missingToken() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> call("fabric_confirm_change", "{}"));
            assertTrue(e.getMessage().contains("confirm_token"), e.getMessage());
        }
    }
}
