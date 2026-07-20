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

package api.equinix.javasdk.mcp.server.broker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProposalStore — the confirm-token state machine (mint/consume/expire/replay)")
class ProposalStoreTest {

    private static final String SPEC = "{\"name\":\"X\"}";
    private static final String SHA = SpecHash.sha256Hex(SPEC);

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

    @Test
    @DisplayName("mint → consume returns the exact pending change, exactly once")
    void mintAndConsumeOnce() {
        ProposalStore store = new ProposalStore();
        PendingChange minted = store.mint(ChangeType.CONNECTION_CREATE, SPEC, SHA);

        assertTrue(minted.token().startsWith("chg-"), minted.token());
        assertEquals(1, store.size());

        ProposalStore.Consumption first = store.consume(minted.token());
        assertEquals(ProposalStore.Outcome.CONSUMED, first.outcome());
        assertEquals(ChangeType.CONNECTION_CREATE, first.change().changeType());
        assertEquals(SPEC, first.change().canonicalSpec());
        assertEquals(SHA, first.change().specSha256());
        assertEquals(0, store.size(), "consumption removes the proposal");
    }

    @Test
    @DisplayName("a second consume of the same token is REPLAYED — consumed on attempt, forever")
    void replayDetected() {
        ProposalStore store = new ProposalStore();
        PendingChange minted = store.mint(ChangeType.NETWORK_CREATE, SPEC, SHA);

        assertEquals(ProposalStore.Outcome.CONSUMED, store.consume(minted.token()).outcome());
        ProposalStore.Consumption replay = store.consume(minted.token());
        assertEquals(ProposalStore.Outcome.REPLAYED, replay.outcome());
        assertNull(replay.change(), "a replayed token never yields the change again");
    }

    @Test
    @DisplayName("an unconfirmed token is EXPIRED after the TTL, then eventually UNKNOWN")
    void expiry() {
        SteppingClock clock = new SteppingClock();
        ProposalStore store = new ProposalStore(clock, Duration.ofMinutes(10), 20);
        PendingChange minted = store.mint(ChangeType.SERVICE_TOKEN_CREATE, SPEC, SHA);

        clock.advance(Duration.ofMinutes(9));
        assertEquals(1, store.size(), "still live just before the TTL");

        clock.advance(Duration.ofMinutes(2));
        ProposalStore.Consumption late = store.consume(minted.token());
        assertEquals(ProposalStore.Outcome.EXPIRED, late.outcome());
        assertNull(late.change());

        // The expired tombstone is remembered for one further TTL window, then forgotten.
        clock.advance(Duration.ofMinutes(11));
        assertEquals(ProposalStore.Outcome.UNKNOWN, store.consume(minted.token()).outcome());
    }

    @Test
    @DisplayName("a token never minted here is UNKNOWN")
    void unknownToken() {
        ProposalStore store = new ProposalStore();
        assertEquals(ProposalStore.Outcome.UNKNOWN, store.consume("chg-999-doesnotexist").outcome());
    }

    @Test
    @DisplayName("capacity eviction drops the oldest pending proposal")
    void capacityEviction() {
        SteppingClock clock = new SteppingClock();
        ProposalStore store = new ProposalStore(clock, Duration.ofMinutes(10), 2);

        PendingChange first = store.mint(ChangeType.CONNECTION_CREATE, SPEC, SHA);
        PendingChange second = store.mint(ChangeType.CONNECTION_CREATE, SPEC, SHA);
        PendingChange third = store.mint(ChangeType.CONNECTION_CREATE, SPEC, SHA);

        assertEquals(2, store.size());
        assertEquals(ProposalStore.Outcome.UNKNOWN, store.consume(first.token()).outcome(),
                "the evicted proposal is gone entirely");
        assertEquals(ProposalStore.Outcome.CONSUMED, store.consume(second.token()).outcome());
        assertEquals(ProposalStore.Outcome.CONSUMED, store.consume(third.token()).outcome());
    }

    @Test
    @DisplayName("tokens are unique and unguessable-shaped; configuration is reported")
    void tokensAndConfiguration() {
        ProposalStore store = new ProposalStore();
        PendingChange a = store.mint(ChangeType.CONNECTION_CREATE, SPEC, SHA);
        PendingChange b = store.mint(ChangeType.CONNECTION_CREATE, SPEC, SHA);
        assertNotEquals(a.token(), b.token(), "each mint yields a fresh token");
        assertTrue(a.token().matches("^chg-\\d+-[0-9a-f]{24}$"), a.token());
        assertEquals(ProposalStore.DEFAULT_TTL, store.ttl());
        assertEquals(ProposalStore.DEFAULT_CAPACITY, store.capacity());
    }
}
