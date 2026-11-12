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

package com.eqixiac.equinix.messaging.transport;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** In-memory {@link EmgReceiver}: hands out queued batches and records settlements. No network. */
final class FakeReceiver implements EmgReceiver {

    final Deque<Supplier<List<EmgDelivery>>> batches = new ArrayDeque<>();
    final List<String> settlements = new ArrayList<>();
    final AtomicInteger receiveCalls = new AtomicInteger();
    volatile boolean closed;

    FakeReceiver batch(FakeDelivery... deliveries) {
        List<EmgDelivery> list = List.of(deliveries);
        batches.add(() -> list);
        return this;
    }

    FakeReceiver failNextReceive(RuntimeException e) {
        batches.add(() -> {
            throw e;
        });
        return this;
    }

    FakeDelivery delivery(String id, String body) {
        return new FakeDelivery(id, body.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public synchronized List<EmgDelivery> receive(int maxMessages, Duration maxWait) {
        receiveCalls.incrementAndGet();
        if (closed) {
            throw new IllegalStateException("receiver closed");
        }
        Supplier<List<EmgDelivery>> next = batches.poll();
        return next == null ? List.of() : next.get();
    }

    @Override
    public void close() {
        closed = true;
    }

    /** A delivery that records its settlement into the receiver's log. */
    final class FakeDelivery implements EmgDelivery {
        final String id;
        final byte[] body;
        RuntimeException settleFailure;
        String deadLetterReason;
        String deadLetterDescription;

        FakeDelivery(String id, byte[] body) {
            this.id = id;
            this.body = body;
        }

        FakeDelivery failingSettlement() {
            settleFailure = new IllegalStateException("lock lost");
            return this;
        }

        private void settle(String what) {
            if (settleFailure != null) {
                throw settleFailure;
            }
            synchronized (settlements) {
                settlements.add(what + ":" + id);
            }
        }

        @Override public byte[] body() { return body.clone(); }
        @Override public String messageId() { return id; }
        @Override public long deliveryCount() { return 1; }
        @Override public void complete() { settle("complete"); }
        @Override public void abandon() { settle("abandon"); }

        @Override
        public void deadLetter(String reason, String description) {
            deadLetterReason = reason;
            deadLetterDescription = description;
            settle("deadLetter");
        }
    }
}
