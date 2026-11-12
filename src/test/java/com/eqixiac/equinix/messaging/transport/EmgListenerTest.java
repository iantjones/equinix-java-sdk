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

import com.eqixiac.equinix.messaging.EmgMessageHandler;
import com.eqixiac.equinix.messaging.IbxNotification;
import com.eqixiac.equinix.messaging.UnknownNotification;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmgListenerTest {

    private static final String IBX = "{\"Task\":{\"Id\":\"t1\",\"Resource\":\"DataCenter.Maintenance\","
            + "\"Body\":{\"ServicerId\":\"5-1\",\"Impact\":\"SERVICE_IMPACTING\"}},\"Signature\":\"s\"}";
    private static final String UNKNOWN = "{\"Task\":{\"Id\":\"t2\",\"Resource\":\"Brand.New\",\"Body\":{}}}";
    private static final String GARBAGE = "this is not json SECRET-PAYLOAD";

    private final FakeReceiver receiver = new FakeReceiver();

    private EmgListener listener(EmgMessageHandler handler) {
        return new EmgListener(receiver, handler, 10, Duration.ofMillis(10), Duration.ofMillis(10),
                Duration.ofSeconds(5));
    }

    @Test
    void completesOnSuccessAbandonsOnHandlerFailureDeadLettersUnparseable() {
        FakeReceiver.FakeDelivery bad = receiver.delivery("m3", GARBAGE);
        receiver.batch(receiver.delivery("m1", IBX), receiver.delivery("m2", UNKNOWN), bad,
                receiver.delivery("m4", IBX.replace("t1", "boom")));
        EmgListener l = listener(new EmgMessageHandler() {
            @Override
            public void onIbxNotification(IbxNotification n) {
                if ("boom".equals(n.id())) {
                    throw new IllegalStateException("downstream failed");
                }
            }
        });

        assertEquals(4, l.pollOnce());

        assertEquals(List.of("complete:m1", "complete:m2", "deadLetter:m3", "abandon:m4"), receiver.settlements);
        assertEquals(EmgListener.DEAD_LETTER_REASON, bad.deadLetterReason);
        assertFalse(bad.deadLetterDescription.contains("SECRET-PAYLOAD"), "dead-letter detail must not echo the body");
    }

    @Test
    void unknownTypesReachTheHandlerAndAreCompleted() {
        receiver.batch(receiver.delivery("m1", UNKNOWN));
        List<String> seen = new java.util.ArrayList<>();
        listener(new EmgMessageHandler() {
            @Override
            public void onUnknownNotification(UnknownNotification n) {
                seen.add(n.header().resource());
            }
        }).pollOnce();
        assertEquals(List.of("Brand.New"), seen);
        assertEquals(List.of("complete:m1"), receiver.settlements);
    }

    @Test
    void checkedExceptionFromHandlerAbandons() {
        receiver.batch(receiver.delivery("m1", IBX));
        listener(new EmgMessageHandler() {
            @Override
            public void onIbxNotification(IbxNotification n) throws Exception {
                throw new java.io.IOException("db down");
            }
        }).pollOnce();
        assertEquals(List.of("abandon:m1"), receiver.settlements);
    }

    @Test
    void settlementFailureDoesNotStopTheBatch() {
        receiver.batch(receiver.delivery("m1", IBX).failingSettlement(), receiver.delivery("m2", IBX));
        assertEquals(2, listener(new EmgMessageHandler() { }).pollOnce());
        assertEquals(List.of("complete:m2"), receiver.settlements);
    }

    @Test
    void closeDuringABatchAbandonsTheRestAndClosesTheReceiver() {
        EmgListener[] self = new EmgListener[1];
        receiver.batch(receiver.delivery("m1", IBX), receiver.delivery("m2", IBX), receiver.delivery("m3", IBX));
        self[0] = listener(new EmgMessageHandler() {
            @Override
            public void onIbxNotification(IbxNotification n) {
                self[0].close();   // e.g. a shutdown signal arriving while m1 is in flight
            }
        });
        self[0].pollOnce();
        assertEquals(List.of("complete:m1", "abandon:m2", "abandon:m3"), receiver.settlements);
        assertTrue(receiver.closed);
        assertEquals(0, self[0].pollOnce(), "a closed listener receives nothing");
    }

    @Test
    void runLoopsUntilClosedAndRetriesAfterReceiveErrors() throws Exception {
        CountDownLatch handled = new CountDownLatch(2);
        receiver.batch(receiver.delivery("m1", IBX))
                .failNextReceive(new RuntimeException("transient AMQP error"))
                .batch(receiver.delivery("m2", IBX));
        EmgListener l = listener(new EmgMessageHandler() {
            @Override
            public void onIbxNotification(IbxNotification n) {
                handled.countDown();
            }
        });
        Thread t = new Thread(l::run, "emg-listener-test");
        t.start();
        assertTrue(handled.await(5, TimeUnit.SECONDS), "both messages handled despite the receive error");
        l.close();
        t.join(5000);
        assertFalse(t.isAlive(), "run() returns after close()");
        assertTrue(receiver.closed);
        assertEquals(List.of("complete:m1", "complete:m2"), receiver.settlements);
        assertTrue(receiver.receiveCalls.get() >= 3);
    }

    @Test
    void closeWaitsForTheInFlightMessageToBeSettled() throws Exception {
        CountDownLatch inHandler = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        receiver.batch(receiver.delivery("m1", IBX));
        EmgListener l = listener(new EmgMessageHandler() {
            @Override
            public void onIbxNotification(IbxNotification n) throws InterruptedException {
                inHandler.countDown();
                release.await();
            }
        });
        Thread runner = new Thread(l::run);
        runner.start();
        assertTrue(inHandler.await(5, TimeUnit.SECONDS));
        Thread closer = new Thread(l::close);
        closer.start();
        closer.join(200);
        assertTrue(closer.isAlive(), "close() blocks while a message is in flight");
        assertFalse(receiver.closed);
        release.countDown();
        closer.join(5000);
        runner.join(5000);
        assertEquals(List.of("complete:m1"), receiver.settlements);
        assertTrue(receiver.closed);
    }

    @Test
    void closeFromAHandlerOnTheLoopThreadDoesNotDeadlock() throws Exception {
        EmgListener[] self = new EmgListener[1];
        receiver.batch(receiver.delivery("m1", IBX), receiver.delivery("m2", IBX));
        self[0] = new EmgListener(receiver, new EmgMessageHandler() {
            @Override
            public void onIbxNotification(IbxNotification n) {
                self[0].close();
            }
        }, 10, Duration.ofMillis(10), Duration.ofMillis(10), Duration.ofSeconds(30));
        Thread t = new Thread(self[0]::run);
        t.start();
        t.join(5000);
        assertFalse(t.isAlive(), "run() must exit promptly, not wait out the 30 s close timeout");
        assertEquals(List.of("complete:m1", "abandon:m2"), receiver.settlements);
        assertTrue(receiver.closed);
    }

    @Test
    void runIsSingleUse() {
        EmgListener l = listener(new EmgMessageHandler() { });
        l.close();
        l.close();   // idempotent
        assertThrows(IllegalStateException.class, l::run);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new EmgListener(receiver, new EmgMessageHandler() { },
                0, Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(NullPointerException.class, () -> new EmgListener(null, new EmgMessageHandler() { }));
        assertThrows(NullPointerException.class, () -> new EmgListener(receiver, null));
    }
}
