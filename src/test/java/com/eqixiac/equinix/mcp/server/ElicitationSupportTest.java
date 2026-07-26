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

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ElicitationSupport — chooseOne over a stub client exchange")
class ElicitationSupportTest {

    private static final List<ElicitationSupport.Option> OPTIONS = List.of(
            new ElicitationSupport.Option("a", "Account A", "111"),
            new ElicitationSupport.Option("b", "Account B", "222"));

    @Test
    @DisplayName("a null exchange is UNSUPPORTED so the caller can fall back")
    void nullExchangeUnsupported() {
        ElicitationSupport.Outcome outcome = ElicitationSupport.chooseOne(null, "Pick one", OPTIONS, 1_000);
        assertEquals(ElicitationSupport.Status.UNSUPPORTED, outcome.status());
        assertFalse(outcome.picked());
        assertNull(outcome.option());
    }

    @Test
    @DisplayName("a client that did not declare elicitation is UNSUPPORTED and is never prompted")
    void noCapabilityUnsupported() {
        ElicitationSupport.Outcome outcome = ElicitationSupport.chooseOne(
                StubExchanges.unsupported(), "Pick one", OPTIONS, 1_000);
        assertEquals(ElicitationSupport.Status.UNSUPPORTED, outcome.status());
    }

    @Test
    @DisplayName("ACCEPT with a matching choice returns PICKED with that option")
    void acceptPicks() {
        ElicitationSupport.Outcome outcome = ElicitationSupport.chooseOne(
                StubExchanges.accepts("b"), "Pick one", OPTIONS, 1_000);
        assertTrue(outcome.picked());
        assertEquals("b", outcome.option().id());
        assertEquals("Account B", outcome.option().label());
    }

    @Test
    @DisplayName("ACCEPT with an unmatched choice degrades to DECLINED (nothing forced)")
    void acceptUnmatchedDeclines() {
        ElicitationSupport.Outcome outcome = ElicitationSupport.chooseOne(
                StubExchanges.accepts("zzz"), "Pick one", OPTIONS, 1_000);
        assertEquals(ElicitationSupport.Status.DECLINED, outcome.status());
        assertNull(outcome.option());
    }

    @Test
    @DisplayName("DECLINE returns DECLINED so the caller keeps its default")
    void declineDeclines() {
        ElicitationSupport.Outcome outcome = ElicitationSupport.chooseOne(
                StubExchanges.declines(), "Pick one", OPTIONS, 1_000);
        assertEquals(ElicitationSupport.Status.DECLINED, outcome.status());
    }

    @Test
    @DisplayName("CANCEL returns DECLINED")
    void cancelDeclines() {
        McpSyncServerExchange exchange = StubExchanges.stub(StubExchanges.elicitationCapable(),
                request -> new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.CANCEL, Map.of()));
        ElicitationSupport.Outcome outcome = ElicitationSupport.chooseOne(exchange, "Pick one", OPTIONS, 1_000);
        assertEquals(ElicitationSupport.Status.DECLINED, outcome.status());
    }

    @Test
    @DisplayName("a thrown transport error is FAILED, not a leaked exception")
    void transportErrorFails() {
        McpSyncServerExchange exchange = StubExchanges.stub(StubExchanges.elicitationCapable(),
                request -> {
                    throw new IllegalStateException("connection reset");
                });
        ElicitationSupport.Outcome outcome = ElicitationSupport.chooseOne(exchange, "Pick one", OPTIONS, 1_000);
        assertEquals(ElicitationSupport.Status.FAILED, outcome.status());
        assertTrue(outcome.detail().contains("connection reset"), outcome.detail());
    }

    @Test
    @Timeout(10)
    @DisplayName("a client that never answers is bounded by the timeout and returns FAILED (never blocks forever)")
    void stalledClientTimesOut() {
        McpSyncServerExchange exchange = StubExchanges.stub(StubExchanges.elicitationCapable(),
                request -> {
                    try {
                        Thread.sleep(60_000);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.ACCEPT, Map.of());
                });
        long start = System.nanoTime();
        ElicitationSupport.Outcome outcome = ElicitationSupport.chooseOne(exchange, "Pick one", OPTIONS, 200);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertEquals(ElicitationSupport.Status.FAILED, outcome.status());
        assertTrue(elapsedMs < 5_000, "the hard timeout bounds the wait: " + elapsedMs + " ms");
    }

    @Test
    @DisplayName("supportsForm reads the negotiated client capability off the exchange")
    void supportsFormDetection() {
        assertFalse(ElicitationSupport.supportsForm(null));
        assertFalse(ElicitationSupport.supportsForm(StubExchanges.unsupported()));
        assertTrue(ElicitationSupport.supportsForm(StubExchanges.accepts("a")));
    }
}
